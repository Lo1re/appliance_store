package com.epam.rd.autocode.assessment.appliances.security.jwt.refresh;

import com.epam.rd.autocode.assessment.appliances.logging.SafeFailure;
import com.epam.rd.autocode.assessment.appliances.model.RefreshToken;
import com.epam.rd.autocode.assessment.appliances.model.User;
import com.epam.rd.autocode.assessment.appliances.repository.RefreshTokenRepository;
import com.epam.rd.autocode.assessment.appliances.repository.UserRepository;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountPrincipal;
import com.epam.rd.autocode.assessment.appliances.security.identity.AccountRole;
import com.epam.rd.autocode.assessment.appliances.security.jwt.TokenPair;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenIssuer;
import com.epam.rd.autocode.assessment.appliances.security.jwt.access.AccessTokenPrincipal;
import com.epam.rd.autocode.assessment.appliances.security.jwt.config.ConditionalOnJwt;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/** Serializes credential lifecycle on the user row; HTTP and cookie concerns live elsewhere. */
@Slf4j
@Component
@ConditionalOnJwt
public class RefreshTokenService {
    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final RefreshTokenCodec codec;
    private final RefreshTokenProperties properties;
    private final AccessTokenIssuer access;
    private final Clock clock;
    private final TransactionTemplate transactions;

    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, RefreshTokenCodec codec,
            RefreshTokenProperties properties, AccessTokenIssuer access, Clock clock, PlatformTransactionManager manager) {
        this.tokens = tokens; this.users = users; this.codec = codec; this.properties = properties; this.access = access; this.clock = clock;
        transactions = new TransactionTemplate(manager);
        transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // Owner lookup may happen before waiting for the user lock; subsequent reads need a fresh snapshot.
        transactions.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public TokenPair issue(Authentication authentication) { return issue(authentication, null); }

    public TokenPair issue(Authentication authentication, String previousRefresh) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AccountPrincipal principal) || principal.getAccountId() == null)
            throw new InvalidRefreshTokenException();
        String previousHash;
        try { previousHash = codec.hash(previousRefresh); } catch (InvalidRefreshTokenException ignored) { previousHash = null; }
        final String prior = previousHash;
        TokenPair pair = inTransaction(status -> {
            var user = lockLoginUsers(principal.getAccountId(), prior);
            var role = AccountRole.from(user).orElseThrow(InvalidRefreshTokenException::new);
            var supported = authentication.getAuthorities().stream().map(authority -> authority.getAuthority())
                    .filter(authority -> authority.equals(AccountRole.CLIENT.authority()) || authority.equals(AccountRole.EMPLOYEE.authority())).toList();
            if (supported.size() != 1 || !supported.get(0).equals(role.authority())) {
                throw new InvalidRefreshTokenException();
            }
            if (prior != null) revokeChain(tokens.findByTokenHash(prior).orElse(null), clock.instant());
            var created = create(user, clock.instant());
            return created.pair();
        });
        log.info("Refresh access issued (accountId={})", principal.getAccountId());
        return pair;
    }

    public TokenPair rotate(String raw) {
        final String hash;
        try { hash = codec.hash(raw); }
        catch (InvalidRefreshTokenException invalid) { log.warn("Refresh credential rejected"); throw invalid; }
        Rotation result = inTransaction(status -> {
            Long owner = tokens.findOwnerByHash(hash).orElse(null);
            if (owner == null || users.lockIdentity(owner).isEmpty()) {
                return null;
            }
            var token = tokens.findByTokenHash(hash).orElse(null);
            if (token == null) {
                return null;
            }
            Instant now = clock.instant();
            if (token.getRevokedAt() != null) {
                int count = tokens.revokeAll(owner, now);
                return new Rotation(null, owner, count); // Return, commit, then report the authentication failure.
            }
            if (token.expiredAt(now)) {
                return null;
            }
            var user = users.findById(owner).orElse(null);
            if (user == null || AccountRole.from(user).isEmpty()) {
                return null;
            }
            var next = create(user, now);
            token.replace(now, next.id());
            return new Rotation(next.pair(), owner, 0);
        });
        if (result == null) { log.warn("Refresh access rejected"); throw new InvalidRefreshTokenException(); }
        if (result.pair() == null) {
            log.warn("Refresh reuse rejected; account refresh access revoked (accountId={}, revokedCount={})", result.owner(), result.revoked());
            throw new InvalidRefreshTokenException();
        }
        log.info("Refresh access rotated (accountId={})", result.owner());
        return result.pair();
    }

    public void revoke(String raw) {
        final String hash;
        try { hash = codec.hash(raw); } catch (InvalidRefreshTokenException invalid) { return; }
        Long revokedOwner = inTransaction(status -> {
            Long owner = tokens.findOwnerByHash(hash).orElse(null);
            if (owner == null || users.lockIdentity(owner).isEmpty()) {
                return null;
            }
            revokeChain(tokens.findByTokenHash(hash).orElse(null), clock.instant());
            return owner;
        });
        if (revokedOwner != null) log.info("Refresh access revoked (accountId={})", revokedOwner);
    }

    public int cleanup() {
        Instant cutoff = clock.instant().minus(properties.retention());
        var owners = inTransaction(status -> tokens.findExpiredOwners(cutoff, PageRequest.of(0, 100)));
        int removed = 0;
        for (Long id : owners) {
            removed += inTransaction(status -> users.lockIdentity(id).isPresent() ? tokens.deleteExpired(id, cutoff) : 0);
        }
        return removed;
    }

    private User lockLoginUsers(Long currentId, String previousHash) {
        var ids = new TreeSet<Long>(); ids.add(currentId);
        if (previousHash != null) tokens.findOwnerByHash(previousHash).ifPresent(ids::add);
        // Switching browser accounts can touch two owners: acquire user rows in one stable order.
        for (Long id : ids) if (users.lockIdentity(id).isEmpty() && id.equals(currentId))
            throw new InvalidRefreshTokenException();
        return users.findById(currentId).orElseThrow(InvalidRefreshTokenException::new);
    }

    private void revokeChain(RefreshToken token, Instant now) {
        if (token == null) return;
        Long owner = token.getUserId(); var visited = new HashSet<Long>();
        while (token != null && owner.equals(token.getUserId()) && visited.add(token.getId())) {
            token.revoke(now);
            // A refresh that won the lock before logout must not leave its successor alive.
            token = token.getReplacedBy() == null ? null : tokens.findById(token.getReplacedBy()).orElse(null);
        }
    }

    private Created create(User user, Instant now) {
        var role = AccountRole.from(user).orElseThrow(InvalidRefreshTokenException::new);
        String raw = codec.generate(); Instant expiry = now.plus(properties.ttl());
        var token = tokens.saveAndFlush(new RefreshToken(user.getId(), codec.hash(raw), expiry));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(new AccessTokenPrincipal(user.getId()), null,
                List.of(new SimpleGrantedAuthority(role.authority())));
        return new Created(token.getId(), new TokenPair(access.issue(authentication), raw, expiry));
    }
    private <T> T inTransaction(TransactionCallback<T> operation) {
        try { return transactions.execute(operation); }
        catch (DataAccessException | TransactionException failure) {
            log.error("Refresh storage operation failed", SafeFailure.snapshot(failure));
            throw new AuthenticationServiceException("Authentication temporarily unavailable", SafeFailure.snapshot(failure));
        }
    }
    private record Rotation(TokenPair pair, Long owner, int revoked) { }
    private record Created(Long id, TokenPair pair) { }
}
