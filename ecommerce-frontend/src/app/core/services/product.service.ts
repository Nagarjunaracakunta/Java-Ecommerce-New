import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Product } from '../models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private base = `${environment.apiUrl}/products`;
  constructor(private http: HttpClient) {}

  getAll()                        { return this.http.get<Product[]>(this.base); }
  getById(id: number)             { return this.http.get<Product>(`${this.base}/${id}`); }
  create(p: Partial<Product>)     { return this.http.post<Product>(this.base, p); }
  update(id: number, p: Partial<Product>) { return this.http.put<Product>(`${this.base}/${id}`, p); }
  delete(id: number)              { return this.http.delete<void>(`${this.base}/${id}`); }
}