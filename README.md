# Appliance store test(Spring Boot)

> The original assignment below is retained for reference. Current database, mapper and launch instructions are in **Current foundation setup**. Do not apply the historical spring.sql.init instructions to the current implementation.

The purpose of this exercise is to check your knowledge and understanding Spring Boot technology.

Estimated workload of this exercise is **15 hrs**.

## Description

In this exercise, you will implement an "Appliances shop" using Spring Boot.
Your application has to imitate work of an online shop.

Employees should be able to do the following:
1. show every table;
2. add, correct, and delete `Employee`, `Client`, `Appliance`, `Manufacturer`;
3. approve orders.

Clients and employees should be able to do the following:
1. have navigation and provide access for permitted elements front-end;
2. add, correct, and delete orders.

Users must be able to work in two languages(native and English). 

The class diagram of the Domain model is shown in the figure below:
![img.png](img.png)

Your application must implement next layers:
* Repository - work with necessary entities (JPA);
* Service - contains business logic your application;
* Controller - is front end your application. 

### Requirements:
* Use a skeleton application;
* Use H2 database to realisation storage;
* Use annotations to describe DB entities. List of entities:
  * `User`;
  * `Client`;
  * `Employee`;
  * `Manufacturer`;
  * `Appliance`;
  * `Order`;
  * `OrderRow`.
* Use Spring JPA technology;
* Use Spring Security;
* Implement internationalization and localization in controllers;
* Implement validation for the Domain model and controllers.

### Would be nice
* to use logging;
* to implement pagination and sorting in controllers;
* to explore other Spring technologies;
* to write tests for controllers and services.

### Recommendations
* Use `Lombok`
* Implement In-Memory Authentication(Spring Security).
* You have files with initial data, but you can use other data.
  To load initial data, use
  `spring.sql.init.data-locations=classpath:manufacturer.sql, classpath:client.sql, classpath:employee.sql, classpath:appliance.sql`
  in `application.properties`.
* Files with initial data located `resources`. The list of files includes `manufacturer.sql`, `client.sql`, `employee.sql`, `appliance.sql`.
* Additionally, you may to using files to frond-end from `resources\templates\**`, or use yourself front-end.  
* If you do not have enough time to implement all the requirements, you can choose to implement only part of the functionality. For example, you can focus on add, delete, and update for employees and not implement them for clients.

Good luck.

## Current foundation setup


### Package organization

The existing layers are retained. Inside them, classes are grouped by responsibility:

```text
controller/
  account/          Client and employee HTML controllers
  auth/             Login and renewal HTML controllers
  catalog/          Appliance and manufacturer HTML controllers
  order/            Order controller and its package-private filter
  advice/           Shared MVC model/error advice and section annotation
api/
  controller/       JSON endpoints
  error/            Safe localized JSON errors
service/
  impl/account/     Account implementations and their internal credential helper
  impl/catalog/     Catalog implementations
  impl/order/       Order implementation and row factory
  lookup/           Bounded form lookup service
  policy/           Domain operation policies
security/
  identity/         Principals, current account and active-account guards
  authentication/   Database and in-memory credential providers
  authorization/    URL and order-access rules
  config/           Shared security setup and browser-mode properties
  login/            Password throttling/counters and password failure presentation
  session/          Explicit session-mode policy and revocation
  logging/          Security events and safe metadata
  web/              Shared HTML security failures
  jwt/
    access/         Access-token issuing, claims, principal and authentication
    refresh/        Opaque-token codec, persistence lifecycle and retention
    cookie/         Browser-cookie attributes, CSRF and credential-cookie lifecycle
    config/         Shared JWT activation/keys/properties and browser chain
    web/            Browser token resolver, account filter and login/logout handlers
    api/            JSON API chain, token resolver and password-login adapter
    TokenPair       Internal result shared by JWT transports
dto/query/          MVC/API list-query and pagination response contracts
web/form/           Form preparation
web/pagination/     Shared pagination/sort policy
web/error/          HTML error representation and route classification
```

Model/repository packages and service-interface names remain in their original locations.
Tests follow the packages of their subjects; multi-layer tests remain under integration.
Moving packages does not change routes, bean names, configuration keys, SQL migrations or
business methods. Package-private collaborators stay together: OrderFilter with the order
controller, AccountCredentialsService with account service implementations, and the logout
handlers with each other. AOP references use the new annotation path; the active-account
guard still covers all service subpackages.

After pulling these moves, run Maven clean then test before restarting. Old compiled
classes in target can otherwise register duplicate Spring beans. The package refactor was
checked statically for unchanged code bodies and preserved Java type bindings; compilation,
tests and application startup were not run after these moves.


The application has two profiles: `local` uses PostgreSQL and `test` uses H2 in memory. There are no additional runtime or Maven environment profiles. The original exercise description above is retained as historical assignment context.

Flyway is the only schema initializer. `V1__initial_schema.sql` creates the schema; `R__demo_data.sql` supplies local/test data. SQL initialization and Hibernate schema generation are disabled. Hibernate validates the migrated schema.

Before the first local run after switching to Flyway, stop the application and recreate only the disposable `appliance_store` database in pgAdmin, with owner `postgres`. The old records are intentionally not preserved. Keep the system `postgres` database, roles and other applications' databases. This is a one-time manual reset, not a startup action. Baseline-on-migrate is disabled intentionally; an old populated schema without Flyway history will refuse startup.

The previously created `appliance_store_test` PostgreSQL database is not used and can be deleted. No PostgreSQL test database or TEST_DB_* settings are needed. H2 is created automatically by the tests and needs no setup in pgAdmin.

Use the `local` profile in the IDEA application run configuration with these variables:

```properties
DB_URL=jdbc:postgresql://localhost:5432/appliance_store
DB_USERNAME=postgres
DB_PASSWORD=your_local_password
```

Maven tests activate `test` by default and use H2. Do not activate `local` in the Maven test configuration. Existing tests may use separate named H2 databases to isolate fixtures. Flyway creates the schema and test data in H2 as well as in the application PostgreSQL database.

After reloading Maven dependencies in IDEA, run Maven `clean` and then `test`. Cleaning is necessary once after the removed/moved configuration and mapper classes so that stale classes in target are not loaded. Then start the application with `local`. Neither test compilation nor test execution has been performed.

The current Spring Boot 3.2 dependency management supplies Flyway. PostgreSQL 18 is newer than that Flyway release's verified database range and can produce an upgrade recommendation. Actual H2/PostgreSQL startup must be checked. The separate supported-framework upgrade in the project specification remains outstanding.

