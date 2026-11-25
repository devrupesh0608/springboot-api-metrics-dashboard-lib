# API Metric Library

Add this library to a Servlet Spring MVC application and it records which APIs exist, how often they are called, and how long they take. It stores that data in its own MySQL database and serves a dashboard. A monitoring failure does not change the response your application returns to its callers.

The library is written in Kotlin and runs on Java 17. Your application can stay Java or Kotlin.

## What you need

- Java 17 or later
- A Servlet Spring MVC application (Spring Web). WebFlux is not supported.
- MySQL 8, or a compatible MariaDB, with a database used only for monitoring
- Spring Boot 4.2. The published starter is built against Spring Boot 4.2.0.

This library creates its own connection pool. Do not add `spring-boot-starter-jdbc` or `spring-boot-starter-flyway` for it, and do not point it at your application's DataSource. Those would let monitoring migrations or pool settings affect the business database.

## 1. Add the dependency

The starter is on Maven Central. Add it next to your existing Spring Web dependency. Do not add another Maven repository.

Gradle Kotlin:

```kotlin
dependencies {
    implementation("io.github.devrupesh0608:api-metric-spring-boot-starter:1.0.0")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
}
```

Gradle Groovy:

```groovy
dependencies {
    implementation 'io.github.devrupesh0608:api-metric-spring-boot-starter:1.0.0'
    implementation 'org.springframework.boot:spring-boot-starter-webmvc'
}
```

Maven, when the project already imports the Spring Boot BOM:

```xml
<dependency>
    <groupId>io.github.devrupesh0608</groupId>
    <artifactId>api-metric-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

`spring-boot-starter-webmvc` is required because capture uses the Servlet filter chain and Spring MVC handler mappings. The starter does not bring that stack in for you. Your Spring Boot plugin or parent supplies the webmvc version. The starter version is `1.0.0`.

That one starter dependency also brings the auto-configuration, the MySQL storage module, and the MySQL driver. You do not declare those modules yourself.

## 2. Create the monitoring database

Create an empty database before the first startup. The library creates its own tables. It does not create the database itself.

```sql
CREATE DATABASE api_metrics CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Use a dedicated user when you can. The sample on this machine uses the local XAMPP root account only so it can be started without extra setup.

## 3. Turn the library on

Add this to `application.yml`. Monitoring stays off until `api-metric.enabled` is `true`.

```yaml
api-metric:
  enabled: true
  dashboard:
    enabled: true
    path: /api-metric-dashboard
  internal-path: /api-metric/internal
  storage:
    mysql:
      url: jdbc:mysql://localhost:3306/api_metrics?characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
      username: api_metrics
      password: ${API_METRIC_DB_PASSWORD}
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout-ms: 3000
  capture:
    request-payload: true
    response-payload: true
    capture-headers: false
    max-payload-size: 10240
    excluded-paths:
      - /actuator/**
  security:
    enabled: true
    allow-unauthenticated: false
    username: admin
    password: ${API_METRIC_DASHBOARD_PASSWORD}
  analytics:
    unused-api-threshold-days: 30
  retention:
    request-logs-days: 30
```

Set `API_METRIC_DB_PASSWORD` in the environment. The password property must be present. An empty password is valid for a local database that has no password. A missing password stops startup. The library does not fall back to memory.

`security.username` and `security.password` turn on the browser username and password dialog for the dashboard and the internal JSON API. Keep `allow-unauthenticated` at `false` when you set them. The password can come from an environment variable. Business APIs are not asked for this password. The library does not register a Spring Security filter chain.

`security.allow-unauthenticated: true` leaves the dashboard and the internal JSON API open. Use that only on a machine that is not reachable from outside. If the username or password is missing and `allow-unauthenticated` is `false`, those paths stay hidden. Setting only one of the two login values stops startup.

## 4. Start the application

Start the host application the way you usually do. On startup the library:

