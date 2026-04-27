# Inventory Management System (IMS)

Production-grade **Java 17 + Spring Boot 3 + MongoDB + Angular 17** full-stack application with Redis caching, Kafka event streaming, JWT auth, Swagger UI, and comprehensive Postman collection.

---

> Implementation note (April 27, 2026): the IMS was refactored for vendor-specific product pricing, purchase-order lifecycle (`CREATED -> ACCEPTED -> SHIPPED -> RECEIVED`), inventory receive/sell flow, and vendor portal APIs.  
> Migration guide: [`docs/migrations/2026-04-27-vendor-order-refactor.md`](docs/migrations/2026-04-27-vendor-order-refactor.md)
>
> Multi-tenant + scale guide: [`docs/multitenant-scale-notes.md`](docs/multitenant-scale-notes.md)

---

## 🚀 What's New

- ✅ **MongoDB** replaces MySQL — document-based storage with atomic `findAndModify` for stock reservation
- ✅ **Angular 17 Frontend** — standalone components, signals-ready, responsive UI
- ✅ **Swagger UI** — interactive API docs at `/swagger-ui.html`
- ✅ **Postman Collection** — 30+ requests with auto-token management
- ✅ **application.properties** — replaces YAML for simpler config
- ✅ **Many-to-many Product ↔ Vendor supplies** with vendor-specific pricing
- ✅ **Purchase order lifecycle** with vendor actions (`accept`, `ship`) and warehouse `receive`
- ✅ **Inventory sales flow** to reduce stock on customer sale and track sold units

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   Angular Frontend (Port 4200)                  │
│  Dashboard | Products | Inventory | Orders | Vendors            │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP + JWT
┌───────────────────────────▼─────────────────────────────────────┐
│              Spring Boot Backend (Port 8081)                    │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐    │
│  │  JWT Filter  │   │ Rate Limiter │   │ Swagger UI       │    │
│  └──────┬───────┘   └──────────────┘   └──────────────────┘    │
│         │                                                       │
│  ┌──────▼──────────────────────────────────────────────────┐   │
│  │              REST Controllers                           │   │
│  │  Auth | Vendor | Warehouse | Product | Inventory | Order│   │
│  └──────┬──────────────────────────────────────────────────┘   │
│         │                                                       │
│  ┌──────▼──────────────────────────────────────────────────┐   │
│  │                   Services                              │   │
│  │  @Cacheable/@CacheEvict  │  @Transactional              │   │
│  └──────┬──────────────────────────────┬───────────────────┘   │
│         │                              │                        │
│  ┌──────▼──────┐              ┌────────▼────────┐              │
│  │    Redis    │              │  Kafka Producer │              │
│  │  (Cache)    │              │  (Events)       │              │
│  └─────────────┘              └────────┬────────┘              │
│                                        │                        │
│  ┌─────────────────────────────────────▼───────────────────┐   │
│  │              MongoDB Repositories                       │   │
│  └─────────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────────┘
                            │
              ┌─────────────▼──────────────┐
              │         MongoDB            │
              │  users, vendors,           │
              │  warehouses, products,     │
              │  inventory, orders         │
              └────────────────────────────┘

Kafka Topics:
  order-placed-events     ──► Notification / Analytics
  inventory-update-events ──► Reorder / Reporting
```

---

## MongoDB vs MySQL: Why the Switch?

| Aspect | MongoDB | MySQL (Previous) |
|--------|---------|------------------|
| **Schema** | Flexible documents, embedded OrderItems | Rigid tables, JOIN for order items |
| **Concurrency** | Atomic `findAndModify` with conditional update | Pessimistic lock + atomic UPDATE |
| **Scaling** | Horizontal sharding built-in | Vertical scaling, complex sharding |
| **Denormalization** | Store `vendorName` in Product doc | JOIN on every read |
| **Indexes** | Compound indexes on nested fields | Standard B-tree indexes |

**Concurrency in MongoDB:**
```javascript
// Atomic stock reservation — no separate lock needed
db.inventory.findAndModify({
  query: { productId: "...", warehouseId: "...", 
           $expr: { $gte: [{ $subtract: ["$quantity", "$reservedQuantity"] }, requestedQty] } },
  update: { $inc: { reservedQuantity: requestedQty } }
})
```
If the condition fails (insufficient stock), `findAndModify` returns `null` → we throw `InsufficientStockException`.

---

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2**
- **MongoDB** (document store)
- **Redis** (caching with TTL)
- **Apache Kafka** (event streaming)
- **JWT** (authentication)
- **Swagger/OpenAPI** (API docs)
- **Bucket4j** (rate limiting)
- **JUnit 5 + Mockito** (testing)

### Frontend
- **Angular 17** (standalone components)
- **TypeScript 5.2**
- **RxJS** (reactive programming)
- **CSS Variables** (theming)

### DevOps
- **Docker + Docker Compose**
- **Maven** (build tool)

---

## Quick Start

### 1. Backend (Spring Boot)

```bash
cd /path/to/Inventory