Repeatable seed runs when its checksum changes. It inserts missing demo entries and preserves existing passwords and business changes. An unchanged seed does not run again at every restart. Never edit an applied versioned migration. Add the next version for a schema change. Do not use repair/baseline/clean to conceal migration errors.

### Local/test demo accounts

These are public disposable demo credentials, not production secrets. Each account has a distinct password and separately generated BCrypt hash. Real credentials must never be committed.

| Email | Role | Demo password |
| --- | --- | --- |
| mercury@gmail.com | CLIENT | `MercuryLocal2026!` |
| venus@gmail.com | CLIENT | `VenusLocal2026!` |
| earth@gmail.com | CLIENT | `EarthLocal2026!` |
| iamgod@gmail.com | CLIENT | `MarsLocal2026!` |
| jupiter@gmail.com | CLIENT | `JupiterLocal2026!` |
| saturn@gmail.com | CLIENT | `SaturnLocal2026!` |
| uranus@gmail.com | CLIENT | `UranusLocal2026!` |
| neptune@gmail.com | CLIENT | `NeptuneLocal2026!` |
| phobos@gmail.com | EMPLOYEE | `PhobosLocal2026!` |
| moon@gmail.com | EMPLOYEE | `MoonLocal2026!` |
| deimos@gmail.com | EMPLOYEE | `DeimosLocal2026!` |
| europa@gmail.com | EMPLOYEE | `EuropaLocal2026!` |

### Mapper generation

MapStruct 1.6.3 generates Spring mapper implementations during Maven compilation. Lombok and its MapStruct binding are configured in the compiler annotation processor path. Reload Maven in IDEA and enable annotation processing. Do not edit or commit `target/generated-sources`. Runtime ModelMapper is no longer used. Request mappings preserve IDs, credential fields and relationships managed by services.

No build/test result is implied by this change. Run Maven `test` in IDEA after reloading dependencies, then the normal `verify` coverage check when ready.


### Logging

The `local` profile writes application DEBUG logs to the IDEA console and `logs/store.log`, relative to the run configuration's working directory. The file is created automatically. Archives go to `logs/archive`, rotate daily or after 10 MB, keep 14 days, and have a 200 MB archive cap. The active file is additional to that archive cap. The `test` profile uses console logging only. No extra profile or database is needed.

Each request gets a server-generated UUID returned in `X-Request-ID` and included in log lines as `requestId`. HTTP logs contain method, safe path, route pattern, controller method when available, status/outcome, duration, client IP and request byte count (-1 when unknown). Known routes use their template instead of actual path parameters. Unknown/wildcard routes show a sanitized path, with sensitive, encoded or long segments hidden. Query parameters, request/response bodies, cookie values and authorization values are never recorded. An unknown route can have `route=-` while `path=/appliances1` identifies the typo.

`@Loggable` records technical service execution. Separate business events record action, actor ID, entity ID and the captured request ID only after the actual enclosing transaction commits; rollback produces no business-success record. Security logs record successful/failed authentication, logout and authorization rejection without printing principals or credentials. Forwarded IP headers are not trusted for logging.

Unexpected failures retain exception/cause types and stack frames, but arbitrary exception messages are stripped from logged stack traces because SQL and other failures can embed credentials or submitted values. User-facing pages remain localized and do not display those details. Treat log files as operational data; they are excluded from Git.


HTTP completion severity is INFO for success/redirect, WARN for 4xx and ERROR for 5xx. DEBUG records request start and safe sizes. TRACE adds only boolean header-presence/async diagnostics; it is disabled by default. To temporarily enable it, set `logging.level.com.epam.rd.autocode.assessment.appliances.logging=TRACE` in local properties. TRACE does not enable credential/body logging.


### Database constraint conflicts

Write methods declare `@PersistenceBoundary` with an explicit operation. Its aspect wraps
the transaction advisor and translates only known unique/FK constraints after rollback.
Existing field errors are reused for duplicate emails/names and references removed during
a write; referenced deletions produce the existing conflict response. Unknown constraints,
CHECK/NOT NULL failures and missing metadata remain server errors. The original exception
is retained as the cause, without exposing its database message to the user.

If a method joins an outer transaction, translation is deferred to that transaction's
boundary: the inner aspect preserves the original failure. Future orchestration methods
must explicitly declare their own boundary. No independent transaction is started to hide
a failed transaction. H2 tests cover rollback, commit callbacks, FK rejection and joined
transactions; PostgreSQL concurrency still requires verification against PostgreSQL.


### Active accounts and form lookups

All public application-service entry points now validate the persisted account through
ActiveAccountGuard. ActiveAccountFilter clears stale session authentication before URL
authorization. Client/employee deletion expires registered account sessions only after a
successful commit; an outer rollback keeps them active. Session tracking currently has no
concurrent-session limit. This is a single-instance registry, not distributed session storage.
The checks reject subsequent access; they do not cancel a request already executing when
another transaction deletes the account.

Product forms use ApplianceFormAssembler; order forms use OrderFormAssembler. Both obtain
lightweight SelectOption values from LookupService. The initial search returns at most 20
matches, plus up to 50 distinct selected IDs to preserve editing/validation state. Browser
search uses read-only /lookups/products, /lookups/manufacturers and /lookups/clients;
client options require EMPLOYEE at both HTTP and service boundaries. Search queries are
limited to 100 characters. A failed search preserves the current selection. These endpoints
support MVC forms; they do not implement the planned stateless JWT API.

DatabaseConstraintExtractor reads SQL metadata; PersistenceConflictTranslator maps only
known constraints to domain errors. SQL migrations and database profiles are unchanged.


### List pagination and search

MVC lists bind shared ListQuery parameters. ListPageFactory applies each resource's sort
allowlist, adds id as a stable tie-breaker and clamps negative page numbers to zero.
store.pagination.page-size defaults to 10 and accepts values from 1 to 100. Invalid page
binding or search/sort values longer than 100 characters return HTTP 400. Order status
remains a separate filter. ApplianceRepository escapes custom LIKE patterns using !;
derived Containing queries retain Spring Data's own escaping. %, _ and ! are literal
search characters, not additional wildcard controls.


### Principal identity

StorePrincipal implements AccountPrincipal and delegates UserDetails credential handling to
Spring Security User. Equality and hashCode use the immutable accountId, not the editable
email. SessionRegistry therefore groups sessions before and after an email change, while
reusing an email for a different account cannot merge those accounts. A principal with a
missing accountId remains invalid and is rejected by CurrentActor. Authentication adapters
added later must implement symmetric identity equality and hashCode by accountId.
The production login path uses AccountRole directly; the string constructor is retained for
existing callers and validates through the enum. This step does not add OAuth or JWT login.