1. Opens its own MySQL pool.
2. Creates or updates the monitoring tables.
3. Scans your `@RestController` and `@Controller` routes.
4. Starts a background writer. Request threads only enqueue a small event. If that queue is full, the event is dropped and the business response is unchanged.

Call any of your APIs, then open:

```text
http://localhost:8080/api-metric-dashboard
```

Use your application's port. The sample in this repository listens on port 8088.

The dashboard has three pages:

- **Dashboard** shows total hits, discovered routes, unhit routes, the latest calls, and never-used routes.
- **All Requests** lists captured calls. Filter by method, status group, or time, and open one call to see the body and headers.
- **API Endpoints** shows each route, hits in the selected time range, average response time, and last hit.

An API is identified by its HTTP method and route template, for example `GET /orders/{id}`. A concrete id such as `/orders/15` is stored as that template, not as a separate API.

## 5. Check that capture is working

Call one of your endpoints, then request the inventory:

```bash
curl -u admin:$API_METRIC_DASHBOARD_PASSWORD http://localhost:8080/api-metric/internal/apis
```

You should see the route with `totalHits` greater than zero after the background writer flushes, which is normally within about a second. Recent calls are at:

```text
GET /api-metric/internal/requests?size=20
GET /api-metric/internal/overview
```

Use the same username and password on those calls. When `allow-unauthenticated` is `false` and no login is configured, these paths stay hidden.

## What is stored

Request and response bodies are stored only when `capture.request-payload` and `capture.response-payload` are true. Bodies are cut at `max-payload-size` bytes. Multipart and `application/octet-stream` bodies are skipped. Paths in `excluded-paths` are not captured. The dashboard path and the internal API are not recorded as business traffic.

Header capture is off unless `capture.capture-headers` is true. `password`, `token`, `accessToken`, `refreshToken`, `authorization`, and `secret` are replaced with `***` in JSON bodies. `Cookie` and `Set-Cookie` are always masked when headers are captured. Add more names with `capture.mask-fields`.

Routes with `totalHits` of zero and no last access are never-used. A route whose last access is older than `analytics.unused-api-threshold-days` is an unused candidate. Request rows older than `retention.request-logs-days` are deleted in the background. Hit totals on each API are kept.

## Run the included sample

The sample is a small MVC application with item, order, and versioned product APIs.

```bash
./gradlew :api-metric-sample-application:bootRun
```

Its configuration is `api-metric-sample-application/src/main/resources/application.yml`. On this workspace that file uses database `api-metrics` on `localhost:3306` and the local root account. The sample dashboard login is username `admin` and password `admin`. That password is only for this local sample. The dashboard is:

```text
http://localhost:8088/api-metric-dashboard
```

Try the versioned product API:

```bash
curl http://localhost:8088/api/v1/products
curl -X POST http://localhost:8088/api/v2/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Bag","sku":"BG-9","price":40}'
```

`/api/v1/products` returns id, name, and price. `/api/v2/products` also returns sku, and create or update returns 400 when sku is missing.

## Build and test

```bash
./gradlew check
```

Unit tests do not need MySQL. The sample context test sets `api-metric.enabled=false` so it can start without a database. `bootRun` enables monitoring and does need the database from the sample yaml.

## Modules

| Module | Role |
| --- | --- |
| `api-metric-core` | API identity, usage classification, and storage contracts |
| `api-metric-storage-mysql` | Dedicated pool, schema, and MySQL repositories |
| `api-metric-spring-boot-autoconfigure` | Capture, discovery, internal API, and dashboard |
| `api-metric-spring-boot-starter` | The dependency your application should add |
| `api-metric-sample-application` | Runnable example |

## If startup says the schema is inconsistent

Some local MariaDB installs have a broken `mysql.proc` catalog. Flyway then cannot check whether the schema is empty. The library logs a warning and applies the monitoring SQL itself. Capture and the dashboard still work. Repairing that catalog with `mysql_upgrade`, run as the database server user, lets a later startup use Flyway normally.
