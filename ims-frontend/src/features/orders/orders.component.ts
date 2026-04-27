import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService, ProductService, VendorService } from '../../core/services/api.service';
import { Order, OrderRequest, Product, ReceiveOrderRequest, Vendor } from '../../shared/models';
import { AuthService } from '../../core/services/auth.service';

interface OrderDraftItem {
  productId: string;
  quantity: number;
  vendorPrice: number;
}

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="font-size:20px;font-weight:700">{{ isVendor ? 'Incoming Purchase Orders' : 'Purchase Orders' }}</h2>
      <button *ngIf="isAdmin" class="btn btn-primary" (click)="openCreateOrder()">+ Create Order</button>
    </div>

    <div *ngIf="isAdmin" style="margin-bottom:16px;display:flex;gap:8px;align-items:center;flex-wrap:wrap">
      <label style="font-size:13px;font-weight:500">View:</label>
      <button class="btn btn-sm" [class.btn-primary]="!adminViewAll" [class.btn-secondary]="adminViewAll" (click)="adminViewAll=false;load()">My Orders</button>
      <button class="btn btn-sm" [class.btn-primary]="adminViewAll" [class.btn-secondary]="!adminViewAll" (click)="adminViewAll=true;load()">All Orders</button>
      <select class="form-control" style="max-width:180px" [(ngModel)]="statusFilter" (change)="load()">
        <option value="">All Statuses</option>
        <option *ngFor="let s of statuses" [value]="s">{{ s }}</option>
      </select>
    </div>

    <div *ngIf="isVendor" style="margin-bottom:16px;display:flex;gap:8px;align-items:center;flex-wrap:wrap">
      <label style="font-size:13px;font-weight:500">Status:</label>
      <select class="form-control" style="max-width:180px" [(ngModel)]="statusFilter" (change)="load()">
        <option value="">All</option>
        <option *ngFor="let s of statuses" [value]="s">{{ s }}</option>
      </select>
    </div>

    <div class="card">
      <div *ngIf="loading" class="loading-overlay"><div class="spinner"></div></div>
      <div class="table-wrap" *ngIf="!loading">
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Vendor</th>
              <th>Status</th>
              <th>Items</th>
              <th>Total</th>
              <th>Date</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let order of orders">
              <td><code>{{ order.orderReference }}</code></td>
              <td>{{ order.vendorName }}</td>
              <td>
                <span class="badge"
                  [class.badge-info]="order.status==='CREATED'"
                  [class.badge-warning]="order.status==='ACCEPTED'"
                  [class.badge-secondary]="order.status==='SHIPPED'"
                  [class.badge-success]="order.status==='RECEIVED'"
                  [class.badge-danger]="order.status==='CANCELLED'">
                  {{ order.status }}
                </span>
              </td>
              <td>{{ order.items.length }} item(s)</td>
              <td><strong>
                \${{ order.totalAmount | number:'1.2-2' }}
              </strong></td>
              <td>{{ order.createdAt | date:'short' }}</td>
              <td>
                <button class="btn btn-secondary btn-sm" (click)="view(order)">View</button>

                <button *ngIf="isVendor && order.status==='CREATED'" class="btn btn-primary btn-sm" style="margin-left:4px" (click)="accept(order.orderReference)">
                  Accept
                </button>
                <button *ngIf="isVendor && order.status==='ACCEPTED'" class="btn btn-primary btn-sm" style="margin-left:4px" (click)="ship(order.orderReference)">
                  Ship
                </button>

                <button *ngIf="isAdmin && order.status==='SHIPPED'" class="btn btn-success btn-sm" style="margin-left:4px" (click)="openReceive(order)">
                  Receive
                </button>
                <button *ngIf="isAdmin && (order.status==='CREATED' || order.status==='ACCEPTED')" class="btn btn-danger btn-sm" style="margin-left:4px" (click)="cancel(order.orderReference)">
                  Cancel
                </button>
              </td>
            </tr>
            <tr *ngIf="orders.length===0">
              <td colspan="7" class="empty-state">No orders found</td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="padding:12px 16px;display:flex;justify-content:space-between;align-items:center">
        <span style="font-size:13px;color:var(--text-muted)">{{ totalElements }} total</span>
        <div class="pagination">
          <button class="page-btn" [disabled]="page===0" (click)="changePage(page-1)">‹</button>
          <button class="page-btn active">{{ page+1 }}</button>
          <button class="page-btn" [disabled]="page>=totalPages-1" (click)="changePage(page+1)">›</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="selectedOrder" (click)="selectedOrder=null">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>{{ selectedOrder.orderReference }}</h3>
          <button class="close-btn" (click)="selectedOrder=null">×</button>
        </div>
        <div class="modal-body">
          <p><strong>Vendor:</strong> {{ selectedOrder.vendorName }}</p>
          <p><strong>Status:</strong> <span class="badge badge-info">{{ selectedOrder.status }}</span></p>
          <p style="margin-top:8px"><strong>Total:</strong> \${{ selectedOrder.totalAmount | number:'1.2-2' }}</p>
          <table style="margin-top:16px;width:100%">
            <thead><tr><th>Product</th><th>Qty</th><th>Vendor Price</th><th>Subtotal</th></tr></thead>
            <tbody>
              <tr *ngFor="let item of selectedOrder.items">
                <td>{{ item.productName }}</td>
                <td>{{ item.quantity }}</td>
                <td>\${{ item.vendorPrice | number:'1.2-2' }}</td>
                <td>\${{ item.subtotal | number:'1.2-2' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showCreateModal" (click)="showCreateModal=false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Create Purchase Order</h3>
          <button class="close-btn" (click)="showCreateModal=false">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="formError" class="alert alert-danger">{{ formError }}</div>

          <div class="form-group">
            <label class="form-label">Vendor *</label>
            <select class="form-control" [(ngModel)]="draftVendorId" (change)="onVendorChange()">
              <option value="">Select vendor</option>
              <option *ngFor="let v of vendors" [value]="v.id">{{ v.name }}</option>
            </select>
          </div>

          <div style="margin-bottom:12px">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
              <label class="form-label" style="margin:0">Items *</label>
              <button class="btn btn-secondary btn-sm" (click)="addDraftItem()">+ Add Item</button>
            </div>
            <div *ngFor="let item of draftItems; let i=index" style="display:flex;gap:8px;margin-bottom:8px;align-items:center;flex-wrap:wrap">
              <select class="form-control" style="min-width:220px" [(ngModel)]="item.productId" (change)="onDraftProductChange(item)">
                <option value="">Select product</option>
                <option *ngFor="let p of vendorProducts" [value]="p.id">{{ p.name }} ({{ p.sku }})</option>
              </select>
              <input class="form-control" style="max-width:90px" type="number" [(ngModel)]="item.quantity" min="1" placeholder="Qty" />
              <input class="form-control" style="max-width:130px" type="number" [(ngModel)]="item.vendorPrice" min="0.01" step="0.01" placeholder="Price" />
              <button class="btn btn-danger btn-sm" (click)="removeDraftItem(i)">×</button>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="showCreateModal=false">Cancel</button>
          <button class="btn btn-primary" (click)="createOrder()" [disabled]="saving">{{ saving ? 'Saving...' : 'Create Order' }}</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showReceiveModal" (click)="showReceiveModal=false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Receive Order {{ receiveOrderRef }}</h3>
          <button class="close-btn" (click)="showReceiveModal=false">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="formError" class="alert alert-danger">{{ formError }}</div>
          <div *ngFor="let item of receiveItems" style="border:1px solid var(--border);border-radius:10px;padding:10px;margin-bottom:10px">
            <div style="font-weight:600">{{ item.productName }} (Qty: {{ item.quantity }})</div>
            <div style="display:flex;gap:8px;margin-top:8px;flex-wrap:wrap">
              <div style="min-width:180px">
                <label class="form-label">Selling Price *</label>
                <input class="form-control" type="number" [(ngModel)]="item.sellingPrice" min="0.01" step="0.01" />
              </div>
              <div style="min-width:140px">
                <label class="form-label">Reorder Level</label>
                <input class="form-control" type="number" [(ngModel)]="item.reorderLevel" min="0" />
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="showReceiveModal=false">Cancel</button>
          <button class="btn btn-success" (click)="confirmReceive()" [disabled]="saving">{{ saving ? 'Saving...' : 'Confirm Receipt' }}</button>
        </div>
      </div>
    </div>
  `
})
export class OrdersComponent implements OnInit {
  orders: Order[] = [];
  vendors: Vendor[] = [];
  vendorProducts: Product[] = [];

  loading = false;
  saving = false;

  showCreateModal = false;
  showReceiveModal = false;
  selectedOrder: Order | null = null;

  page = 0;
  totalPages = 0;
  totalElements = 0;

  adminViewAll = false;
  statusFilter = '';
  statuses: Order['status'][] = ['CREATED', 'ACCEPTED', 'SHIPPED', 'RECEIVED', 'CANCELLED'];

  draftVendorId = '';
  draftItems: OrderDraftItem[] = [{ productId: '', quantity: 1, vendorPrice: 0 }];

  receiveOrderRef = '';
  receiveItems: Array<{ productId: string; productName: string; quantity: number; sellingPrice: number; reorderLevel: number }> = [];

  formError = '';

  constructor(
    private orderSvc: OrderService,
    private vendorSvc: VendorService,
    private productSvc: ProductService,
    public auth: AuthService
  ) {}

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  get isVendor(): boolean {
    return this.auth.currentUser?.role === 'VENDOR';
  }

  ngOnInit(): void {
    this.load();
    if (this.isAdmin) {
      this.vendorSvc.getAll('', 0, 200).subscribe(r => this.vendors = r.data.content);
    }
  }

  load(): void {
    this.loading = true;

    const req$ = this.isVendor
      ? this.orderSvc.getVendorIncoming(this.statusFilter, this.page)
      : (this.adminViewAll ? this.orderSvc.getAllOrders(this.statusFilter, this.page) : this.orderSvc.getMyOrders(this.page));

    req$.subscribe({
      next: r => {
        this.orders = r.data.content;
        this.totalPages = r.data.totalPages;
        this.totalElements = r.data.totalElements;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  changePage(page: number): void {
    this.page = page;
    this.load();
  }

  view(order: Order): void {
    this.selectedOrder = order;
  }

  openCreateOrder(): void {
    this.formError = '';
    this.draftVendorId = '';
    this.vendorProducts = [];
    this.draftItems = [{ productId: '', quantity: 1, vendorPrice: 0 }];
    this.showCreateModal = true;
  }

  onVendorChange(): void {
    if (!this.draftVendorId) {
      this.vendorProducts = [];
      return;
    }

    this.productSvc.getAll({ vendorId: this.draftVendorId }, 0, 200).subscribe(r => {
      this.vendorProducts = r.data.content;
      this.draftItems = [{ productId: '', quantity: 1, vendorPrice: 0 }];
    });
  }

  addDraftItem(): void {
    this.draftItems.push({ productId: '', quantity: 1, vendorPrice: 0 });
  }

  removeDraftItem(index: number): void {
    this.draftItems.splice(index, 1);
  }

  onDraftProductChange(item: OrderDraftItem): void {
    const product = this.vendorProducts.find(p => p.id === item.productId);
    if (!product) return;
    const supply = product.vendorSupplies.find(s => s.vendorId === this.draftVendorId);
    item.vendorPrice = supply?.vendorPrice ?? 0;
  }

  createOrder(): void {
    if (!this.draftVendorId || this.draftItems.length === 0) {
      this.formError = 'Vendor and at least one item are required';
      return;
    }

    const hasInvalid = this.draftItems.some(i => !i.productId || i.quantity <= 0 || i.vendorPrice <= 0);
    if (hasInvalid) {
      this.formError = 'Each item must have product, quantity and vendor price';
      return;
    }

    const request: OrderRequest = {
      idempotencyKey: crypto.randomUUID(),
      vendorId: this.draftVendorId,
      items: this.draftItems.map(i => ({ productId: i.productId, quantity: i.quantity, vendorPrice: i.vendorPrice }))
    };

    this.saving = true;
    this.orderSvc.placeOrder(request).subscribe({
      next: () => {
        this.saving = false;
        this.showCreateModal = false;
        this.load();
      },
      error: err => {
        this.saving = false;
        this.formError = err.error?.message || 'Failed to create order';
      }
    });
  }

  accept(reference: string): void {
    this.orderSvc.acceptOrder(reference).subscribe(() => this.load());
  }

  ship(reference: string): void {
    this.orderSvc.shipOrder(reference).subscribe(() => this.load());
  }

  openReceive(order: Order): void {
    this.formError = '';
    this.receiveOrderRef = order.orderReference;
    this.receiveItems = order.items.map(item => ({
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      sellingPrice: Number((item.vendorPrice * 1.2).toFixed(2)),
      reorderLevel: 10
    }));
    this.showReceiveModal = true;
  }

  confirmReceive(): void {
    if (!this.receiveOrderRef) return;
    const invalid = this.receiveItems.some(i => i.sellingPrice <= 0 || i.reorderLevel < 0);
    if (invalid) {
      this.formError = 'Selling price must be > 0 and reorder level must be >= 0';
      return;
    }

    const request: ReceiveOrderRequest = {
      items: this.receiveItems.map(i => ({
        productId: i.productId,
        sellingPrice: i.sellingPrice,
        reorderLevel: i.reorderLevel
      }))
    };

    this.saving = true;
    this.orderSvc.receiveOrder(this.receiveOrderRef, request).subscribe({
      next: () => {
        this.saving = false;
        this.showReceiveModal = false;
        this.load();
      },
      error: err => {
        this.saving = false;
        this.formError = err.error?.message || 'Failed to receive order';
      }
    });
  }

  cancel(reference: string): void {
    if (!confirm('Cancel this order?')) return;
    this.orderSvc.cancelOrder(reference).subscribe(() => this.load());
  }
}
