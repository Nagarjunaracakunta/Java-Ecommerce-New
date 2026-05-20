import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { ProductListComponent } from './product-list';
import { ProductService } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { Product } from '../../../core/models';

const mockProducts: Product[] = [
  { id: 1, name: 'Burger', description: 'Tasty', price: 9.99, stock: 5, category: 'BURGER' },
  { id: 2, name: 'Pizza', description: 'Cheesy', price: 14.99, stock: 0, category: 'PIZZA' }
];

describe('ProductListComponent', () => {
  let component: ProductListComponent;
  let fixture: ComponentFixture<ProductListComponent>;
  let productService: jasmine.SpyObj<ProductService>;
  let cartService: jasmine.SpyObj<CartService>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    productService = jasmine.createSpyObj('ProductService', ['getAll']);
    cartService = jasmine.createSpyObj('CartService', ['addItem']);
    authService = jasmine.createSpyObj('AuthService', ['isLoggedIn', 'isAdmin']);

    productService.getAll.and.returnValue(of(mockProducts));
    authService.isLoggedIn.and.returnValue(false);
    authService.isAdmin.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [ProductListComponent, RouterTestingModule, NoopAnimationsModule, MatSnackBarModule],
      providers: [
        { provide: ProductService, useValue: productService },
        { provide: CartService, useValue: cartService },
        { provide: AuthService, useValue: authService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => expect(component).toBeTruthy());

  it('should load products on init', () => {
    expect(component.products.length).toBe(2);
    expect(component.loading).toBeFalse();
  });

  it('should display product cards', () => {
    fixture.detectChanges();
    const cards = fixture.nativeElement.querySelectorAll('mat-card');
    expect(cards.length).toBeGreaterThan(0);
  });

  it('should show spinner while loading', () => {
    component.loading = true;
    fixture.detectChanges();
    const spinner = fixture.nativeElement.querySelector('mat-spinner');
    expect(spinner).toBeTruthy();
  });

  it('should hide spinner after products load', () => {
    component.loading = false;
    fixture.detectChanges();
    const spinner = fixture.nativeElement.querySelector('mat-spinner');
    expect(spinner).toBeFalsy();
  });

  it('should disable Add to Cart button for out-of-stock products', () => {
    fixture.detectChanges();
    const buttons: NodeListOf<HTMLButtonElement> = fixture.nativeElement.querySelectorAll('button[disabled]');
    expect(buttons.length).toBeGreaterThan(0);
  });

  it('should set loading to false on error', () => {
    productService.getAll.and.returnValue(throwError(() => new Error('Network error')));
    component.ngOnInit();
    expect(component.loading).toBeFalse();
    expect(component.products).toEqual([]);
  });

  it('should prompt login when unauthenticated user tries to add to cart', () => {
    authService.isLoggedIn.and.returnValue(false);
    const snackSpy = spyOn((component as any).snack, 'open').and.callThrough();
    component.addToCart(mockProducts[0]);
    expect(snackSpy).toHaveBeenCalled();
    expect(cartService.addItem).not.toHaveBeenCalled();
  });

  it('should call addItem when logged-in user adds to cart', () => {
    authService.isLoggedIn.and.returnValue(true);
    cartService.addItem.and.returnValue(of({ username: 'u', items: [], total: 0 }));
    component.isLoggedIn = true;
    component.addToCart(mockProducts[0]);
    expect(cartService.addItem).toHaveBeenCalledWith({ productId: 1, quantity: 1 });
  });
});