# PalletNow Order Management Service

A Spring Boot REST API for managing customers, products, orders, inventory, and customer-order reports. The implementation follows the requirements in **Assignment - Java FullTime** and exposes the same requests as the supplied Postman collection.

## Requirement alignment

| Assignment requirement | Implementation |
| --- | --- |
| Customer management | `POST /api/customers`, `PUT /api/customers/{id}`, and read endpoints for one or many customers |
| Required and unique customer fields | Bean Validation validates `name`, `email`, and `phone`; service checks duplicates and the database also enforces unique constraints |
| Product management | `POST /api/products`, `PUT /api/products/{id}`, and read endpoints for one or many products |
| Product price and non-negative stock | `BigDecimal` is used for price and `@PositiveOrZero` validates price and stock |
| Place an order | `POST /api/orders` accepts a customer and one or more product lines |
| Stock validation and deduction | The order service locks products for update, validates the complete request, deducts stock, and saves the order in one transaction |
| Fetch orders by customer | `GET /api/orders/customer/{customerId}` |
| Calculate order total | The response contains line totals and `totalAmount`; order items retain product name and unit price snapshots |
| Customer order reporting | `GET /api/reports/orders-by-customer` returns every customer and order count, including customers with zero orders |
| Top five customers | `GET /api/reports/top-customers` returns up to five customers ordered by order count descending |
| Multi-layer architecture and exception handling | Controllers, services, repositories, DTOs, entities, and a `@RestControllerAdvice` exception handler are separated by responsibility |

## Technology and architecture

- Java 21
- Spring Boot 4.1.1
- Spring MVC for REST endpoints
- Spring Data JPA/Hibernate for persistence
- PostgreSQL for local runtime
- Jakarta Bean Validation
- Lombok for boilerplate reduction

The request flow is:

```text
HTTP request
    -> Controller (routing, validation, HTTP status)
    -> Service (business rules and transactions)
    -> Repository (JPA/database access)
    -> Entity/database
```

DTO records keep the API contract separate from persistence entities. `OrderItem` stores the product name and unit price at order time, so historical orders remain meaningful even if the product is later renamed or repriced.

## Technologies and concepts used

### Spring Boot

Spring Boot provides the application foundation, auto-configuration, embedded server support, dependency management, and a production-ready way to run the REST service without extensive XML configuration.

### Spring MVC

Spring MVC handles HTTP requests and maps them to controller methods using annotations such as `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, and `@PutMapping`. It also converts JSON request bodies into Java DTOs and serializes response objects back to JSON.

### Spring Data JPA

Spring Data JPA provides repository abstractions for database access. Interfaces such as `JpaRepository` provide common operations including create, read, update, delete, and pagination without handwritten CRUD SQL. Repository method names such as `findByCustomerId` are translated into queries automatically.

The project also uses a custom grouped query for customer order reporting. This combines a `LEFT JOIN`, aggregation, and a count query so the report can be paginated while still including customers who have placed zero orders.

### JPA and Hibernate

Jakarta Persistence API (JPA) defines the object-relational mapping between Java classes and relational database tables. Hibernate is the JPA implementation used by Spring Boot. Entity annotations such as `@Entity`, `@Id`, `@ManyToOne`, and `@OneToMany` describe the customer, product, order, and order-item relationships.

Hibernate also manages entity state and generates database operations. The project uses lazy relationships where appropriate, disables Open EntityManager in View, and stores order timestamps as UTC `Instant` values.

### Spring transactions

`@Transactional` defines an atomic business operation. Placing an order validates all requested products, locks product rows for update, deducts stock, creates order items, and persists the order as one unit. If validation fails, the transaction does not leave a partially placed order or partially deducted inventory.

### Jakarta Bean Validation

Bean Validation annotations such as `@NotBlank`, `@NotNull`, `@Email`, `@Positive`, `@PositiveOrZero`, and `@NotEmpty` enforce request rules before service processing. The global exception handler converts validation failures into a consistent `400 Bad Request` response.

### Lombok

Lombok reduces repetitive Java boilerplate at compile time. This project uses annotations such as:

- `@Getter` to generate accessor methods
- `@Builder` to provide readable entity construction
- `@NoArgsConstructor` to provide the constructor required by JPA
- `@RequiredArgsConstructor` to generate constructors for required service dependencies

Lombok keeps the domain and service classes focused on behavior while preserving normal compiled Java methods.

### Java records and DTOs

Request and response DTOs use Java records for compact, immutable API contracts. They prevent controllers from exposing persistence entities directly and make the expected request and response fields explicit.

### PostgreSQL

PostgreSQL is the configured local runtime database. It provides durable relational storage, unique constraints for customer email and phone, foreign-key relationships, numeric support for `BigDecimal` prices, and row-level locking needed for safe concurrent inventory updates.

### Maven

Maven manages project dependencies, compilation, packaging, and the Spring Boot application plugin. Dependencies and Java version are declared in `pom.xml`, making the project reproducible across development environments.

### BigDecimal and UTC timestamps

Product prices, unit prices, line totals, and order totals use `BigDecimal` to avoid floating-point rounding problems in financial calculations. Order creation times use `Instant`, with Hibernate configured for UTC JDBC handling so timestamps are consistent across environments.

## Run locally

### Prerequisites

- JDK 21
- PostgreSQL
- A database named `palletnow_order_management`

Create the database, then configure the PostgreSQL username and password in `src/main/resources/application-local.properties` (or supply equivalent Spring datasource properties through environment variables or command-line arguments). Do not commit real credentials.

### Start the application

From the repository root:

```bash
./mvnw spring-boot:run
```

The application listens on `http://localhost:8080`. The checked-in default configuration activates the `local` profile. To select a profile explicitly:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

