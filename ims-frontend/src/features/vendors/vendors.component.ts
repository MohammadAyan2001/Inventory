import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VendorService } from '../../core/services/api.service';
import { Vendor, VendorRequest } from '../../shared/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-vendors',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <h2 style="font-size:20px;font-weight:700">Vendors</h2>
      <button *ngIf="isAdmin" class="btn btn-primary" (click)="openModal()">+ Add Vendor</button>
    </div>

    <div class="card" style="margin-bottom:16px">
      <div class="card-body">
        <input class="form-control" style="max-width:280px" placeholder="Search by name..." [(ngModel)]="search" (input)="load()" />
      </div>
    </div>

    <div class="card">
      <div *ngIf="loading" class="loading-overlay"><div class="spinner"></div></div>
      <div class="table-wrap" *ngIf="!loading">
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Address</th><th *ngIf="isAdmin">Actions</th></tr></thead>
          <tbody>
            <tr *ngFor="let v of vendors">
              <td><strong>{{ v.name }}</strong></td>
              <td>{{ v.email }}</td>
              <td>{{ v.phone || '—' }}</td>
              <td>{{ v.address || '—' }}</td>
              <td *ngIf="isAdmin">
                <button class="btn btn-secondary btn-sm" (click)="openModal(v)">Edit</button>
                <button class="btn btn-danger btn-sm" style="margin-left:4px" (click)="delete(v.id)">Del</button>
              </td>
            </tr>
            <tr *ngIf="vendors.length===0"><td colspan="5" class="empty-state">No vendors found</td></tr>
          </tbody>
        </table>
      </div>
      <div style="padding:12px 16px;display:flex;justify-content:space-between">
        <span style="font-size:13px;color:var(--text-muted)">{{ totalElements }} total</span>
        <div class="pagination">
          <button class="page-btn" [disabled]="page===0" (click)="changePage(page-1)">‹</button>
          <button class="page-btn active">{{ page+1 }}</button>
          <button class="page-btn" [disabled]="page>=totalPages-1" (click)="changePage(page+1)">›</button>
        </div>
      </div>
    </div>

    <div class="modal-overlay" *ngIf="showModal" (click)="closeModal()">
      <div class="modal" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3>{{ editing ? 'Edit Vendor' : 'New Vendor' }}</h3>
          <button class="close-btn" (click)="closeModal()">×</button>
        </div>
        <div class="modal-body">
          <div *ngIf="formError" class="alert alert-danger">{{ formError }}</div>
          <div class="form-group"><label class="form-label">Name *</label><input class="form-control" [(ngModel)]="form.name" /></div>
          <div class="form-group"><label class="form-label">Email *</label><input class="form-control" type="email" [(ngModel)]="form.email" [disabled]="!!editing" /></div>
          <div class="form-group"><label class="form-label">Phone</label><input class="form-control" [(ngModel)]="form.phone" /></div>
          <div class="form-group"><label class="form-label">Address</label><input class="form-control" [(ngModel)]="form.address" /></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-secondary" (click)="closeModal()">Cancel</button>
          <button class="btn btn-primary" (click)="save()" [disabled]="saving">{{ saving ? 'Saving...' : 'Save' }}</button>
        </div>
      </div>
    </div>
  `
})
export class VendorsComponent implements OnInit {
  vendors: Vendor[] = [];
  loading = false; saving = false; showModal = false;
  editing: Vendor | null = null;
  page = 0; totalPages = 0; totalElements = 0;
  search = '';
  form: VendorRequest = { name: '', email: '', phone: '', address: '' };
  formError = '';

  constructor(private svc: VendorService, public auth: AuthService) {}
  get isAdmin() { return this.auth.isAdmin(); }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.svc.getAll(this.search, this.page).subscribe({
      next: r => { this.vendors = r.data.content; this.totalPages = r.data.totalPages; this.totalElements = r.data.totalElements; this.loading = false; },
      error: () => this.loading = false
    });
  }

  changePage(p: number): void { this.page = p; this.load(); }

  openModal(v?: Vendor): void {
    this.editing = v || null;
    this.form = v ? { name: v.name, email: v.email, phone: v.phone || '', address: v.address || '' } : { name: '', email: '', phone: '', address: '' };
    this.formError = ''; this.showModal = true;
  }

  closeModal(): void { this.showModal = false; }

  save(): void {
    if (!this.form.name || !this.form.email) { this.formError = 'Name and Email are required'; return; }
    this.saving = true;
    const obs = this.editing ? this.svc.update(this.editing.id, this.form) : this.svc.create(this.form);
    obs.subscribe({
      next: () => { this.saving = false; this.closeModal(); this.load(); },
      error: err => { this.formError = err.error?.message || 'Save failed'; this.saving = false; }
    });
  }

  delete(id: string): void {
    if (!confirm('Delete this vendor?')) return;
    this.svc.delete(id).subscribe(() => this.load());
  }
}
