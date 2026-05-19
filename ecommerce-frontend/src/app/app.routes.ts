import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'products', pathMatch: 'full' },
  { path: 'login',    loadComponent: () => import('./features/auth/login/login').then(m => m.LoginComponent) },
  { path: 'register', loadComponent: () => import('./features/auth/register/register').then(m => m.RegisterComponent) },
  { path: 'products', loadComponent: () => import('./features/products/product-list/product-list').then(m => m.ProductListComponent) },
  { path: 'products/:id', loadComponent: () => import('./features/products/product-detail/product-detail').then(m => m.ProductDetailComponent) },
  { path: 'cart',          loadComponent: () => import('./features/cart/cart').then(m => m.CartComponent), canActivate: [authGuard] },
  { path: 'checkout',      loadComponent: () => import('./features/checkout/checkout').then(m => m.CheckoutComponent), canActivate: [authGuard] },
  { path: 'orders',        loadComponent: () => import('./features/orders/orders').then(m => m.OrdersComponent), canActivate: [authGuard] },
  { path: 'notifications', loadComponent: () => import('./features/notifications/notifications').then(m => m.NotificationsComponent), canActivate: [authGuard] },
  { path: 'admin',         loadComponent: () => import('./features/admin/admin').then(m => m.AdminComponent), canActivate: [adminGuard] },
  { path: '**', redirectTo: 'products' }
];
