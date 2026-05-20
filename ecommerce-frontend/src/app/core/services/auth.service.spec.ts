import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return false for isLoggedIn when no token', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should return true for isLoggedIn when token exists', () => {
    localStorage.setItem('jwt_token', 'some-token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should store token on successful login', () => {
    service.login({ username: 'user', password: 'pass' }).subscribe();
    const req = http.expectOne('http://localhost:8080/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 'test.jwt.token', tokenType: 'Bearer', expiresIn: 86400000 });
    expect(localStorage.getItem('jwt_token')).toBe('test.jwt.token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  it('should clear token on logout', () => {
    localStorage.setItem('jwt_token', 'some-token');
    service.logout();
    expect(localStorage.getItem('jwt_token')).toBeNull();
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should return token from localStorage', () => {
    localStorage.setItem('jwt_token', 'my-token');
    expect(service.getToken()).toBe('my-token');
  });

  it('should return null for getToken when not logged in', () => {
    expect(service.getToken()).toBeNull();
  });

  it('should parse username from JWT payload', () => {
    const payload = btoa(JSON.stringify({ sub: 'testuser', role: 'ROLE_USER' }));
    localStorage.setItem('jwt_token', `header.${payload}.sig`);
    expect(service.getUsername()).toBe('testuser');
  });

  it('should return null for getUsername when no token', () => {
    expect(service.getUsername()).toBeNull();
  });

  it('should correctly identify admin role', () => {
    const payload = btoa(JSON.stringify({ sub: 'admin', role: 'ROLE_ADMIN' }));
    localStorage.setItem('jwt_token', `header.${payload}.sig`);
    expect(service.isAdmin()).toBeTrue();
  });

  it('should return false for isAdmin with user role', () => {
    const payload = btoa(JSON.stringify({ sub: 'user', role: 'ROLE_USER' }));
    localStorage.setItem('jwt_token', `header.${payload}.sig`);
    expect(service.isAdmin()).toBeFalse();
  });
});