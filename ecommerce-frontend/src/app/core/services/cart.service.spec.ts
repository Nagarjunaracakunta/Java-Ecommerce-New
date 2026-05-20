import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CartService } from './cart.service';
import { CartResponse } from '../models';

const mockCartResponse: CartResponse = {
  username: 'testuser',
  items: [{ productId: 1, productName: 'Burger', price: 9.99, quantity: 2, subtotal: 19.98 }],
  total: 19.98
};

describe('CartService', () => {
  let service: CartService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CartService]
    });
    service = TestBed.inject(CartService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('should GET the cart', () => {
    service.getCart().subscribe(cart => expect(cart).toEqual(mockCartResponse));
    const req = http.expectOne('http://localhost:8080/cart');
    expect(req.request.method).toBe('GET');
    req.flush(mockCartResponse);
  });

  it('should POST to add an item', () => {
    service.addItem({ productId: 1, quantity: 2 }).subscribe(cart => expect(cart).toEqual(mockCartResponse));
    const req = http.expectOne('http://localhost:8080/cart/items');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ productId: 1, quantity: 2 });
    req.flush(mockCartResponse);
  });

  it('should DELETE to remove an item', () => {
    const emptyCart: CartResponse = { username: 'testuser', items: [], total: 0 };
    service.removeItem(1).subscribe(cart => expect(cart.items.length).toBe(0));
    const req = http.expectOne('http://localhost:8080/cart/items/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(emptyCart);
  });

  it('should DELETE to clear the cart', () => {
    service.clearCart().subscribe(() => expect(true).toBeTrue());
    const req = http.expectOne('http://localhost:8080/cart');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});