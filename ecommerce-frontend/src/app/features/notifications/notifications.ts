import { Component, OnInit } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { NgFor, NgIf, DatePipe, CurrencyPipe } from '@angular/common';
import { NotificationService } from '../../core/services/notification.service';
import { Notification } from '../../core/models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [NgFor, NgIf, DatePipe, CurrencyPipe, MatCardModule, MatButtonModule,
            MatIconModule, MatProgressSpinnerModule, MatSnackBarModule, MatChipsModule, MatDividerModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Notifications</h1>
        <button mat-stroked-button (click)="markAllRead()" *ngIf="hasUnread">
          <mat-icon>done_all</mat-icon> Mark All Read
        </button>
      </div>

      <div *ngIf="loading" class="center"><mat-spinner></mat-spinner></div>

      <div *ngIf="!loading && notifications.length === 0" class="empty-state">
        <mat-icon>notifications_none</mat-icon>
        <p>No notifications yet.</p>
      </div>

      <div class="notif-list" *ngIf="!loading && notifications.length > 0">
        <mat-card *ngFor="let n of notifications"
                  [class.unread]="!n.read" class="notif-card">
          <mat-card-content>
            <div class="notif-header">
              <div class="notif-icon-type">
                <mat-icon [class]="n.type === 'ORDER_CONFIRMED' ? 'icon-success' : 'icon-error'">
                  {{ n.type === 'ORDER_CONFIRMED' ? 'check_circle' : 'cancel' }}
                </mat-icon>
                <mat-chip-list>
                  <mat-chip [class]="n.type === 'ORDER_CONFIRMED' ? 'chip-success' : 'chip-error'">
                    {{ n.type === 'ORDER_CONFIRMED' ? 'Confirmed' : 'Cancelled' }}
                  </mat-chip>
                </mat-chip-list>
              </div>
              <div class="notif-meta">
                <span class="notif-time">{{ n.createdAt | date:'MMM d, h:mm a' }}</span>
                <button mat-icon-button *ngIf="!n.read" (click)="markRead(n)">
                  <mat-icon>mark_email_read</mat-icon>
                </button>
              </div>
            </div>
            <p class="notif-message">{{ n.message }}</p>
            <div class="notif-details">
              <span>Order #{{ n.orderId }}</span>
              <span>{{ n.amount | currency }}</span>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:800px; margin:0 auto; }
    .page-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:24px; }
    .page-header h1 { margin:0; }
    .notif-list { display:flex; flex-direction:column; gap:12px; }
    .notif-card { transition:all 0.2s; }
    .notif-card.unread { border-left:4px solid #1976d2; background:#f5f9ff; }
    .notif-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:8px; }
    .notif-icon-type { display:flex; align-items:center; gap:10px; }
    .notif-meta { display:flex; align-items:center; gap:8px; }
    .notif-time { font-size:13px; color:#888; }
    .notif-message { margin:8px 0; color:#333; line-height:1.5; }
    .notif-details { display:flex; gap:16px; font-size:13px; color:#666; margin-top:8px; }
    .icon-success { color:#4caf50; }
    .icon-error { color:#f44336; }
    .chip-success { background:#e8f5e9 !important; color:#2e7d32 !important; }
    .chip-error { background:#ffebee !important; color:#c62828 !important; }
    .center { display:flex; justify-content:center; padding:48px; }
    .empty-state { text-align:center; padding:64px; color:#999; }
    .empty-state mat-icon { font-size:64px; height:64px; width:64px; margin-bottom:16px; }
  `]
})
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [];
  loading = true;
  hasUnread = false;

  constructor(private notifService: NotificationService, private snack: MatSnackBar) {}

  ngOnInit() {
    this.notifService.getAll().subscribe({
      next: n => {
        this.notifications = n;
        this.hasUnread = n.some(x => !x.read);
        this.loading = false;
        this.notifService.getCount().subscribe();
      },
      error: () => { this.loading = false; }
    });
  }

  markRead(n: Notification) {
    this.notifService.markRead(n.id).subscribe({
      next: () => {
        n.read = true;
        this.hasUnread = this.notifications.some(x => !x.read);
        const current = this.notifService.unreadCount$.getValue();
        this.notifService.unreadCount$.next(Math.max(0, current - 1));
      }
    });
  }

  markAllRead() {
    this.notifService.markAllRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.hasUnread = false;
        this.notifService.unreadCount$.next(0);
        this.snack.open('All notifications marked as read', 'Close', { duration: 2000 });
      }
    });
  }
}