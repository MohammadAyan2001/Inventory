// Auth
export interface LoginRequest { email: string; password: string; tenantId?: string; }
export interface RegisterRequest { name: string; email: string; password: string; role: string; tenantId?: string; }
export interface AuthResponse { token: string; tokenType: string; email: string; role: string; vendorId?: string | null; tenantId?: string | null; }

// Vendor
export interface Vendor { id: string; name: string; email: string; phone?: string; address?: string; createdAt?: string; }
export interface VendorRequest { name: string; email: string; phone?: string; address?: string; }

// Warehouse
export interface Warehouse { id: string; name: string; location: string; capacity?: number; createdAt?: string; }
export interface WarehouseRequest { name: string; location: string; capacity?: number; }

// Product + Vendor Supply
export interface VendorSupply {
  vendorId: string;
  vendorName: string;
  vendorPrice: number;
  updatedAt?: string;
}

export interface Product {
  id: string;
  name: string;
  sku: string;
  description?: string;
  category?: string;
  vendorSupplies: VendorSupply[];
  createdAt?: string;
  updatedAt?: string;
}

export interface ProductRequest {
  name: string;
  sku: string;
  description?: string;
  category?: string;
}

export interface VendorProductRequest {
  existingProductId?: string;
  name?: string;
  sku?: string;
  description?: string;
  category?: string;
  vendorPrice: number;
}

// Inventory
export interface Inventory {
  id: string;
  productId: string;
  productName: string;
  productSku: string;
  quantityAvailable: number;
  sellingPrice: number;
  reorderLevel: number;
  lowStock: boolean;
  totalPurchased: number;
  totalSold: number;
  updatedAt?: string;
}

export interface InventoryRequest {
  productId: string;
  quantityAvailable: number;
  sellingPrice: number;
  reorderLevel?: number;
}

export interface SellRequest {
  productId: string;
  quantity: number;
}

// Orders
export interface OrderItem {
  productId: string;
  productName: string;
  quantity: number;
  vendorPrice: number;
  subtotal: number;
}

export interface Order {
  id: string;
  orderReference: string;
  vendorId: string;
  vendorName: string;
  createdByEmail: string;
  status: 'CREATED' | 'ACCEPTED' | 'SHIPPED' | 'RECEIVED' | 'CANCELLED';
  totalAmount: number;
  items: OrderItem[];
  createdAt?: string;
  updatedAt?: string;
}

export interface OrderRequest {
  idempotencyKey: string;
  vendorId: string;
  items: { productId: string; quantity: number; vendorPrice?: number }[];
}

export interface ReceiveOrderRequest {
  items: { productId: string; sellingPrice: number; reorderLevel?: number }[];
}

// API Wrapper
export interface ApiResponse<T> { success: boolean; message?: string; data: T; timestamp?: string; }
export interface PageResponse<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number; }
