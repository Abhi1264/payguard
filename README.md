# PayGuard

API tests for a small payment flow: create a user, create an order, take a payment through a fake provider, then refund it.

The services exist so the tests have something real to call. The automation module is the point of the project.

## What it is

Three Spring Boot services and a JUnit 5 suite.

- `user-service` stores a name and email.
- `order-service` stores an order in `CREATED`, then `PAID`, then `REFUNDED`.
- `payment-service` checks the user and the order, calls WireMock to authorize and capture, then marks the order paid. Refund goes back through WireMock and marks the order refunded.

PostgreSQL holds one database with three schemas: `usersvc`, `ordersvc`, `paymentsvc`.

## Architecture

```
automation (REST Assured, JDBC)
        |            |            |
        v            v            v
  user-service  order-service  payment-service
        \            |            /
         \           |           /
          +---- PostgreSQL -----+
                     |
              payment-service
                     |
                  WireMock
              (payment provider)
```

`payment-service` also calls `user-service` and `order-service` over HTTP. `order-service` checks that the user exists before creating an order.

Every API call except health checks needs `Authorization: Bearer local`. The services send the same token to each other.

## Test strategy

API tests are the main layer. The behavior under test is HTTP: status codes, the response body, and a few database rows after the payment flow.

A passing test checks the status code, the JSON shape, and the business fields. Schema files live in `automation/src/test/resources/schemas`. Database checks are only on the lifecycle tests, where the API status and the stored status can diverge if a later call fails.

WireMock stands in for the card provider. Tests do not call a real processor. Failure stubs match `orderId`, so one test can decline or time out without changing the default success stub used by the others.

Testcontainers starts `docker-compose.yml` when the services are not already healthy. If `make up` is already running, the suite uses that stack.

Retries cover connection failures only, twice, with a short backoff. A 400 or a 409 is a test failure and is not retried.

JUnit runs test classes in parallel and methods in a class on one thread. Each test uses its own user, order, and payment. Do not add a global WireMock reset; that would race.

A local `mvn -pl automation test` of the API classes took 9.5s with parallel classes and 9.2s sequential. Parallel execution did not make this suite faster.

## Repository structure

```
payguard/
  services/user-service
  services/order-service
  services/payment-service
  automation/          REST Assured clients, schemas, features, JDBC checks
  docker/              Postgres init and WireMock default stubs
  docker-compose.yml
  .github/workflows/tests.yml
```

Automation packages:

- `clients` — one Rest Assured setup, then `UserApiClient`, `OrderApiClient`, `PaymentApiClient`
- `models` — request and response records
- `fixtures` — `UserFactory`, `OrderFactory`, `PaymentFactory`, provider stubs
- `assertions` — status/error checks, JSON Schema, database status
- `database` — short JDBC helper
- `steps` — Cucumber steps for the two end-to-end features
- `support` — starts the stack and attaches the last HTTP exchange on failure

## Running locally

Java 26, Maven, and Docker are required.

```
make build
make up
make test
```

`make up` builds the jars, builds the images, and waits until the health checks pass.

Default URLs are in `.env.example`. Copy it to `.env` if you want different ports. The tests read environment variables and fall back to those defaults. Local Postgres uses trust authentication, so there is no database password. API calls send `Authorization: Bearer local`.

## Running tests

```
make test          # automation module, 34 tests
make smoke         # @smoke
make regression    # full automation module
make report        # allure serve, if the Allure CLI is installed
make down
make clean
```

Service unit tests, which do not need Docker:

```
mvn -pl services/order-service,services/payment-service test
```

## Test categories

Tags overlap. A test is counted once by Surefire and may carry more than one tag.

From a local run with the stack already up:

- service unit tests: 3
- automation tests: 34, skipped: 0

Tag counts on those 34:

| Tag | Tests |
| --- | --- |
| smoke | 4 |
| contract | 4 |
| regression | 11 |
| negative | 26 |
| e2e | 4 |
| integration | 4 |

`@smoke` is the happy path: create a user, create an order, pay, refund.
`@regression` is the broader API cases.
`@negative` is rejected input and provider failure.
`@contract` checks the JSON Schema as well as the values.
`@e2e` and `@integration` are the flows that also read PostgreSQL.

Cucumber covers two scenarios: the refund lifecycle, and a declined authorization. The same flows exist as JUnit tests. The Gherkin files are the readable business cases; the JUnit tests hold the rest.

## CI/CD

`.github/workflows/tests.yml`

Pull requests run the service unit tests, package the jars, then automation tests tagged `smoke` or `contract`.
Pushes to `main` run the unit tests and the full automation module.
Allure results are uploaded as a build artifact. The job fails when tests fail.

The workflow starts Docker itself through Testcontainers. It does not call a separate deploy step.

## Reporting

Allure 3 records the JUnit tests and the Cucumber scenarios, including the request and response captured by the REST Assured filter. On failure, the suite also attaches the recent HTTP exchanges and, when the test recorded them, the payment and order rows. The bearer token is redacted. The Allure environment file lists service URLs only.

`make report` serves `automation/target/allure-results` when the `allure` command is on the path.

## Design decisions

API tests are the primary layer because the system is three HTTP services. A browser test would not reach the payment rules.

WireMock is the provider so tests can force a decline, a 500 on capture, and a timeout without a merchant account. The default stubs in `docker/wiremock/mappings` make `docker compose up` succeed before any test adds its own stub.

Testcontainers is only the switch that runs Compose when nothing is listening. The application services are not launched with `@SpringBootTest`. That keeps one runtime for local work and CI.

Database checks exist because a payment can be `CAPTURED` in the response while the order update fails, or the other way around. Checking both is how the lifecycle test proves the order moved to `PAID` and then `REFUNDED`. Other tests stop at the HTTP response.

Retries stay narrow so a wrong status code cannot pass on a second try.

Smoke and regression are separate so a pull request can run the happy path and the schema checks without the timeout case and the rest of the negative matrix. `make regression` runs everything.

A failed provider call still returns 201. The payment row was created; `status` is `FAILED` and the order stays `CREATED`. That is the outcome the tests assert.

`AUTHORIZED` is not a status the client waits on. After a successful authorize, `authorizedAt` is stored, then capture runs in the same request. If capture fails, the status is `FAILED` and `authorizedAt` is still set.

## Known limitations

- One shared bearer token for callers and for service-to-service calls. There are no user accounts.
- If the order update fails after capture, the payment service retries it, then refunds the provider and stores the payment as `FAILED`. If that refund also fails, the payment stays `CAPTURED`.
- `X-Fail-Order-Update: true` forces the order pay call to fail. It exists so that path can be tested.
- WireMock 4 and Spring Boot 4.2 are still pre-release, so the build stays on WireMock 3.13.2 and Spring Boot 4.1.1.
