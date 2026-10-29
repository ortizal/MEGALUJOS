import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient()],
    });
    service = TestBed.inject(AuthService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('inicia sin sesión', () => {
    expect(service.isLoggedIn()).toBe(false);
    expect(service.user()).toBeNull();
    expect(service.permissions()).toEqual([]);
  });

  it('hasPermission refleja los permisos del usuario', () => {
    service.user.set({
      userId: 'u1',
      id: 'u1',
      username: 'admin',
      fullName: 'Admin',
      roles: ['ADMIN'],
      permissions: ['catalog:write', 'reports:view'],
    });
    expect(service.hasPermission('catalog:write')).toBe(true);
    expect(service.hasPermission('users:manage')).toBe(false);
    expect(service.hasAnyPermission(['users:manage', 'reports:view'])).toBe(true);
    expect(service.hasAllPermissions(['users:manage', 'reports:view'])).toBe(false);
  });

  it('logout limpia tokens, usuario y almacenamiento', () => {
    service.user.set({
      userId: 'u1',
      id: 'u1',
      username: 'admin',
      fullName: 'Admin',
      roles: [],
      permissions: [],
    });
    service.accessToken.set('token');
    service.refreshToken.set('refresh');
    service.logout();
    expect(service.accessToken()).toBeNull();
    expect(service.refreshToken()).toBeNull();
    expect(service.user()).toBeNull();
    expect(localStorage.getItem('mega.auth')).toBeNull();
  });

  it('isAuthUrl detecta endpoints de autenticación', () => {
    expect(service.isAuthUrl('/api/v1/auth/login')).toBe(true);
    expect(service.isAuthUrl('/api/v1/auth/refresh')).toBe(true);
    expect(service.isAuthUrl('/api/v1/sales')).toBe(false);
  });
});
