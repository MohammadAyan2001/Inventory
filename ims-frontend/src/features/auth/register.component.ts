import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Create Account</h1>
        <p>Set up your tenant workspace and role</p>

        <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label">Business Tenant</label>
            <input class="form-control" [(ngModel)]="tenantId" name="tenantId" placeholder="acme-store" required />
          </div>
          <div class="form-group">
            <label class="form-label">Full Name</label>
            <input class="form-control" [(ngModel)]="name" name="name" placeholder="John Doe" required />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input class="form-control" type="email" [(ngModel)]="email" name="email" placeholder="john@example.com" required />
          </div>
          <div class="form-group">
            <label class="form-label">Password</label>
            <input class="form-control" type="password" [(ngModel)]="password" name="password" placeholder="Min 8 characters" required />
          </div>
          <div class="form-group">
            <label class="form-label">Role</label>
            <select class="form-control" [(ngModel)]="role" name="role">
              <option value="VENDOR">Vendor</option>
              <option value="ADMIN">Admin</option>
              <option value="WAREHOUSE_OPERATOR">Warehouse Operator</option>
            </select>
          </div>
          <button class="btn btn-primary" style="width:100%" type="submit" [disabled]="loading">
            {{ loading ? 'Creating...' : 'Create Account' }}
          </button>
        </form>

        <p style="margin-top:16px;text-align:center;color:var(--text-muted)">
          Already have an account? <a routerLink="/login">Sign in</a>
        </p>
      </div>
    </div>
  `
})
export class RegisterComponent {
  name = '';
  email = '';
  password = '';
  role = 'VENDOR';
  tenantId = 'public';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {
    this.tenantId = this.auth.getTenantId() || 'public';
  }

  onSubmit(): void {
    this.auth.setTenantId(this.tenantId);
    this.loading = true; this.error = '';
    this.auth.register({
      name: this.name,
      email: this.email,
      password: this.password,
      role: this.role,
      tenantId: this.tenantId
    }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => { this.error = err.error?.message || 'Registration failed'; this.loading = false; }
    });
  }
}