# Start infrastructure (MongoDB, Redis, Kafka)
docker-compose up mongodb redis zookeeper kafka -d

# Run backend
./mvnw spring-boot:run

# Backend runs at http://localhost:8081
# Swagger UI: http://localhost:8081/swagger-ui.html
```

**Default Admin:**
- Email: `admin@ims.com`
- Password: `Admin@1234`

### 2. Frontend (Angular)

```bash
cd ims-frontend

# Install dependencies
npm install

# Start dev server (proxies /api to backend)
npm start

# Frontend runs at http://localhost:4200
```

### 3. Full Stack with Docker

```bash
docker-compose up --build

# All services start:
# - MongoDB: 27017
# - Redis: 6379
# - Kafka: 9092
# - Backend: 8081
# - Frontend: Build and serve via nginx (add nginx service to docker-compose)
```

---

## API Documentation

### Swagger UI
Open **http://localhost:8081/swagger-ui.html** after starting the backend.

- Click **Authorize** button
- Paste your JWT token (get it from `/api/v1/auth/login`)
- All endpoints are now authenticated

### Postman Collection
Import `postman/IMS_Collection.json` into Postman.

**Features:**
- ✅ Auto-saves JWT token after login
- ✅ Auto-populates IDs (vendorId, productId, etc.) from responses
- ✅ 30+ requests covering all endpoints
- ✅ Idempotency test for orders

**Usage:**
1. Import collection
2. Set `baseUrl` variable to `http://localhost:8081`
3. Run `POST /auth/login` → token auto-saved
4. Run any other request — token is auto-attached

---

## Key Features

### Current Business Flow (Refactored)
- Vendor can register/login, add products they supply, and set their own price per product.
- One product can have multiple vendor supplies with different prices.
- Admin creates purchase orders to vendors.
- Vendor updates order status: `CREATED -> ACCEPTED -> SHIPPED`.
- Admin receives shipped orders (`RECEIVED`) and updates inventory quantity + selling price.
- Admin can record customer sales, which reduce `quantityAvailable`.
- Dashboard surfaces low-stock alerts, purchase history, sales units, and vendor-wise supply summary.

### 1. Concurrency-Safe Stock Management
**MongoDB Atomic Operations:**
```java
// InventoryService.reserveStock()
Query query = new Query(Criteria.where("productId").is(productId)
    .and("warehouseId").is(warehouseId)
    .and("$expr").is(new Document("$gte", 
        List.of(new Document("$subtract", List.of("$quantity", "$reservedQuantity")), quantity))));

Update update = new Update().inc("reservedQuantity", quantity);

Inventory updated = mongoTemplate.findAndModify(query, update, 
    FindAndModifyOptions.options().returnNew(true), Inventory.class);

if (updated == null) {
    throw new InsufficientStockException(...);
}
```

**Why this works:**
- MongoDB evaluates the `$expr` condition **atomically** at the document level
- If two concurrent requests try to reserve the last 10 units, only one succeeds
- No pessimistic lock needed — MongoDB's single-document atomicity guarantees correctness

### 2. Idempotent Order Placement
```java
// OrderService.placeOrder()
return orderRepository.findByIdempotencyKey(request.getIdempotencyKey())
    .map(this::toResponse)  // Return existing order
    .orElseGet(() -> createNewOrder(request));  // Create new
```
Client sends a UUID as `idempotencyKey`. Duplicate requests return the same order.

### 3. Redis Caching Strategy
**Read-through with write-invalidation:**
```java
@Cacheable(value = "products", key = "#id")
public ProductResponse getById(String id) { ... }

@CacheEvict(value = "products", key = "#id")
public ProductResponse update(String id, ProductRequest req) { ... }
```

**TTLs:**
- Products: 5 min (rarely change)
- Inventory: 2 min (changes on every order)
- Vendors/Warehouses: 10 min (almost static)

### 4. Kafka Event Streaming
**Topics:**
- `order-placed-events` — published after successful reservation
- `inventory-update-events` — published on restock/reserve/confirm

**Partitioning:**
- Message key = `orderReference` / `productId`
- All events for the same entity land on the same partition → ordering preserved

