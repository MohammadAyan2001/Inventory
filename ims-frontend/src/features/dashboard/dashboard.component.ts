import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { InventoryService, OrderService, ProductService, VendorService } from '../../core/services/api.service';
import { Order } from '../../shared/models';
import { AuthService } from '../../core/services/auth.service';

interface VendorSummary {
  vendorName: string;
  orders: number;
  spend: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="hero-banner" [class.hero-vendor]="isVendor">
      <div>
        <p class="hero-kicker">{{ isVendor ? 'Vendor Portal' : 'Warehouse Control' }}</p>
        <h2 class="hero-title">
          {{ isVendor ? 'Supplier Dashboard' : 'Operations Dashboard' }}
        </h2>
        <p class="hero-sub">
          {{ isVendor
              ? 'Track incoming purchase orders, manage product supplies, and update your shipment flow.'
              : 'Monitor catalog performance, inventory movement, vendor supply, and checkout readiness in one view.' }}
        </p>
      </div>
      <div class="hero-chip">{{ currentRoleLabel }}</div>
    </section>

    <ng-container *ngIf="isVendor; else operationsDashboard">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Products You Supply</div>
          <div class="stat-value" style="color:var(--primary)">{{ vendorStats.suppliedProducts }}</div>
          <div class="stat-sub">Active catalog links</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Incoming Orders</div>
          <div class="stat-value" style="color:var(--warning)">{{ vendorStats.incoming }}</div>
          <div class="stat-sub">Waiting on action</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Accepted</div>
          <div class="stat-value" style="color:var(--success)">{{ vendorStats.accepted }}</div>
          <div class="stat-sub">Accepted by you</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Shipped</div>
          <div class="stat-value" style="color:var(--text)">{{ vendorStats.shipped }}</div>
          <div class="stat-sub">Ready for warehouse receipt</div>
        </div>
      </div>

      <div class="card" style="margin-top:20px">
        <div class="card-header">
          <h2>Incoming Purchase Orders</h2>
          <a routerLink="/orders" class="btn btn-secondary btn-sm">Manage Orders</a>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Reference</th><th>Status</th><th>Items</th><th>Total</th><th>Date</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let order of recentOrders">
                <td><code>{{ order.orderReference }}</code></td>
                <td><span class="badge badge-info">{{ order.status }}</span></td>
                <td>{{ order.items.length }}</td>
                <td>\${{ order.totalAmount | number:'1.2-2' }}</td>
                <td>{{ order.createdAt | date:'short' }}</td>
              </tr>
              <tr *ngIf="recentOrders.length===0">
                <td colspan="5" class="empty-state">No incoming orders yet</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card" style="margin-top:20px">
        <div class="card-header"><h2>Vendor Quick Actions</h2></div>
        <div class="card-body" style="display:flex;gap:12px;flex-wrap:wrap">
          <a routerLink="/products" class="btn btn-primary">Manage Supplies</a>
          <a routerLink="/orders" class="btn btn-secondary">Accept / Ship Orders</a>
        </div>
      </div>
    </ng-container>

    <ng-template #operationsDashboard>
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-label">Total Products</div>
          <div class="stat-value" style="color:var(--primary)">{{ stats.products }}</div>
          <div class="stat-sub">Catalog size</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Total Stock Units</div>
          <div class="stat-value" style="color:var(--success)">{{ stats.totalStock }}</div>
          <div class="stat-sub">Warehouse available quantity</div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Low Stock Alerts</div>
          <div class="stat-value" style="color:var(--warning)">{{ stats.lowStock }}</div>
          <div class="stat-sub"><a routerLink="/inventory">Review inventory →</a></div>
        </div>
        <div class="stat-card">
          <div class="stat-label">Sales Units</div>
          <div class="stat-value" style="color:var(--text)">{{ stats.soldUnits }}</div>
          <div class="stat-sub">Customer sales quantity</div>
        </div>
      </div>

