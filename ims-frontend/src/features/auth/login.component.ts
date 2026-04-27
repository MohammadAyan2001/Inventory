import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <h1>Commerce IMS</h1>
        <p>Tenant-aware inventory and ecommerce control panel</p>

        <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label">Business Tenant</label>
            <input class="form-control" [(ngModel)]="tenantId" name="tenantId"
                   placeholder="acme-store" required />
          </div>
          <div class="form-group">
            <label class="form-label">Email</label>
            <input class="form-control" type="email" [(ngModel)]="email" name="email"
                   placeholder="admin@ims.com" required />
          </div>
          <div class="form-group">
            <label class="form-label">Password</label>
            <input class="form-control" type="password" [(ngModel)]="password" name="password"
                   placeholder="••••••••" required />
          </div>
          <button class="btn btn-primary" style="width:100%" type="submit" [disabled]="loading">
            <span *ngIf="loading" class="spinner"></span>
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>

        <p style="margin-top:16px;text-align:center;color:var(--text-muted)">
          No account? <a routerLink="/register">Register</a>
        </p>
      </div>
    </div>
  `
})
export class LoginComponent {
  email = '';
  password = '';
  tenantId = 'public';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {
    this.tenantId = this.auth.getTenantId() || 'public';
  }

  onSubmit(): void {
    this.auth.setTenantId(this.tenantId);
    this.loading = true; this.error = '';
    this.auth.login({ email: this.email, password: this.password, tenantId: this.tenantId }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => { this.error = err.error?.message || 'Invalid credentials'; this.loading = false; }
    });
  }
}
