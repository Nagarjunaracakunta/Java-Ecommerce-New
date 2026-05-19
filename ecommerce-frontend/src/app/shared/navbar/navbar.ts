import { Component, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { NgIf } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { NotificationService } from '../../core/services/notification.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, NgIf, MatToolbarModule, MatButtonModule,
            MatIconModule, MatBadgeModule, MatMenuModule],
  template: `
    <mat-toolbar color="primary" class="navbar">
      <a routerLink="/products" class="brand">
        <mat-icon>storefront</mat-icon>
        <span>EcommerceHub</span>
      </a>

      <span class="spacer"></span>

      <ng-container *ngIf="auth.isLoggedIn(); else guestLinks">
        <a mat-button routerLink="/products" routerLinkActive="active-link">
          <mat-icon>restaurant_menu</mat-icon> Menu
        </a>
        <a mat-button routerLink="/cart" routerLinkActive="active-link">
          <mat-icon>shopping_cart</mat-icon> Cart
        </a>
        <a mat-button routerLink="/orders" routerLinkActive="active-link">
          <mat-icon>receipt_long</mat-icon> Orders
        </a>
        <a mat-icon-button routerLink="/notifications" routerLinkActive="active-link"
           [matBadge]="unreadCount() || null" matBadgeColor="warn"
           matBadgeSize="small">
          <mat-icon>notifications</mat-icon>
        </a>
        <button mat-button [matMenuTriggerFor]="userMenu">
          <mat-icon>account_circle</mat-icon> {{ auth.getUsername() }}
          <mat-icon>arrow_drop_down</mat-icon>
        </button>
        <mat-menu #userMenu="matMenu">
          <a mat-menu-item routerLink="/admin" *ngIf="auth.isAdmin()">
            <mat-icon>admin_panel_settings</mat-icon> Admin
          </a>
          <button mat-menu-item (click)="auth.logout()">
            <mat-icon>logout</mat-icon> Sign Out
          </button>
        </mat-menu>
      </ng-container>

      <ng-template #guestLinks>
        <a mat-button routerLink="/products">Menu</a>
        <a mat-button routerLink="/login">Sign In</a>
        <a mat-raised-button routerLink="/register" class="register-btn">Register</a>
      </ng-template>

    </mat-toolbar>
  `,
  styles: [`
    .navbar { position:sticky; top:0; z-index:100; box-shadow:0 2px 4px rgba(0,0,0,0.2); }
    .brand { display:flex; align-items:center; gap:8px; color:white; text-decoration:none;
             font-size:20px; font-weight:600; margin-right:16px; }
    .spacer { flex:1; }
    .active-link { background:rgba(255,255,255,0.15); border-radius:4px; }
    .register-btn { color:#1976d2 !important; background:white !important; margin-left:8px; }
    a { color:white; }
  `]
})
export class NavbarComponent implements OnInit {
  auth = inject(AuthService);
  private notifService = inject(NotificationService);
  unreadCount = this.notifService.unreadCount;

  ngOnInit() {
    if (this.auth.isLoggedIn()) {
      this.notifService.getCount().subscribe();
    }
  }
}