**Consumer:**
```java
@KafkaListener(topics = "order-placed-events", groupId = "ims-group")
public void consumeOrderPlaced(OrderPlacedEvent event) {
    // Trigger email notification, update analytics, etc.
}
```

### 5. JWT Authentication
**Flow:**
1. `POST /api/v1/auth/login` → returns JWT token
2. Frontend stores token in `localStorage`
3. Angular `authInterceptor` attaches `Authorization: Bearer <token>` to every request
4. Backend `JwtAuthFilter` validates token and sets `SecurityContext`

**RBAC:**
- `ADMIN` — full access
- `VENDOR` — can create/edit products, place orders

---

## Database Schema (MongoDB)

### Collections

**users**
```json
{
  "_id": "ObjectId",
  "email": "admin@ims.com",
  "password": "$2a$10$...",
  "name": "Admin User",
  "role": "ADMIN",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**vendors**
```json
{
  "_id": "ObjectId",
  "name": "Acme Corp",
  "email": "acme@supplies.com",
  "phone": "+1-555-0100",
  "address": "123 Industrial Ave",
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**products**
```json
{
  "_id": "ObjectId",
  "name": "Laptop Pro 15",
  "sku": "LAP-PRO-15",
  "category": "Electronics",
  "price": 1299.99,
  "vendorId": "ObjectId",
  "vendorName": "Acme Corp",  // Denormalized
  "createdAt": "2024-01-01T00:00:00Z"
}
```

**inventory**
```json
{
  "_id": "ObjectId",
  "productId": "ObjectId",
  "productSku": "LAP-PRO-15",
  "productName": "Laptop Pro 15",
  "warehouseId": "ObjectId",
  "warehouseName": "Main WH",
  "quantity": 500,
  "reservedQuantity": 20,
  "reorderLevel": 50,
  "version": 3,  // Optimistic locking
  "updatedAt": "2024-01-01T12:00:00Z"
}
```

**orders**
```json
{
  "_id": "ObjectId",
  "orderReference": "ORD-A1B2C3D4",
  "idempotencyKey": "uuid-from-client",
  "userId": "ObjectId",
  "warehouseId": "ObjectId",
  "status": "RESERVED",
  "totalAmount": 2599.98,
  "items": [  // Embedded documents
    {
      "productId": "ObjectId",
      "productName": "Laptop Pro 15",
      "productSku": "LAP-PRO-15",
      "quantity": 2,
      "unitPrice": 1299.99,
      "subtotal": 2599.98
    }
  ],
  "createdAt": "2024-01-01T10:00:00Z"
}
```

### Indexes
```javascript
// Unique indexes
db.users.createIndex({ email: 1 }, { unique: true })
db.vendors.createIndex({ email: 1 }, { unique: true })
db.products.createIndex({ sku: 1 }, { unique: true })
db.inventory.createIndex({ productId: 1, warehouseId: 1 }, { unique: true })
db.orders.createIndex({ orderReference: 1 }, { unique: true })
db.orders.createIndex({ idempotencyKey: 1 }, { unique: true })

// Query optimization indexes
db.products.createIndex({ vendorId: 1, category: 1 })
db.products.createIndex({ category: 1 })
db.inventory.createIndex({ productId: 1 })
db.orders.createIndex({ userId: 1, status: 1 })
```

---

## Frontend Structure

```
ims-frontend/
├── src/
│   ├── app/
│   │   ├── app.component.ts       # Root component
│   │   ├── app.routes.ts          # Route definitions
│   │   ├── app.config.ts          # App providers
│   │   └── shell.component.ts     # Layout with sidebar
│   ├── core/
│   │   ├── guards/
│   │   │   └── auth.guard.ts      # Route protection
│   │   ├── interceptors/
│   │   │   └── auth.interceptor.ts # JWT attachment
│   │   └── services/
│   │       ├── auth.service.ts    # Login/logout
│   │       └── api.service.ts     # All API calls
│   ├── features/
│   │   ├── auth/
│   │   │   ├── login.component.ts
│   │   │   └── register.component.ts
│   │   ├── dashboard/
│   │   │   └── dashboard.component.ts
│   │   ├── products/
│   │   │   └── products.component.ts
│   │   ├── inventory/
│   │   │   └── inventory.component.ts
│   │   ├── orders/
│   │   │   └── orders.component.ts
│   │   └── vendors/
│   │       └── vendors.component.ts
│   ├── shared/
│   │   └── models/
│   │       └── index.ts           # TypeScript interfaces
│   ├── environments/
│   │   └── environment.ts
│   ├── styles.css                 # Global styles
│   ├── index.html
│   └── main.ts
├── proxy.conf.json                # Proxy /api to backend
├── angular.json
├── package.json
└── tsconfig.json
```

---

## Testing

### Backend Tests
```bash
./mvnw test

# Output:
# InventoryServiceTest: 6 tests ✓
# OrderServiceTest: 3 tests ✓
# VendorServiceTest: 4 tests ✓
# Total: 13 tests, 0 failures
```

**Key Test Cases:**
- ✅ Stock reservation with atomic `findAndModify`
- ✅ Insufficient stock exception when race condition occurs
- ✅ Order idempotency — duplicate key returns existing order
- ✅ Vendor CRUD operations
- ✅ Cache eviction on update

---

## Configuration

### application.properties
```properties
# MongoDB
spring.data.mongodb.uri=mongodb://localhost:27017/ims_db

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Kafka
spring.kafka.bootstrap-servers=localhost:9092

# JWT
app.jwt.secret=<your-secret>
app.jwt.expiration=86400000  # 24 hours

# Cache TTLs (seconds)
app.cache.product-ttl=300
app.cache.vendor-ttl=600

# Rate Limiting
app.rate-limit.capacity=100
app.rate-limit.refill-tokens=100
app.rate-limit.refill-duration=60

# Swagger
springdoc.swagger-ui.path=/swagger-ui.html

# CORS (for Angular)
app.cors.allowed-origins=http://localhost:4200
```

---

## Docker Compose

```yaml
services:
  mongodb:
    image: mongo:7.0
    ports: ["27017:27017"]
    volumes: [mongo_data:/data/db]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
    command: redis-server --maxmemory 256mb --maxmemory-policy allkeys-lru

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]

  app:
    build: .
    ports: ["8080:8080"]
    environment:
      MONGO_HOST: mongodb
      REDIS_HOST: redis
      KAFKA_SERVERS: kafka:29092
    depends_on: [mongodb, redis, kafka]
```

---

## API Endpoints Summary

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| **Auth** |
| POST | `/api/v1/auth/register` | ✗ | Register user |
| POST | `/api/v1/auth/login` | ✗ | Login, get JWT |
| **Vendors** |
| POST | `/api/v1/vendors` | ADMIN | Create vendor |
| GET | `/api/v1/vendors` | Any | List vendors |
| GET | `/api/v1/vendors/{id}` | Any | Get by ID |
| PUT | `/api/v1/vendors/{id}` | ADMIN | Update |
| DELETE | `/api/v1/vendors/{id}` | ADMIN | Delete |
| **Products** |
| POST | `/api/v1/products` | ADMIN/VENDOR | Create product |
| GET | `/api/v1/products` | Public | List with filters |
| GET | `/api/v1/products/{id}` | Public | Get by ID (cached) |
| PUT | `/api/v1/products/{id}` | ADMIN/VENDOR | Update |
| DELETE | `/api/v1/products/{id}` | ADMIN | Delete |
| **Inventory** |
| POST | `/api/v1/inventory` | ADMIN | Create record |
| GET | `/api/v1/inventory/{id}` | Any | Get by ID |
| GET | `/api/v1/inventory/product/{id}` | Any | Stock by product |
| GET | `/api/v1/inventory/low-stock` | ADMIN | Low stock alerts |
| PATCH | `/api/v1/inventory/{id}/restock` | ADMIN | Add stock |
| **Orders** |
| POST | `/api/v1/orders` | Any | Place order |
| GET | `/api/v1/orders/{reference}` | Any | Get by reference |
| GET | `/api/v1/orders/my` | Any | My orders |
| GET | `/api/v1/orders` | ADMIN | All orders |
| PATCH | `/api/v1/orders/{ref}/cancel` | Any | Cancel order |

---

## Production Checklist

- [ ] Change `app.jwt.secret` to a strong random value
- [ ] Enable MongoDB authentication
- [ ] Set Redis password
- [ ] Configure Kafka ACLs
- [ ] Add HTTPS/TLS
- [ ] Set up MongoDB replica set for high availability
- [ ] Configure Redis Sentinel for failover
- [ ] Increase Kafka replication factor to 3
- [ ] Add distributed rate limiting (Redis-backed Bucket4j)
- [ ] Set up monitoring (Prometheus + Grafana)
- [ ] Configure log aggregation (ELK stack)
- [ ] Add health checks (`/actuator/health`)
- [ ] Set up CI/CD pipeline
- [ ] Configure backup strategy for MongoDB

---

## License

MIT

---

## Support

For issues or questions:
- Backend: Check Swagger UI at `/swagger-ui.html`
- Frontend: Check browser console for errors
- Postman: Import collection and test endpoints
- Logs: `docker-compose logs -f app`
