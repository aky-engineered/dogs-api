# dogs-api

A RESTful API for managing the dogs registered with a police force, along with the lookup data they reference (breeds, suppliers, statuses and leaving reasons).

## Tech Stack

- Java 21, Spring Boot 4.1
- Spring Data JPA (Hibernate) with MySQL 8
- Liquibase for schema migrations and reference data
- MapStruct for entity ↔ DTO mapping
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, MockMvc and Testcontainers

## Prerequisites

- Java 21
- Docker Desktop (running)

Maven does not need to be installed. The Maven wrapper (`./mvnw`) is included.

## How to Run

```bash
./mvnw spring-boot:run
```

The application starts on http://localhost:8080.

MySQL is started automatically from `compose.yaml` via `spring-boot-docker-compose`, and Liquibase creates the schema and seeds the reference data on startup. To manage the database yourself:

```bash
docker compose up -d       # start MySQL
docker compose down        # stop, keeping data
docker compose down -v     # stop and wipe all data
```

## Swagger Docs

Swagger UI is available at http://localhost:8080/swagger-ui.html. Every endpoint can be exercised from there.

## Running the Tests

```bash
./mvnw test
```

Docker must be running. The repository and integration tests start a real MySQL container with Testcontainers and apply the Liquibase migrations to it.

## Endpoints

All endpoints sit under `/api/dogs`, consume and produce `application/json`, and require no authorisation.

| Resource        | Base path                   | Operations                                      |
|:----------------|:----------------------------|:------------------------------------------------|
| Dogs            | `/api/dogs/dogs`            | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| Breeds          | `/api/dogs/breeds`          | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| Suppliers       | `/api/dogs/suppliers`       | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| Statuses        | `/api/dogs/statuses`        | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |
| Leaving Reasons | `/api/dogs/leaving-reasons` | `POST /`, `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` |

### List query parameters

| Parameter        | Applies to | Default | Description |
|:-----------------|:-----------|:--------|:------------|
| `page`           | All lists  | `0`     | Zero-based page number |
| `size`           | All lists  | `20`    | Page size (max `100`) |
| `sort`           | All lists  |         | e.g. `sort=name,desc` |
| `includeDeleted` | All lists  | `false` | Include soft-deleted records |
| `filter`         | Dogs only  |         | JSON filter on `name`, `breed` and `supplier` |

List responses use a consistent page envelope:

```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
```

### Examples

Create a dog:

```bash
curl -X POST http://localhost:8080/api/dogs/dogs \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Rex",
        "breedId": 1,
        "supplierId": 1,
        "badgeId": "PD1001",
        "gender": "MALE",
        "birthDate": "2020-04-12",
        "dateAcquired": "2021-01-05",
        "currentStatus": "IN_TRAINING",
        "kennellingCharacteristic": "Reactive to other males"
      }'
```

Filter dogs (the JSON must be URL-encoded):

```bash
curl -G http://localhost:8080/api/dogs/dogs \
  --data-urlencode 'filter={"name": "rex", "breed": "shepherd"}' \
  --data-urlencode 'sort=name'
```

Create a status:

```bash
curl -X POST http://localhost:8080/api/dogs/statuses \
  -H "Content-Type: application/json" \
  -d '{"code": "ON_LOAN", "name": "On Loan"}'
```

### Seeded reference data

| Statuses                    | Leaving Reasons                            |
|:----------------------------|:-------------------------------------------|
| `IN_TRAINING` - In Training | `TRANSFERRED` - Transferred                |
| `IN_SERVICE` - In Service   | `RETIRED_PUT_DOWN` - Retired (Put Down)    |
| `RETIRED` - Retired         | `KIA` - KIA                                |
| `LEFT` - Left               | `REJECTED` - Rejected                      |
|                             | `RETIRED_REHOUSED` - Retired (Re-housed)   |
|                             | `DIED` - Died                              |

## Design Decisions

### Data model