### Optional in-memory password authentication

The default `store.security.auth-mode=database` keeps the normal PostgreSQL/H2 account
passwords. To demonstrate in-memory credentials locally:

1. Copy `config/auth-inmemory.properties.example` to `config/auth-inmemory.properties`.
2. Use emails of existing local client/employee accounts. Set MEMORY_CLIENT_PASSWORD_HASH
   and MEMORY_EMPLOYEE_PASSWORD_HASH in the IDE environment to BCrypt hashes. For local
   demo use, hashes may be copied from the corresponding demo SQL; the password is the
   clear-text demo password documented above, not the hash. To choose another password,
   generate its hash with BCryptPasswordEncoder in an IDE scratch/debug evaluation.
3. Keep Spring profile `local` and add this application program argument in IntelliJ:
   `--spring.config.additional-location=optional:file:./config/auth-inmemory.properties`.
4. Start the application normally. Only configured memory credentials can authenticate;
   the database still stores business accounts, roles, catalog and orders.
5. Remove the additional-location argument to return to the default database login.

No new Spring/Maven profile or PostgreSQL test database is needed. `inmemory` is rejected
outside local/test and whenever prod is active. Clear-text configured passwords, duplicate
canonical emails and configured/domain role mismatches are rejected. Missing local domain
accounts refuse login without crashing startup. Unknown auth-mode values fail binding.
The local properties file is ignored by Git. Do not commit real credentials.

### Foundation acceptance

Run Maven `test`, then `verify` yourself. The existing JaCoCo gate is unchanged: zero missed
lines and branches outside the model exclusions. Passing `test` alone does not establish
passing coverage. Inspect `target/site/jacoco/index.html` after verify; no passing verify or
coverage claim is made for the new code before it has actually run. Existing reports before
this step contained 300 tests with zero failures/errors (20 November 2026).

Manually check PostgreSQL startup/restart, database login for both roles, all CRUD/order
forms, search/sort/page links and language switching, duplicate/reference errors, and session
expiration after account deletion. Then opt into memory authentication and check both roles
with their configured passwords. H2 service races cover duplicate email/name and a new FK
reference committed after a precheck; they do not establish PostgreSQL concurrency behavior.
No DB reset, Flyway clean/repair or additional migration is required by this code change.
The external Autocode result remains a separate acceptance check.


### Security response policy

Client order queries are scoped by accountId at the repository level, including write locks.
A foreign and an absent order use the same NOT_FOUND code and localized 404 template.
The ownership policy remains a defensive check for loaded objects. Employee-only actions
are rejected with 403 for clients before loading an order, independently of its ID. CSRF
failures also remain 403 before resource access. An anonymous MVC request to a protected
route redirects to login; an unknown GET/HEAD route returns 404. Route classification checks
handler mappings only, not database existence. Known route names are not treated as secrets.
Error responses do not expose owners, object fields, stack traces or database details;
Spring Security cache-control prevents caching personalized responses. These assertions
are covered by ResourcePrivacyIntegrationTest and ScopedOrderAccessTest, pending execution.
Equivalent status/content does not imply constant-time responses or a complete security audit.

InMemoryAuthenticationValidator checks local settings and configured/domain role consistency;
InMemoryAuthenticationConfig only constructs the selected authentication components.
MvcRouteClassifier classifies mappings; MvcSecurityFailures handles the security response.


### Password login limits and sessions

V2__login_attempt.sql adds durable canonical-email counters. Flyway applies it on the next
startup/test run; existing migrations remain unchanged and no database reset is needed.
Five failed password logins within 15 minutes lock password login for 15 minutes. The same
policy applies to unknown accounts and to database/in-memory authentication. Settings are
store.security.login.max-failures, window and lock-duration. Successful unlocked login
clears the counter; blocked retries do not extend the deadline. MVC redirects blocked
attempts to /login?locked with a generic EN/UK message. API 429/Retry-After belongs to the
future API handler and is not implemented by this MVC redirect.

LoginAttemptPolicy owns time-window rules. LoginAttemptService owns independent counter
transactions, locking and retries. Explicit Persistable new-state prevents merge from
silently overwriting another transaction's initial count. LoginAttemptEvents records one
BadCredentials event per password failure and resets successful password authentication;
locked retries and third-party token events are excluded. The provider checks current lock
state before/after password verification. Cleanup removes expired counters after 24 hours,
once per hour; retention and cleanup-interval-ms are configurable. Raw Hibernate SQL error
logging is disabled for constraint races; safe application error/security logging remains.

Sessions expire after 30 minutes of inactivity. At most two active sessions are allowed;
a third login expires the oldest, whose next request redirects to /login?expired. Session
IDs change on authentication. Cookies use HttpOnly and SameSite=Lax; Secure/HTTPS and
production deployment configuration are still part of the later HTTPS block. Maximum
sessions is store.security.sessions.maximum (1–10). The registry remains single-instance.

Verification: run Maven test, then verify. LoginProtectionIntegrationTest covers lock
expiry, generic unknown-account behavior, canonicalization, rollback persistence, cleanup,
simultaneous initial counters, session eviction and session ID rotation. In-memory login
has a lock regression scenario too. Tests have been added but not run.
For a manual check, submit five wrong passwords, then the correct password: login remains
blocked until lock expiry. Use three separate browser profiles to check session eviction.
Per-email locking is this project's specified policy, not a complete anti-bot/IP rate limiter.


### One browser login: JWT cookie for the website and API

The normal launch uses store.security.web-auth=jwt. Open /login and submit the usual
email/password form. The server checks either database credentials (default) or the
explicit in-memory provider and issues the same HttpOnly store_access cookie for HTML
pages and the cookie-mode JSON API. There is no second browser login, no api-access.js,
and no token in HTML, localStorage or JavaScript. Existing order-row/lookup scripts stay.

