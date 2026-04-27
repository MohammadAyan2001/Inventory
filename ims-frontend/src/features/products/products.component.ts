import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService, VendorService } from '../../core/services/api.service';
import { Product, ProductRequest, Vendor, VendorProductRequest } from '../../shared/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="font-size:20px;font-weight:700">{{ isVendor ? 'My Product Supplies' : 'Products' }}</h2>
      <button *ngIf="isVendor" class="btn btn-primary" (click)="openVendorSupplyModal()">+ Add Product Supply</button>
      <button *ngIf="isAdmin" class="btn btn-primary" (click)="openAdminModal()">+ Add Product</button>
    </div>

    <div class="card" style="margin-bottom:16px" *ngIf="!isVendor">
      <div class="card-body" style="display:flex;gap:12px;flex-wrap:wrap">
        <input class="form-control" style="max-width:200px" placeholder="Search name..." [(ngModel)]="filterName" (input)="load()" />
        <input class="form-control" style="max-width:160px" placeholder="Category..." [(ngModel)]="filterCategory" (input)="load()" />
        <select class="form-control" style="max-width:180px" [(ngModel)]="filterVendorId" (change)="load()">
          <option value="">All Vendors</option>
          <option *ngFor="let v of vendors" [value]="v.id">{{ v.name }}</option>
        </select>
      </div>
    </div>

    <div class="card">
      <div *ngIf="loading" class="loading-overlay"><div class="spinner"></div></div>
      <div class="table-wrap" *ngIf="!loading">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>SKU</th>
              <th>Category</th>
              <th>Suppliers</th>
              <th>{{ isVendor ? 'My Price' : 'Best Price' }}</th>
              <th *ngIf="isVendor || isAdmin">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let p of products">
              <td><strong>{{ p.name }}</strong><br><small style="color:var(--text-muted)">{{ p.description || '—' }}</small></td>
              <td><code>{{ p.sku }}</code></td>
              <td><span class="badge badge-info">{{ p.category || '—' }}</span></td>
              <td>{{ p.vendorSupplies.length }}</td>
              <td><strong>
                \${{ (isVendor ? getMyVendorPrice(p) : getBestVendorPrice(p)) | number:'1.2-2' }}
              </strong></td>
              <td *ngIf="isVendor || isAdmin">
                <button *ngIf="isVendor" class="btn btn-secondary btn-sm" (click)="openPriceModal(p)">Update Price</button>
                <button *ngIf="isAdmin" class="btn btn-secondary btn-sm" (click)="openAdminModal(p)">Edit</button>
                <button *ngIf="isAdmin" class="btn btn-danger btn-sm" style="margin-left:4px" (click)="delete(p.id)">Del</button>
              </td>
            </tr>
            <tr *ngIf="products.length === 0">
              <td [attr.colspan]="isVendor || isAdmin ? 6 : 5" class="empty-state">No products found</td>
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

    <div class="modal-overlay" *ngIf="showAdminModal" (click)="closeAdminModal()">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>{{ editing ? 'Edit Product' : 'New Product' }}</h3>
          <button class="close-btn" (click)="closeAdminModal()">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="formError" class="alert alert-danger">{{ formError }}</div>
          <div class="form-group">
            <label class="form-label">Name *</label>
            <input class="form-control" [(ngModel)]="adminForm.name" placeholder="Product name" />
          </div>
          <div class="form-group">
            <label class="form-label">SKU *</label>
            <input class="form-control" [(ngModel)]="adminForm.sku" placeholder="PROD-001" [disabled]="!!editing" />
          </div>
          <div class="form-group">
            <label class="form-label">Category</label>
            <input class="form-control" [(ngModel)]="adminForm.category" placeholder="Category" />
          </div>
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea class="form-control" [(ngModel)]="adminForm.description" rows="2"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="closeAdminModal()">Cancel</button>
          <button class="btn btn-primary" (click)="saveAdminProduct()" [disabled]="saving">
            {{ saving ? 'Saving...' : 'Save' }}
          </button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showVendorSupplyModal" (click)="closeVendorSupplyModal()">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Add Product Supply</h3>
          <button class="close-btn" (click)="closeVendorSupplyModal()">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="formError" class="alert alert-danger">{{ formError }}</div>

          <div class="form-group">
            <label class="form-label">Use Existing Product (Optional)</label>
            <select class="form-control" [(ngModel)]="vendorSupplyForm.existingProductId">
              <option value="">Create New Product Entry</option>
              <option *ngFor="let p of allCatalogProducts" [value]="p.id">{{ p.name }} ({{ p.sku }})</option>
            </select>
          </div>

          <div *ngIf="!vendorSupplyForm.existingProductId">
            <div class="form-group">
              <label class="form-label">Product Name *</label>
              <input class="form-control" [(ngModel)]="vendorSupplyForm.name" />
            </div>
            <div class="form-group">
              <label class="form-label">SKU *</label>
              <input class="form-control" [(ngModel)]="vendorSupplyForm.sku" />
            </div>
            <div class="form-group">
              <label class="form-label">Category</label>
              <input class="form-control" [(ngModel)]="vendorSupplyForm.category" />
            </div>
            <div class="form-group">
              <label class="form-label">Description</label>
              <textarea class="form-control" [(ngModel)]="vendorSupplyForm.description" rows="2"></textarea>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Your Supply Price *</label>
            <input class="form-control" type="number" [(ngModel)]="vendorSupplyForm.vendorPrice" min="0.01" step="0.01" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="closeVendorSupplyModal()">Cancel</button>
          <button class="btn btn-primary" (click)="saveVendorSupply()" [disabled]="saving">
            {{ saving ? 'Saving...' : 'Save Supply' }}
          </button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showPriceModal" (click)="showPriceModal=false">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>Update Price: {{ selectedProduct?.name }}</h3>
          <button class="close-btn" (click)="showPriceModal=false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">New Vendor Price *</label>
            <input class="form-control" type="number" [(ngModel)]="updatedPrice" min="0.01" step="0.01" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="showPriceModal=false">Cancel</button>
          <button class="btn btn-primary" (click)="saveVendorPrice()" [disabled]="saving">Update</button>
        </div>
      </div>
    </div>
  `
})
export class ProductsComponent implements OnInit {
  products: Product[] = [];
  vendors: Vendor[] = [];
  allCatalogProducts: Product[] = [];
  loading = false;
  saving = false;

  showAdminModal = false;
  showVendorSupplyModal = false;
  showPriceModal = false;

  editing: Product | null = null;
  selectedProduct: Product | null = null;
  updatedPrice = 0;

  page = 0;
  totalPages = 0;
  totalElements = 0;

  filterName = '';
  filterCategory = '';
  filterVendorId = '';

  adminForm: ProductRequest = { name: '', sku: '', category: '', description: '' };
  vendorSupplyForm: VendorProductRequest = {
    existingProductId: '',
    name: '',
    sku: '',
    category: '',
    description: '',
    vendorPrice: 0
  };

  formError = '';

  constructor(private svc: ProductService, private vendorSvc: VendorService, public auth: AuthService) {}

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  get isVendor(): boolean {
    return this.auth.currentUser?.role === 'VENDOR';
  }

  ngOnInit(): void {
    this.load();
    if (!this.isVendor) {
      this.vendorSvc.getAll('', 0, 100).subscribe(r => this.vendors = r.data.content);
    }
    this.svc.getAll({}, 0, 200).subscribe(r => this.allCatalogProducts = r.data.content);
  }

  load(): void {
    this.loading = true;

    const request$ = this.isVendor
      ? this.svc.getMyVendorProducts(this.page)
      : this.svc.getAll({ name: this.filterName, category: this.filterCategory, vendorId: this.filterVendorId }, this.page);

    request$.subscribe({
      next: r => {
        this.products = r.data.content;
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

  getMyVendorPrice(product: Product): number {
    const myVendorId = this.auth.currentUser?.vendorId;
    const found = product.vendorSupplies.find(s => s.vendorId === myVendorId);
    return found?.vendorPrice ?? 0;
  }

  getBestVendorPrice(product: Product): number {
    if (product.vendorSupplies.length === 0) return 0;
    return product.vendorSupplies.reduce((min, current) => current.vendorPrice < min ? current.vendorPrice : min, product.vendorSupplies[0].vendorPrice);
  }

  openAdminModal(product?: Product): void {
    this.editing = product ?? null;
    this.adminForm = product
      ? { name: product.name, sku: product.sku, category: product.category || '', description: product.description || '' }
      : { name: '', sku: '', category: '', description: '' };
    this.formError = '';
    this.showAdminModal = true;
  }

  closeAdminModal(): void {
    this.showAdminModal = false;
  }

  saveAdminProduct(): void {
    if (!this.adminForm.name || !this.adminForm.sku) {
      this.formError = 'Name and SKU are required';
      return;
    }

    this.saving = true;
    const req$ = this.editing
      ? this.svc.update(this.editing.id, this.adminForm)
      : this.svc.create(this.adminForm);

    req$.subscribe({
      next: () => {
        this.saving = false;
        this.showAdminModal = false;
        this.load();
      },
      error: err => {
        this.saving = false;
        this.formError = err.error?.message || 'Save failed';
      }
    });
  }

  openVendorSupplyModal(): void {
    this.formError = '';
    this.vendorSupplyForm = {
      existingProductId: '',
      name: '',
      sku: '',
      category: '',
      description: '',
      vendorPrice: 0
    };
    this.showVendorSupplyModal = true;
  }

  closeVendorSupplyModal(): void {
    this.showVendorSupplyModal = false;
  }

  saveVendorSupply(): void {
    if (!this.vendorSupplyForm.existingProductId && (!this.vendorSupplyForm.name || !this.vendorSupplyForm.sku)) {
      this.formError = 'Name and SKU are required for new products';
      return;
    }

    if (!this.vendorSupplyForm.vendorPrice || this.vendorSupplyForm.vendorPrice <= 0) {
      this.formError = 'Vendor price must be greater than 0';
      return;
    }

    this.saving = true;
    this.svc.addMySupply(this.vendorSupplyForm).subscribe({
      next: () => {
        this.saving = false;
        this.showVendorSupplyModal = false;
        this.load();
      },
      error: err => {
        this.saving = false;
        this.formError = err.error?.message || 'Failed to add supply';
      }
    });
  }

  openPriceModal(product: Product): void {
    this.selectedProduct = product;
    this.updatedPrice = this.getMyVendorPrice(product);
    this.showPriceModal = true;
  }

  saveVendorPrice(): void {
    if (!this.selectedProduct) return;
    if (this.updatedPrice <= 0) return;

    this.saving = true;
    this.svc.updateMyPrice(this.selectedProduct.id, this.updatedPrice).subscribe({
      next: () => {
        this.saving = false;
        this.showPriceModal = false;
        this.load();
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  delete(id: string): void {
    if (!confirm('Delete this product?')) return;
    this.svc.delete(id).subscribe(() => this.load());
  }
}
