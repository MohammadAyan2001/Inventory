import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService, ProductService } from '../../core/services/api.service';
import { Inventory, InventoryRequest, Product } from '../../shared/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="font-size:20px;font-weight:700">Inventory</h2>
      <div style="display:flex;gap:8px">
        <button *ngIf="isAdmin" class="btn btn-secondary" (click)="loadLowStock()">⚠️ Low Stock</button>
        <button class="btn btn-secondary" (click)="loadAll()">Refresh</button>
        <button *ngIf="isAdmin" class="btn btn-primary" (click)="openCreateModal()">+ Add Record</button>
      </div>
    </div>

    <div class="card" style="margin-bottom:16px">
      <div class="card-body" style="display:flex;gap:12px;flex-wrap:wrap">
        <input class="form-control" style="max-width:280px" placeholder="Search by product name..." [(ngModel)]="search" (input)="loadAll()" />
      </div>
    </div>

    <div class="card">
      <div *ngIf="loading" class="loading-overlay"><div class="spinner"></div></div>
      <div class="table-wrap" *ngIf="!loading">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th>SKU</th>
              <th>Available</th>
              <th>Selling Price</th>
              <th>Reorder Level</th>
              <th>Total Purchased</th>
              <th>Total Sold</th>
              <th>Status</th>
              <th *ngIf="isAdmin">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of items">
              <td><strong>{{ item.productName }}</strong></td>
              <td><code>{{ item.productSku }}</code></td>
              <td><strong>{{ item.quantityAvailable }}</strong></td>
              <td>\${{ item.sellingPrice | number:'1.2-2' }}</td>
              <td>{{ item.reorderLevel }}</td>
              <td>{{ item.totalPurchased }}</td>
              <td>{{ item.totalSold }}</td>
              <td>
                <span class="badge" [class.badge-danger]="item.lowStock" [class.badge-success]="!item.lowStock">
                  {{ item.lowStock ? 'Low Stock' : 'OK' }}
                </span>
              </td>
              <td *ngIf="isAdmin">
                <button class="btn btn-secondary btn-sm" (click)="openRestockModal(item)">Restock</button>
                <button class="btn btn-secondary btn-sm" style="margin-left:4px" (click)="openPriceModal(item)">Set Price</button>
              </td>
            </tr>
            <tr *ngIf="items.length === 0">
              <td [attr.colspan]="isAdmin ? 9 : 8" class="empty-state">No inventory found</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showRestock" (click)="showRestock=false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Restock: {{ selectedItem?.productName }}</h3>
          <button class="close-btn" (click)="showRestock=false">×</button>
        </div>
        <div class="modal-body">
          <p style="color:var(--text-muted);margin-bottom:16px">Current available: <strong>{{ selectedItem?.quantityAvailable }}</strong></p>
          <div class="form-group">
            <label class="form-label">Quantity to Add *</label>
            <input class="form-control" type="number" [(ngModel)]="restockQty" min="1" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="showRestock=false">Cancel</button>
          <button class="btn btn-primary" (click)="doRestock()" [disabled]="saving">{{ saving ? 'Saving...' : 'Add Stock' }}</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showPrice" (click)="showPrice=false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Set Selling Price: {{ selectedItem?.productName }}</h3>
          <button class="close-btn" (click)="showPrice=false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Selling Price *</label>
            <input class="form-control" type="number" [(ngModel)]="priceValue" min="0.01" step="0.01" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="showPrice=false">Cancel</button>
          <button class="btn btn-primary" (click)="doUpdatePrice()" [disabled]="saving">{{ saving ? 'Saving...' : 'Update' }}</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showCreate" (click)="showCreate=false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>New Inventory Record</h3>
          <button class="close-btn" (click)="showCreate=false">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="formError" class="alert alert-danger">{{ formError }}</div>
          <div class="form-group">
            <label class="form-label">Product *</label>
            <select class="form-control" [(ngModel)]="createForm.productId">
              <option value="">Select product</option>
              <option *ngFor="let p of products" [value]="p.id">{{ p.name }} ({{ p.sku }})</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Initial Quantity *</label>
            <input class="form-control" type="number" [(ngModel)]="createForm.quantityAvailable" min="0" />
          </div>
          <div class="form-group">
            <label class="form-label">Selling Price *</label>
            <input class="form-control" type="number" [(ngModel)]="createForm.sellingPrice" min="0.01" step="0.01" />
          </div>
          <div class="form-group">
            <label class="form-label">Reorder Level</label>
            <input class="form-control" type="number" [(ngModel)]="createForm.reorderLevel" min="0" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="showCreate=false">Cancel</button>
          <button class="btn btn-primary" (click)="createRecord()" [disabled]="saving">{{ saving ? 'Saving...' : 'Create' }}</button>
        </div>
      </div>
    </div>
  `
})
export class InventoryComponent implements OnInit {
  items: Inventory[] = [];
  products: Product[] = [];

  loading = false;
  saving = false;

  showRestock = false;
  showPrice = false;
  showCreate = false;

  selectedItem: Inventory | null = null;
  restockQty = 0;
  priceValue = 0;

  search = '';
  formError = '';

  createForm: InventoryRequest = {
    productId: '',
    quantityAvailable: 0,
    sellingPrice: 0,
    reorderLevel: 10
  };

  constructor(private svc: InventoryService, private productSvc: ProductService, public auth: AuthService) {}

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  ngOnInit(): void {
    this.loadAll();
    if (this.isAdmin) {
      this.productSvc.getAll({}, 0, 300).subscribe(r => this.products = r.data.content);
    }
  }

  loadAll(): void {
    this.loading = true;
    this.svc.getAll(this.search).subscribe({
      next: r => {
        this.items = r.data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadLowStock(): void {
    this.loading = true;
    this.svc.getLowStock().subscribe({
      next: r => {
        this.items = r.data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openRestockModal(item: Inventory): void {
    this.selectedItem = item;
    this.restockQty = 0;
    this.showRestock = true;
  }

  openPriceModal(item: Inventory): void {
    this.selectedItem = item;
    this.priceValue = item.sellingPrice;
    this.showPrice = true;
  }

  openCreateModal(): void {
    this.formError = '';
    this.createForm = { productId: '', quantityAvailable: 0, sellingPrice: 0, reorderLevel: 10 };
    this.showCreate = true;
  }

  doRestock(): void {
    if (!this.selectedItem || this.restockQty <= 0) return;
    this.saving = true;
    this.svc.restock(this.selectedItem.id, this.restockQty).subscribe({
      next: () => {
        this.saving = false;
        this.showRestock = false;
        this.loadAll();
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  doUpdatePrice(): void {
    if (!this.selectedItem || this.priceValue <= 0) return;
    this.saving = true;
    this.svc.updateSellingPrice(this.selectedItem.productId, this.priceValue).subscribe({
      next: () => {
        this.saving = false;
        this.showPrice = false;
        this.loadAll();
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  createRecord(): void {
    if (!this.createForm.productId) {
      this.formError = 'Product is required';
      return;
    }
    if (this.createForm.sellingPrice <= 0) {
      this.formError = 'Selling price must be > 0';
      return;
    }

    this.saving = true;
    this.svc.create(this.createForm).subscribe({
      next: () => {
        this.saving = false;
        this.showCreate = false;
        this.loadAll();
      },
      error: err => {
        this.saving = false;
        this.formError = err.error?.message || 'Failed to create inventory record';
      }
    });
  }
}