Reload Maven for the resource-server dependency. Configure JWT_SECRET_BASE64 in the IDE
run environment even if the optional JSON API is disabled: the website now needs JWT too.
The secret must encode at least 32 cryptographically random bytes. Generate it locally:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtKeyGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtKeyGenerator.GetBytes($jwtKeyBytes)
[Convert]::ToBase64String($jwtKeyBytes)
$jwtKeyGenerator.Dispose()
```

Keep the key out of Git. Properties now live under store.security.jwt.*, shared by HTML
and API; the environment variable JWT_SECRET_BASE64 is unchanged. For JSON endpoints also
set STORE_API_ENABLED=true. Default STORE_API_TOKEN_TRANSPORT=cookie makes the ordinary
/login cookie work for both /orders and /api/orders. No additional login is needed.
When the API is disabled, /login and the website still use JWT.

### Cookie and CSRF behavior

The access JWT uses HS256, TTL 15 minutes, clock skew 30 seconds and checks signature,
algorithm, issuer, audience, subject and required claims. Each authenticated request
rechecks the local account and exact supported role before URL authorization; services
also enforce their own access rules. Deleted accounts and changed roles cannot keep
using old claims. JWT requests never reset password-failure counters.

store_access is host-only, HttpOnly, SameSite=Lax, with Path=/ (or the application's
context path). Max-Age never exceeds token expiry. Successful login/logout expires the
previous /api-scoped access cookie too. Shared cookie security is controlled by
store.security.cookies.secure=true. Only local/test explicitly allow false for HTTP;
HTTPS requests still receive Secure cookies. A production/non-local false is rejected.

Both browser and API cookie chains are stateless with NullSecurityContextRepository and
NullRequestCache. Existing JSESSIONID security contexts do not authenticate them. The
separate HttpOnly STORE-CSRF cookie and masked hidden form field/header protect unsafe
requests, including login/logout. SameSite does not replace CSRF. CookieCsrfProtection
restores unsafe-method checks after Resource Server's automatic bearer exemption. JWT chains explicitly
use NullAuthenticatedSessionStrategy for both session management and CSRF authentication
hooks. Otherwise Spring Security 6.2 treats every stateless JWT request as a new login and
can remove the CSRF cookie on background JSON/404 requests. JwtCookieLifecycle alone
rotates the cookie at actual login/refresh/logout; CsrfFilter still validates every unsafe
cookie request. RefreshHttpIntegrationTest covers a form, intervening API/DevTools 404
requests, and logout using the original CSRF token.
Failed password login redirects with generic localized errors without creating a session.

Use the usual POST /logout. It clears matching access/CSRF cookies. HTML missing login
redirects to /login; forbidden mapped resources return 403; unknown GET/HEAD return 404;
foreign/missing client orders remain 404. API errors remain safe localized JSON with
status/code. Invalid/expired HTML credentials are cleared before redirecting to /login.

The JWT currently expires 15 minutes after issuance, rather than extending on activity.
Refresh access is now backed by the lifecycle described below. Logout revokes the presented
refresh credential; a previously copied access JWT may still work until its expiry.
The 30-minute idle timeout and maximum-two sessions apply only to the session demo below.

### JSON clients and optional header transport

Cookie-mode clients first GET /api/auth/csrf, retain STORE-CSRF, and use the returned
masked token and headerName on unsafe requests. POST /api/auth/login with JSON
{"email":"...","password":"..."} sets the same access cookie and returns only expiresIn
and expiresAt. Obtain fresh CSRF after login. POST /api/auth/logout requires CSRF and
clears that same browser credential; it also signs this browser out of the website.

Explicit STORE_API_TOKEN_TRANSPORT=header is for external API clients. JSON login then
returns accessToken/tokenType/expiresIn/expiresAt; API requires Authorization Bearer,
ignores credential cookies and disables API CSRF only for that explicit header chain.
The website still uses its cookie and CSRF. URL/form access_token never authenticates.

Catalog endpoints: GET /api/appliances, /api/appliances/{id}, /api/manufacturers,
/api/manufacturers/{id}. Orders: GET/POST /api/orders, GET/PUT/DELETE /api/orders/{id},
POST /api/orders/{id}/approve. Controllers call existing application services; prices,
ownership and approval remain server rules. POST order JSON:
{"clientId":null,"order":{"items":[{"applianceId":1,"quantity":2}]}}. PUT uses
{"items":[{"applianceId":1,"quantity":2}]}. Lists have bounded pagination/search/sort.
API error language uses Accept-Language EN/UK. Locked password login returns 429/Retry-After.

### Explicit session demonstration and tests

store.security.web-auth=session retains the existing session implementation for checking
idle timeout, concurrent-session limits and session invalidation, which EPAM recommends
for non-JWT implementations. It is not a second login used alongside JWT in normal mode.
The test profile explicitly selects it for existing session regression tests. The new
JwtCookieIntegrationTest and JwtInMemoryLoginIntegrationTest explicitly select jwt and
verify the ordinary login, HTML/API sharing, real CSRF, absence of security sessions,
database/in-memory credentials, stale roles, deleted accounts and safe error behavior.
JwtApiIntegrationTest separately checks the external header API and session isolation.

Run Maven clean then test yourself: removed/moved Spring components must not remain in
target. Then run local with JWT_SECRET_BASE64 and STORE_API_ENABLED=true; open /login,
log in, visit /orders and /api/orders without another login, and check cookies in DevTools.
Test missing CSRF gives 403 and normal logout removes the cookie. Clear old localhost
cookies once when switching versions. H2 tests and PostgreSQL local launch are unchanged.
An additive V3 migration is prepared below. No database operation, build/test run or commit
was performed.

### Refresh credentials, rotation and browser renewal

Successful JWT login now issues an access/refresh pair. The access lifetime remains 15
minutes. Refresh is an opaque 256-bit SecureRandom value, valid for 7 days from issuance.
Only a unique SHA-256 fingerprint is stored in refresh_token, together with owner,
expiry, revocation and successor id. Raw refresh credentials never enter database/logs.

Both store_access and store_refresh are HttpOnly, SameSite=Lax, host-only and use the
application-root path. Refresh uses this scope so the SAME ordinary POST /logout can
receive and revoke it, including when optional JSON API endpoints are disabled. This
replaces the earlier planned API-only refresh scope. Secure rules and matching deletion
attributes are shared by AuthTokenCookies. Cookie JSON responses contain expiry metadata
only. CSRF remains mandatory on browser and cookie-mode API renewal/logout.

When access expires, a protected HTML request can redirect to /auth/renew. It displays a
normal Thymeleaf form, without JavaScript or token values. Press Continue to POST with
CSRF, atomically rotate refresh, receive a new pair and return home. The interrupted
unsafe action is NOT automatically replayed. Failure clears the credential cookies and
redirects to /login?expired. You can also open /auth/renew explicitly to renew earlier.

POST /api/auth/refresh is available when JSON API is enabled. Cookie-mode clients send
store_refresh plus a fresh masked CSRF header obtained from GET /api/auth/csrf; no body
credential should be supplied. Header-mode clients send {"refreshToken":"..."} in JSON.
Header login/refresh returns accessToken/tokenType/expiresIn/expiresAt plus refreshToken
and refreshExpiresAt; those values are explicitly redacted from DTO toString. API refresh
errors use HTTP 401 with code refresh.invalid. Expired access credentials do not block
login/refresh/logout. Header-mode JSON requests ignore browser cookies and do not clear
the website's credentials. Use HTTPS for real credential traffic; TLS deployment remains
a later block.

Rotation serializes on the base users row, then loads the refresh record under that lock.
This avoids FOR UPDATE against nullable joins in the polymorphic User hierarchy. The old
credential is revoked and links to its successor in one transaction. Reusing any retained
revoked credential revokes ALL refresh access for that user. Revocation commits in its own
transaction BEFORE the generic authentication failure is raised, even if a caller later
rolls back. Two parallel requests using the same old token cannot retain two successors;
the losing reuse also revokes the winner's refresh continuation. Do not retry the old
credential automatically after a timeout. No raw value is logged; success/revocation logs
are written after commit.

Normal logout revokes the presented refresh record AND its rotation successors, and clears
both credentials. It does not revoke unrelated browser chains.
Repeating logout is harmless; unknown credentials do not revoke arbitrary accounts.
A copied access JWT is still bounded by its existing 15-minute expiry, not a denylist.
Account deletion cascades its refresh records. Hooks for password change/reset revocation
must be added in the password-management block in the SAME credential-change transaction;
that user-facing password-change/reset flow is not claimed implemented here.

Hourly retention cleanup processes at most 100 account owners per pass, acquiring the
same user-first lock. Expired refresh rows remain for another 8 days for reuse detection;
active records are not deleted. Configure store.security.refresh.ttl/retention and
cleanup-interval-ms if needed; retention must be at least the selected TTL.

V3__refresh_token.sql adds the table, FK cascade and indexes. V1/V2 and demo seeds are
unchanged. Flyway applies V3 automatically on YOUR next application/test startup; no
database reset or manual schema edits are needed. Existing test migration assertions now
expect V1, V2, V3 and the demo repeatable script. RefreshTokenIntegrationTest covers hash
storage, rotation, committed reuse revocation, concurrency, logout isolation, retention
and cascade. RefreshHttpIntegrationTest checks real browser CSRF/renewal/logout and JSON
cookie rotation. JwtApiIntegrationTest also covers header refresh/logout. Tests were
prepared but NOT run.

Manual check: Maven clean then test; restart local and sign in. In Chrome DevTools remove
ONLY store_access, retain store_refresh, and visit /orders. On /auth/renew press Continue;
both cookies should be newly issued and orders should open again. Normal logout should
remove both, and the presented refresh_token row should have revoked_at. Do not publish
or paste credential values from DevTools. No new library installation is needed.

### JWT/refresh audit corrections

Logout follows replaced_by under the same owner lock. If refresh rotated immediately
before logout read an older cookie, its successor is also revoked. Repeated logout stays
idempotent and unrelated browser chains remain valid; this differs from the deliberately
broader all-account revocation policy on detected refresh REUSE.

A successful password re-login in cookie mode retires the previous browser chain in the
same transaction as issuing its replacement. This also handles switching local accounts;
both owner rows are locked in ascending id order to avoid cross-account lock cycles.
An incorrect password never revokes the previous browser's access. Explicit header clients
manage their own distinct refresh credentials; cookie values never choose their session.
Refresh transactions explicitly use READ_COMMITTED, so owner discovery before a lock wait
cannot preserve an old repeatable-read snapshot of a token that has already rotated.

Authentication storage/transaction failures return safe HTTP 503 rather than wrong-password
redirects, 401 or 403. A temporary account-lookup outage preserves browser cookies. If logout
cannot persist revocation, it clears the local cookies but returns 503 instead of claiming
server logout succeeded. This does not claim a copied refresh was revoked during an outage.
Backend exception causes are sanitized before entering authentication exceptions so framework
logging cannot print arbitrary SQL/connection messages. Genuine invalid credentials, role
denials, CSRF failures and missing resources keep their existing distinct behavior.

Added regression coverage for rotated-token logout, password re-login, concurrent account
switches, preserved cookies on an outage, backend 503 responses and failed server logout.
The earlier browser-background-CSRF regression remains. Only static checks were performed;
Maven/build/test/application/database execution remains for the user. No migration changed.

### Change your own database password

Database authentication now offers GET/POST /password/change through the signed-in
navigation. The Thymeleaf form requires the current password, a strong new password and
matching confirmation. It uses CSRF and never re-renders any password value. The current
secret is checked against fresh locked credential state and the shared password-attempt
limit; five wrong current-password checks cannot bypass the existing login lock. Failed
attempts remain persisted even when the change transaction rolls back. Successful current
verification is not a separate login and does not reset counters ahead of commit.

The new BCrypt hash, revocation of ALL account refresh records and removal of the login
counter are one transaction. Reusing the current password as the new password is rejected.
PasswordChanged contains only accountId. After successful commit, separate listeners expire
tracked sessions and log safe security metadata. The controller then clears the current
JWT/CSRF cookies and HTTP session and redirects to /login?passwordChanged. Sign in again.
A previously copied access JWT remains bounded by its existing <=15-minute TTL, as in the
accepted access-token policy; the operation does not introduce an access-token denylist.

In-memory credentials are configuration-backed and cannot be changed by writing the DB.
Consequently this form/service are disabled in the explicit in-memory demo mode; the
navigation hides the action and direct GET returns 404. Database-backed authentication is
the default production-capable path. Password reset/email and OAuth remain later blocks.

The new folders keep concerns separate: dto/password, validation/password, controller/password,
service/password, security/password, repository/lock and repository/projection. Existing
StrongPassword constraints are grouped with PasswordsMatch under validation/password.
AccountRowLock locks the users base row and refreshes the JPA first-level entity state.
Account profile update/delete also obtain this lock before touching joined tables, so a
stale outer profile transaction cannot restore a previous password hash. Employee deletion
locks its target parent before the existing employee-set lock. Package-private account
helpers remain together, and the existing deletion tests are adapted.

No schema migration, new dependency, SMTP secret or provider registration is needed for
this block. H2 tests cover hash storage, atomic rollback, refresh/session revocation,
password lock, validation/no echo, JWT-cookie cleanup, legacy session logout and stale
profile concurrency. They were prepared but not run.

Manual check after Maven clean/test and restart: create a disposable client, sign in as
that client, open Change password, try a wrong current password and a confirmation
mismatch, then change correctly. The form must not echo values; success signs you out,
old password fails and new password works. Previously captured refresh credentials can
no longer renew. Do not test by publishing real credentials or resetting shared demo
users unless you intend to keep their changed password. Flyway never resets it on startup.

Pagination review: MVC and API now share web.pagination.ListPageFactory/ListPage.
The duplicate ApiPageFactory was removed; the shared factory accepts transport-neutral
values and uses store.pagination.page-size when API size is omitted. Request DTOs remain
separate because MVC clamps negative page numbers while API rejects them. Both reject page
numbers over 1,000,000 before querying. API-only error helpers are conditional on enabled.
After this package move, run Maven clean then test to remove stale target/classes entries.
No database or migration changes are required by the move.

### Demo migration credentials

`R__demo_data.sql` contains placeholder names, without embedded password hashes or plaintext passwords. `config.demo.DemoMigrationConfiguration` supplies freshly salted BCrypt hashes for local/test demo accounts only; production does not enable this configuration. Demo login passwords remain `<Name>Local2026!` (for example, `MercuryLocal2026!`). These are public demonstration credentials, never production credentials.

Fresh salts change the repeatable migration checksum on a new application context, so Flyway can execute the demo script again on restart. Its conditional inserts preserve existing account passwords and user changes. Re-running migrate within the same context uses the same placeholders. Never copy these demo settings into production. Database password columns must always contain one-way hashes.

### Local password recovery with Mailpit

Start Mailpit separately:

```
mailpit.exe --smtp 127.0.0.1:1025 --listen 127.0.0.1:8025
```

Run the store with its existing `local` Spring profile and database/JWT environment settings. No email account, email password, external provider, or paid service is needed. Mailpit SMTP is configured at `127.0.0.1:1025`; its inbox is http://127.0.0.1:8025. Sender: `appliance-store@example.test`. Even if the recipient is `mercury@gmail.com`, the message stays in local Mailpit and does not go to Gmail.

On `/login`, select “Forgot password?”, enter the email of a database account, and open the captured email in Mailpit. Follow its link and choose a strong password. The old password and reuse of the same link must fail; the new password must work. Requesting a second link invalidates the first; links expire after 30 minutes. Unknown emails and throttled requests get the same generic response. New secrets are never echoed into password inputs. No SMTP health check is required at startup; delivery failure is logged without recipient/link/token and does not disclose whether the account exists.

The new `V4__password_reset_token.sql` stores only SHA-256 digests, one active reset token per account. Successful redemption deletes the token atomically with the password change, refresh revocation and login-lock reset. Rollback preserves the old password/token. Parent-account locking serializes concurrent redemption. Sessions expire after commit. Existing access JWTs can remain valid until their normal expiry (up to 15 minutes). Changing a known password also invalidates outstanding reset links. Expired token rows are cleaned hourly.

Recovery is enabled only in `local`, never `prod`, with `store.mail.enabled=true` and database authentication. Other profiles keep the feature disabled; production email delivery is not implemented in this block. The bounded single-instance limiter defaults to 5 requests/email and 20/source IP per hour, including unknown emails. Configure `store.security.password-reset.max-email-requests`, `max-source-requests`, and `window-seconds` if needed. It uses the direct remote address, not untrusted forwarded headers; counters reset on restart and do not coordinate multiple instances. A distributed production deployment needs shared limits. Keep Mailpit bound to loopback as above; do not expose its inbox publicly.

Run `clean` then `test` in Maven and restart the application before manual testing. Tests use a mocked mail sender and never deliver real mail. Maven and application verification remain a manual acceptance step. SMTP dependency download occurs during your Maven reload/build.

### Fast unit checks and the full test suite

Use `./mvnw.cmd -Punit-tests test` (Windows) for fast unit checks without a full Spring/H2 startup. In IntelliJ's Maven tool window, explicitly enable the `unit-tests` Maven profile before running `test`; disable it before the full acceptance run. This is a Maven test-selection profile, not a Spring application profile.

The fast profile excludes `*IntegrationTest`, repository slice tests, the full application smoke test and the two full-context home/login controller tests. It does not replace the integration suite or establish full coverage. The existing `src/test/java/com/epam/rd/autocode/assessment/appliances/integration` directory remains in place; integration tests in other layer-specific packages retain their existing locations and scenarios.

Use `./mvnw.cmd test` with the `unit-tests` profile disabled for all tests, including database authentication, repositories, JWT cookies, password reset and transactional races. To reproduce just the reset regression, use `./mvnw.cmd -Dtest=PasswordResetIntegrationTest test`. Keep full tests enabled for acceptance/coverage; do not use the selective unit profile as the final verification.

Demo BCrypt hashes are generated once per JVM and shared across isolated Spring test contexts. Passwords, random salts, BCrypt strength and normal credential checks remain unchanged. The cache contains only immutable public demo hashes, never real user credentials. Fresh salts on a new JVM may still rerun the insert-only repeatable demo migration; creating another context in the same JVM no longer regenerates all 12 hashes.

Password replacement uses a native compare-and-set `UPDATE users` instead of a polymorphic JPQL bulk update. For the JOINED User hierarchy, Hibernate's H2 temporary-table strategy can perform DDL with implicit commits, persisting reset-token deletion before an enclosing rollback. Direct base-table DML removes that DDL while retaining the old-hash check, account lock, shared transaction and persistence-context clear. The existing integration rollback/one-time/concurrency assertions are unchanged. No runtime benchmark or Maven run was performed.

### Recovery resend protection and responsive forms

Local reset requests now have a 60-second cooldown per normalized email, in addition to the hourly email/source limits. The same gate applies to unknown addresses and to different source IPs requesting the same email. Rapid repeated submissions return the same generic confirmation and do not send another email or invalidate the first link. Configure `store.security.password-reset.cooldown-seconds` if necessary; the existing replacement-link scenario test explicitly uses zero cooldown, while limiter unit tests cover the real cooldown boundaries.

After commit, delivery is queued to a dedicated single mail worker with at most 100 waiting messages. The HTTP response does not wait for SMTP. Queue/delivery errors are logged without addresses or tokens; the local queue is in memory and is not a durable mail outbox. The confirmation page hides the submission form and explains the wait. The reset form contains one main submit action; its redundant footer sign-in link was removed.

Resetting to the current password is rejected with the existing localized `password.new.unchanged` field error. The transaction rolls back token consumption so the same valid link can be used to choose a different password. Invalid/used tokens are still checked before returning a password comparison error. Mail integration assertions now wait for background delivery instead of assuming synchronous SMTP.

Manual check after tests/restart: submit one address rapidly twice (one new Mailpit email), request again after at least 60 seconds (one more email, replacing the earlier link), and try the current password through the latest link (field error; choosing a different password with that link succeeds). Five sends/email/hour and source limits still apply. Reopening an old browser tab does not bypass server-side limits.

### Final project mail scope and resend feedback

Mailpit is the final mail transport for this project, by the user's decision. Connecting an external SMTP provider or delivering to real recipients is not a remaining acceptance task. Here “production quality” means safe handling of data, errors, access and secrets; it does not mean enabling public email delivery. Existing local-only safeguards remain.

When a recovery request is throttled, the form now returns HTTP 429 with `Retry-After` and a localized remaining-seconds message. The value is calculated from the active email cooldown and hourly email/source windows, rounded up; accepted and unknown addresses follow identical rules. The submitted email stays in the form and no mail/token is issued on rejection. The displayed number is the server's value at response time, not a JavaScript countdown. The footer sign-in link on the forgot-password page was removed; the existing header sign-in navigation remains.

### Recovery quota accounting correction

Cooldown and other rejected requests no longer increment either hourly counter. Quota is reserved only after all gates pass, identically for known and unknown emails; this counts accepted recovery requests, not confirmed SMTP deliveries. For example, one accepted request followed by 100 repeats during its 60-second cooldown uses one hourly slot. The next request can succeed after the cooldown if hourly capacity remains. Real hourly limits still return their remaining wait. Restart the running app after applying the fix; existing in-memory counters from the earlier version are not rewritten in a running JVM.

### Google sign-in (OAuth2 / OpenID Connect)

Only Google is supported. Create an OAuth client of type **Web application** in Google Cloud Console / Google Auth Platform. Fill Branding, choose an appropriate Audience (External / Testing for personal development), and add the Google accounts you will test with under Test users when required. Add this exact **Authorized redirect URI**:

```
http://localhost:8080/login/oauth2/code/google
```

This is a server-side authorization-code flow; JavaScript origins are not required. Keep using the same `localhost` origin in the browser. Add these environment variables yourself to the normal application launch configuration:

```
GOOGLE_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID=<your client ID>
GOOGLE_CLIENT_SECRET=<your client secret>
```

Keep existing `DB_*` and `JWT_SECRET_BASE64` values. Do not send secrets in chat, add them to source code, screenshots, archives or Git. Google sign-in is disabled by default. Enabling it requires database authentication, JWT website mode and complete client settings; malformed settings fail startup with a sanitized configuration error. The default fixed callback can be overridden with `GOOGLE_REDIRECT_URI`; HTTPS is required except loopback HTTP in local/test. The redirect must match Google exactly. Do not download credentials JSON into the project.

Reload Maven, run `clean` then `test` and restart with the existing `local` profile. `clean` is important because the mail listener was renamed to `SecurityMailEventListener`. Mailpit remains the final project mail transport at `127.0.0.1:1025`, inbox `http://127.0.0.1:8025`.