To build and run the packaged application:

```bash
./mvnw clean package
java -jar target/orderManagementSystem-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## Spring profiles and configuration

`src/main/resources/application.properties` contains shared settings:

- Application name: `orderManagementSystem`
- Active profile: `local`
- `spring.jpa.hibernate.ddl-auto=update` for local development
- UTC Hibernate JDBC timezone
- Open EntityManager in View disabled
- Maximum pageable size of 100

`application-local.properties` contains the local PostgreSQL connection. The profile-specific file is loaded on top of the shared properties when `local` is active.

In a deployment environment, the same profile mechanism can be used with externalized datasource credentials and a migration-managed schema rather than relying on `ddl-auto=update`.

## API conventions

### Successful responses

- Create customer, product, or order: `201 Created`
- Update and fetch operations: `200 OK`
- Collection endpoints return a consistent `PageResponse` envelope.

### Error responses

Errors use this shape:

```json
{
  "timestamp": "2026-09-23T04:28:30.000Z",
  "status": 409,
  "message": "Email is already in use"
}
```

The exception handler maps:

| Situation | Status |
| --- | --- |
| Missing customer or product | `404 Not Found` |
| Duplicate customer email or phone | `409 Conflict` |
| Insufficient stock | `409 Conflict` |
| Invalid or missing request fields | `400 Bad Request` |

## Exception handling

The application uses custom runtime exceptions for business failures and a centralized `@RestControllerAdvice` so every error is returned in a consistent API format. Controllers and services do not need to repeat HTTP response-building logic for each failure case.

### Custom exceptions

- `ResourceNotFoundException` is thrown when a requested customer or product does not exist. It is mapped to `404 Not Found`.
- `ConflictException` is thrown when a customer email or phone number is already in use. It is mapped to `409 Conflict`.
- `InsufficientStockException` is thrown when an order requests more inventory than is available. It is also mapped to `409 Conflict`.

These exceptions are raised from the service layer, where the business rule is known. For example, `CustomerService` checks duplicate identity fields and `OrderService` validates stock before changing inventory.

### Global controller advice

`GlobalExceptionHandler` is annotated with `@RestControllerAdvice`, which allows it to handle exceptions from all REST controllers in one place:

1. The controller delegates the request to the service layer.
2. The service either completes the operation or throws a domain exception.
3. Spring routes the exception to the matching handler method in `GlobalExceptionHandler`.
4. The handler creates an `ApiError` response containing the timestamp, HTTP status, and readable message.

Request validation failures are handled separately through `MethodArgumentNotValidException`. When a request DTO violates annotations such as `@NotBlank`, `@Email`, `@Positive`, or `@NotEmpty`, the handler collects the invalid field messages and returns `400 Bad Request`.

This approach keeps error behavior predictable for API consumers, avoids leaking stack traces or persistence details, and keeps business logic separate from HTTP concerns.

## Pagination

Customer, product, customer-order, and orders-by-customer endpoints accept Spring Data pagination parameters:

- `page`: zero-based page number; the first page is `page=0`
- `size`: number of records requested
- `sort`: optional property and direction, for example `sort=name,asc`

The default sizes and ordering are:

| Endpoint | Default size | Default sort |
| --- | ---: | --- |
| Customers | 20 | `id,asc` |
| Products | 20 | `id,asc` |
| Orders for a customer | 20 | `createdAt,desc` |
| Orders by customer report | 20 | `customerId,asc` |

The application caps requested page size at 100. A paginated response looks like:

```json
{
  "data": [],
  "page": 0,
  "size": 30,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

Use the next zero-based page until `last` is `true`. For example:

```bash
curl "http://localhost:8080/api/products?page=1&size=10&sort=name,asc"
```

`top-customers` is deliberately not paginated because its contract is a bounded top-five result.

## Complete cURL examples

The following commands are equivalent to the requests in `palletnow Order management.postman_collection.json`. Run the create commands first and replace the example IDs with IDs returned by the API where necessary.

### Customers

#### Add customer

```bash
curl --request POST "http://localhost:8080/api/customers" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Reeshu raj",
    "email": "reeshu.raj@example.com",
    "phone": "+911234567890"
  }'
```

#### Get all customers

```bash
curl "http://localhost:8080/api/customers?page=0&size=30"
```

#### Get customer by ID

```bash
curl "http://localhost:8080/api/customers/1"
```

#### Update customer

```bash
curl --request PUT "http://localhost:8080/api/customers/1" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "John Updated",
    "email": "john.updated@example.com",
    "phone": "8978909090"
  }'
```

### Products

#### Add product

```bash
curl --request POST "http://localhost:8080/api/products" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Laptop",
    "price": 999.99,
    "stock": 25
  }'
```

#### Update product

```bash
curl --request PUT "http://localhost:8080/api/products/1" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "Updated Laptop",
    "price": 1099.99,
    "stock": 30
  }'
```

#### Get all products

```bash
curl "http://localhost:8080/api/products?page=0&size=30"
```

#### Get product by ID

```bash
curl "http://localhost:8080/api/products/1"
```

### Orders

#### Place a new order

```bash
curl --request POST "http://localhost:8080/api/orders" \
  --header "Content-Type: application/json" \
  --data '{
    "customerId": 1,
    "items": [
      {
        "productId": 2,
        "quantity": 5
      }
    ]
  }'
```

#### Get orders by customer

```bash
curl "http://localhost:8080/api/orders/customer/2?page=0&size=30"
```

### Reporting

#### Orders by customer

```bash
curl "http://localhost:8080/api/reports/orders-by-customer?page=0&size=30"
```

#### Top five customers

```bash
curl "http://localhost:8080/api/reports/top-customers"
```

## Important business scenarios

### Successful order placement

The service first verifies the customer, groups repeated product lines by product ID, locks and loads each product, validates the aggregated quantity against stock, deducts stock, creates order items, and commits the order. If any product fails validation, the transaction fails without saving a partial order or partial stock deduction.

### Repeated product lines

If a request contains the same `productId` more than once, quantities are summed before stock validation. For example, quantities `2` and `3` require five units, not two separate stock checks.

### Insufficient stock

An order requiring more inventory than available returns `409 Conflict` with an explanatory message. Stock is not reduced.

### Duplicate customer identity fields

Creating a customer with an existing email or phone, or updating one customer to another customer's email or phone, returns `409 Conflict`. Both service-level checks and database unique constraints protect this rule.

### Missing resources

An unknown customer or product returns `404 Not Found`. Looking up orders for an unknown customer also validates the customer first and returns `404 Not Found`.

### Invalid input

Blank names, malformed email addresses, missing required fields, negative price or stock, empty order items, missing IDs, and non-positive quantities return `400 Bad Request` with the invalid field names in the message.

## Features that strengthen the implementation

- **Transactional inventory integrity:** order placement is `@Transactional`; product rows are fetched for update so concurrent orders cannot safely oversell the same stock.
- **Stable financial calculations:** prices and totals use `BigDecimal`, not floating-point values.
- **Historical order accuracy:** order items snapshot product name and unit price at the time of purchase.
- **Consistent API errors:** one `@RestControllerAdvice` translates validation and domain exceptions into predictable JSON responses.
- **Database-level integrity:** required columns, foreign keys, and unique customer email/phone constraints complement application validation.
- **Efficient report queries:** customer order counts use a grouped repository query with a count query suitable for pagination and a left join that includes customers with no orders.
- **Clean separation of concerns:** controllers do not contain persistence or business logic; services own workflows and repositories own data access.
- **Operationally safer defaults:** open-in-view is disabled, pageable requests are capped at 100, and timestamps are stored as `Instant` with UTC JDBC configuration.

## Project layout

```text
src/main/java/com/palletnow/orderManagementSystem/
├── controller/     REST endpoints
├── service/        Business workflows and transactions
├── repository/     Spring Data JPA repositories and report query
├── entity/         Persistence model
├── dto/            Request, response, and pagination contracts
└── exception/      Domain exceptions and global error mapping
```

The supplied Postman collection can be imported directly into Postman for interactive execution; the cURL commands above are its command-line equivalent.
