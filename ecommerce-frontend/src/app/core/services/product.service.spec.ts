import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProductService } from './product.service';
import { Product } from '../models';

const mockProduct: Product = {
  id: 1, name: 'Burger', description: 'Juicy burger', price: 9.99, stock: 10, category: 'BURGER'
};

describe('ProductService', () => {
  let service: ProductService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProductService]
    });
    service = TestBed.inject(ProductService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('should GET all products', () => {
    service.getAll().subscribe(products => {
      expect(products.length).toBe(1);
      expect(products[0]).toEqual(mockProduct);
    });
    const req = http.expectOne('http://localhost:8080/products');
    expect(req.request.method).toBe('GET');
    req.flush([mockProduct]);
  });

  it('should GET product by id', () => {
    service.getById(1).subscribe(p => expect(p).toEqual(mockProduct));
    const req = http.expectOne('http://localhost:8080/products/1');
    expect(req.request.method).toBe('GET');
    req.flush(mockProduct);
  });

  it('should POST to create a product', () => {
    const newProduct = { name: 'Pizza', price: 12.99, category: 'PIZZA', stock: 5, description: 'Yum' };
    service.create(newProduct).subscribe(p => expect(p.name).toBe('Pizza'));
    const req = http.expectOne('http://localhost:8080/products');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newProduct);
    req.flush({ id: 2, ...newProduct });
  });

  it('should PUT to update a product', () => {
    service.update(1, { name: 'Updated Burger' }).subscribe(p => expect(p.name).toBe('Updated Burger'));
    const req = http.expectOne('http://localhost:8080/products/1');
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockProduct, name: 'Updated Burger' });
  });

  it('should DELETE a product', () => {
    service.delete(1).subscribe(() => expect(true).toBeTrue());
    const req = http.expectOne('http://localhost:8080/products/1');
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});