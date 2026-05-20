import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { Notification } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private base = `${environment.apiUrl}/notifications`;
  unreadCount$ = new BehaviorSubject<number>(0);

  constructor(private http: HttpClient) {}

  getUnread()  { return this.http.get<Notification[]>(this.base); }
  getAll()     { return this.http.get<Notification[]>(`${this.base}/all`); }
  getCount()   {
    return this.http.get<{ unread: number }>(`${this.base}/count`).pipe(
      tap(r => this.unreadCount$.next(r.unread))
    );
  }
  markRead(id: string) { return this.http.patch<void>(`${this.base}/${id}/read`, {}); }
  markAllRead()        { return this.http.patch<void>(`${this.base}/read-all`, {}); }
}