
# Angular Frontend Guide
# EcommerceHub — Angular 20 · Angular Material · Standalone Components · JWT Auth · Role-Based Guards

---

## Table of Contents

**Setup**
1. [Prerequisites](#1-prerequisites)
2. [Project Setup from Scratch](#2-project-setup-from-scratch)
3. [Project Structure](#3-project-structure)
4. [Environment Configuration](#4-environment-configuration)

**Core Layer**
5. [Models — TypeScript Interfaces](#5-models--typescript-interfaces)
6. [Services — HTTP Communication](#6-services--http-communication)
7. [JWT Interceptor — Auto-Attach Bearer Token](#7-jwt-interceptor--auto-attach-bearer-token)
8. [Route Guards — Auth and Admin Protection](#8-route-guards--auth-and-admin-protection)

**App Bootstrap**
9. [App Config — Providers and DI Setup](#9-app-config--providers-and-di-setup)
10. [App Routes — Lazy Loading](#10-app-routes--lazy-loading)
11. [App Component — Shell and Navbar](#11-app-component--shell-and-navbar)

**Feature Components**
12. [Navbar Component](#12-navbar-component)
13. [Login Component](#13-login-component)
14. [Register Component](#14-register-component)
15. [Product List Component](#15-product-list-component)
16. [Product Detail Component](#16-product-detail-component)
17. [Cart Component](#17-cart-component)
18. [Checkout Component](#18-checkout-component)
19. [Orders Component](#19-orders-component)
20. [Notifications Component](#20-notifications-component)
21. [Admin Component](#21-admin-component)

**Security Deep Dive**
22. [Security Architecture — Complete Picture](#22-security-architecture--complete-picture)
23. [JWT Token Structure and Decoding](#23-jwt-token-structure-and-decoding)
24. [Role-Based Access Control (RBAC)](#24-role-based-access-control-rbac)
25. [Complete Request Flow — Authenticated User](#25-complete-request-flow--authenticated-user)
26. [Complete Request Flow — Unauthenticated User](#26-complete-request-flow--unauthenticated-user)
27. [Complete Request Flow — Admin User](#27-complete-request-flow--admin-user)

**Angular Concepts Explained**
28. [Standalone Components vs NgModules](#28-standalone-components-vs-ngmodules)
29. [Signals — Reactive State Without RxJS](#29-signals--reactive-state-without-rxjs)
30. [inject() vs Constructor Injection](#30-inject-vs-constructor-injection)
31. [Lazy Loading — How and Why](#31-lazy-loading--how-and-why)
32. [Functional Guards — Modern Angular Pattern](#32-functional-guards--modern-angular-pattern)
33. [Functional Interceptors — Modern Angular Pattern](#33-functional-interceptors--modern-angular-pattern)

**Running and Testing**
34. [Running the Frontend](#34-running-the-frontend)
35. [End-to-End User Flow with curl + Browser](#35-end-to-end-user-flow-with-curl--browser)
36. [Common Errors and Fixes](#36-common-errors-and-fixes)

---

## 1. Prerequisites

| Tool | Version | Install |
|---|---|---|
| Node.js | 18+ (22 used here) | `https://nodejs.org` |
| npm | 9+ (10.9 used here) | Comes with Node |
| Angular CLI | 20+ | `npm install -g @angular/cli` |
| Java backend | Running on port 8080 | See SERVICES_GUIDE.md |

Verify:
```bash
node --version    # v22.x.x
npm --version     # 10.x.x
ng version        # Angular CLI: 20.x.x
```

The backend (api-gateway) must be running on `http://localhost:8080` before the frontend can load any data.

---

## 2. Project Setup from Scratch

### Step 1 — Scaffold the Angular project
```bash
ng new ecommerce-frontend \
  --routing=true \       # generates app.routes.ts
  --style=scss \         # SCSS instead of plain CSS
  --ssr=false \          # no server-side rendering (SPA only)
  --skip-git \           # we use the parent repo's git
  --skip-tests \         # no spec files generated
  --standalone           # standalone components (no NgModules)
```

### Step 2 — Add Angular Material
```bash
cd ecommerce-frontend
ng add @angular/material
# Choose a theme (Azure/Blue used here), yes to typography, yes to animations
```

`ng add` does more than `npm install` — it also:
- Updates `angular.json` with the Material prebuilt theme
- Adds the Roboto font link to `index.html`
- Updates `styles.scss` with Material theming setup

### Step 3 — Create the directory structure
```bash
mkdir -p src/app/core/models \
         src/app/core/services \
         src/app/core/guards \
         src/app/core/interceptors \
         src/app/features/auth/login \
         src/app/features/auth/register \
         src/app/features/products/product-list \
         src/app/features/products/product-detail \
         src/app/features/cart \
         src/app/features/checkout \
         src/app/features/orders \
         src/app/features/notifications \
         src/app/features/admin \
         src/app/shared/navbar \
         src/environments
```

### Step 4 — Run the dev server
```bash
ng serve --port 4200
# Open http://localhost:4200
```

---

## 3. Project Structure

```
ecommerce-frontend/
├── src/
│   ├── environments/
│   │   └── environment.ts          ← API base URL
│   ├── styles.scss                 ← global styles + Material theme
│   └── app/
│       ├── app.ts                  ← root component (shell: navbar + router-outlet)
│       ├── app.config.ts           ← DI providers (HttpClient, Router, interceptors)
│       ├── app.routes.ts           ← all routes with guards + lazy loading
│       │
│       ├── core/                   ← singleton services, guards, interceptors (loaded once)
│       │   ├── models/
│       │   │   └── index.ts        ← all TypeScript interfaces (Product, Order, etc.)
│       │   ├── services/
│       │   │   ├── auth.service.ts
│       │   │   ├── product.service.ts
│       │   │   ├── cart.service.ts
│       │   │   ├── order.service.ts
│       │   │   └── notification.service.ts
│       │   ├── guards/
│       │   │   ├── auth.guard.ts   ← blocks unauthenticated users
│       │   │   └── admin.guard.ts  ← blocks non-admin users
│       │   └── interceptors/
│       │       └── jwt.interceptor.ts  ← attaches Authorization header
│       │
│       ├── features/               ← one folder per page/feature
│       │   ├── auth/
│       │   │   ├── login/login.ts
│       │   │   └── register/register.ts
│       │   ├── products/
│       │   │   ├── product-list/product-list.ts
│       │   │   └── product-detail/product-detail.ts
│       │   ├── cart/cart.ts
│       │   ├── checkout/checkout.ts
│       │   ├── orders/orders.ts
│       │   ├── notifications/notifications.ts
│       │   └── admin/admin.ts
│       │
│       └── shared/                 ← components reused across features
│           └── navbar/navbar.ts
```

### Why this structure?
- **`core/`** — services declared `providedIn: 'root'` are singletons. They go in `core/` to signal "don't duplicate these".
- **`features/`** — each page is self-contained. Adding a new page means adding a new folder here and one route in `app.routes.ts`.
- **`shared/`** — components used in multiple places (Navbar appears on every page).

---

## 4. Environment Configuration

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'   // api-gateway base URL
};
```

Every service imports this and constructs URLs like:
```typescript
`${environment.apiUrl}/products`  // → http://localhost:8080/products
```

For a production build you'd add `environment.prod.ts` with the deployed API URL and register it in `angular.json` under `fileReplacements`.

---

## 5. Models — TypeScript Interfaces

All TypeScript interfaces live in `src/app/core/models/index.ts`. They mirror the JSON shape returned by the Spring Boot backend.

```typescript
// ── Auth ──────────────────────────────────────────────────────────
export interface LoginRequest  { username: string; password: string; }
export interface RegisterRequest { username: string; password: string; role?: string; }
export interface AuthResponse  { token: string; tokenType: string; expiresIn: number; }

// ── Products ──────────────────────────────────────────────────────
export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;       // availability is derived: stock > 0
  category: string;
}

// ── Cart ──────────────────────────────────────────────────────────
export interface CartItem         { productId: number; quantity: number; }
export interface CartItemResponse { productId: number; productName: string;
                                    price: number; quantity: number; subtotal: number; }
export interface CartResponse     { username: string; items: CartItemResponse[]; total: number; }

// ── Orders ────────────────────────────────────────────────────────
export interface PlaceOrderRequest  { shippingAddress: string; }
export interface OrderItemResponse  { productId: number; productName: string;
                                      price: number; quantity: number; subtotal: number; }
export interface Order {
  id: number; username: string; status: string;
  totalAmount: number; shippingAddress: string;
  items: OrderItemResponse[]; createdAt: string; updatedAt: string;
}

// ── Notifications ─────────────────────────────────────────────────
export interface Notification {
  id: string;           // MongoDB ObjectId (hex string)
  username: string; type: string; message: string;
  orderId: number; paymentId: number; amount: number;
  read: boolean; createdAt: string;
}
```

### Why interfaces instead of classes?
Angular HTTP responses are plain JSON — TypeScript interfaces are erased at runtime and add zero bundle size. Classes would add runtime overhead and require explicit instantiation. Interfaces are the right choice for data shapes.

### Why one `index.ts` for all models?
It creates a single import point:
```typescript
import { Product, Order, Notification } from '../../../core/models';
```
Instead of:
```typescript
import { Product } from '../../../core/models/product.model';
import { Order } from '../../../core/models/order.model';
```

---

## 6. Services — HTTP Communication

Each service is a thin wrapper around `HttpClient`. They live in `core/services/` and are declared `providedIn: 'root'` — this tells Angular's DI to create exactly one instance for the entire application.

### AuthService
```typescript
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'jwt_token';
  isLoggedIn = signal(this.hasToken());   // Angular signal — reactive state

  login(body: LoginRequest) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, body).pipe(
      tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.token);  // persist token
        this.isLoggedIn.set(true);                         // update signal → navbar reacts
      })
    );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    this.isLoggedIn.set(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null { return localStorage.getItem(this.TOKEN_KEY); }

  getUsername(): string | null {
    const token = this.getToken();
    if (!token) return null;
    const payload = JSON.parse(atob(token.split('.')[1]));  // decode JWT payload
    return payload.sub;                                      // 'sub' = username
  }

  getRole(): string | null {
    const payload = JSON.parse(atob(this.getToken()!.split('.')[1]));
    return payload.role;    // 'ROLE_USER' or 'ROLE_ADMIN'
  }

  isAdmin(): boolean { return this.getRole() === 'ROLE_ADMIN'; }
}
```

**Key decisions:**
- Token stored in `localStorage` — survives page refresh. A production app might prefer `sessionStorage` or `HttpOnly` cookies.
- `isLoggedIn` is a **signal** — components that read it re-render automatically when it changes (e.g., navbar shows correct links after login without needing an event).
- `tap()` is an RxJS side-effect operator — it runs the token storage without consuming the Observable chain. The component still receives the `AuthResponse`.

### ProductService
```typescript
@Injectable({ providedIn: 'root' })
export class ProductService {
  private base = `${environment.apiUrl}/products`;

  getAll()                              { return this.http.get<Product[]>(this.base); }
  getById(id: number)                   { return this.http.get<Product>(`${this.base}/${id}`); }
  create(p: Partial<Product>)           { return this.http.post<Product>(this.base, p); }
  update(id: number, p: Partial<Product>) { return this.http.put<Product>(`${this.base}/${id}`, p); }
  delete(id: number)                    { return this.http.delete<void>(`${this.base}/${id}`); }
}
```

### CartService
```typescript
@Injectable({ providedIn: 'root' })
export class CartService {
  private base = `${environment.apiUrl}/cart`;

  getCart()                    { return this.http.get<CartResponse>(this.base); }
  addItem(item: CartItem)      { return this.http.post<CartResponse>(`${this.base}/items`, item); }
  removeItem(productId: number){ return this.http.delete<CartResponse>(`${this.base}/items/${productId}`); }
  clearCart()                  { return this.http.delete<void>(this.base); }
}
```

### OrderService
```typescript
@Injectable({ providedIn: 'root' })
export class OrderService {
  private base = `${environment.apiUrl}/orders`;

  placeOrder(req: PlaceOrderRequest) { return this.http.post<Order>(this.base, req); }
  getMyOrders()                      { return this.http.get<Order[]>(this.base); }
  getById(id: number)                { return this.http.get<Order>(`${this.base}/${id}`); }
  cancelOrder(id: number)            { return this.http.delete<Order>(`${this.base}/${id}`); }
}
```

### NotificationService
```typescript
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private base = `${environment.apiUrl}/notifications`;
  unreadCount = signal(0);    // signal drives the navbar badge in real-time

  getUnread() { return this.http.get<Notification[]>(this.base); }
  getAll()    { return this.http.get<Notification[]>(`${this.base}/all`); }

  getCount() {
    return this.http.get<{ unread: number }>(`${this.base}/count`).pipe(
      tap(r => this.unreadCount.set(r.unread))   // update signal → navbar badge reacts
    );
  }

  markRead(id: string)  { return this.http.patch<void>(`${this.base}/${id}/read`, {}); }
  markAllRead()         { return this.http.patch<void>(`${this.base}/read-all`, {}); }
}
```

**Why `signal(0)` for unreadCount?**
The navbar badge needs to update whenever:
1. The notifications page loads (calls `getCount()`)
2. The user marks a notification as read
3. The app starts (navbar calls `getCount()` on init)

A signal propagates these changes reactively — any template that reads `unreadCount()` re-renders automatically. Without a signal, you'd need an `EventEmitter`, `Subject`, or shared state library.

---

## 7. JWT Interceptor — Auto-Attach Bearer Token

```typescript
// src/app/core/interceptors/jwt.interceptor.ts
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

### How it works

```
Component calls:
  this.http.get('/cart')
        │
        ▼
  jwtInterceptor runs
  ├── reads token from localStorage via AuthService.getToken()
  ├── if token exists → clone the request, add Authorization header
  └── passes cloned request to next handler
        │
        ▼
  HTTP request goes out with:
    Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...
        │
        ▼
  api-gateway receives request
  ├── JwtAuthFilter validates the token
  ├── extracts username + role
  ├── adds X-Username and X-User-Role headers
  └── forwards to backend service
```

**Why clone?** HttpRequest objects are immutable in Angular. You cannot set headers on an existing request — you must clone it with the new headers. The original request is unchanged; the cloned one carries the token.

**Why `HttpInterceptorFn` (functional)?** Angular 15+ introduced functional interceptors. They don't require a class with `implements HttpInterceptor`. They're registered directly in `provideHttpClient(withInterceptors([jwtInterceptor]))` rather than in a module's providers array.

**Registration in `app.config.ts`:**
```typescript
provideHttpClient(withInterceptors([jwtInterceptor]))
```

This registers the interceptor globally — every `HttpClient` request in the application goes through it. You don't need to do anything per-component.

---

## 8. Route Guards — Auth and Admin Protection

### authGuard — blocks unauthenticated users

```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) return true;        // signal value — has token in localStorage
  inject(Router).navigate(['/login']);        // redirect to login
  return false;                              // block the route
};
```

Applied to: `/cart`, `/checkout`, `/orders`, `/notifications`

**What happens when a guest visits `/cart`:**
1. Angular's router checks `canActivate: [authGuard]` before activating the route
2. `authGuard` calls `auth.isLoggedIn()` — reads the signal, which checks `localStorage`
3. No token → returns `false` and redirects to `/login`
4. The cart page component is never instantiated — no HTTP call is made

### adminGuard — blocks non-admin users

```typescript
export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn() && auth.isAdmin()) return true;
  inject(Router).navigate(['/products']);    // non-admins sent to products page
  return false;
};
```

Applied to: `/admin`

`isAdmin()` decodes the JWT payload and checks if `role === 'ROLE_ADMIN'`. This check is on the **frontend** — it controls UI access. The actual protection is on the **backend** (`@PreAuthorize("hasRole('ADMIN')")` on product endpoints). Both layers are necessary:
- Frontend guard: UX — prevent admin routes from loading for regular users
- Backend check: Security — a malicious user could bypass the frontend guard

### Guard return types

Guards can return three things:
| Return value | Effect |
|---|---|
| `true` | Route is activated, component loads |
| `false` | Route is blocked, nothing happens |
| `UrlTree` (via `router.navigate()`) | Route is blocked and redirected |

---

## 9. App Config — Providers and DI Setup

```typescript
// src/app/app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withRouterConfig({ onSameUrlNavigation: 'reload' })),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimationsAsync()
  ]
};
```

This is the Angular 17+ way of bootstrapping an application without `AppModule`. Each `provide*` function registers services into the root DI container.

| Provider | What it sets up |
|---|---|
| `provideBrowserGlobalErrorListeners()` | Catches unhandled errors and logs them |
| `provideZoneChangeDetection({ eventCoalescing: true })` | Batches multiple DOM events into a single change detection cycle (performance) |
| `provideRouter(routes, ...)` | Registers the router with the route table |
| `withRouterConfig({ onSameUrlNavigation: 'reload' })` | Re-runs the component lifecycle when navigating to the same URL (e.g., clicking "Orders" while already on `/orders`) |
| `provideHttpClient(withInterceptors([jwtInterceptor]))` | Creates the `HttpClient` singleton and registers the JWT interceptor globally |
| `provideAnimationsAsync()` | Loads Angular animations lazily — Material components need this for ripple effects, dialogs, etc. |

---

## 10. App Routes — Lazy Loading

```typescript
export const routes: Routes = [
  // Redirects
  { path: '', redirectTo: 'products', pathMatch: 'full' },

  // Public routes — no guard
  { path: 'login',      loadComponent: () => import('./features/auth/login/login').then(m => m.LoginComponent) },
  { path: 'register',   loadComponent: () => import('./features/auth/register/register').then(m => m.RegisterComponent) },
  { path: 'products',   loadComponent: () => import('./features/products/product-list/product-list').then(m => m.ProductListComponent) },
  { path: 'products/:id', loadComponent: () => import('./features/products/product-detail/product-detail').then(m => m.ProductDetailComponent) },

  // Protected routes — require login
  { path: 'cart',          loadComponent: () => import('./features/cart/cart').then(m => m.CartComponent),
                            canActivate: [authGuard] },
  { path: 'checkout',      loadComponent: () => import('./features/checkout/checkout').then(m => m.CheckoutComponent),
                            canActivate: [authGuard] },
  { path: 'orders',        loadComponent: () => import('./features/orders/orders').then(m => m.OrdersComponent),
                            canActivate: [authGuard] },
  { path: 'notifications', loadComponent: () => import('./features/notifications/notifications').then(m => m.NotificationsComponent),
                            canActivate: [authGuard] },

  // Admin-only route
  { path: 'admin',         loadComponent: () => import('./features/admin/admin').then(m => m.AdminComponent),
                            canActivate: [adminGuard] },

  // Catch-all
  { path: '**', redirectTo: 'products' }
];
```

### `loadComponent` vs `component`

**Without lazy loading:**
```typescript
{ path: 'products', component: ProductListComponent }
// ProductListComponent AND all its imports are bundled into main.js
// The browser downloads everything on first load
```

**With lazy loading (`loadComponent`):**
```typescript
{ path: 'products', loadComponent: () => import('./features/products/product-list/product-list').then(m => m.ProductListComponent) }
// ProductListComponent is in its own JS chunk
// The browser only downloads it when the user navigates to /products
```

The build output shows each route as its own chunk:
```
main.js              → app shell, navbar (always downloaded)
chunk-xxx.js (login) → login component (downloaded when user visits /login)
chunk-xxx.js (admin) → admin panel (downloaded only when admin visits /admin)
```

This keeps the initial page load fast — a guest visiting the product listing never downloads the admin panel code.

### Route parameters

```typescript
{ path: 'products/:id', loadComponent: ... }
```

`:id` is a URL parameter. In the component:
```typescript
const id = Number(this.route.snapshot.paramMap.get('id'));
```

`ActivatedRoute.snapshot.paramMap.get('id')` reads `'id'` from the URL at the time the component loaded.

---

## 11. App Component — Shell and Navbar

```typescript
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <app-navbar></app-navbar>
    <main>
      <router-outlet></router-outlet>
    </main>
  `
})
export class App {}
```

`<router-outlet>` is Angular's placeholder that renders the component matching the current URL. When the user navigates to `/products`, Angular replaces `<router-outlet>` with `ProductListComponent`. When they go to `/cart`, it replaces it with `CartComponent`.

The Navbar stays mounted at all times — it's not inside the outlet, it's a sibling of it. This means:
- The user's login state is always visible
- The notification badge updates without re-mounting the navbar

---

## 12. Navbar Component

```typescript
@Component({ selector: 'app-navbar', standalone: true, ... })
export class NavbarComponent implements OnInit {
  auth = inject(AuthService);
  private notifService = inject(NotificationService);
  unreadCount = this.notifService.unreadCount;   // signal reference

  ngOnInit() {
    if (this.auth.isLoggedIn()) {
      this.notifService.getCount().subscribe();   // fetch unread count on startup
    }
  }
}
```

**Template logic:**
```html
<!-- Shows different links based on login state -->
<ng-container *ngIf="auth.isLoggedIn(); else guestLinks">
  <!-- Logged-in links: Menu, Cart, Orders, Notifications, User menu -->
  <a mat-icon-button routerLink="/notifications"
     [matBadge]="unreadCount() || null"    <!-- signal call: () reads the value -->
     matBadgeColor="warn">
    <mat-icon>notifications</mat-icon>
  </a>

  <button mat-button [matMenuTriggerFor]="userMenu">
    {{ auth.getUsername() }}               <!-- shows logged-in username -->
  </button>
  <mat-menu #userMenu="matMenu">
    <a mat-menu-item routerLink="/admin" *ngIf="auth.isAdmin()">  <!-- admin only -->
      Admin
    </a>
    <button mat-menu-item (click)="auth.logout()">Sign Out</button>
  </mat-menu>
</ng-container>

<ng-template #guestLinks>
  <!-- Guest links: Menu, Sign In, Register -->
</ng-template>
```

**Why `unreadCount() || null` on the badge?**
`matBadge` hides itself when the value is `null` or `undefined`. When unread count is 0, we pass `null` to hide the badge dot rather than showing "0".

**`routerLinkActive="active-link"`** — Angular adds the CSS class `active-link` to the link that matches the current URL. The style `.active-link { background: rgba(255,255,255,0.15) }` gives a subtle highlight to the active nav item.

---

## 13. Login Component

```typescript
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);

  form = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });
  loading = false;

  submit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.auth.login(this.form.value as any).subscribe({
      next: () => this.router.navigate(['/products']),
      error: err => {
        this.loading = false;
        this.snack.open(err.error?.error || 'Login failed', 'Close', { duration: 3000 });
      }
    });
  }
}
```

**Reactive Forms flow:**
1. `FormBuilder.group()` creates a `FormGroup` with two `FormControl`s
2. `[formGroup]="form"` binds the group to the `<form>` element
3. `formControlName="username"` binds the input to the `username` control
4. `form.invalid` is true if any control fails validation
5. `submit()` is called on `(ngSubmit)` — the form's submit event

**Login flow:**
```
User clicks "Sign In"
    │
    ▼
submit() called
    ├── form.invalid? → return early (button is also [disabled] when invalid)
    └── auth.login({ username, password })
            │
            ▼ HTTP POST /auth/login
            │
    ├── Success → localStorage.setItem('jwt_token', token)
    │             isLoggedIn signal set to true
    │             router.navigate(['/products'])
    └── Error  → MatSnackBar shows error message
                 loading = false (re-enables button)
```

**Why `inject()` instead of constructor injection for `fb`?**

When a class field uses `this.someInjected` (e.g. `form = this.fb.group(...)`), TypeScript initializes class fields **before** the constructor runs. This means `this.fb` is undefined when `form` is being assigned — a "used before initialization" error.

The fix is to use `inject()` at the class field level:
```typescript
private fb = inject(FormBuilder);   // runs during field initialization
form = this.fb.group({ ... });       // fb is now available
```

This is also the Angular 17+ recommended style for service injection.

---

## 14. Register Component

Same pattern as Login but calls `auth.register()` and redirects to `/login` on success. The form has `minLength` validators:
```typescript
form = this.fb.group({
  username: ['', [Validators.required, Validators.minLength(3)]],
  password: ['', [Validators.required, Validators.minLength(6)]]
});
```

`mat-error` elements are shown only when the control is invalid AND the user has touched/submitted the form:
```html
<mat-error *ngIf="form.get('username')?.hasError('minlength')">
  Min 3 characters
</mat-error>
```

---

## 15. Product List Component

```typescript
export class ProductListComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  isLoggedIn = false;
  isAdmin = false;

  ngOnInit() {
    this.isLoggedIn = this.auth.isLoggedIn();
    this.isAdmin = this.auth.isAdmin();
    this.productService.getAll().subscribe({
      next: p => { this.products = p; this.loading = false; },
      error: () => { this.loading = false; ... }
    });
  }

  addToCart(p: Product) {
    if (!this.isLoggedIn) {
      // Show snack with "Sign In" action button
      this.snack.open('Please sign in to add items to cart', 'Sign In', { duration: 3000 })
        .onAction().subscribe(() => this.router.navigate(['/login']));
      return;
    }
    this.cartService.addItem({ productId: p.id, quantity: 1 }).subscribe({
      next: () => this.snack.open(`${p.name} added to cart!`, 'View Cart', { duration: 2500 })
                    .onAction().subscribe(() => this.router.navigate(['/cart'])),
      ...
    });
  }
}
```

**Availability from stock:**
```html
<p class="availability" [class.unavailable]="p.stock === 0">
  {{ p.stock > 0 ? '✓ In Stock (' + p.stock + ')' : '✗ Out of Stock' }}
</p>
<button mat-raised-button [disabled]="p.stock === 0" (click)="addToCart(p)">
  Add to Cart
</button>
```

The backend `Product` entity has a `stock: int` field. A product is available when `stock > 0`. The button is enabled for all users (logged-in or not) — unauthenticated users get the "Sign in" snack instead.

**Spinner while loading:**
```html
<div *ngIf="loading" class="center">
  <mat-spinner></mat-spinner>
</div>
<div class="product-grid" *ngIf="!loading">
  ...
</div>
```

`loading` starts `true`, set to `false` in either the success or error handler. The spinner shows until the HTTP call completes.

---

## 16. Product Detail Component

Same HTTP call pattern but uses `ActivatedRoute` to read the product ID from the URL:
```typescript
ngOnInit() {
  const id = Number(this.route.snapshot.paramMap.get('id'));
  this.productService.getById(id).subscribe({ ... });
}
```

`snapshot` gives you the route state at the moment the component loaded. For live updates as the URL changes, you'd use `this.route.paramMap` (an Observable) instead — but for a product detail page that always loads fresh, `snapshot` is sufficient.

---

## 17. Cart Component

```typescript
export class CartComponent implements OnInit {
  items: CartItemResponse[] = [];
  total = 0;
  loading = true;
  cols = ['name', 'price', 'qty', 'subtotal', 'remove'];

  ngOnInit() { this.loadCart(); }

  loadCart() {
    this.cartService.getCart().subscribe({
      next: c => { this.items = c.items; this.total = c.total; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  remove(productId: number) {
    this.cartService.removeItem(productId).subscribe({
      next: c => { this.items = c.items; this.total = c.total; }   // response is the updated cart
    });
  }
}
```

The cart backend returns the full updated cart on every mutation (add, remove). No need to refetch — just replace the local arrays with what the server returned.

**Material Table:**
```html
<table mat-table [dataSource]="items">
  <ng-container matColumnDef="name">
    <th mat-header-cell *matHeaderCellDef>Item</th>
    <td mat-cell *matCellDef="let row">{{ row.productName }}</td>
  </ng-container>
  ...
  <tr mat-header-row *matHeaderRowDef="cols"></tr>
  <tr mat-row *matRowDef="let row; columns: cols;"></tr>
</table>
```

`cols = ['name', 'price', 'qty', 'subtotal', 'remove']` defines column order. Each `matColumnDef` matches a string in `cols`. You can reorder columns just by changing the array.

This route has `canActivate: [authGuard]` — an unauthenticated user who visits `/cart` is immediately redirected to `/login`. The HTTP call is never made.

---

## 18. Checkout Component

```typescript
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
    this.orderService.placeOrder({ shippingAddress: this.form.value.shippingAddress! })
      .subscribe({
        next: order => {
          this.snack.open(`Order #${order.id} placed! Processing payment...`, 'Close', { duration: 4000 });
          this.router.navigate(['/orders']);
        },
        error: ...
      });
  }
}
```

**What happens after `placeOrder()`:**
```
POST /orders { shippingAddress: "..." }
        │
        ▼ order-service
        ├── reads cart from cart-service (service token)
        ├── creates Order with status PENDING
        ├── clears the cart
        └── calls payment-service (service token)
                │
                ▼ payment-service
                ├── creates Payment record
                ├── processes payment (simulated)
                └── publishes PAYMENT_SUCCESS to Kafka
                        │
                        ├── order-service consumer: updates Order to CONFIRMED
                        └── notification-service consumer: saves notification

Response returns immediately:
{ id: 6, status: "PENDING", ... }

Angular app:
└── shows snack "Order #6 placed! Processing payment..."
└── navigates to /orders
```

The frontend doesn't wait for Kafka — it shows PENDING immediately and the user sees CONFIRMED when they refresh orders a few seconds later.

---

## 19. Orders Component

```typescript
export class OrdersComponent implements OnInit {
  orders: Order[] = [];
  loading = true;

  ngOnInit() {
    this.orderService.getMyOrders().subscribe({
      next: o => { this.orders = o; this.loading = false; }
    });
  }

  cancel(order: Order) {
    this.orderService.cancelOrder(order.id).subscribe({
      next: updated => { order.status = updated.status; }  // mutate in-place
    });
  }
}
```

**Material Expansion Panel** shows order items on click:
```html
<mat-accordion>
  <mat-expansion-panel *ngFor="let order of orders">
    <mat-expansion-panel-header>
      Order #{{ order.id }} — {{ order.status }} — {{ order.totalAmount | currency }}
    </mat-expansion-panel-header>

    <div>  <!-- expanded content: items list, cancel button -->
      <div *ngFor="let item of order.items">
        {{ item.productName }} × {{ item.quantity }} — {{ item.subtotal | currency }}
      </div>
      <button *ngIf="order.status === 'PENDING'" (click)="cancel(order)">Cancel</button>
    </div>
  </mat-expansion-panel>
</mat-accordion>
```

Cancel only shown for PENDING orders. After cancellation, `order.status` is updated in-place without a full refresh.

**Status chip styling:**
```html
<mat-chip [class]="'status-' + order.status.toLowerCase()">{{ order.status }}</mat-chip>
```
```scss
.status-pending   { background: #fff3e0; color: #e65100; }
.status-confirmed { background: #e8f5e9; color: #2e7d32; }
.status-cancelled { background: #ffebee; color: #c62828; }
```

Dynamic class binding: `'status-' + order.status.toLowerCase()` produces `status-pending`, `status-confirmed`, etc.

---

## 20. Notifications Component

```typescript
export class NotificationsComponent implements OnInit {
  notifications: Notification[] = [];
  hasUnread = false;

  ngOnInit() {
    this.notifService.getAll().subscribe({
      next: n => {
        this.notifications = n;
        this.hasUnread = n.some(x => !x.read);
        this.notifService.getCount().subscribe();  // sync navbar badge
      }
    });
  }

  markRead(n: Notification) {
    this.notifService.markRead(n.id).subscribe({
      next: () => {
        n.read = true;                                           // update in-place
        this.hasUnread = this.notifications.some(x => !x.read); // recompute
        this.notifService.unreadCount.update(c => Math.max(0, c - 1));  // decrement signal
      }
    });
  }

  markAllRead() {
    this.notifService.markAllRead().subscribe({
      next: () => {
        this.notifications.forEach(n => n.read = true);
        this.hasUnread = false;
        this.notifService.unreadCount.set(0);  // zero signal → badge disappears
      }
    });
  }
}
```

**Signal update after mark-read:**
`this.notifService.unreadCount.update(c => Math.max(0, c - 1))` uses the `update()` form of a signal — it receives the current value and returns the new value. The navbar's badge reads this signal and re-renders immediately.

**Unread styling:**
```html
<mat-card [class.unread]="!n.read">
```
```scss
.notif-card.unread { border-left: 4px solid #1976d2; background: #f5f9ff; }
```

Unread notifications get a blue left border. After marking read, `n.read = true` and Angular's change detection removes the class.

---

## 21. Admin Component

The admin panel has two sections: a form to create/edit products and a table listing all products.

```typescript
export class AdminComponent implements OnInit {
  private fb = inject(FormBuilder);
  editingId: number | null = null;

  form = this.fb.group({
    name:        ['', Validators.required],
    description: [''],
    price:       [null as number | null, Validators.required],
    category:    ['', Validators.required],
    stock:       [0, Validators.required]
  });

  edit(p: Product) {
    this.editingId = p.id;
    this.form.patchValue({ name: p.name, description: p.description,
                           price: p.price, category: p.category, stock: p.stock });
    window.scrollTo({ top: 0, behavior: 'smooth' });  // scroll to top to see the form
  }

  save() {
    const req = this.editingId
      ? this.productService.update(this.editingId, this.form.value as any)   // PUT
      : this.productService.create(this.form.value as any);                  // POST
    req.subscribe({ next: () => { this.cancelEdit(); this.loadProducts(); } });
  }

  delete(p: Product) {
    if (!confirm(`Delete "${p.name}"?`)) return;  // browser confirm dialog
    this.productService.delete(p.id).subscribe({
      next: () => { this.products = this.products.filter(x => x.id !== p.id); }
    });
  }
}
```

**Edit mode flow:**
1. User clicks edit icon on a row → `edit(p)` called
2. `form.patchValue()` fills the form fields with existing product data
3. `editingId` is set to the product's ID
4. Template shows "Update" button instead of "Add Product"
5. On save: `update(editingId, form.value)` called → PUT request
6. `cancelEdit()` resets `editingId = null` and clears the form

**Route query param for quick edit:**
```typescript
// In ProductListComponent template:
<a mat-icon-button [routerLink]="['/admin']" [queryParams]="{edit: p.id}">
```
```typescript
// In AdminComponent.ngOnInit():
const editId = this.route.snapshot.queryParamMap.get('edit');
if (editId) {
  this.productService.getById(Number(editId)).subscribe(p => this.edit(p));
}
```

Clicking the edit icon on the product list navigates to `/admin?edit=1`, which pre-fills the admin form with that product. This is guarded by `adminGuard` — only admins see the edit icon.

---

## 22. Security Architecture — Complete Picture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Angular Frontend                            │
│                                                                 │
│  localStorage                                                   │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  jwt_token: eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN...   │   │
│  └─────────────────────────────────────────────────────────┘   │
│           ↑ written on login                                    │
│           │ read by jwtInterceptor on every HTTP request        │
│                                                                 │
│  Route Guards (before component loads)                          │
│  ┌──────────────┐  ┌──────────────────────────────────────┐   │
│  │  authGuard   │  │  adminGuard                          │   │
│  │  checks:     │  │  checks:                             │   │
│  │  isLoggedIn()│  │  isLoggedIn() && isAdmin()           │   │
│  │  (signal)    │  │  (reads JWT payload.role)            │   │
│  └──────────────┘  └──────────────────────────────────────┘   │
│                                                                 │
│  JWT Interceptor (on every HTTP call)                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  req.clone({ setHeaders: { Authorization: Bearer ... } })│  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                          │ HTTP
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                     api-gateway :8080                           │
│                                                                 │
│  JwtAuthFilter (Spring WebFlux filter)                         │
│  ├── extracts Bearer token from Authorization header           │
│  ├── validates signature + expiry                              │
│  ├── injects X-Username and X-User-Role headers                │
│  └── forwards to backend service                               │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Backend Services                               │
│                                                                 │
│  Spring Security reads X-Username / X-User-Role headers        │
│  @PreAuthorize("hasRole('ADMIN')") enforces role checks        │
└─────────────────────────────────────────────────────────────────┘
```

### Two layers of security — why both are needed

| Layer | Where | What it does | What it can't do |
|---|---|---|---|
| Route guards | Angular frontend | Prevents unauthenticated users from seeing pages | Can't stop a developer from calling the API directly |
| Backend auth | Spring Security | Enforces access control on every API call | Can't give a good UX to blocked users |

Both layers are necessary. The frontend provides UX (redirect to login, hide admin links). The backend provides actual security (API calls with no/invalid token return 401/403).

---

## 23. JWT Token Structure and Decoding

A JWT has three Base64-encoded parts separated by dots:
```
eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGUiOiJST0xFX0FETUlOIiwiaWF0IjoxNzc5MjA5MzI1LCJleHAiOjE3NzkyOTU3MjV9.LC6ESLkxVWE...
│                   │ │                                                                                                               │ │           │
│     header        │ │                              payload                                                                         │ │ signature │
```

**Decoded payload:**
```json
{
  "sub":  "testuser",
  "role": "ROLE_ADMIN",
  "iat":  1779209325,
  "exp":  1779295725
}
```

**How Angular reads the payload:**
```typescript
getUsername(): string | null {
  const token = this.getToken();
  if (!token) return null;
  // Split by '.' → take index 1 (payload) → base64 decode → parse JSON
  const payload = JSON.parse(atob(token.split('.')[1]));
  return payload.sub;   // 'sub' = subject = username (set by auth-service)
}

getRole(): string | null {
  const payload = JSON.parse(atob(this.getToken()!.split('.')[1]));
  return payload.role;  // 'ROLE_USER' or 'ROLE_ADMIN'
}
```

**Important:** The frontend reads the role claim to control UI (show/hide admin links, guard `/admin`). The backend **independently** validates the token's signature and reads the role from its own decoding — it does not trust the frontend's UI state.

**Token expiry:**
- The backend sets `exp` (Unix timestamp). The JWT is valid until that time.
- The frontend currently doesn't check `exp` — expired tokens are sent to the backend, which returns 401. A production app would check `exp` and proactively show a "Session expired" message.

---

## 24. Role-Based Access Control (RBAC)

### Role values
| Role | Granted to | Can do |
|---|---|---|
| `ROLE_USER` | Any registered user | Browse products, cart, checkout, orders, notifications |
| `ROLE_ADMIN` | Admin accounts | All of USER + create/edit/delete products |

### Frontend enforcement

**Route level (adminGuard):**
```typescript
{ path: 'admin', canActivate: [adminGuard] }

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn() && auth.isAdmin()) return true;
  inject(Router).navigate(['/products']);   // non-admins sent away
  return false;
};
```

**Template level (show/hide elements):**
```html
<!-- Admin nav link — only visible to ROLE_ADMIN -->
<a mat-menu-item routerLink="/admin" *ngIf="auth.isAdmin()">
  Admin
</a>

<!-- Edit icon on product card — only visible to ROLE_ADMIN -->
<ng-container *ngIf="isAdmin">
  <a mat-icon-button [routerLink]="['/admin']" [queryParams]="{edit: p.id}">
    <mat-icon>edit</mat-icon>
  </a>
</ng-container>
```

### Backend enforcement (Spring Security)

```java
// product-service SecurityConfig
http.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
    .requestMatchers(HttpMethod.POST, "/products").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/products/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/products/**").hasRole("ADMIN")
    .anyRequest().authenticated()
);
```

The backend reads `ROLE_ADMIN` from the `X-User-Role` header that the api-gateway injects after validating the JWT. A `ROLE_USER` token trying to `POST /products` gets 403 Forbidden regardless of what the Angular frontend shows.

### What happens when a ROLE_USER tries to access /admin

1. Browser navigates to `http://localhost:4200/admin`
2. Angular router checks `canActivate: [adminGuard]`
3. `adminGuard` calls `auth.isAdmin()` → `getRole() === 'ROLE_ADMIN'` → false for ROLE_USER
4. Guard returns `false` and redirects to `/products`
5. Admin component is never loaded
6. No HTTP calls are made to the backend

Even if a user bypasses the frontend guard (e.g., by calling the API directly from curl):
```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer <ROLE_USER_TOKEN>" \
  -d '{"name":"Hack"}'
```
The backend returns `403 Forbidden`.

---

## 25. Complete Request Flow — Authenticated User

**Scenario: Logged-in user adds product #1 to cart**

```
1. User clicks "Add to Cart" on product card
   ProductListComponent.addToCart(product)

2. isLoggedIn = true → skip the "sign in" snack

3. cartService.addItem({ productId: 1, quantity: 1 }) called
   HttpClient.post('http://localhost:8080/cart/items', { productId: 1, quantity: 1 })

4. jwtInterceptor runs:
   reads localStorage['jwt_token']
   → req.clone({ setHeaders: { Authorization: 'Bearer eyJ...' } })

5. HTTP POST to http://localhost:8080/cart/items
   Headers: { Authorization: 'Bearer eyJ...', Content-Type: 'application/json' }
   Body: { "productId": 1, "quantity": 1 }

6. api-gateway receives request:
   JwtAuthFilter validates token
   → extracts username='testuser', role='ROLE_USER'
   → adds X-Username: testuser, X-User-Role: ROLE_USER
   → routes to cart-service:8083/cart/items

7. cart-service receives:
   JwtAuthFilter validates token again (defense in depth)
   SecurityContext set with username='testuser', role=ROLE_USER
   CartController.addItem() called
   → Principal.getName() = 'testuser'
   → adds item to Redis hash 'cart:testuser'
   → returns updated CartResponse

8. Response flows back:
   cart-service → api-gateway → Angular

9. Angular receives CartResponse { items: [...], total: 23.98 }
   MatSnackBar shows "Classic Burger added to cart!"
   onAction() → router.navigate(['/cart'])
```

---

## 26. Complete Request Flow — Unauthenticated User

**Scenario: Guest visits /cart**

```
1. User types http://localhost:4200/cart in browser

2. Angular Router activates the route:
   { path: 'cart', canActivate: [authGuard] }

3. authGuard runs:
   auth.isLoggedIn() → signal reads localStorage → no token → false
   router.navigate(['/login'])
   returns false

4. CartComponent is NEVER instantiated
   No HTTP requests made
   Browser URL changes to http://localhost:4200/login

5. User sees the Login page
```

**Scenario: Guest clicks "Add to Cart"**

```
1. "Add to Cart" button is NOT disabled for guests (stock > 0)
2. Click calls addToCart(product)
3. isLoggedIn = false
4. MatSnackBar.open('Please sign in...', 'Sign In', ...)
5. .onAction().subscribe(() => router.navigate(['/login']))
6. No cartService.addItem() called
7. If user clicks "Sign In" on the snack → navigated to /login
```

---

## 27. Complete Request Flow — Admin User

**Scenario: Admin creates a new product**

```
1. Admin navigates to http://localhost:4200/admin
2. adminGuard: isLoggedIn() && isAdmin() → both true → route activates
3. AdminComponent loads, ngOnInit calls productService.getAll()
4. Admin fills in form: name, description, price, category, stock
5. Clicks "Add Product"
6. save() called → editingId is null → productService.create(form.value)

HTTP POST http://localhost:8080/products
Headers: Authorization: Bearer <ADMIN_TOKEN>
Body: { name: "New Item", description: "...", price: 12.99, category: "BURGER", stock: 50 }

api-gateway:
  JwtAuthFilter → role = ROLE_ADMIN → adds X-User-Role: ROLE_ADMIN
  routes to product-service:8082

product-service:
  SecurityConfig: POST /products requires hasRole('ADMIN')
  Role is ADMIN → authorized
  ProductController.createProduct() creates Product entity
  Returns 201 Created with the new product

Angular:
  snack.open('Product created')
  cancelEdit()
  loadProducts() → refreshes the table
```

**If a ROLE_USER token was used:**
```
product-service SecurityConfig: POST /products requires hasRole('ADMIN')
Role is USER → 403 Forbidden
Angular receives error → snack.open('Save failed')
```

---

## 28. Standalone Components vs NgModules

**Old way (NgModules — Angular 1–16):**
```typescript
@NgModule({
  declarations: [ProductListComponent],
  imports: [CommonModule, MatCardModule, MatButtonModule],
  exports: [ProductListComponent]
})
export class ProductModule {}
```

Each component had to be declared in a module. Modules were shared between components. You'd get "Component not declared in any module" errors.

**New way (Standalone — Angular 17+):**
```typescript
@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [NgFor, NgIf, MatCardModule, MatButtonModule, ...]  // each component declares its own deps
})
export class ProductListComponent {}
```

Each component is self-contained. No modules to manage. Benefits:
- Tree-shaking is more precise — unused components don't sneak in through modules
- Lazy loading works per-component, not per-module
- Less boilerplate — no `declarations` array to maintain
- `inject()` works anywhere inside the component class

All components in this project use `standalone: true`.

---

## 29. Signals — Reactive State Without RxJS

Angular 17 introduced **Signals** — a primitive for reactive state that's simpler than RxJS Observables for local state management.

**Creating a signal:**
```typescript
import { signal } from '@angular/core';
unreadCount = signal(0);     // initial value = 0
isLoggedIn  = signal(false);
```

**Reading a signal (in TypeScript):**
```typescript
console.log(this.unreadCount());   // call like a function
```

**Reading a signal (in templates):**
```html
<span>{{ unreadCount() }}</span>
[matBadge]="unreadCount() || null"
```

**Writing to a signal:**
```typescript
this.unreadCount.set(5);                           // replace value
this.unreadCount.update(c => c - 1);              // compute new value from old
```

**Why signals instead of a Subject/BehaviorSubject?**

BehaviorSubject approach (more verbose):
```typescript
private unreadCountSubject = new BehaviorSubject<number>(0);
unreadCount$ = this.unreadCountSubject.asObservable();
// template: {{ unreadCount$ | async }}
// update: this.unreadCountSubject.next(5)
```

Signal approach:
```typescript
unreadCount = signal(0);
// template: {{ unreadCount() }}
// update: this.unreadCount.set(5)
```

Signals are synchronous, simpler, and have better integration with Angular's change detection. They're the recommended way to manage local reactive state in Angular 17+.

---

## 30. inject() vs Constructor Injection

**Constructor injection (traditional):**
```typescript
export class MyComponent {
  form = this.fb.group({ ... });  // ❌ ERROR: fb used before initialization

  constructor(private fb: FormBuilder) {}
}
```

TypeScript class field initializers run before the constructor body. When `form = this.fb.group(...)` executes, `this.fb` hasn't been assigned yet by the constructor.

**inject() (Angular 14+):**
```typescript
export class MyComponent {
  private fb = inject(FormBuilder);   // ✅ runs during field initialization
  form = this.fb.group({ ... });       // fb is available
}
```

`inject()` resolves the dependency from the current DI context at the time the class field is initialized. It can be called anywhere in a constructor context — class fields, the constructor body, or functions called from there.

**When to use each:**
- Use `inject()` when you need the injected value to initialize another class field
- Use constructor injection when you just need dependencies available in methods

In this project, `inject()` is used throughout because it's cleaner and solves the initialization order issue.

---

## 31. Lazy Loading — How and Why

```typescript
{ path: 'admin', loadComponent: () =>
    import('./features/admin/admin').then(m => m.AdminComponent) }
```

This is a dynamic import — a JavaScript feature where modules are loaded on demand. When the user navigates to `/admin`:
1. Angular Router evaluates the `loadComponent` function
2. `import('./features/admin/admin')` returns a Promise that loads the chunk
3. `.then(m => m.AdminComponent)` extracts the component class
4. Angular instantiates and renders the component

**Build output with lazy loading:**
```
main.js         — core app shell (always loaded): ~17 kB
chunk-admin.js  — admin panel (loaded only when /admin visited): ~30 kB
chunk-login.js  — login (loaded when /login visited): ~12 kB
...
```

**Without lazy loading:**
```
main.js  — everything bundled together: ~120 kB
```

A regular user never visits `/admin`, so they never download 30 kB of admin code. This is the performance benefit of lazy loading.

---

## 32. Functional Guards — Modern Angular Pattern

Traditional class-based guard:
```typescript
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private auth: AuthService, private router: Router) {}

  canActivate(): boolean {
    if (this.auth.isLoggedIn()) return true;
    this.router.navigate(['/login']);
    return false;
  }
}
```

Functional guard (Angular 15+):
```typescript
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) return true;
  inject(Router).navigate(['/login']);
  return false;
};
```

No class, no `@Injectable`, no constructor. `inject()` works inside functional guards because the Angular DI context is active when the guard runs. Benefits:
- Less boilerplate
- Easier to test (just a function)
- Can be composed: `canActivate: [authGuard, someOtherGuard]`

---

## 33. Functional Interceptors — Modern Angular Pattern

Traditional class-based interceptor:
```typescript
@Injectable()
export class JwtInterceptor implements HttpInterceptor {
  constructor(private auth: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const token = this.auth.getToken();
    if (token) {
      req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
    }
    return next.handle(req);
  }
}
// Must also be added to providers with HTTP_INTERCEPTORS token
```

Functional interceptor (Angular 15+):
```typescript
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
// Registered in app.config.ts:
provideHttpClient(withInterceptors([jwtInterceptor]))
```

Functional interceptors are registered in `provideHttpClient()` directly — no separate providers array entry needed.

---

## 34. Running the Frontend

### Start the backend first
```bash
# From the project root — start all services
docker compose up -d

# Verify api-gateway is up
curl http://localhost:8080/products
```

### Start the Angular dev server
```bash
cd ecommerce-frontend
ng serve --port 4200

# Open http://localhost:4200
```

### Build for production
```bash
ng build --configuration=production
# Output: dist/ecommerce-frontend/browser/
# Serve with any static file server: nginx, Apache, serve, etc.
npx serve dist/ecommerce-frontend/browser
```

### Route summary

| URL | Access | Description |
|---|---|---|
| `http://localhost:4200/` | Public | Redirects to `/products` |
| `http://localhost:4200/products` | Public | Product listing |
| `http://localhost:4200/products/1` | Public | Product detail |
| `http://localhost:4200/login` | Public | Login page |
| `http://localhost:4200/register` | Public | Registration page |
| `http://localhost:4200/cart` | Login required | Shopping cart |
| `http://localhost:4200/checkout` | Login required | Place order |
| `http://localhost:4200/orders` | Login required | Order history |
| `http://localhost:4200/notifications` | Login required | Notification feed |
| `http://localhost:4200/admin` | ROLE_ADMIN only | Product management |

---

## 35. End-to-End User Flow with curl + Browser

### Register and login
```bash
# 1. Register a new user
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}'

# 2. Login (save the token)
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Equivalent in the browser: fill login form → Submit
# Token is stored automatically in localStorage by AuthService
```

### Browse products
```bash
# Public — no token needed
curl http://localhost:8080/products

# In browser: http://localhost:4200/products
# Shows product cards with stock counts and "Add to Cart" buttons
```

### Add to cart and checkout
```bash
# Add item to cart
curl -X POST http://localhost:8080/cart/items \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"productId":1,"quantity":2}'

# Place order
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"shippingAddress":"123 Main St"}'
# → {"id":7,"status":"PENDING",...}

# In browser: Add to Cart → View Cart → Proceed to Checkout → Place Order
```

### Check order confirmation and notifications
```bash
# Wait 3-5 seconds for Kafka event to propagate

# Check order status
curl http://localhost:8080/orders \
  -H "Authorization: Bearer $TOKEN"
# → [{"id":7,"status":"CONFIRMED",...}]

# Check notifications
curl http://localhost:8080/notifications \
  -H "Authorization: Bearer $TOKEN"
# → [{"type":"ORDER_CONFIRMED","message":"Your order #7 has been confirmed!...","read":false}]

# In browser: /orders shows CONFIRMED; /notifications shows the notification with blue border
```

### Admin operations (requires ROLE_ADMIN token)
```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Create product
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Veggie Wrap","description":"Fresh veggie wrap","price":8.99,"category":"MAIN_COURSE","stock":30}'

# In browser: /admin → fill form → Add Product
```

---

## 36. Common Errors and Fixes

---

### Products show "✗ Out of Stock" and button is disabled

**Cause:** The Angular `Product` interface had `available: boolean` but the backend returns `stock: number`. Since `p.available` was `undefined` (falsy), all products appeared unavailable.

**Fix:** Update the interface and template to use `stock`:
```typescript
// models/index.ts — correct
export interface Product { ...; stock: number; }

// template — correct
[disabled]="p.stock === 0"
{{ p.stock > 0 ? '✓ In Stock' : '✗ Out of Stock' }}
```

---

### "Add to Cart" works for guests (no error, but cart is empty)

**Cause:** Guest clicks Add to Cart → `cartService.addItem()` is called → `jwtInterceptor` adds no token (none in localStorage) → api-gateway returns 401 → Angular's `error` handler shows the snack.

**Fix:** Check `isLoggedIn` before calling the service:
```typescript
addToCart(p: Product) {
  if (!this.isLoggedIn) {
    this.snack.open('Please sign in', 'Sign In', ...)
      .onAction().subscribe(() => this.router.navigate(['/login']));
    return;
  }
  this.cartService.addItem(...).subscribe(...);
}
```

---

### `TS2729: Property 'fb' is used before its initialization`

**Cause:** `form = this.fb.group(...)` runs before the constructor assigns `this.fb`.

**Fix:** Use `inject()` instead of constructor injection for `fb`:
```typescript
// ❌ Broken
constructor(private fb: FormBuilder) {}
form = this.fb.group({ ... });

// ✅ Fixed
private fb = inject(FormBuilder);
form = this.fb.group({ ... });
```

---

### Navbar doesn't update after login/logout

**Cause:** The navbar reads `auth.isLoggedIn()` once at component creation instead of reactively.

**Fix:** `isLoggedIn` is a signal in `AuthService`. The template calls `auth.isLoggedIn()` — the parentheses make it a signal read, which Angular tracks for reactivity. On login, `this.isLoggedIn.set(true)` triggers re-render of any template that called `isLoggedIn()`.

---

### Badge shows 0 instead of disappearing

**Cause:** `[matBadge]="unreadCount()"` passes `0` to the badge, which displays "0".

**Fix:** Pass `null` to hide the badge when count is zero:
```html
[matBadge]="unreadCount() || null"
```
`0 || null` evaluates to `null`. Material's badge hides when value is null/undefined.

---

### CORS error in browser console

**Symptom:** `Access to XMLHttpRequest at 'http://localhost:8080' from origin 'http://localhost:4200' has been blocked by CORS policy`

**Fix:** The api-gateway's `CorsFilter` must be running. Verify the gateway is up:
```bash
docker compose ps api-gateway
curl -I http://localhost:8080/products  # check for Access-Control-Allow-Origin header
```

If the gateway is down or was not built with the CORS filter, restart it:
```bash
docker compose up -d api-gateway
```

---

### Admin panel not accessible after login

**Symptom:** Navigating to `/admin` redirects to `/products` even though you're logged in.

**Cause:** Your account has `ROLE_USER`, not `ROLE_ADMIN`. The `adminGuard` checks `isAdmin()` → `getRole() === 'ROLE_ADMIN'`.

**Fix:** Use an account registered with the admin role. Check your token in the browser DevTools → Application → Local Storage → `jwt_token`, then decode it at `jwt.io` to verify the role claim.