import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="layout">
      <aside class="sidebar">
        <div class="sidebar-logo">
          <span class="logo-mark">IMS</span>
          <small>{{ tenantId }}</small>
        </div>
        <nav class="sidebar-nav">
          <a class="nav-item" routerLink="/dashboard" routerLinkActive="active">
            <span class="icon">▦</span> Dashboard
          </a>
          <a class="nav-item" routerLink="/products" routerLinkActive="active">
            <span class="icon">◫</span> Products
          </a>
          <a *ngIf="isAdmin" class="nav-item" routerLink="/inventory" routerLinkActive="active">
            <span class="icon">◧</span> Inventory
          </a>
          <a class="nav-item" routerLink="/orders" routerLinkActive="active">
            <span class="icon">◎</span> Orders
          </a>
          <a *ngIf="isAdmin" class="nav-item" routerLink="/store" routerLinkActive="active">
            <span class="icon">◍</span> Storefront
          </a>
          <a *ngIf="isAdmin" class="nav-item" routerLink="/vendors" routerLinkActive="active">
            <span class="icon">◨</span> Vendors
          </a>
          <a *ngIf="isAdmin" class="nav-item" routerLink="/sales" routerLinkActive="active">
            <span class="icon">◔</span> Sales
          </a>
        </nav>
        <div class="sidebar-foot">
          <div style="font-size:12px;color:var(--text-muted);margin-bottom:8px">
            {{ currentUser?.email }}<br>
            <span class="badge badge-info">{{ currentUser?.role }}</span>
          </div>
          <button class="btn btn-secondary btn-sm" style="width:100%" (click)="logout()">Sign Out</button>
        </div>
      </aside>

      <!-- Main -->
      <div class="main-content">
        <header class="topbar">
          <span style="font-weight:600;font-size:15px">Commerce Command Center</span>
          <span style="color:var(--text-muted);font-size:13px">Tenant: {{ tenantId }}</span>
        </header>
        <div class="page-content">
          <router-outlet />
        </div>
      </div>
    </div>
  `
})
export class ShellComponent {
  constructor(private auth: AuthService) {}
  get currentUser() { return this.auth.currentUser; }
  get isAdmin() { return this.auth.isAdmin(); }
  get tenantId() { return this.auth.getTenantId() || 'public'; }
  logout() { this.auth.logout(); }
}
