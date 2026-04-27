-- ============================================================
-- IMS Database Schema
-- Hibernate ddl-auto=update handles table creation,
-- this script adds extra indexes and seed data.
-- ============================================================

CREATE DATABASE IF NOT EXISTS ims_db;
USE ims_db;

-- Composite index for inventory lookups by product across warehouses
-- Used heavily in stock reservation queries
-- CREATE INDEX IF NOT EXISTS idx_inventory_product_warehouse
--   ON inventory(product_id, warehouse_id);

-- Covering index for order status filtering with date sort
-- CREATE INDEX IF NOT EXISTS idx_orders_status_created
--   ON orders(status, created_at DESC);

-- ── Seed: default admin user (password: Admin@1234) ──────────────────────────
-- BCrypt hash of "Admin@1234"
INSERT IGNORE INTO users (email, password, name, role, created_at)
VALUES (
  'admin@ims.com',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHHi',
  'System Admin',
  'ADMIN',
  NOW()
);
