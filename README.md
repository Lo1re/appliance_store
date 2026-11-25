# Appliance Store

Spring Boot web application for managing an appliance shop. The application provides a Thymeleaf storefront and an optional JSON API, with Ukrainian and English localization.

## Features

- Appliance catalog with product details, search, server-side pagination and sorting.
- Employee management of clients, employees, appliances and manufacturers.
- Order creation, editing, deletion and employee approval, with ownership checks and server-calculated totals.
- Database password authentication, JWT access cookies, refresh-token rotation and logout.
- Client registration, email verification, password change and password recovery through local Mailpit.
- Optional Google OAuth2 / OpenID Connect sign-in and explicit account linking.
- Server-side validation, localized errors and safe request and business-event logging.

## Technology

Java 17, Spring Boot 3.5.16, Spring MVC, Spring Security, Spring Data JPA, Hibernate, Thymeleaf, Bean Validation, MapStruct and Lombok.

PostgreSQL is used locally; H2 is used for ordinary automated tests. Flyway manages the database schema. JUnit, Mockito, MockMvc, Testcontainers and JaCoCo support testing.

## Architecture

```text
api/          JSON controllers and API error responses
controller/   HTML controllers and shared MVC advice
service/      Business operations and policies
repository/   Database access and account locking
model/        Persistent domain entities
dto/         Request and response contracts
mapper/       MapStruct entity/DTO mappings
security/     Authentication, authorization, JWT, Google and password flows
validation/   Input validation and normalization
web/          Form preparation, pagination and web presentation helpers
mail/         Security emails and local delivery
logging/      Request and operation logging
persistence/  Database constraint error translation
config/       Application configuration
```

Controllers delegate business operations to services. Repositories handle persistence; DTOs define input and output contracts. MapStruct implementations are generated during compilation. JavaScript supports form interaction and lookup requests; validation, permissions, sorting and order totals remain authoritative on the server.

## Local setup

Requirements: JDK 17, PostgreSQL and Docker Desktop running Linux containers. Run commands from the project root.

Create a PostgreSQL database named `appliance_store` and configure its connection through environment variables:

```text
SPRING_PROFILES_ACTIVE=local
DB_URL=jdbc:postgresql://localhost:5432/appliance_store
DB_USERNAME=postgres
DB_PASSWORD=<your database password>
JWT_SECRET_BASE64=<Base64-encoded random key of at least 32 bytes>
SSL_KEY_STORE_PASSWORD=<your local keystore password>
```

Generate a random JWT key in PowerShell:

```powershell
$jwtKey = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($jwtKey)
$rng.Dispose()
[Convert]::ToBase64String($jwtKey)
```

Create the local HTTPS certificate once, choosing the keystore password used above:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-local-https.ps1 -TrustCertificate
```

`-TrustCertificate` adds the development certificate to the current Windows user's trusted certificates. The keystore is stored under `config/certs/` and excluded from Git. Keep passwords and keys in the launch environment.

Start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

Open **https://localhost:8443**. HTTP GET/HEAD requests on port 8080 redirect to HTTPS; HTTP write requests are rejected.

The `local` profile starts Mailpit through Docker Compose. SMTP listens at `127.0.0.1:1025`; the inbox is available at **http://127.0.0.1:8025**. Mailpit remains running when the application stops. Only local email delivery is configured.

```powershell
docker compose ps
docker compose stop mailpit
```

Flyway applies common schema migrations and local demo data. Hibernate validates the schema; SQL initialization and automatic schema generation are disabled. Start with an empty application database. Preserve applied versioned migrations and add a new migration for subsequent schema changes.

## Demo accounts

The local/test seed includes these disposable accounts:

| Role | Email | Password |
| --- | --- | --- |
| Client | mercury@gmail.com | `MercuryLocal2026!` |
| Employee | phobos@gmail.com | `PhobosLocal2026!` |

The normal authentication mode reads accounts from the database. Optional in-memory authentication is configured separately through `config/auth-inmemory.properties.example`.

## Optional Google sign-in

Create a Google OAuth client of type Web application and register this redirect URI:

```text
https://localhost:8443/login/oauth2/code/google
```

Set the following variables in the application launch environment:

```text
GOOGLE_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID=<your client ID>
GOOGLE_CLIENT_SECRET=<your client secret>
```

Google sign-in is disabled by default. New users complete a client profile; an existing local account must explicitly link its Google identity. Email verification uses Mailpit where required.

## Optional JSON API

Enable the API with `store.api.enabled=true`. API controllers return JSON and localized `application/problem+json` errors. The default cookie transport shares the browser login and retains CSRF protection. Browser authentication uses JWT by default; an explicit session mode is available for session-policy demonstrations and regression tests.

## Testing

Ordinary tests use the `test` profile and isolated H2 databases. They do not require local database credentials, a personal certificate or Mailpit.

```powershell
.\mvnw.cmd test
.\mvnw.cmd -Punit-tests test
```

Run the complete verification, including PostgreSQL Testcontainers and coverage checks, with Docker Desktop running:

```powershell
.\mvnw.cmd clean verify -Pcontainer-tests
```

PostgreSQL tests use disposable containers rather than the local application database. The coverage report is generated at `target/site/jacoco/index.html`; models are included, and the global checks require zero missed lines and branches. Coverage measures executed code and does not guarantee every possible scenario.

## Logging

Local logs are written to the console and `logs/store.log`, with rotation. Requests have an `X-Request-ID` for correlation. Passwords, tokens, cookies and request bodies are excluded from logs. Log files and generated build output are excluded from Git.
