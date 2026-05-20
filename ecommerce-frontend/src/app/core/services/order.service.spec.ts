import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { Order } from '../models';

const mockOrder: Order = {
  id: 1, username: 'testuser', status: 'PENDING', totalAmount: 19.98,
  shippingAddress: '123 Main St', items: [], createdAt: '2026-05-20T10:00:00Z', updatedAt: '2026-05-20T10:00:00Z'
};

describe('OrderService', () => {
  let service: OrderService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [OrderService]
    });
    service = TestBed.inject(OrderService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('should POST to place an order', () => {
    service.placeOrder({ shippingAddress: '123 Main St' }).subscribe(o => expect(o).toEqual(mockOrder));
    const req = http.expectOne('http://localhost:8080/orders');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ shippingAddress: '123 Main St' });
    req.flush(mockOrder);
  });

  it('should GET user orders', () => {
    service.getMyOrders().subscribe(orders => {
      expect(orders.length).toBe(1);
      expect(orders[0]).toEqual(mockOrder);
    });
    const req = http.expectOne('http://localhost:8080/orders');
    expect(req.request.method).toBe('GET');
    req.flush([mockOrder]);
  });

  it('should GET order by id', () => {
    service.getById(1).subscribe(o => expect(o).toEqual(mockOrder));
    const req = http.expectOne('http://localhost:8080/orders/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockOrder);
  });

  it('should DELETE to cancel an order', () => {
    const cancelled = { ...mockOrder, status: 'CANCELLED' };
    service.cancelOrder(1).subscribe(o => expect(o.status).toBe('CANCELLED'));
    const req = http.expectOne('http://localhost:8080/orders/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(cancelled);
  });
});