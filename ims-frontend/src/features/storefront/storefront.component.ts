import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, from } from 'rxjs';
import { concatMap, toArray } from 'rxjs/operators';
import { InventoryService, ProductService } from '../../core/services/api.service';
import { Inventory, Product } from '../../shared/models';

interface StoreProduct {
  productId: string;
  name: string;
  sku: string;
  category: string;
  description: string;
  price: number;
  available: number;
  totalSold: number;
}

interface CartLine {
  productId: string;
  name: string;
  category: string;
  price: number;
  quantity: number;
  maxAvailable: number;
}

@Component({
  selector: 'app-storefront',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="store-hero">
      <div>
        <p class="store-kicker">Ecommerce Operations</p>
        <h2>Multi-Category Storefront</h2>
        <p>
          Browse by category, add items to cart, and complete customer checkout in one flow.
        </p>
      </div>
      <div class="store-hero-metrics">
        <div><span>{{ filteredProducts.length }}</span> Products</div>
        <div><span>{{ totalUnitsAvailable }}</span> In Stock</div>
      </div>
    </section>

    <div class="store-layout">
      <section>
        <div class="card store-filter-card">
          <div class="store-filter-row">
            <input
              class="form-control"
              placeholder="Search products..."
              [(ngModel)]="searchTerm" />
            <select class="form-control" [(ngModel)]="selectedCategory">
              <option value="">All categories</option>
              <option *ngFor="let category of categories" [value]="category">{{ category }}</option>
            </select>
            <select class="form-control" [(ngModel)]="sortBy">
              <option value="popular">Most Popular</option>
              <option value="price-asc">Price Low to High</option>
              <option value="price-desc">Price High to Low</option>
              <option value="stock">Available Stock</option>
            </select>
            <button class="btn btn-secondary" (click)="refreshCatalog()">Refresh</button>
          </div>
        </div>

        <div class="store-grid">
          <article class="store-product-card" *ngFor="let product of filteredProducts">
            <div class="store-product-head">
              <span class="badge badge-info">{{ product.category }}</span>
              <span class="store-stock" [class.store-stock-low]="product.available < 10">
                {{ product.available }} left
              </span>
            </div>
            <h3>{{ product.name }}</h3>
            <p>{{ product.description || 'Premium catalog item' }}</p>
            <div class="store-product-foot">
              <div>
                <strong>\${{ product.price | number:'1.2-2' }}</strong>
                <small>{{ product.sku }}</small>
              </div>
              <button
                class="btn btn-primary btn-sm"
                (click)="addToCart(product)"
                [disabled]="product.available <= 0">
                Add To Cart
              </button>
            </div>
          </article>
        </div>
      </section>

      <aside class="card store-cart">
        <div class="card-header">
          <h2>Customer Cart</h2>
          <span class="badge badge-secondary">{{ cartCount }} items</span>
        </div>

        <div class="card-body">
          <div class="store-cart-empty" *ngIf="cart.length === 0">
            No items in cart yet.
          </div>

          <div class="store-cart-line" *ngFor="let item of cart">
            <div>
              <strong>{{ item.name }}</strong>
              <small>{{ item.category }} · \${{ item.price | number:'1.2-2' }}</small>
            </div>
            <div class="store-cart-actions">
              <button class="btn btn-secondary btn-sm" (click)="changeQuantity(item, -1)">-</button>
              <span>{{ item.quantity }}</span>
              <button class="btn btn-secondary btn-sm" (click)="changeQuantity(item, 1)">+</button>
              <button class="btn btn-danger btn-sm" (click)="removeFromCart(item.productId)">×</button>
            </div>
          </div>

          <div class="store-cart-summary">
            <div><span>Subtotal</span><strong>\${{ cartSubtotal | number:'1.2-2' }}</strong></div>
            <div><span>Estimated Tax</span><strong>\${{ cartTax | number:'1.2-2' }}</strong></div>
            <div class="store-total">
              <span>Total</span>
              <strong>\${{ cartTotal | number:'1.2-2' }}</strong>
            </div>
          </div>

          <div *ngIf="message" class="alert" [class.alert-success]="success" [class.alert-danger]="!success">
            {{ message }}
          </div>

          <button class="btn btn-success" style="width:100%" (click)="checkout()" [disabled]="cart.length === 0 || processing">
            {{ processing ? 'Processing Checkout...' : 'Checkout Order' }}
          </button>
        </div>
      </aside>
    </div>
  `
})
export class StorefrontComponent implements OnInit {
  products: StoreProduct[] = [];
  cart: CartLine[] = [];
  categories: string[] = [];

  searchTerm = '';
  selectedCategory = '';
  sortBy: 'popular' | 'price-asc' | 'price-desc' | 'stock' = 'popular';

  processing = false;
  message = '';
  success = true;

  constructor(
    private inventoryService: InventoryService,
    private productService: ProductService
  ) {}

  ngOnInit(): void {
    this.refreshCatalog();
  }

  get filteredProducts(): StoreProduct[] {
    const search = this.searchTerm.trim().toLowerCase();

    const filtered = this.products.filter(product => {
      const categoryMatch = !this.selectedCategory || product.category === this.selectedCategory;
      const searchMatch = !search
        || product.name.toLowerCase().includes(search)
        || product.sku.toLowerCase().includes(search);
      return categoryMatch && searchMatch;
    });

    const sorted = [...filtered];
    if (this.sortBy === 'price-asc') {
      sorted.sort((a, b) => a.price - b.price);
    } else if (this.sortBy === 'price-desc') {
      sorted.sort((a, b) => b.price - a.price);
    } else if (this.sortBy === 'stock') {
      sorted.sort((a, b) => b.available - a.available);
    } else {
      sorted.sort((a, b) => b.totalSold - a.totalSold);
    }

    return sorted;
  }

  get cartCount(): number {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0);
  }

  get cartSubtotal(): number {
    return this.cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }

  get cartTax(): number {
    return this.cartSubtotal * 0.18;
  }

  get cartTotal(): number {
    return this.cartSubtotal + this.cartTax;
  }

  get totalUnitsAvailable(): number {
    return this.products.reduce((sum, product) => sum + product.available, 0);
  }

  refreshCatalog(): void {
    forkJoin({
      inventory: this.inventoryService.getAll(),
      products: this.productService.getAll({}, 0, 1000)
    }).subscribe({
      next: ({ inventory, products }) => {
        this.products = this.mapCatalog(inventory.data, products.data.content);
        this.categories = this.buildCategories(this.products);
      },
      error: err => {
        this.success = false;
        this.message = err.error?.message || 'Failed to load storefront catalog';
      }
    });
  }

  addToCart(product: StoreProduct): void {
    const existing = this.cart.find(item => item.productId === product.productId);
    if (existing) {
      if (existing.quantity < existing.maxAvailable) {
        existing.quantity += 1;
      }
      return;
    }

    this.cart.push({
      productId: product.productId,
      name: product.name,
      category: product.category,
      price: product.price,
      quantity: 1,
      maxAvailable: product.available
    });
  }

  changeQuantity(item: CartLine, delta: number): void {
    const next = item.quantity + delta;
    if (next <= 0) {
      this.removeFromCart(item.productId);
      return;
    }
    if (next > item.maxAvailable) {
      return;
    }
    item.quantity = next;
  }

  removeFromCart(productId: string): void {
    this.cart = this.cart.filter(item => item.productId !== productId);
  }

  checkout(): void {
    if (this.cart.length === 0) {
      return;
    }

    this.processing = true;
    this.message = '';

    const payload = this.cart.map(item => ({ productId: item.productId, quantity: item.quantity }));

    from(payload).pipe(
      concatMap(item => this.inventoryService.sell(item)),
      toArray()
    ).subscribe({
      next: () => {
        const amount = this.cartTotal;
        this.processing = false;
        this.success = true;
        this.message = `Order processed successfully. Total charged: $${amount.toFixed(2)}`;
        this.cart = [];
        this.refreshCatalog();
      },
      error: err => {
        this.processing = false;
        this.success = false;
        this.message = err.error?.message || 'Checkout failed. Please review stock and try again.';
      }
    });
  }

  private mapCatalog(inventory: Inventory[], products: Product[]): StoreProduct[] {
    const productMap = new Map(products.map(product => [product.id, product]));

    return inventory.map(item => {
      const product = productMap.get(item.productId);
      return {
        productId: item.productId,
        name: item.productName,
        sku: item.productSku,
        category: product?.category || 'General',
        description: product?.description || '',
        price: item.sellingPrice,
        available: item.quantityAvailable,
        totalSold: item.totalSold
      };
    });
  }

  private buildCategories(products: StoreProduct[]): string[] {
    return Array.from(new Set(products.map(product => product.category))).sort((a, b) => a.localeCompare(b));
  }
}