On `/login`, choose **Sign in with Google**. Spring Security handles authorization-code exchange, Google ID-token validation, state, nonce and PKCE. The authorization request is held in a temporary session with a hard 15-minute lifetime. The application does not retain Google API access/refresh tokens in its authorized-client repository or database and never uses a Google ID token as the shop access JWT.

For a new Google subject, the site asks for name, email and an optional loyalty-card number. No employee role can be requested. Google `email_verified=true` is accepted only for the same normalized email. An edited, missing or unverified address must be confirmed through Mailpit using a session-bound one-time 256-bit challenge; only its hash is stored server-side, it expires after 10 minutes, and the 15-minute overall onboarding deadline still applies. Open that link in the same browser and click Continue again. No domain account or shop credentials are created before all required data and verification are complete. The password is initialized to a random unknown strong secret stored as BCrypt; the user can establish a known password through the existing recovery flow.

Creation of User/Client and `oauth_identity` is atomic. External identity is keyed by Google `sub`, never email; changing the local email does not break an established Google login. A matching local email does **not** auto-link accounts: sign in with the existing password, open **Google account**, and explicitly submit **Link Google account**. Linking requires CSRF and the same unexpired shop JWT throughout the flow. A changed account/cookie, expired intent, or identity already owned by another user is rejected. The schema uniquely constrains provider+subject and user+provider; user deletion cascades to external identity metadata.

