import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NotificationService]
    });
    service = TestBed.inject(NotificationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should be created', () => expect(service).toBeTruthy());

  it('should initialize unreadCount$ with 0', () => {
    expect(service.unreadCount$.getValue()).toBe(0);
  });

  it('should GET unread notifications', () => {
    service.getUnread().subscribe();
    const req = http.expectOne('http://localhost:8080/notifications');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should GET all notifications', () => {
    service.getAll().subscribe();
    const req = http.expectOne('http://localhost:8080/notifications/all');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('should update unreadCount$ from getCount response', () => {
    service.getCount().subscribe();
    const req = http.expectOne('http://localhost:8080/notifications/count');
    req.flush({ unread: 5 });
    expect(service.unreadCount$.getValue()).toBe(5);
  });

  it('should PATCH to mark a notification read', () => {
    service.markRead('notif-1').subscribe();
    const req = http.expectOne('http://localhost:8080/notifications/notif-1/read');
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });

  it('should PATCH to mark all notifications read', () => {
    service.markAllRead().subscribe();
    const req = http.expectOne('http://localhost:8080/notifications/read-all');
    expect(req.request.method).toBe('PATCH');
    req.flush(null);
  });
});