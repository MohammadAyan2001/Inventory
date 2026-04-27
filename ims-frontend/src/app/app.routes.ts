import { Routes } from '@angular/router';
import { adminGuard, authGuard } from '../core/guards/auth.guard';
import { ShellComponent } from './shell.component';
import { LoginComponent } from '../features/auth/login.component';
import { RegisterComponent } from '../features/auth/register.component';
import { DashboardComponent } from '../features/dashboard/dashboard.component';
import { ProductsComponent } from '../features/products/products.component';
import { InventoryComponent } from '../features/inventory/inventory.component';
import { OrdersComponent } from '../features/orders/orders.component';
import { VendorsComponent } from '../features/vendors/vendors.component';
import { SalesComponent } from '../features/sales/sales.component';
import { StorefrontComponent } from '../features/storefront/storefront.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'products', component: ProductsComponent },
      { path: 'inventory', component: InventoryComponent, canActivate: [adminGuard] },
      { path: 'orders', component: OrdersComponent },
      { path: 'vendors', component: VendorsComponent, canActivate: [adminGuard] },
      { path: 'sales', component: SalesComponent, canActivate: [adminGuard] },
      { path: 'store', component: StorefrontComponent, canActivate: [adminGuard] },
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