      <div class="card" style="margin-top:20px" *ngIf="lowStockItems.length > 0">
        <div class="card-header">
          <h2>Low Stock Alerts</h2>
          <a routerLink="/inventory" class="btn btn-secondary btn-sm">Open Inventory</a>
        </div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Product</th><th>Available</th><th>Reorder</th><th>Status</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let item of lowStockItems.slice(0, 8)">
                <td>{{ item.productName }}</td>
                <td>{{ item.quantityAvailable }}</td>
                <td>{{ item.reorderLevel }}</td>
                <td><span class="badge badge-danger">Low</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card" style="margin-top:20px">
        <div class="card-header"><h2>Purchase History</h2></div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Reference</th><th>Vendor</th><th>Status</th><th>Total</th><th>Date</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let order of recentOrders">
                <td><code>{{ order.orderReference }}</code></td>
                <td>{{ order.vendorName }}</td>
                <td><span class="badge badge-info">{{ order.status }}</span></td>
                <td>\${{ order.totalAmount | number:'1.2-2' }}</td>
                <td>{{ order.createdAt | date:'short' }}</td>
              </tr>
              <tr *ngIf="recentOrders.length===0">
                <td colspan="5" class="empty-state">No purchase orders yet</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card" style="margin-top:20px">
        <div class="card-header"><h2>Vendor-Wise Supply Data</h2></div>
        <div class="table-wrap">
          <table>
            <thead>
              <tr><th>Vendor</th><th>Orders</th><th>Total Spend</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let row of vendorSummary">
                <td>{{ row.vendorName }}</td>
                <td>{{ row.orders }}</td>
                <td>\${{ row.spend | number:'1.2-2' }}</td>
              </tr>
              <tr *ngIf="vendorSummary.length===0">
                <td colspan="3" class="empty-state">No vendor supply data</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="card" style="margin-top:20px">
        <div class="card-header"><h2>Operations Quick Actions</h2></div>
        <div class="card-body" style="display:flex;gap:12px;flex-wrap:wrap">
          <a routerLink="/products" class="btn btn-primary">Manage Products</a>
          <a routerLink="/orders" class="btn btn-secondary">Purchase Orders</a>
          <a routerLink="/inventory" class="btn btn-secondary">Inventory</a>
          <a routerLink="/store" class="btn btn-secondary">Storefront Checkout</a>
        </div>
      </div>
    </ng-template>
  `
})
export class DashboardComponent implements OnInit {
  stats = {
    products: 0,
    totalStock: 0,
    lowStock: 0,
    soldUnits: 0
  };

  vendorStats = {
    suppliedProducts: 0,
    incoming: 0,
    accepted: 0,
    shipped: 0
  };

  recentOrders: Order[] = [];
  vendorSummary: VendorSummary[] = [];
  lowStockItems: Array<{ productName: string; quantityAvailable: number; reorderLevel: number }> = [];

  constructor(
    private productSvc: ProductService,
    private vendorSvc: VendorService,
    private inventorySvc: InventoryService,
    private orderSvc: OrderService,
    public auth: AuthService
  ) {}

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  get isVendor(): boolean {
    return this.auth.isVendor();
  }

  get currentRoleLabel(): string {
    if (this.auth.currentUser?.role === 'VENDOR') return 'Supplier';
    if (this.auth.currentUser?.role === 'WAREHOUSE_OPERATOR') return 'Warehouse Operator';
    return 'Administrator';
  }

  ngOnInit(): void {
    if (this.isVendor) {
      this.loadVendorDashboard();
      return;
    }
    this.loadOperationsDashboard();
  }

  private loadVendorDashboard(): void {
    forkJoin({
      myProducts: this.productSvc.getMyVendorProducts(0, 200),
      orders: this.orderSvc.getVendorIncoming('', 0, 100)
    }).subscribe(res => {
      const orders = res.orders.data.content;

      this.vendorStats.suppliedProducts = res.myProducts.data.totalElements;
      this.vendorStats.incoming = orders.filter(o => o.status === 'CREATED').length;
      this.vendorStats.accepted = orders.filter(o => o.status === 'ACCEPTED').length;
      this.vendorStats.shipped = orders.filter(o => o.status === 'SHIPPED').length;

      this.recentOrders = [...orders]
        .sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
        .slice(0, 8);
    });
  }

  private loadOperationsDashboard(): void {
    forkJoin({
      products: this.productSvc.getAll({}, 0, 1),
      vendors: this.vendorSvc.getAll('', 0, 1),
      inventory: this.inventorySvc.getAll(),
      lowStock: this.inventorySvc.getLowStock(),
      orders: this.orderSvc.getAllOrders('', 0, 50)
    }).subscribe(res => {
      const inventory = res.inventory.data;
      const orders = res.orders.data.content;

      this.stats.products = res.products.data.totalElements;
      this.stats.totalStock = inventory.reduce((sum, i) => sum + i.quantityAvailable, 0);
      this.stats.lowStock = res.lowStock.data.length;
      this.stats.soldUnits = inventory.reduce((sum, i) => sum + i.totalSold, 0);

      this.lowStockItems = res.lowStock.data.map(i => ({
        productName: i.productName,
        quantityAvailable: i.quantityAvailable,
        reorderLevel: i.reorderLevel
      }));

      this.recentOrders = [...orders]
        .sort((a, b) => (b.createdAt || '').localeCompare(a.createdAt || ''))
        .slice(0, 8);

      this.vendorSummary = this.buildVendorSummary(orders);
    });
  }

  private buildVendorSummary(orders: Order[]): VendorSummary[] {
    const map = new Map<string, VendorSummary>();

    for (const order of orders) {
      const key = order.vendorName || 'Unknown Vendor';
      const existing = map.get(key) ?? { vendorName: key, orders: 0, spend: 0 };
      existing.orders += 1;
      existing.spend += order.totalAmount;
      map.set(key, existing);
    }

    return Array.from(map.values()).sort((a, b) => b.spend - a.spend).slice(0, 10);
  }
}
