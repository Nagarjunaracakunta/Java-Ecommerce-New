import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatIconModule } from '@angular/material/icon';
import { NgIf } from '@angular/common';
import { OrderService } from '../../core/services/order.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgIf, MatCardModule, MatFormFieldModule,
            MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatSnackBarModule, MatIconModule],
  template: `
    <div class="page-container">
      <a mat-button routerLink="/cart"><mat-icon>arrow_back</mat-icon> Back to Cart</a>

      <mat-card class="checkout-card">
        <mat-card-header>
          <mat-card-title>Checkout</mat-card-title>
          <mat-card-subtitle>Enter your shipping details</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <div class="info-banner">
            <mat-icon>info</mat-icon>
            <span>Payment is processed automatically. Your order will be confirmed via notification.</span>
          </div>
          <form [formGroup]="form" (ngSubmit)="placeOrder()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Shipping Address</mat-label>
              <textarea matInput formControlName="shippingAddress" rows="3"
                        placeholder="123 Main St, City, State 12345"></textarea>
              <mat-error *ngIf="form.get('shippingAddress')?.hasError('required')">
                Shipping address is required
              </mat-error>
            </mat-form-field>
            <button mat-raised-button color="primary" type="submit"
                    [disabled]="form.invalid || loading" class="full-width">
              <mat-spinner diameter="20" *ngIf="loading" class="btn-spinner"></mat-spinner>
              <span *ngIf="!loading">Place Order</span>
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:600px; margin:0 auto; }
    .checkout-card { margin-top:16px; padding:16px; }
    .full-width { width:100%; margin-bottom:16px; }
    .info-banner { display:flex; align-items:flex-start; gap:8px; background:#e3f2fd; padding:12px 16px;
                   border-radius:4px; margin-bottom:20px; color:#1565c0; font-size:14px; }
    mat-card-header { margin-bottom:16px; }
  `]
})
export class CheckoutComponent {
  private fb = inject(FormBuilder);
  private orderService = inject(OrderService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  form = this.fb.group({ shippingAddress: ['', Validators.required] });
  loading = false;

  placeOrder() {
    if (this.form.invalid) return;
    this.loading = true;
    this.orderService.placeOrder({ shippingAddress: this.form.value.shippingAddress! }).subscribe({
      next: order => {
        this.snack.open(`Order #${order.id} placed! Processing payment...`, 'Close', { duration: 4000 });
        this.router.navigate(['/orders']);
      },
      error: err => {
        this.loading = false;
        this.snack.open(err.error?.error || 'Failed to place order', 'Close', { duration: 3000 });
      }
    });
  }
}