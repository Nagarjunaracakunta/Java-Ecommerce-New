import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { CartComponent } from './cart';
import { CartService } from '../../core/services/cart.service';
import { CartResponse } from '../../core/models';

const mockCart: CartResponse = {
  username: 'testuser',
  items: [{ productId: 1, productName: 'Burger', price: 9.99, quantity: 2, subtotal: 19.98 }],
  total: 19.98
};

describe('CartComponent', () => {
  let component: CartComponent;
  let fixture: ComponentFixture<CartComponent>;
  let cartService: jasmine.SpyObj<CartService>;

  beforeEach(async () => {
    cartService = jasmine.createSpyObj('CartService', ['getCart', 'removeItem', 'clearCart']);
    cartService.getCart.and.returnValue(of(mockCart));

    await TestBed.configureTestingModule({
      imports: [CartComponent, RouterTestingModule, NoopAnimationsModule, MatSnackBarModule],
      providers: [{ provide: CartService, useValue: cartService }]
    }).compileComponents();

    fixture = TestBed.createComponent(CartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => expect(component).toBeTruthy());

  it('should load cart on init', () => {
    expect(component.items).toEqual(mockCart.items);
    expect(component.total).toBe(19.98);
    expect(component.loading).toBeFalse();
  });

  it('should show empty state when cart has no items', () => {
    cartService.getCart.and.returnValue(of({ username: 'u', items: [], total: 0 }));
    component.ngOnInit();
    fixture.detectChanges();
    expect(component.items.length).toBe(0);
    const empty = fixture.nativeElement.querySelector('.empty-state');
    expect(empty).toBeTruthy();
  });

  it('should handle null items in cart response without crashing', () => {
    cartService.getCart.and.returnValue(of({ username: 'u', items: null as any, total: 0 }));
    component.ngOnInit();
    expect(component.items).toEqual([]);
    expect(component.loading).toBeFalse();
  });

  it('should set loading false and show empty state on API error', () => {
    cartService.getCart.and.returnValue(throwError(() => new Error('Unauthorized')));
    component.ngOnInit();
    expect(component.loading).toBeFalse();
    expect(component.items).toEqual([]);
  });

  it('should remove item and update state', () => {
    const updatedCart: CartResponse = { username: 'u', items: [], total: 0 };
    cartService.removeItem.and.returnValue(of(updatedCart));
    component.remove(1);
    expect(cartService.removeItem).toHaveBeenCalledWith(1);
    expect(component.items).toEqual([]);
    expect(component.total).toBe(0);
  });

  it('should clear cart and reset state', () => {
    cartService.clearCart.and.returnValue(of(undefined as any));
    component.clearCart();
    expect(cartService.clearCart).toHaveBeenCalled();
    expect(component.items).toEqual([]);
    expect(component.total).toBe(0);
  });
});