After successful onboarding/login, the temporary session and SecurityContext are cleared and the existing shop JWT/refresh cookie lifecycle is used. Ordinary pages/API continue to authenticate via JWT, not a login session. Pending Google onboarding has no shop account/role. Cancel/error removes temporary state. Linking preserves the original shop credentials; provider credentials are never exposed in UI, logs or DTO output. `V5__oauth_identity.sql` is additive; previous versioned migrations remain unchanged.

Automated tests use fake OAuth client credentials, mocked mail and synthetic OIDC users. A single Google integration context checks authorization redirect/state/nonce/PKCE parameters, invalid state, profile completion, Mailpit verification, JWT cookies, email collisions, stable subject mapping and explicit linking. Unit tests cover issuer/claims, temporary state expiry, one-time email challenges and account pinning. These tests do not demonstrate a real Google token-endpoint exchange; confirm it manually after configuring your own client. The test Spring profile disables Google by default so real environment credentials cannot silently enable network-dependent tests. Validation status: static checks completed; Maven tests, application startup and a real Google callback still require verification.

Manual acceptance: verified new Google account -> complete profile -> CLIENT JWT -> orders; known subject -> existing account; existing local email -> password login and explicit linking; edited profile email -> Mailpit -> confirm in same browser -> account; cancel/expired/invalid state -> safe localized error; second user's account cannot steal an identity; Google sign-in remains disabled cleanly when feature flag is off. Check `store_access`/`store_refresh` HttpOnly cookies and absence of an authentication JSESSIONID after success. Google callbacks/temporary onboarding are the deliberate short-session exception to the stateless shop flow.

