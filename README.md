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

After reloading Maven dependencies in IDEA, run Maven `clean` and then `test`. Cleaning is necessary once after the removed/moved configuration and mapper classes so that stale classes in target are not loaded. Then start the application with `local`. Neither test compilation nor test execution has been performed by the assistant.

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
has a lock regression scenario too. Tests have been added but not run by the assistant.
For a manual check, submit five wrong passwords, then the correct password: login remains
blocked until lock expiry. Use three separate browser profiles to check session eviction.
Per-email locking is this project's specified policy, not a complete anti-bot/IP rate limiter.
