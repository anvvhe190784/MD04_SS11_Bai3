# MD04_SS11_Bai3: Asynchronous Communication with Redis Pub/Sub

A Spring Boot application demonstrating **Asynchronous Event-Driven Communication** and **Message Broker (Fire-and-forget)** using **Redis Pub/Sub** and **Spring Data Redis**.

---

## 1. System Overview & Architecture

When a pharmacy imports additional medicines into inventory, the system must immediately broadcast a notification to all active branch manager dashboards without tight coupling or synchronous blocking:
1. **Lightweight Message Broker**: Redis Pub/Sub provides low-latency, fire-and-forget message distribution across channels.
2. **Channel**: `pharmacy-alerts`
3. **Publisher (Producer)**: When medicine is imported successfully via `POST /api/v1/medicines/import`, the service serializes `PharmacyAlertMessage` to JSON using `ObjectMapper` and publishes it to `pharmacy-alerts`:
   ```json
   {
     "type": "IMPORT",
     "message": "Đã nhập 100 hộp Panadol"
   }
   ```
4. **Subscriber (Consumer)**: A Spring `@Service` implementing `MessageListener` registered in `RedisMessageListenerContainer` that listens to `pharmacy-alerts`, deserializes JSON with `ObjectMapper`, and logs the notification to the console in real time.

---

## 2. Tech Stack

- **Java**: 21 LTS
- **Framework**: Spring Boot 4.1.1 (Spring Data Redis, Spring Data JPA, Spring Web MVC)
- **Message Broker & Cache**: Redis 7 (via Lettuce client)
- **Serialization**: Jackson `ObjectMapper` (with JavaTimeModule)
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

Verify Redis is healthy:

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

## 4. API Endpoints & Verification

### 1. Import Medicine (Triggers Redis Pub/Sub Broadcast)

```bash
curl -X POST http://localhost:8080/api/v1/medicines/import \
  -H "Content-Type: application/json" \
  -d '{
    "medicineId": 1,
    "quantity": 100
  }'
```

**Expected Console Log on Subscriber**:
```text
================================================================================
[REDIS SUBSCRIBER] Received message from channel 'pharmacy-alerts':
Type: IMPORT
Message: Đã nhập 100 hộp Paracetamol 500mg
Raw JSON: {"type":"IMPORT","message":"Đã nhập 100 hộp Paracetamol 500mg"}
================================================================================
```

### 2. Publish Custom Alert Directly

```bash
curl -X POST http://localhost:8080/api/v1/medicines/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "type": "IMPORT",
    "message": "Đã nhập 100 hộp Panadol"
  }'
```

**Expected Console Log on Subscriber**:
```text
================================================================================
[REDIS SUBSCRIBER] Received message from channel 'pharmacy-alerts':
Type: IMPORT
Message: Đã nhập 100 hộp Panadol
Raw JSON: {"type":"IMPORT","message":"Đã nhập 100 hộp Panadol"}
================================================================================
```

### 3. Listen to Redis Channel via Redis CLI

You can also attach a Redis CLI subscriber directly:

```bash
docker exec -it medicine-redis redis-cli SUBSCRIBE pharmacy-alerts
```

---

## 5. Automated Testing

Run the test suite verifying Cache aside, TTL eviction, and asynchronous Redis Pub/Sub message delivery:

```bash
.\gradlew.bat test
```