### Password setup after Google signup and security notification emails

A newly created Google account has an unknown random strong secret stored as BCrypt, not the user's Google password. Its initial value cannot be recovered; it was never kept in plaintext. The password page now explains the distinction for Google-linked accounts and offers a separate CSRF-protected **Set password through email** form. This form needs no current store password: it sends the existing single-use recovery link to the authenticated account's current database email. It does not accept an arbitrary recipient from the browser. The normal current-password change form remains available for users who already chose a store password.

POST `/password/setup` requires a current CLIENT/EMPLOYEE identity, applies the same cooldown/hourly limits as forgot password, and preserves HTTP 429/Retry-After feedback. After following the email link and selecting a password, the existing atomic reset/session/refresh revocation rules apply. This is password setup for a Google account, not a change to the Google password. Mailpit remains the only destination for local application emails.

After successful Google account creation, Mailpit receives an **Account created** notification. After successful password change/reset/setup, it receives a **Password changed** notification. Notifications are localized, contain no password/hash/access/refresh values, and are queued only after the database transaction commits. Rejected operations and rollbacks do not emit success notifications. Existing Google users are not sent a new welcome email for every sign-in. Verified Google email does not need an extra confirmation challenge unless the user chooses another address; welcome notification is separate from email verification.

Tests cover the own-email setup route, CSRF, ignored forged recipient, actual known-password establishment, success emails and rollback behavior. The mail integration tests drain the worker queue between cases to prevent late messages interfering with mock state. Run the updated tests and restart before manual checks; no Maven or application run was performed for this change.

