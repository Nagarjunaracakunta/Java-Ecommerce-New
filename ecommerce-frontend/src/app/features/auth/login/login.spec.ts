import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login';
import { AuthService } from '../../../core/services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj('AuthService', ['login']);

    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule, NoopAnimationsModule, MatSnackBarModule],
      providers: [{ provide: AuthService, useValue: authService }]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => expect(component).toBeTruthy());

  it('should have an invalid form when fields are empty', () => {
    expect(component.form.invalid).toBeTrue();
  });

  it('should be valid when both fields are filled', () => {
    component.form.setValue({ username: 'user', password: 'pass' });
    expect(component.form.valid).toBeTrue();
  });

  it('should not call login when form is invalid', () => {
    component.submit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should call auth.login with form values on valid submit', () => {
    authService.login.and.returnValue(of({ token: 'tok', tokenType: 'Bearer', expiresIn: 86400 }));
    component.form.setValue({ username: 'testuser', password: 'password' });
    component.submit();
    expect(authService.login).toHaveBeenCalledWith({ username: 'testuser', password: 'password' });
  });

  it('should set loading to false on login error', () => {
    authService.login.and.returnValue(throwError(() => ({ error: { error: 'Invalid credentials' } })));
    component.form.setValue({ username: 'bad', password: 'wrong' });
    component.submit();
    expect(component.loading).toBeFalse();
  });
});