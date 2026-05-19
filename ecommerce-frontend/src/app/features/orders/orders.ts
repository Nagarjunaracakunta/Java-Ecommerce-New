import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDividerModule } from '@angular/material/divider';
import { NgFor, NgIf, CurrencyPipe, DatePipe } from '@angular/common';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [RouterLink, NgFor, NgIf, CurrencyPipe, DatePipe, MatCardModule, MatButtonModule,
            MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule,
            MatExpansionModule, MatDividerModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>My Orders</h1>
        <a mat-raised-button color="primary" routerLink="/products">
          <mat-icon>add</mat-icon> New Order
        </a>
      </div>

      <div *ngIf="loading" class="center"><mat-spinner></mat-spinner></div>

      <div *ngIf="!loading && orders.length === 0" class="empty-state">
        <mat-icon>receipt_long</mat-icon>
        <p>No orders yet.</p>
        <a mat-raised-button color="primary" routerLink="/products">Start Shopping</a>
      </div>

      <mat-accordion *ngIf="!loading && orders.length > 0">
        <mat-expansion-panel *ngFor="let order of orders" class="order-panel">
          <mat-expansion-panel-header>
            <mat-panel-title>
              <span class="order-id">Order #{{ order.id }}</span>
            </mat-panel-title>
            <mat-panel-description>
              <mat-chip [class]="'status-' + order.status.toLowerCase()">{{ order.status }}</mat-chip>
              <span class="order-amount">{{ order.totalAmount | currency }}</span>
              <span class="order-date">{{ order.createdAt | date:'MMM d, y' }}</span>
            </mat-panel-description>
          </mat-expansion-panel-header>

          <div class="order-details">
            <p class="shipping"><mat-icon>location_on</mat-icon> {{ order.shippingAddress }}</p>
            <mat-divider></mat-divider>
            <div class="item-list">
              <div *ngFor="let item of order.items" class="order-item">
                <span>{{ item.productName }} × {{ item.quantity }}</span>
                <span>{{ item.subtotal | currency }}</span>
              </div>
            </div>
            <mat-divider></mat-divider>
            <div class="order-total">
              <strong>Total: {{ order.totalAmount | currency }}</strong>
            </div>
            <div class="order-actions">
              <button mat-stroked-button color="warn"
                      *ngIf="order.status === 'PENDING'"
                      (click)="cancel(order)">
                Cancel Order
              </button>
            </div>
          </div>
        </mat-expansion-panel>
      </mat-accordion>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:900px; margin:0 auto; }
    .page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
    .page-header h1 { margin:0; }
    .order-panel { margin-bottom:8px; }
    mat-panel-description { display:flex; align-items:center; gap:16px; }
    .order-id { font-weight:600; }
    .order-amount { font-weight:bold; color:#1976d2; }
    .order-date { color:#888; font-size:13px; }
    .order-details { padding:16px 0; }
    .shipping { display:flex; align-items:center; gap:6px; color:#555; margin-bottom:12px; }
    .item-list { padding:12px 0; }
    .order-item { display:flex; justify-content:space-between; padding:6px 0; border-bottom:1px solid #f0f0f0; }
    .order-total { padding:12px 0; font-size:16px; }
    .order-actions { margin-top:12px; }
    .status-pending { background:#fff3e0 !important; color:#e65100 !important; }
    .status-confirmed { background:#e8f5e9 !important; color:#2e7d32 !important; }
    .status-cancelled { background:#ffebee !important; color:#c62828 !important; }
    .center { display:flex; justify-content:center; padding:48px; }
    .empty-state { text-align:center; padding:64px; color:#999; }
    .empty-state mat-icon { font-size:64px; height:64px; width:64px; margin-bottom:16px; }
  `]
})
export class OrdersComponent implements OnInit {
  orders: Order[] = [];
  loading = true;

  constructor(private orderService: OrderService, private snack: MatSnackBar) {}

  ngOnInit() {
    this.orderService.getMyOrders().subscribe({
      next: o => { this.orders = o; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  cancel(order: Order) {
    this.orderService.cancelOrder(order.id).subscribe({
      next: updated => {
        order.status = updated.status;
        this.snack.open(`Order #${order.id} cancelled`, 'Close', { duration: 3000 });
      },
      error: err => this.snack.open(err.error?.error || 'Cancel failed', 'Close', { duration: 3000 })
    });
  }
}