### Explicit password-setup state

`V6__password_credential_state.sql` adds credential metadata separately from `User`, preserving the original User fields and constructor contracts. New Google signups store `password_set=false`; random placeholder hashes do not count as a usable local password. Password login and current-password change are rejected for an explicitly passwordless account, while Google/JWT access continues to work.

The password page initially shows only **Create a store password** and its email action. Successful verified setup/reset atomically marks the password as set. The next visit shows the regular current-password change form. Linking Google to an existing local account records its existing password status without overwriting an explicit passwordless marker. Local accounts without metadata retain normal password behavior.

Old Google associations cannot reliably reveal whether their existing BCrypt hash is an unknown signup placeholder or a password already chosen by the user. The migration does not guess or overwrite credentials. Such accounts start with the setup view and a secondary **I already have a store password** option. A valid current-password change or verified reset establishes an explicit set state. Existing passwords/Google access are preserved. Legacy records are not globally marked passwordless, and no database reset is needed.

Tests cover passwordless login rejection, separate setup/change markup, state rollback with token/password rollback, and the legacy fallback. No Maven tests/application startup were performed for this change. Run tests, restart to apply V6, then verify the two stages of the page.

### Immediate cross-browser revocation after password changes

This policy replaces the earlier 15-minute residual-access allowance **for password change, verified password setup and password recovery**. `V7__account_security_state.sql` stores a shared per-account `credential_version`. New shop JWTs contain that version. On every JWT-authenticated site/API request, the existing account/role check also compares the token's version to the current database version. Old versions are rejected even if their signature and expiry are valid. Google linking checks it as well.

Password operations advance the version under the same base-user lock and transaction as the password update and refresh revocation. Commit invalidates all previously issued access JWTs; rollback preserves them. Issuance/refresh uses the same user-row lock and reads the current version, preventing a concurrent password change from leaving newly issued stale credentials usable. Version metadata has no in-memory cache. No server-side authentication session or per-token blacklist is introduced. Tokens from before this change count as version zero, and are rejected after the first version advancement.

On the next protected request from another browser, its old access/refresh cookies are cleared and the browser is sent to login rather than offered renewal with the revoked refresh token. Cookie/header API access gets HTTP 401. A storage outage remains HTTP 503 and does not clear credentials. A page already rendered in another browser stays on screen until it makes a request; no JavaScript push logout is implemented, and requests already authorized before the password change are not retroactively canceled.

Ordinary logout still revokes the relevant refresh chain and clears that browser's cookies; it does not increment the account-wide version or become a sign-out-everywhere operation. The new immediate policy is specifically for password operations. Fresh password/Google logins after the change issue tokens with the new version. Tests cover two browser cookie sets, site/API rejection before token expiry, new-login success, setup/reset revocation and rollback preservation. Run tests and restart to apply V7; the updated Maven/application was not run during implementation.


### Client registration

Local database mode enables `store.registration.enabled=true` with Mailpit.
Open `/register`, enter name, email, optional loyalty-card number, password and
confirmation. Open the email link from Mailpit in any browser or on another device
within 30 minutes. GET only displays a confirmation form; its CSRF-protected POST
creates a CLIENT and redirects to the normal JWT-cookie login. It never accepts
role, ID, email or password overrides at confirmation. No automatic login occurs.

V9__pending_registration.sql adds pending_registration. It stores profile details,
a BCrypt password hash, a SHA-256 digest of the random 256-bit token and an absolute
expiry. No raw password or raw token is stored there. Registration no longer uses
an HTTP session, and a restart preserves live applications. The confirmation email
is queued only AFTER_COMMIT; rollback does not send a link to missing data.

Confirmation locks its pending row and creates the client and deletes that row in
one transaction. Replays/expired/invalid links fail safely; rollback preserves the
application. Concurrent confirmation of one token has one winner. Further pending
applications for an already-created email cannot create another account because
the existing email constraint applies. Expired applications are cleaned hourly;
expiry is checked on every confirmation regardless of cleanup timing.

Duplicate email/card field errors and request limits remain. Worker creation stays
restricted to existing employees. Authentication remains JWT-based. Google OAuth
state/nonce/linking keeps its separate browser-bound flow; this change applies to
ordinary email/password client registration only.

Regression tests include cross-browser confirmation with real cookie CSRF,
no registration session, expiry/cleanup, replay, concurrent confirmation,
transaction rollback and AFTER_COMMIT mail delivery. The assistant has not run
Maven or applied V9 to the live database. Run a clean build to remove the deleted
RegistrationSession class, then restart; old session-bound links require a new
registration request. Database reset is not required.

### Optional unique loyalty cards (V8)

Client card means a loyalty-card number. It is optional in employee management,
public signup and Google onboarding. Blank input becomes NULL. A supplied number
is trimmed and unique; comparison preserves case. Editing the same account with
its unchanged number is allowed. Clearing it releases the number.
Named database constraints enforce uniqueness under concurrent requests; expected
email/card conflicts map to localized field errors without SQL/internal details.

V8__optional_unique_client_card.sql drops NOT NULL, normalizes existing blank/trimmed
values and adds unique/canonical constraints. Existing non-empty duplicate numbers
are not reassigned or deleted automatically: resolve those assignments before
applying V8. To find conflicts in the existing PostgreSQL database, run:

```sql
SELECT trim(card) AS card, count(*) AS accounts
FROM client
WHERE nullif(trim(card), '') IS NOT NULL
GROUP BY trim(card)
HAVING count(*) > 1;
```

No database reset or Flyway repair is required. Existing V1-V7 files remain intact.
New tests cover optional cards, duplicate create/edit, preserving/clearing a card,
registration field errors, named-constraint translation and actual V8 SQL on H2.
The assistant has not run Maven, applied V8 or changed the running database.
