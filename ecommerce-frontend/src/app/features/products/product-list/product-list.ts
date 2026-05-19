import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { NgFor, NgIf, CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product } from '../../../core/models';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [RouterLink, NgFor, NgIf, CurrencyPipe, MatCardModule, MatButtonModule,
            MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, MatBadgeModule, MatChipsModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Our Menu</h1>
        <a routerLink="/cart" mat-stroked-button color="primary">
          <mat-icon>shopping_cart</mat-icon> View Cart
        </a>
      </div>

      <div *ngIf="loading" class="center">
        <mat-spinner></mat-spinner>
      </div>

      <div class="product-grid" *ngIf="!loading">
        <mat-card *ngFor="let p of products" class="product-card">
          <mat-card-header>
            <mat-card-title>{{ p.name }}</mat-card-title>
            <mat-card-subtitle>
              <mat-chip-set>
                <mat-chip>{{ p.category }}</mat-chip>
              </mat-chip-set>
            </mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p class="description">{{ p.description }}</p>
            <p class="price">{{ p.price | currency }}</p>
            <p class="availability" [class.unavailable]="p.stock === 0">
              {{ p.stock > 0 ? '✓ In Stock (' + p.stock + ')' : '✗ Out of Stock' }}
            </p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" [disabled]="p.stock === 0"
                    (click)="addToCart(p)">
              <mat-icon>add_shopping_cart</mat-icon> Add to Cart
            </button>
            <a mat-button [routerLink]="['/products', p.id]">Details</a>
            <ng-container *ngIf="isAdmin">
              <a mat-icon-button color="accent" [routerLink]="['/admin']" [queryParams]="{edit: p.id}">
                <mat-icon>edit</mat-icon>
              </a>
            </ng-container>
          </mat-card-actions>
        </mat-card>
      </div>

      <div *ngIf="!loading && products.length === 0" class="empty-state">
        <mat-icon>restaurant_menu</mat-icon>
        <p>No products available yet.</p>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:1200px; margin:0 auto; }
    .page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
    .page-header h1 { margin:0; }
    .product-grid { display:grid; grid-template-columns:repeat(auto-fill, minmax(280px, 1fr)); gap:20px; }
    .product-card { display:flex; flex-direction:column; }
    mat-card-content { flex:1; }
    .description { color:#666; font-size:14px; margin-bottom:8px; }
    .price { font-size:24px; font-weight:bold; color:#1976d2; margin:8px 0; }
    .availability { font-size:13px; color:#4caf50; }
    .availability.unavailable { color:#f44336; }
    mat-card-actions { padding:8px 16px; }
    .center { display:flex; justify-content:center; padding:48px; }
    .empty-state { text-align:center; padding:64px; color:#999; }
    .empty-state mat-icon { font-size:64px; height:64px; width:64px; }
  `]
})
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  isLoggedIn = false;
  isAdmin = false;

  constructor(private productService: ProductService, private cartService: CartService,
              private auth: AuthService, private snack: MatSnackBar, private router: Router) {}

  ngOnInit() {
    this.isLoggedIn = this.auth.isLoggedIn();
    this.isAdmin = this.auth.isAdmin();
    this.productService.getAll().subscribe({
      next: p => { this.products = p; this.loading = false; },
      error: () => { this.loading = false; this.snack.open('Failed to load products', 'Close', { duration: 3000 }); }
    });
  }

  addToCart(p: Product) {
    if (!this.isLoggedIn) {
      this.snack.open('Please sign in to add items to cart', 'Sign In', { duration: 3000 })
        .onAction().subscribe(() => this.router.navigate(['/login']));
      return;
    }
    this.cartService.addItem({ productId: p.id, quantity: 1 }).subscribe({
      next: () => this.snack.open(`${p.name} added to cart!`, 'View Cart', { duration: 2500 })
        .onAction().subscribe(() => this.router.navigate(['/cart'])),
      error: err => this.snack.open(err.error?.error || 'Failed to add to cart', 'Close', { duration: 3000 })
    });
  }
}