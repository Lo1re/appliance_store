package com.epam.rd.autocode.assessment.appliances.repository.password;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PasswordResetRepository {
    private final JdbcTemplate jdbc;
    public Optional<Long> owner(String hash) {
        return jdbc.query("select user_id from password_reset_token where token_hash = ?", (rs, row) -> rs.getLong(1), hash).stream().findFirst();
    }
    public void replace(Long userId, String hash, Instant expires) {
        jdbc.update("delete from password_reset_token where user_id = ?", userId);
        jdbc.update("insert into password_reset_token(user_id, token_hash, expires_at) values (?, ?, ?)",
                userId, hash, java.sql.Timestamp.from(expires));
    }
    public boolean consume(Long userId, String hash, Instant now) {
        return jdbc.update("delete from password_reset_token where user_id = ? and token_hash = ? and expires_at > ?",
                userId, hash, java.sql.Timestamp.from(now)) == 1;
    }
    public void deleteAll(Long userId) {
        jdbc.update("delete from password_reset_token where user_id = ?", userId);
    }
    public int deleteExpired(Instant now) {
        return jdbc.update("delete from password_reset_token where expires_at <= ?", java.sql.Timestamp.from(now));
    }
}