| Dog attribute             | Modelled as |
|:--------------------------|:------------|
| Breed                     | Lookup table, referenced by `breedId` |
| Supplier                  | Lookup table, referenced by `supplierId` (many dogs → one supplier) |
| Current Status            | Lookup table, referenced by `code` (required) |
| Leaving Reason            | Lookup table, referenced by `code` (optional) |
| Gender                    | Java enum: `MALE`, `FEMALE`, `UNKNOWN` (default) |
| Kennelling Characteristic | Free text |
| Badge Id                  | Unique string |

- **Status and leaving reason are database tables, not enums.** The spec describes their values as "*currently* possible", so they need to change without a redeploy, which is what the extra CRUD endpoints are for. Gender has no such wording, so it stays a fixed enum.
- **Breed and supplier are lookup tables** rather than free text, which keeps the list controlled (no "Alsatian" vs "German Shepherd" drift). The spec also states that suppliers are shared across dogs.
- **Status and leaving reason are referenced by code** (`"currentStatus": "IN_TRAINING"`), while breed and supplier use IDs. A code is a stable identifier that clients can safely send. It is set on creation and cannot be changed afterwards, whereas the `name` is an editable display label.

### Soft delete

Nothing is ever removed from the database, to support auditing.

- Every table has a nullable `deleted_at` timestamp. `DELETE` sets it rather than removing the row.
- Lists exclude deleted records by default, and `?includeDeleted=true` includes them. `GET /{id}` returns `404` for a deleted record.
- A deleted lookup value (e.g. a retired status) disappears from its own list and cannot be assigned to a dog, but dogs that already reference it still display it and can still be updated with it.

### Filtering and sorting

- `filter` is a JSON object with optional `name`, `breed` and `supplier` keys. Each is a case-insensitive "contains" match, all supplied fields must match, and unknown keys are rejected with `400` so a typo can't silently return every dog.
- The search is a single JPQL query in `DogRepository` where a `null` parameter switches that condition off. Filtering, paging and sorting all happen in the database.
- Because the query is hand-written, sort fields are checked against an allow-list in `DogService`.

### Requests and responses

- `PUT` is a full replacement: any field omitted from the body is cleared.
- Lookup IDs and codes are validated when a dog is saved, and an unknown or deleted value returns `400`.
- Errors use the RFC 7807 `ProblemDetail` format:

| Status | When |
|:-------|:-----|
| `400`  | Validation failure (field errors under `errors`), malformed JSON or filter, unknown lookup reference, invalid sort field |
| `404`  | Record not found or soft-deleted |
| `409`  | Duplicate unique value (lookup name or code, badge ID) |

## Project Structure

```
src/main/java/com/dogs/api
├── config/       OpenAPI config and the filter query-param converter
├── controller/   REST controllers (one per resource)
├── dto/          Request/response records
├── exception/    Custom exceptions and the global exception handler
├── mapper/       MapStruct mappers
├── model/        JPA entities (all extend BaseEntity)
├── repository/   Spring Data repositories
└── service/      Business logic and transactions
src/main/resources/db/changelog   Liquibase migrations
```

## Areas of Improvement

With more time, I would:

**Testing**
- Cover more error paths: `404` on update and delete, deleting a record twice, and `409` for duplicate names, codes and badge IDs.
- Cover more validation edge cases: future dates, invalid gender values, lower-case codes, unknown filter keys and blank filters.
- Test the lookup code rules fully: sending a code to breeds and suppliers, changing a code, and creating a leaving reason without one.
- Add more dog scenarios: unknown breed IDs and leaving-reason codes, assigning a deleted status, changing a dog's status, and dogs without a breed or supplier.
- Test pagination and sorting more thoroughly: later pages, page totals, `includeDeleted=true` and invalid sort fields end to end.

**Functionality**
- Enforce business rules between fields, e.g. a leaving date and reason should only be set when the status is Left or Retired, and the leaving date should not be before the acquired date.
- Record who made each change (`created_by`, `deleted_by`) once authentication exists. Authentication was out of scope for this task.
- Add optimistic locking (`@Version`) so concurrent updates cannot overwrite each other.
- Add `PATCH` for partial updates alongside the full-replacement `PUT`.
- Split the lookup request into separate DTOs for coded and non-coded lookups, so validation can be declared on the DTOs and Swagger only shows `code` where it applies.
