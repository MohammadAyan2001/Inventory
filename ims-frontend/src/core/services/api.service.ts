import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ApiResponse, PageResponse,
  Vendor, VendorRequest,
  Warehouse, WarehouseRequest,
  Product, ProductRequest, VendorProductRequest,
  Inventory, InventoryRequest, SellRequest,
  Order, OrderRequest, ReceiveOrderRequest
} from '../../shared/models';

const API = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class VendorService {
  constructor(private http: HttpClient) {}

  getAll(name = '', page = 0, size = 20): Observable<ApiResponse<PageResponse<Vendor>>> {
    const params = new HttpParams().set('page', page).set('size', size).set('name', name);
    return this.http.get<ApiResponse<PageResponse<Vendor>>>(`${API}/vendors`, { params });
  }

  getById(id: string): Observable<ApiResponse<Vendor>> {
    return this.http.get<ApiResponse<Vendor>>(`${API}/vendors/${id}`);
  }

  create(req: VendorRequest): Observable<ApiResponse<Vendor>> {
    return this.http.post<ApiResponse<Vendor>>(`${API}/vendors`, req);
  }

  update(id: string, req: VendorRequest): Observable<ApiResponse<Vendor>> {
    return this.http.put<ApiResponse<Vendor>>(`${API}/vendors/${id}`, req);
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${API}/vendors/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class WarehouseService {
  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 20): Observable<ApiResponse<PageResponse<Warehouse>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<Warehouse>>>(`${API}/warehouses`, { params });
  }

  getById(id: string): Observable<ApiResponse<Warehouse>> {
    return this.http.get<ApiResponse<Warehouse>>(`${API}/warehouses/${id}`);
  }

  create(req: WarehouseRequest): Observable<ApiResponse<Warehouse>> {
    return this.http.post<ApiResponse<Warehouse>>(`${API}/warehouses`, req);
  }

  update(id: string, req: WarehouseRequest): Observable<ApiResponse<Warehouse>> {
    return this.http.put<ApiResponse<Warehouse>>(`${API}/warehouses/${id}`, req);
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${API}/warehouses/${id}`);
  }
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  constructor(private http: HttpClient) {}

  getAll(filters: { category?: string; vendorId?: string; name?: string } = {}, page = 0, size = 20): Observable<ApiResponse<PageResponse<Product>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.category) params = params.set('category', filters.category);
    if (filters.vendorId) params = params.set('vendorId', filters.vendorId);
    if (filters.name) params = params.set('name', filters.name);
    return this.http.get<ApiResponse<PageResponse<Product>>>(`${API}/products`, { params });
  }

  getById(id: string): Observable<ApiResponse<Product>> {
    return this.http.get<ApiResponse<Product>>(`${API}/products/${id}`);
  }

  create(req: ProductRequest): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(`${API}/products`, req);
  }

  update(id: string, req: ProductRequest): Observable<ApiResponse<Product>> {
    return this.http.put<ApiResponse<Product>>(`${API}/products/${id}`, req);
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${API}/products/${id}`);
  }

  getMyVendorProducts(page = 0, size = 20): Observable<ApiResponse<PageResponse<Product>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<Product>>>(`${API}/products/vendor/my`, { params });
  }

  addMySupply(req: VendorProductRequest): Observable<ApiResponse<Product>> {
    return this.http.post<ApiResponse<Product>>(`${API}/products/vendor/my/supplies`, req);
  }

  updateMyPrice(productId: string, vendorPrice: number): Observable<ApiResponse<Product>> {
    return this.http.patch<ApiResponse<Product>>(`${API}/products/${productId}/vendor/my/price`, { vendorPrice });
  }
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  constructor(private http: HttpClient) {}

  getAll(productName = ''): Observable<ApiResponse<Inventory[]>> {
    let params = new HttpParams();
    if (productName) params = params.set('productName', productName);
    return this.http.get<ApiResponse<Inventory[]>>(`${API}/inventory`, { params });
  }

  getById(id: string): Observable<ApiResponse<Inventory>> {
    return this.http.get<ApiResponse<Inventory>>(`${API}/inventory/${id}`);
  }

  getByProduct(productId: string): Observable<ApiResponse<Inventory>> {
    return this.http.get<ApiResponse<Inventory>>(`${API}/inventory/product/${productId}`);
  }

  getLowStock(): Observable<ApiResponse<Inventory[]>> {
    return this.http.get<ApiResponse<Inventory[]>>(`${API}/inventory/low-stock`);
  }

  create(req: InventoryRequest): Observable<ApiResponse<Inventory>> {
    return this.http.post<ApiResponse<Inventory>>(`${API}/inventory`, req);
  }

  restock(id: string, quantity: number): Observable<ApiResponse<Inventory>> {
    return this.http.patch<ApiResponse<Inventory>>(`${API}/inventory/${id}/restock?quantity=${quantity}`, {});
  }

  updateSellingPrice(productId: string, value: number): Observable<ApiResponse<Inventory>> {
    return this.http.patch<ApiResponse<Inventory>>(`${API}/inventory/product/${productId}/selling-price?value=${value}`, {});
  }

  sell(req: SellRequest): Observable<ApiResponse<Inventory>> {
    return this.http.post<ApiResponse<Inventory>>(`${API}/inventory/sell`, req);
  }
}

@Injectable({ providedIn: 'root' })
export class OrderService {
  constructor(private http: HttpClient) {}

  placeOrder(req: OrderRequest): Observable<ApiResponse<Order>> {
    return this.http.post<ApiResponse<Order>>(`${API}/orders`, req);
  }

  getByReference(ref: string): Observable<ApiResponse<Order>> {
    return this.http.get<ApiResponse<Order>>(`${API}/orders/${ref}`);
  }

  getMyOrders(page = 0, size = 20): Observable<ApiResponse<PageResponse<Order>>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<ApiResponse<PageResponse<Order>>>(`${API}/orders/my`, { params });
  }

  getVendorIncoming(status = '', page = 0, size = 20): Observable<ApiResponse<PageResponse<Order>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<Order>>>(`${API}/orders/vendor/incoming`, { params });
  }

  getAllOrders(status = '', page = 0, size = 20): Observable<ApiResponse<PageResponse<Order>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PageResponse<Order>>>(`${API}/orders`, { params });
  }

  acceptOrder(ref: string): Observable<ApiResponse<Order>> {
    return this.http.patch<ApiResponse<Order>>(`${API}/orders/${ref}/accept`, {});
  }

  shipOrder(ref: string): Observable<ApiResponse<Order>> {
    return this.http.patch<ApiResponse<Order>>(`${API}/orders/${ref}/ship`, {});
  }

  receiveOrder(ref: string, req: ReceiveOrderRequest): Observable<ApiResponse<Order>> {
    return this.http.patch<ApiResponse<Order>>(`${API}/orders/${ref}/receive`, req);
  }

  cancelOrder(ref: string): Observable<ApiResponse<Order>> {
    return this.http.patch<ApiResponse<Order>>(`${API}/orders/${ref}/cancel`, {});
  }
}
