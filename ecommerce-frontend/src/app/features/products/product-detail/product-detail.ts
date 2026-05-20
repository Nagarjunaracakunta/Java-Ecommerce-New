import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { NgIf, CurrencyPipe } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product } from '../../../core/models';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [RouterLink, NgIf, CurrencyPipe, MatCardModule, MatButtonModule,
            MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule],
  template: `
    <div class="page-container">
      <a mat-button routerLink="/products"><mat-icon>arrow_back</mat-icon> Back to Menu</a>

      <div *ngIf="loading" class="center"><mat-spinner></mat-spinner></div>

      <mat-card *ngIf="product && !loading" class="detail-card">
        <mat-card-header>
          <mat-card-title>{{ product.name }}</mat-card-title>
          <mat-card-subtitle>
            <mat-chip-set><mat-chip>{{ product.category }}</mat-chip></mat-chip-set>
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p class="description">{{ product.description }}</p>
          <div class="info-row">
            <span class="price">{{ product.price | currency }}</span>
            <span class="availability" [class.unavailable]="product.stock === 0">
              {{ product.stock > 0 ? '✓ In Stock (' + product.stock + ')' : '✗ Out of Stock' }}
            </span>
          </div>
        </mat-card-content>
        <mat-card-actions>
          <button mat-raised-button color="primary"
                  [disabled]="product.stock === 0"
                  (click)="addToCart()">
            <mat-icon>add_shopping_cart</mat-icon> Add to Cart
          </button>
          <a mat-stroked-button routerLink="/cart">View Cart</a>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:700px; margin:0 auto; }
    .detail-card { margin-top:16px; padding:16px; }
    .description { font-size:16px; color:#555; margin:16px 0; line-height:1.6; }
    .info-row { display:flex; align-items:center; gap:24px; margin-top:16px; }
    .price { font-size:32px; font-weight:bold; color:#1976d2; }
    .availability { font-size:14px; color:#4caf50; }
    .availability.unavailable { color:#f44336; }
    .center { display:flex; justify-content:center; padding:48px; }
    mat-card-actions { padding:16px; }
  `]
})
export class ProductDetailComponent implements OnInit {
  product: Product | null = null;
  loading = true;
  isLoggedIn = false;

  constructor(private route: ActivatedRoute, private productService: ProductService,
              private cartService: CartService, private auth: AuthService,
              private snack: MatSnackBar) {}

  ngOnInit() {
    this.isLoggedIn = this.auth.isLoggedIn();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.productService.getById(id).subscribe({
      next: p => { this.product = p; this.loading = false; },
      error: () => { this.loading = false; this.snack.open('Product not found', 'Close', { duration: 3000 }); }
    });
  }

  addToCart() {
    if (!this.product) return;
    this.cartService.addItem({ productId: this.product.id, quantity: 1 }).subscribe({
      next: () => this.snack.open('Added to cart!', 'Close', { duration: 2500 }),
      error: err => this.snack.open(err.error?.error || 'Failed to add', 'Close', { duration: 3000 })
    });
  }
}