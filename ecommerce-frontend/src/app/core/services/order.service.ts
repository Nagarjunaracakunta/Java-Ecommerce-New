import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { Order, PlaceOrderRequest } from '../models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private base = `${environment.apiUrl}/orders`;
  constructor(private http: HttpClient) {}

  placeOrder(req: PlaceOrderRequest) { return this.http.post<Order>(this.base, req); }
  getMyOrders()                      { return this.http.get<Order[]>(this.base); }
  getById(id: number)                { return this.http.get<Order>(`${this.base}/${id}`); }
  cancelOrder(id: number)            { return this.http.delete<Order>(`${this.base}/${id}`); }
}