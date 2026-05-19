import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { NgIf } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, NgIf, MatCardModule, MatFormFieldModule,
            MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatSnackBarModule],
  template: `
    <div class="auth-container">
      <mat-card class="auth-card">
        <mat-card-header>
          <mat-card-title>Create Account</mat-card-title>
          <mat-card-subtitle>Join EcommerceHub today</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="submit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Username</mat-label>
              <input matInput formControlName="username" autocomplete="username">
              <mat-error *ngIf="form.get('username')?.hasError('required')">Required</mat-error>
              <mat-error *ngIf="form.get('username')?.hasError('minlength')">Min 3 characters</mat-error>
            </mat-form-field>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="new-password">
              <mat-error *ngIf="form.get('password')?.hasError('required')">Required</mat-error>
              <mat-error *ngIf="form.get('password')?.hasError('minlength')">Min 6 characters</mat-error>
            </mat-form-field>
            <button mat-raised-button color="primary" class="full-width" type="submit"
                    [disabled]="form.invalid || loading">
              <mat-spinner diameter="20" *ngIf="loading" class="btn-spinner"></mat-spinner>
              <span *ngIf="!loading">Create Account</span>
            </button>
          </form>
        </mat-card-content>
        <mat-card-actions>
          <p class="center-text">Already have an account? <a routerLink="/login">Sign in</a></p>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-container { display:flex; justify-content:center; align-items:center; min-height:80vh; padding:16px; }
    .auth-card { width:100%; max-width:420px; padding:16px; }
    .full-width { width:100%; margin-bottom:12px; }
    .center-text { text-align:center; margin:8px 0; }
    mat-card-header { margin-bottom:16px; }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  form = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });
  loading = false;

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.auth.register(this.form.value as any).subscribe({
      next: () => {
        this.snack.open('Account created! Please sign in.', 'Close', { duration: 3000 });
        this.router.navigate(['/login']);
      },
      error: err => {
        this.loading = false;
        this.snack.open(err.error?.error || 'Registration failed', 'Close', { duration: 3000 });
      }
    });
  }
}