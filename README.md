# PricePulse — Product Price Drop Tracker

PricePulse is a backend REST service, built with Java and Spring Boot, for tracking product prices over time. A client registers a product with a target price, records new prices as they change, and asks the service whether the price has dropped to the target. The service can also analyze the stored price history.

There is no frontend. All APIs are demonstrated with **Postman**.

> **Status:** Stage 0 (architecture and planning). The sections below describe the **planned** design. Each feature will be implemented and verified in its own development stage (see [Development Stages](#development-stages)).

---

## Problem Statement

Online prices change frequently. A buyer who wants a product often waits for it to fall to an acceptable price. Doing that by hand has three problems:

- You have to keep checking the product page.
- You forget what the price was last week, so you can't tell whether today's "sale" is a real drop.
- There is no single record that answers questions like "what was the lowest price?" or "is the price trending up or down?"

PricePulse solves this by storing each product's **current price**, a **target price**, and a **history of recorded prices**. It answers three questions:

1. *Has this product reached my target price?*
2. *How has its price changed over time?*
3. *What are the lowest, highest and average prices, and which way is the price moving?*

**Scope note:** PricePulse does not scrape websites or call external product APIs. Prices are submitted to the API by the client (for example from Postman). This keeps the project focused on backend design: REST, persistence, relationships, validation and business logic.

---

## Main Features (Planned)

| # | Feature | Stage |
|---|---------|-------|
| 1 | Health check endpoint | 1 |
| 2 | Product persistence in PostgreSQL | 2 |
| 3 | Product CRUD REST APIs | 3 |
| 4 | Request validation and consistent JSON error responses | 4 |
| 5 | Price history recording (one-to-many relationship) | 5 |
| 6 | Price drop detection (current price vs target price) | 6 |
| 7 | Price history analysis with Streams and lambdas | 7 |
| 8 | Price trend analysis (latest vs previous price, percentage change) | 8 |

---

## Technology Stack

| Area | Technology |
|------|------------|
| Language | Java 21, SQL |
| Framework | Spring Boot 3.5 (Spring MVC, Spring Data JPA, Bean Validation) |
| ORM | Hibernate (the JPA implementation bundled with Spring Data JPA) |
| Database | PostgreSQL |
| Build tool | Maven |
| Tools | Git, Postman |

Deliberately **not** used: Lombok, Spring Security/JWT, Docker, message brokers, caches, NoSQL, frontends, scraping libraries, or external product APIs. Every class is plain Java with explicit constructors, getters and setters.

---

## Architecture

PricePulse uses a classic **layered architecture**. Each layer talks only to the layer directly below it.

```
            Postman (HTTP client)
                    │  JSON over HTTP
                    ▼
┌───────────────────────────────────────────┐
│ Controller layer   (@RestController)      │  HTTP in/out, DTOs, status codes,
│                                           │  @Valid request validation
└───────────────────────────────────────────┘
                    │  calls methods with DTOs / ids
                    ▼
┌───────────────────────────────────────────┐
│ Service layer      (@Service)             │  Business rules, transactions,
│                                           │  price-drop logic, Streams analysis,
│                                           │  entity ↔ DTO mapping
└───────────────────────────────────────────┘
                    │  calls repository methods with entities
                    ▼
┌───────────────────────────────────────────┐
│ Repository layer   (JpaRepository)        │  Persistence only (CRUD, derived
│                                           │  queries). No business logic.
└───────────────────────────────────────────┘
                    │  Hibernate generates SQL
                    ▼
               PostgreSQL
```

Exceptions thrown anywhere in the request are converted into clean JSON error responses by a single `@RestControllerAdvice` class. Clients never see stack traces.

### Layer responsibilities

| Layer | Responsible for | Must NOT do |
|-------|-----------------|-------------|
| Controller | Mapping URLs/HTTP methods, reading path variables and bodies, triggering validation, choosing HTTP status codes | Business decisions, direct repository access |
| Service | Business rules, `@Transactional` boundaries, throwing domain exceptions, mapping entities to response DTOs | Knowing about HTTP details |
| Repository | Loading and saving entities | Business rules |
| Entity | Mapping Java objects to database tables | Being returned directly as API JSON |
| DTO | Defining the API contract (request/response shapes) | Containing persistence annotations |

### Planned package structure

```
com.pricepulse
├── PricePulseApplication.java      Spring Boot entry point
├── controller                      REST controllers
├── service                         Business logic
├── repository                      Spring Data JPA repositories
├── entity                          JPA entities (Product, PriceHistory)
├── dto                             Request and response objects
├── exception                       Custom exceptions + global handler
├── config                          Configuration classes (only if needed)
└── util                            Small stateless helpers (only if needed)
```

`config` and `util` are created only when there is real code to put in them. Empty packages are not created for show.

---

## Domain Model

### Product

A product being tracked.

| Field | Java type | Notes |
|-------|-----------|-------|
| `id` | `Long` | Primary key, generated by the database |
| `name` | `String` | Required, not blank |
| `productUrl` | `String` | Required, link to the product page (stored, not scraped) |
| `currentPrice` | `BigDecimal` | Required, positive, `NUMERIC(12,2)` |
| `targetPrice` | `BigDecimal` | Required, positive, `NUMERIC(12,2)` |
| `currency` | `String` | Required, 3-letter ISO 4217 code such as `INR`, `USD`, `EUR` |
| `active` | `boolean` | Whether the product is still being tracked |
| `createdAt` | `LocalDateTime` | Set once when the product is created |
| `updatedAt` | `LocalDateTime` | Updated whenever the product changes |

### PriceHistory

One price observed for a product at a point in time.

| Field | Java type | Notes |
|-------|-----------|-------|
| `id` | `Long` | Primary key |
| `product` | `Product` | Owning side of the relationship (foreign key `product_id`) |
| `price` | `BigDecimal` | Positive, `NUMERIC(12,2)` |
| `recordedAt` | `LocalDateTime` | When the price was recorded |

### Why these types

- **`BigDecimal` for money:** `double` and `float` are binary floating-point types and cannot represent values like `0.1` exactly. That causes rounding errors in comparisons and sums. `BigDecimal` stores exact decimal values.
- **`LocalDateTime` (java.time):** an immutable, thread-safe date/time type that replaces the legacy `java.util.Date`.
- **Currency as an ISO code string:** a simple rule that is easy to validate (`^[A-Z]{3}$`). Prices are never converted between currencies, and every price for a product uses that product's currency.

### Key business rules

1. **Target reached:** `currentPrice <= targetPrice` means the target is reached. The comparison uses `BigDecimal.compareTo`, not `equals`, because `equals` also compares scale (`800.0` is not equal to `800.00`).
2. **Single source of truth for price changes:** once price history exists (Stage 5), the current price changes only by recording a new price through `POST /api/products/{id}/prices`. That endpoint saves a `PriceHistory` row **and** updates `Product.currentPrice` in one transaction, so the two can never disagree. `PUT /api/products/{id}` updates descriptive fields such as name, URL, target price, currency and active status.
3. **No notifications:** the API exposes the price-drop status. It does not send emails or push messages.

---

## Database Relationships

```
┌───────────────────────────┐           ┌───────────────────────────┐
│ products                  │           │ price_history             │
├───────────────────────────┤           ├───────────────────────────┤
│ id            BIGINT  PK  │ 1       * │ id           BIGINT  PK   │
│ name          VARCHAR     │───────────│ product_id   BIGINT  FK   │
│ product_url   VARCHAR     │           │ price        NUMERIC(12,2)│
│ current_price NUMERIC     │           │ recorded_at  TIMESTAMP    │
│ target_price  NUMERIC     │           └───────────────────────────┘
│ currency      VARCHAR(3)  │
│ active        BOOLEAN     │
│ created_at    TIMESTAMP   │
│ updated_at    TIMESTAMP   │
└───────────────────────────┘
```

- **One product → many price history rows.** In SQL the relationship exists only as the foreign key column `price_history.product_id`.
- **`PriceHistory.product` is annotated `@ManyToOne(fetch = FetchType.LAZY)` and is the owning side.** The entity that contains the foreign key column owns the relationship. `@ManyToOne` is EAGER by default in JPA, so we set LAZY explicitly to avoid loading the product every time a history row is loaded.
- **`Product.priceHistory` is annotated `@OneToMany(mappedBy = "product")` and is the inverse side.** `mappedBy` tells Hibernate that the foreign key is managed by the `product` field in `PriceHistory`. Without it, Hibernate would create an unnecessary join table. `@OneToMany` is already LAZY by default.
- **No blanket `CascadeType.ALL`.** Price history rows are saved explicitly through `PriceHistoryRepository`. Cascade options will be chosen only where a specific operation needs them, and each choice will be explained in Stage 5. Deleting a product must also deal with its history rows, because the foreign key would otherwise block the delete.
- **Entities are never serialized directly to JSON.** Doing so can cause infinite recursion (Product → history → Product → ...), lazy-loading exceptions, and accidental exposure of internal fields. Responses use DTOs.

---

## Planned REST API

Base path: `/api`

| Method | Endpoint | Purpose | Success | Errors | Stage |
|--------|----------|---------|---------|--------|-------|
| GET | `/api/ping` | Health check | 200 | — | 1 |
| POST | `/api/products` | Create a product | 201 | 400 | 3 |
| GET | `/api/products` | List products | 200 | — | 3 |
| GET | `/api/products/{id}` | Get one product | 200 | 404 | 3 |
| PUT | `/api/products/{id}` | Update a product | 200 | 400, 404 | 3 |
| DELETE | `/api/products/{id}` | Delete a product | 204 | 404 | 3 |
| POST | `/api/products/{id}/prices` | Record a new price (also updates current price) | 201 | 400, 404 | 5 |
| GET | `/api/products/{id}/prices` | Get price history | 200 | 404 | 5 |
| GET | `/api/products/{id}/price-drop` | Has the target price been reached? | 200 | 404 | 6 |
| GET | `/api/products/{id}/price-analysis` | Lowest, highest, average and count | 200 | 404 | 7 |
| GET | `/api/products/{id}/price-trend` | Latest vs previous price, direction, % change | 200 | 404 | 8 |

Price history is nested under products (`/products/{id}/prices`) because a price record has no meaning without its product.

### Example payloads (planned shapes)

Create product request:

```json
{
  "name": "Sony WH-1000XM5 Headphones",
  "productUrl": "https://example.com/sony-wh-1000xm5",
  "currentPrice": 29990.00,
  "targetPrice": 25000.00,
  "currency": "INR"
}
```

Price-drop response:

```json
{
  "productId": 1,
  "currentPrice": 24500.00,
  "targetPrice": 25000.00,
  "targetReached": true
}
```

Error response (validation or not found):

```json
{
  "status": 400,
  "message": "Current price must be positive"
}
```

Final request and response shapes will be documented once each endpoint is implemented.

---

## Where Streams and Lambdas Will Be Used

Streams are used where they naturally express **"transform or aggregate a collection"**. They are not used for simple loops or single-value logic.

| Use case | Stream operations | Stage |
|----------|-------------------|-------|
| Map list of entities → list of response DTOs | `map(...)`, `toList()` | 3, 5 |
| Collect validation error messages | `map(...)`, `collect(Collectors.joining(...))` | 4 |
| Lowest / highest recorded price | `map(PriceHistory::getPrice)`, `min/max(Comparator.naturalOrder())` → `Optional` | 7 |
| Number of recorded prices | `count()` | 7 |
| Average price (`BigDecimal`) | `map(...)`, `reduce(BigDecimal.ZERO, BigDecimal::add)` then divide | 7 |
| Recent price records | `sorted(Comparator.comparing(PriceHistory::getRecordedAt).reversed())`, `limit(n)` | 7 |
| Count of increases vs decreases | comparing consecutive prices, `filter(...)`, `count()` | 7 |
| Latest and previous price for trend | `sorted(...)`, `limit(2)`, `Optional` handling | 8 |

Lambdas and method references will appear in:

- Stream pipelines (`p -> p.getPrice()`, `PriceHistory::getPrice`, `BigDecimal::add`)
- `Comparator.comparing(...)` for sorting by timestamp
- `Optional` handling: `repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id))`

**Cases where Streams will not be used:** the single `compareTo` used for target-reached detection, and straightforward field-by-field updates. A plain `if` or setter is clearer there.

---

## Concepts Demonstrated

**Core Java:** OOP and encapsulation (entities with private fields and controlled setters), inheritance and polymorphism (custom exceptions extending `RuntimeException`), Collections (`List`, `Map`), generics (`JpaRepository<Product, Long>`, `ResponseEntity<T>`, `Optional<T>`), `Optional`, checked vs unchecked exceptions, `BigDecimal`, `LocalDateTime`, lambdas, method references, Streams.

**Spring:** IoC container, dependency injection, beans, `@SpringBootApplication`, `@RestController`, `@Service`, `@Repository`, constructor injection, `@RequestMapping` / `@GetMapping` / ..., `@PathVariable`, `@RequestBody`, `@Valid`, `@RestControllerAdvice`, `@ExceptionHandler`, `@Transactional`.

**JPA / Hibernate:** `@Entity`, `@Id`, `@GeneratedValue`, `@Column` (nullable, length, precision/scale), primary and foreign keys, `@OneToMany` / `@ManyToOne`, owning side and `mappedBy`, lazy loading, cascade trade-offs, persistence context (high level), repository abstraction, derived query methods, the N+1 problem (high level).

**REST:** resource naming, GET/POST/PUT/DELETE, status codes (200, 201, 204, 400, 404), DTOs, path variables, query parameters, consistent error handling.

---

## Development Stages

Each stage is one Git commit.

| Stage | Scope | Commit message |
|-------|-------|----------------|
| 0 | Project definition and architecture (this README) | `docs: define PricePulse architecture` |
| 1 | Spring Boot setup, package structure, `GET /api/ping` | `feat: initialize PricePulse Spring Boot project` |
| 2 | PostgreSQL connection, `Product` entity, `ProductRepository` | `feat: configure PostgreSQL and product entity` |
| 3 | Product CRUD with controller → service → repository and DTOs | `feat: add product CRUD REST APIs` |
| 4 | Bean Validation, custom exceptions, global error handler | `feat: add validation and global exception handling` |
| 5 | `PriceHistory` entity, one-to-many relationship, price endpoints | `feat: add product price history` |
| 6 | Price drop detection endpoint | `feat: add price drop detection` |
| 7 | Price history analysis with Streams and lambdas | `feat: add price history analysis with streams` |
| 8 | Price trend analysis (latest vs previous, % change) | `feat: add price trend analysis` |
| 9 | API review, cleanup, Postman collection | `refactor: clean up PricePulse API design` |
| 10 | Full verification and simple meaningful tests | `test: finalize PricePulse API verification` |
| 11 | Final documentation and portfolio review | `docs: finalize PricePulse documentation` |

---

## Running Locally

### Prerequisites

- JDK 21
- PostgreSQL (developed against PostgreSQL 18)
- No separate Maven install needed. The project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`).

### Database setup

Create the database once:

```sql
CREATE DATABASE pricepulse;
```

Tables are created by Hibernate on startup (`spring.jpa.hibernate.ddl-auto=update`). That setting is for development only.

### Configuration

Copy `.env.example` to `.env` in the project root and fill in your values:

```
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/pricepulse
DB_USERNAME=postgres
DB_PASSWORD=change_me
```

`.env` is git-ignored. Spring Boot loads it through `spring.config.import`. Real OS environment variables with the same names take precedence.

### Build and run

The build runs tests against the database, so PostgreSQL must be running.

```bash
# Windows
mvnw.cmd clean package
mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw clean package
./mvnw spring-boot:run
```

The application starts on `SERVER_PORT` (default `8080`).

### Health check

```
GET http://localhost:8080/api/ping
→ 200 OK
pong
```
