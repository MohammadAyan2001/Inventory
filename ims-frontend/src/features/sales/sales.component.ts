import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventoryService } from '../../core/services/api.service';
import { Inventory } from '../../shared/models';

@Component({
  selector: 'app-sales',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="font-size:20px;font-weight:700">Sales Entry</h2>
      <button class="btn btn-secondary" (click)="loadInventory()">Refresh Stock</button>
    </div>

    <div class="card" style="max-width:760px">
      <div class="card-header"><h2>Record Customer Sale</h2></div>
      <div class="card-body">
        <div *ngIf="message" class="alert" [class.alert-success]="success" [class.alert-danger]="!success">{{ message }}</div>

        <div class="form-group">
          <label class="form-label">Product *</label>
          <select class="form-control" [(ngModel)]="selectedProductId" (change)="onProductChange()">
            <option value="">Select product</option>
            <option *ngFor="let i of inventory" [value]="i.productId">
              {{ i.productName }} (Available: {{ i.quantityAvailable }})
            </option>
          </select>
        </div>

        <div *ngIf="selectedItem" style="display:grid;grid-template-columns:repeat(3,minmax(120px,1fr));gap:12px;margin-bottom:12px">
          <div><small style="color:var(--text-muted)">Available</small><div style="font-weight:700">{{ selectedItem.quantityAvailable }}</div></div>
          <div><small style="color:var(--text-muted)">Selling Price</small><div style="font-weight:700">\${{ selectedItem.sellingPrice | number:'1.2-2' }}</div></div>
          <div><small style="color:var(--text-muted)">SKU</small><div style="font-weight:700">{{ selectedItem.productSku }}</div></div>
        </div>

        <div class="form-group">
          <label class="form-label">Quantity *</label>
          <input class="form-control" type="number" [(ngModel)]="quantity" min="1" />
        </div>

        <button class="btn btn-primary" (click)="recordSale()" [disabled]="saving || !selectedItem">
          {{ saving ? 'Saving...' : 'Record Sale' }}
        </button>
      </div>
    </div>

    <div class="card" style="margin-top:20px">
      <div class="card-header"><h2>Current Stock Snapshot</h2></div>
      <div class="table-wrap">
        <table>
          <thead><tr><th>Product</th><th>Available</th><th>Selling Price</th><th>Total Sold</th></tr></thead>
          <tbody>
            <tr *ngFor="let i of inventory">
              <td>{{ i.productName }}</td>
              <td>{{ i.quantityAvailable }}</td>
              <td>\${{ i.sellingPrice | number:'1.2-2' }}</td>
              <td>{{ i.totalSold }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class SalesComponent implements OnInit {
  inventory: Inventory[] = [];
  selectedProductId = '';
  selectedItem: Inventory | null = null;
  quantity = 1;
  saving = false;
  message = '';
  success = true;

  constructor(private inventorySvc: InventoryService) {}

  ngOnInit(): void {
    this.loadInventory();
  }

  loadInventory(): void {
    this.inventorySvc.getAll().subscribe(r => {
      this.inventory = r.data;
      if (this.selectedProductId) {
        this.selectedItem = this.inventory.find(i => i.productId === this.selectedProductId) ?? null;
      }
    });
  }

  onProductChange(): void {
    this.selectedItem = this.inventory.find(i => i.productId === this.selectedProductId) ?? null;
  }

  recordSale(): void {
    if (!this.selectedItem || this.quantity <= 0) return;

    this.saving = true;
    this.message = '';

    this.inventorySvc.sell({ productId: this.selectedItem.productId, quantity: this.quantity }).subscribe({
      next: () => {
        this.success = true;
        this.message = 'Sale recorded successfully';
        this.quantity = 1;
        this.saving = false;
        this.loadInventory();
      },
      error: err => {
        this.success = false;
        this.message = err.error?.message || 'Failed to record sale';
        this.saving = false;
      }
    });
  }
}
