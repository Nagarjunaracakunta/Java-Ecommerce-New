import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { NgFor, NgIf, CurrencyPipe } from '@angular/common';
import { CartService } from '../../core/services/cart.service';
import { CartItemResponse } from '../../core/models';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [RouterLink, NgIf, CurrencyPipe, MatCardModule, MatButtonModule,
            MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, MatDividerModule, MatTableModule],
  template: `
    <div class="page-container">
      <h1>Your Cart</h1>

      <div *ngIf="loading" class="center"><mat-spinner></mat-spinner></div>

      <div *ngIf="!loading && items.length === 0" class="empty-state">
        <mat-icon>shopping_cart</mat-icon>
        <p>Your cart is empty.</p>
        <a mat-raised-button color="primary" routerLink="/products">Browse Menu</a>
      </div>

      <div *ngIf="!loading && items.length > 0">
        <table mat-table [dataSource]="items" class="cart-table mat-elevation-z2">
          <ng-container matColumnDef="name">
            <th mat-header-cell *matHeaderCellDef>Item</th>
            <td mat-cell *matCellDef="let row">{{ row.productName }}</td>
          </ng-container>
          <ng-container matColumnDef="price">
            <th mat-header-cell *matHeaderCellDef>Price</th>
            <td mat-cell *matCellDef="let row">{{ row.price | currency }}</td>
          </ng-container>
          <ng-container matColumnDef="qty">
            <th mat-header-cell *matHeaderCellDef>Qty</th>
            <td mat-cell *matCellDef="let row">{{ row.quantity }}</td>
          </ng-container>
          <ng-container matColumnDef="subtotal">
            <th mat-header-cell *matHeaderCellDef>Subtotal</th>
            <td mat-cell *matCellDef="let row">{{ row.subtotal | currency }}</td>
          </ng-container>
          <ng-container matColumnDef="remove">
            <th mat-header-cell *matHeaderCellDef></th>
            <td mat-cell *matCellDef="let row">
              <button mat-icon-button color="warn" (click)="remove(row.productId)">
                <mat-icon>delete</mat-icon>
              </button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="cols"></tr>
          <tr mat-row *matRowDef="let row; columns: cols;"></tr>
        </table>

        <mat-card class="summary-card">
          <mat-card-content>
            <div class="total-row">
              <span class="total-label">Total</span>
              <span class="total-amount">{{ total | currency }}</span>
            </div>
          </mat-card-content>
          <mat-card-actions>
            <a mat-raised-button color="primary" routerLink="/checkout">Proceed to Checkout</a>
            <button mat-stroked-button color="warn" (click)="clearCart()">Clear Cart</button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:900px; margin:0 auto; }
    h1 { margin-bottom:24px; }
    .cart-table { width:100%; }
    .summary-card { margin-top:24px; }
    .total-row { display:flex; justify-content:space-between; align-items:center; padding:8px 0; }
    .total-label { font-size:18px; font-weight:500; }
    .total-amount { font-size:28px; font-weight:bold; color:#1976d2; }
    mat-card-actions { display:flex; gap:12px; padding:16px; }
    .center { display:flex; justify-content:center; padding:48px; }
    .empty-state { text-align:center; padding:64px; color:#999; }
    .empty-state mat-icon { font-size:64px; height:64px; width:64px; margin-bottom:16px; }
  `]
})
export class CartComponent implements OnInit {
  items: CartItemResponse[] = [];
  total = 0;
  loading = true;
  cols = ['name', 'price', 'qty', 'subtotal', 'remove'];

  constructor(private cartService: CartService, private snack: MatSnackBar) {}

  ngOnInit() { this.loadCart(); }

  loadCart() {
    this.loading = true;
    this.cartService.getCart().subscribe({
      next: c => { this.items = c.items; this.total = c.total; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  remove(productId: number) {
    this.cartService.removeItem(productId).subscribe({
      next: c => { this.items = c.items; this.total = c.total; },
      error: err => this.snack.open(err.error?.error || 'Remove failed', 'Close', { duration: 3000 })
    });
  }

  clearCart() {
    this.cartService.clearCart().subscribe({
      next: () => { this.items = []; this.total = 0; },
      error: () => this.snack.open('Failed to clear cart', 'Close', { duration: 3000 })
    });
  }
}