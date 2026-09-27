# MD04_SS11_Bai2: Data Synchronization and TTL (Time-To-Live) with Redis Cache

A Spring Boot application demonstrating **Data Synchronization**, **TTL (Time-To-Live = 60 minutes)**, and **Cache Eviction (`@CacheEvict`)** with **Redis** and **Spring Data JPA**.

---

## 1. System Overview & Architecture

When medicine prices or details change, stale cache entries can mislead users with old pricing. This project implements:
1. **Cache TTL (60 minutes)**: Ensures entries expire automatically if untouched.
2. **Cache-Aside Pattern (`@Cacheable`)**: Fast retrieval from Redis for read queries.
3. **Cache Eviction (`@CacheEvict`)**: When medicine information or price is updated, the old cache entry (`medicines::{id}`) is invalidated immediately so the subsequent query fetches fresh data from the database.

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

```bash
docker compose up -d
```

Verify Redis is running:

```bash
docker exec medicine-redis redis-cli ping
# Response: PONG
```

### Step 2: Build and Run the Application

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

---

## 4. Demonstrating Data Synchronization & Cache Eviction (4 Steps)

### Step 1: Query Medicine ID=1 (Cache Miss)
```bash
curl http://localhost:8080/api/v1/medicines/1
```
- Console Output shows database query:
  ```text
  >>> [DATABASE HIT] Cache miss for medicine id: 1. Querying database...
  Hibernate: select ... from medicines where id=?
  >>> [DATABASE SUCCESS] Found medicine 'Paracetamol 500mg' from database.
  ```

### Step 2: Query Medicine ID=1 Again (Cache Hit)
```bash
curl http://localhost:8080/api/v1/medicines/1
```
- Served directly from Redis (`< 10ms`).
- **No SQL statements executed.**

### Step 3: Update Medicine Price (Cache Evict)
```bash
curl -X PUT http://localhost:8080/api/v1/medicines/1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Paracetamol 500mg",
    "activeIngredient": "Paracetamol",
    "manufacturer": "Hau Giang Pharma",
    "unitPrice": 3500.0,
    "stockQuantity": 5000
  }'
```
- Log confirms eviction:
  ```text
  >>> [CACHE EVICT] Evicting cache for medicine id: 1. Updating database...
  Hibernate: update medicines set ... unit_price=? where id=?
  ```
- Verify key removal in Redis:
  ```bash
  docker exec medicine-redis redis-cli keys "*"
  # medicines::1 is removed immediately!
  ```

### Step 4: Query Medicine ID=1 Again (Cache Miss & Refresh)
```bash
curl http://localhost:8080/api/v1/medicines/1
```
- SQL query executed again to fetch the updated price (`3500.0`).
- Fresh data is stored back into Redis with a 60-minute TTL.

---

## 5. Running Automated Tests

Run the integration test suite verifying both Cache Hit and Cache Eviction:

```bash
.\gradlew.bat test
```
