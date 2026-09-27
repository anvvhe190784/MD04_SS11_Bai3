# MD04_SS11_Bai1: Medicine Catalog Caching with Redis

A Spring Boot application demonstrating the **Cache-Aside Pattern** with **Redis** and **Spring Data JPA** for an extensive medicine catalog in a pharmacy management system.

---

## 1. System Overview & Architecture

When searching or retrieving medicine details from thousands of items, hitting the database repeatedly causes high latency and high database load. This service implements the **Cache-Aside Pattern**:

1. **Client Request**: Client queries `GET /api/v1/medicines/{id}`.
2. **Cache Check**: Spring Cache checks Redis for the key `medicines::{id}`.
3. **Cache Miss (1st Call)**:
   - System executes query to the H2 database (`MedicineRepository.findById(id)`).
   - SQL query is logged to the console.
   - Result is serialized to JSON and stored into Redis with a 10-minute TTL.
4. **Cache Hit (2nd+ Call)**:
   - Result is served directly from Redis without executing database queries.
   - Response time drops significantly to `< 10ms`.

```
Client ───> [MedicineController] ───> [MedicineService]
                                            │
                           ┌────────────────┴────────────────┐
                           │                                 │
                   (1) Check Cache                   (2) If Cache Miss
                           ▼                                 ▼
                     [Redis Cache]                    [H2 Database]
                  (medicines::{id})                   (JPA Entity)
```

---

## 2. Tech Stack

- **Java**: 21 LTS
- **Framework**: Spring Boot 4.1.1 (Spring 6 / Spring Data Redis / Spring Data JPA)
- **Cache Provider**: Redis 7 (via Lettuce client)
- **Database**: H2 In-Memory Database
- **Build Tool**: Gradle 9.7 (with Gradle Wrapper)
- **Containerization**: Docker Compose

---

## 3. Getting Started

### Prerequisites

- Java 21 JDK installed
- Docker and Docker Compose installed and running

### Step 1: Start Redis

Start the Redis server container using Docker Compose:

```bash
docker compose up -d
```

Verify Redis is running:

```bash
docker exec medicine-redis redis-cli ping
# Response: PONG
```

### Step 2: Build and Run the Application

Using Gradle Wrapper:

```bash
# On Linux/macOS
./gradlew bootRun

# On Windows
.\gradlew.bat bootRun
```

---

## 4. API Endpoints & Testing

### Initial Seed Data

The application automatically seeds 7 medicine records on startup:
- `ID 1`: Paracetamol 500mg
- `ID 2`: Panadol Extra
- `ID 3`: Amoxicillin 500mg
- `ID 4`: Ibuprofen 400mg
- `ID 5`: Cetirizine 10mg
- `ID 6`: Augmentin 1g
- `ID 7`: Berberin 100mg

### 1. Get Medicine Detail (Cached)

```http
GET /api/v1/medicines/{id}
```

**Example Request:**

```bash
curl http://localhost:8080/api/v1/medicines/1
```

**Sample Response:**

```json
{
  "id": 1,
  "name": "Paracetamol 500mg",
  "activeIngredient": "Paracetamol",
  "manufacturer": "Hau Giang Pharma",
  "unitPrice": 1500.0,
  "stockQuantity": 5000
}
```

### 2. Observing the Cache-Aside Behavior

- **Call 1 (Cold Cache)**:
  ```bash
  curl http://localhost:8080/api/v1/medicines/1
  ```
  Console Output:
  ```text
  >>> [DATABASE HIT] Cache miss for medicine id: 1. Querying database...
  Hibernate: select m1_0.id,m1_0.active_ingredient,m1_0.manufacturer,m1_0.name,m1_0.stock_quantity,m1_0.unit_price from medicines m1_0 where m1_0.id=?
  >>> [DATABASE SUCCESS] Found medicine 'Paracetamol 500mg' from database.
  ```

- **Call 2 (Warm Cache)**:
  ```bash
  curl http://localhost:8080/api/v1/medicines/1
  ```
  Console Output:
  - **No SQL statements are executed.**
  - Response time is `< 10ms`.

- **Inspect Cached Data in Redis**:
  ```bash
  docker exec -it medicine-redis redis-cli keys "*"
  # Output: 1) "medicines::1"

  docker exec -it medicine-redis redis-cli get "medicines::1"
  # Output: JSON representation of MedicineResponse
  ```

### 3. Get All Medicines

```http
GET /api/v1/medicines
```

---

## 5. Running Automated Tests

Run the integration test suite verifying cache miss and cache hit:

```bash
.\gradlew.bat test
```
