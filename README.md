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
