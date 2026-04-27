import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { ApiResponse, AuthResponse, LoginRequest, RegisterRequest } from '../../shared/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'ims_token';
  private readonly USER_KEY  = 'ims_user';
  private readonly TENANT_KEY = 'ims_tenant';

  private currentUserSubject = new BehaviorSubject<AuthResponse | null>(this.loadUser());
  currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    const currentTenant = this.currentUserSubject.value?.tenantId;
    if (!this.getTenantId() && currentTenant) {
      this.setTenantId(currentTenant);
    }
  }

  login(req: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    this.setTenantId(req.tenantId || this.getTenantId() || 'public');
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/login`, req).pipe(
      tap(res => this.storeSession(res.data))
    );
  }

  register(req: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    this.setTenantId(req.tenantId || this.getTenantId() || 'public');
    return this.http.post<ApiResponse<AuthResponse>>(`${environment.apiUrl}/auth/register`, req).pipe(
      tap(res => this.storeSession(res.data))
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.currentUserSubject.next(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getTenantId(): string | null {
    return localStorage.getItem(this.TENANT_KEY);
  }

  setTenantId(tenantId: string): void {
    const normalized = tenantId?.trim().toLowerCase();
    if (normalized) {
      localStorage.setItem(this.TENANT_KEY, normalized);
    }
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const role = this.currentUserSubject.value?.role;
    return role === 'ADMIN' || role === 'WAREHOUSE_OPERATOR';
  }

  isVendor(): boolean {
    return this.currentUserSubject.value?.role === 'VENDOR';
  }

  get currentUser(): AuthResponse | null {
    return this.currentUserSubject.value;
  }

  private storeSession(user: AuthResponse): void {
    localStorage.setItem(this.TOKEN_KEY, user.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    if (user.tenantId) {
      this.setTenantId(user.tenantId);
    }
    this.currentUserSubject.next(user);
  }

  private loadUser(): AuthResponse | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
}
