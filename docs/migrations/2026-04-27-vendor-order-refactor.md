# Mongo Migration: Vendor Supply + Purchase Order Lifecycle

Date: 2026-04-27

This project now uses:
- `products.vendorSupplies[]` for many-vendor pricing
- Purchase order status lifecycle: `CREATED -> ACCEPTED -> SHIPPED -> RECEIVED`
- Product-level inventory fields: `quantityAvailable`, `sellingPrice`, `totalPurchased`, `totalSold`
- `users.vendorId` to link vendor accounts to vendor profiles

## 1) Products Collection

Old shape (single vendor):
- `vendorId`
- `vendorName`
- `price`

New shape (multi-vendor):
- `vendorSupplies: [{ vendorId, vendorName, vendorPrice, updatedAt }]`

### Suggested migration

```javascript
// mongosh
use ims_db;

db.products.find({ vendorId: { $exists: true, $ne: null } }).forEach(function(p) {
  const supplies = [];
  if (p.vendorId && p.price) {
    supplies.push({
      vendorId: p.vendorId,
      vendorName: p.vendorName || "",
      vendorPrice: p.price,
      updatedAt: new Date()
    });
  }

  db.products.updateOne(
    { _id: p._id },
    {
      $set: {
        vendorSupplies: supplies,
        updatedAt: p.updatedAt || new Date()
      },
      $unset: {
        vendorId: "",
        vendorName: "",
        price: ""
      }
    }
  );
});
```

## 2) Orders Collection

Old statuses:
- `PENDING`, `RESERVED`, `CONFIRMED`, `FAILED`

New statuses:
- `CREATED`, `ACCEPTED`, `SHIPPED`, `RECEIVED`, `CANCELLED`

Old fields:
- `userId`, `warehouseId`
- `items[].unitPrice`

New fields:
- `createdByUserId`, `createdByEmail`, `vendorId`, `vendorName`
- `items[].vendorPrice`

### Suggested status mapping

```javascript
use ims_db;

db.orders.updateMany(
  { status: "PENDING" },
  { $set: { status: "CREATED" } }
);

db.orders.updateMany(
  { status: "RESERVED" },
  { $set: { status: "ACCEPTED" } }
);

db.orders.updateMany(
  { status: "CONFIRMED" },
  { $set: { status: "RECEIVED" } }
);

db.orders.updateMany(
  { status: "FAILED" },
  { $set: { status: "CANCELLED" } }
);

db.orders.find({ "items.unitPrice": { $exists: true } }).forEach(function(o) {
  const items = (o.items || []).map(function(i) {
    return {
      productId: i.productId,
      productName: i.productName,
      productSku: i.productSku,
      quantity: i.quantity,
      vendorPrice: i.vendorPrice || i.unitPrice,
      subtotal: i.subtotal
    };
  });

  db.orders.updateOne(
    { _id: o._id },
    {
      $set: {
        items: items,
        createdByUserId: o.createdByUserId || o.userId || null,
        updatedAt: o.updatedAt || new Date()
      },
      $unset: {
        userId: "",
        warehouseId: ""
      }
    }
  );
});
```

## 3) Inventory Collection

Old fields:
- `quantity`, `reservedQuantity`, `warehouseId`, `warehouseName`

New fields:
- `quantityAvailable`, `sellingPrice`, `totalPurchased`, `totalSold`

### Suggested migration

```javascript
use ims_db;

db.inventory.find({}).forEach(function(i) {
  const qty = i.quantity || 0;
  const reserved = i.reservedQuantity || 0;
  const available = Math.max(qty - reserved, 0);

  db.inventory.updateOne(
    { _id: i._id },
    {
      $set: {
        quantityAvailable: available,
        totalPurchased: i.totalPurchased || qty,
        totalSold: i.totalSold || 0,
        sellingPrice: i.sellingPrice || 1,
        updatedAt: i.updatedAt || new Date()
      },
      $unset: {
        quantity: "",
        reservedQuantity: "",
        warehouseId: "",
        warehouseName: ""
      }
    }
  );
});
```

## 4) Users Collection

New field:
- `vendorId` for users with role `VENDOR`

### Suggested backfill

```javascript
use ims_db;

db.users.find({ role: "VENDOR", $or: [{ vendorId: { $exists: false } }, { vendorId: null }] }).forEach(function(u) {
  const vendor = db.vendors.findOne({ email: u.email });
  if (vendor) {
    db.users.updateOne({ _id: u._id }, { $set: { vendorId: vendor._id.toString() } });
  }
});
```

## 5) Index Refresh

After migration, ensure indexes are recreated by restarting the Spring Boot app.

