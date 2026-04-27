# Multi-Tenant + High-Scale Notes

This project now uses tenant-aware request scoping with `X-Tenant-Id` and tenant fields on core entities (`users`, `vendors`, `products`, `inventory`, `orders`, `warehouses`).

## Implemented in code

- `TenantFilter` + `TenantContext` for request-scoped tenant resolution.
- JWT tokens include `tenantId`; JWT validation checks tenant match.
- Compound Mongo indexes for tenant isolation and per-tenant uniqueness:
  - `tenantId + email`
  - `tenantId + sku`
  - `tenantId + productId` (inventory)
  - `tenantId + orderReference`
  - `tenantId + idempotencyKey`
- Services/repositories now query/write data inside current tenant only.
- Frontend auth + interceptor propagate `X-Tenant-Id`.
- Startup backfill sets missing `tenantId` fields to the default tenant (`public`) for legacy data.

## To handle millions of users/orders

1. Use MongoDB sharding on `tenantId` (or `tenantId + createdAt` for hot tenants).
2. Move rate limiting to distributed Redis-backed Bucket4j (current in-memory filter is single-node).
3. Add read replicas and offload catalog/product reads to cache.
4. Introduce async checkout/order pipeline with Kafka consumers for inventory reservations and finalization.
5. Add API gateway with per-tenant quotas, WAF, and edge caching.
6. Split auth/catalog/order/inventory into independently scalable services when traffic grows.
