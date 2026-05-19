import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule } from '@angular/material/dialog';
import { NgFor, NgIf, CurrencyPipe } from '@angular/common';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [ReactiveFormsModule, NgIf, CurrencyPipe, MatCardModule, MatFormFieldModule,
            MatInputModule, MatButtonModule, MatIconModule, MatSelectModule,
            MatProgressSpinnerModule, MatSnackBarModule, MatTableModule, MatDialogModule],
  template: `
    <div class="page-container">
      <h1>Product Management</h1>

      <!-- Product Form -->
      <mat-card class="form-card">
        <mat-card-header>
          <mat-card-title>{{ editingId ? 'Edit Product' : 'Add New Product' }}</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="save()">
            <div class="form-row">
              <mat-form-field appearance="outline" class="field-name">
                <mat-label>Name</mat-label>
                <input matInput formControlName="name">
                <mat-error *ngIf="form.get('name')?.hasError('required')">Required</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="field-price">
                <mat-label>Price</mat-label>
                <input matInput type="number" step="0.01" formControlName="price">
                <mat-error *ngIf="form.get('price')?.hasError('required')">Required</mat-error>
              </mat-form-field>
            </div>
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Description</mat-label>
              <textarea matInput formControlName="description" rows="2"></textarea>
            </mat-form-field>
            <div class="form-row">
              <mat-form-field appearance="outline" class="field-category">
                <mat-label>Category</mat-label>
                <mat-select formControlName="category">
                  <mat-option value="BURGER">Burger</mat-option>
                  <mat-option value="PIZZA">Pizza</mat-option>
                  <mat-option value="DRINKS">Drinks</mat-option>
                  <mat-option value="SIDES">Sides</mat-option>
                  <mat-option value="DESSERTS">Desserts</mat-option>
                  <mat-option value="OTHER">Other</mat-option>
                </mat-select>
                <mat-error *ngIf="form.get('category')?.hasError('required')">Required</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline" class="field-stock">
                <mat-label>Stock</mat-label>
                <input matInput type="number" formControlName="stock">
              </mat-form-field>
            </div>
            <div class="form-actions">
              <button mat-raised-button color="primary" type="submit"
                      [disabled]="form.invalid || saving">
                <mat-spinner diameter="18" *ngIf="saving"></mat-spinner>
                <span *ngIf="!saving">{{ editingId ? 'Update' : 'Add Product' }}</span>
              </button>
              <button mat-button type="button" *ngIf="editingId" (click)="cancelEdit()">Cancel</button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- Product Table -->
      <mat-card class="table-card">
        <mat-card-header><mat-card-title>All Products</mat-card-title></mat-card-header>
        <mat-card-content>
          <div *ngIf="loading" class="center"><mat-spinner></mat-spinner></div>
          <table mat-table [dataSource]="products" class="full-width mat-elevation-z1" *ngIf="!loading">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef>#</th>
              <td mat-cell *matCellDef="let p">{{ p.id }}</td>
            </ng-container>
            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef>Name</th>
              <td mat-cell *matCellDef="let p">{{ p.name }}</td>
            </ng-container>
            <ng-container matColumnDef="category">
              <th mat-header-cell *matHeaderCellDef>Category</th>
              <td mat-cell *matCellDef="let p">{{ p.category }}</td>
            </ng-container>
            <ng-container matColumnDef="price">
              <th mat-header-cell *matHeaderCellDef>Price</th>
              <td mat-cell *matCellDef="let p">{{ p.price | currency }}</td>
            </ng-container>
            <ng-container matColumnDef="available">
              <th mat-header-cell *matHeaderCellDef>Stock</th>
              <td mat-cell *matCellDef="let p">
                <span [style.color]="p.stock > 0 ? '#4caf50' : '#f44336'">
                  {{ p.stock > 0 ? p.stock + ' units' : 'Out of stock' }}
                </span>
              </td>
            </ng-container>
            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let p">
                <button mat-icon-button color="accent" (click)="edit(p)"><mat-icon>edit</mat-icon></button>
                <button mat-icon-button color="warn" (click)="delete(p)"><mat-icon>delete</mat-icon></button>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="cols"></tr>
            <tr mat-row *matRowDef="let row; columns: cols;"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding:24px; max-width:1100px; margin:0 auto; }
    h1 { margin-bottom:24px; }
    .form-card { margin-bottom:24px; padding:8px; }
    .table-card { padding:8px; }
    .form-row { display:flex; gap:16px; align-items:flex-start; flex-wrap:wrap; }
    .field-name { flex:2; min-width:200px; }
    .field-price { flex:1; min-width:120px; }
    .field-category { flex:2; min-width:180px; }
    .field-stock { flex:1; min-width:100px; }
    .full-width { width:100%; }
    .form-actions { display:flex; gap:12px; align-items:center; margin-top:8px; }
    .full-width.mat-elevation-z1 { width:100%; }
    mat-card-header { margin-bottom:16px; }
    .center { display:flex; justify-content:center; padding:32px; }
  `]
})
export class AdminComponent implements OnInit {
  private fb = inject(FormBuilder);
  private productService = inject(ProductService);
  private snack = inject(MatSnackBar);
  private route = inject(ActivatedRoute);

  products: Product[] = [];
  loading = true;
  saving = false;
  editingId: number | null = null;
  cols = ['id', 'name', 'category', 'price', 'available', 'actions'];

  form = this.fb.group({
    name: ['', Validators.required],
    description: [''],
    price: [null as number | null, Validators.required],
    category: ['', Validators.required],
    stock: [0, Validators.required]
  });

  ngOnInit() {
    this.loadProducts();
    const editId = this.route.snapshot.queryParamMap.get('edit');
    if (editId) {
      this.productService.getById(Number(editId)).subscribe(p => this.edit(p));
    }
  }

  loadProducts() {
    this.loading = true;
    this.productService.getAll().subscribe({
      next: p => { this.products = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  edit(p: Product) {
    this.editingId = p.id;
    this.form.patchValue({ name: p.name, description: p.description,
      price: p.price, category: p.category, stock: p.stock });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelEdit() { this.editingId = null; this.form.reset({ stock: 0 }); }

  save() {
    if (this.form.invalid) return;
    this.saving = true;
    const data = this.form.value as any;
    const req = this.editingId
      ? this.productService.update(this.editingId, data)
      : this.productService.create(data);

    req.subscribe({
      next: () => {
        this.saving = false;
        this.snack.open(this.editingId ? 'Product updated' : 'Product created', 'Close', { duration: 2500 });
        this.cancelEdit();
        this.loadProducts();
      },
      error: err => {
        this.saving = false;
        this.snack.open(err.error?.error || 'Save failed', 'Close', { duration: 3000 });
      }
    });
  }

  delete(p: Product) {
    if (!confirm(`Delete "${p.name}"?`)) return;
    this.productService.delete(p.id).subscribe({
      next: () => {
        this.products = this.products.filter(x => x.id !== p.id);
        this.snack.open('Product deleted', 'Close', { duration: 2500 });
      },
      error: err => this.snack.open(err.error?.error || 'Delete failed', 'Close', { duration: 3000 })
    });
  }
}