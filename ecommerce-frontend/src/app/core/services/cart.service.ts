import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { CartItem, CartResponse } from '../models';

@Injectable({ providedIn: 'root' })
export class CartService {
  private base = `${environment.apiUrl}/cart`;
  constructor(private http: HttpClient) {}

  getCart()                  { return this.http.get<CartResponse>(this.base); }
  addItem(item: CartItem)    { return this.http.post<CartResponse>(`${this.base}/items`, item); }
  removeItem(productId: number) { return this.http.delete<CartResponse>(`${this.base}/items/${productId}`); }
  clearCart()                { return this.http.delete<void>(this.base); }
}