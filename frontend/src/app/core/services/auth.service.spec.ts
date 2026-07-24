import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { TokenService } from './token.service';

describe('AuthService', () => {
  let service: AuthService;
  let tokenService: TokenService;
  let httpMock: HttpTestingController;

  const authBody = {
    success: true,
    data: {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      user: { id: '1', name: 'Alice', email: 'alice@example.test', currency: null },
    },
    timestamp: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    tokenService = TestBed.inject(TokenService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('logs in and stores tokens', () => {
    service.login({ email: 'alice@example.test', password: 'Password1!' }).subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(authBody);

    expect(tokenService.getAccessToken()).toBe('access-token');
    expect(tokenService.getRefreshToken()).toBe('refresh-token');
    expect(service.currentUser()?.email).toBe('alice@example.test');
  });

  it('refreshes the access token', () => {
    tokenService.setTokens('old-access', 'refresh-token');

    service.refreshToken().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/refresh');
    expect(req.request.body).toEqual({ refreshToken: 'refresh-token' });
    req.flush(authBody);

    expect(tokenService.getAccessToken()).toBe('access-token');
  });

  it('loads current user through /api/users/me', () => {
    service.loadCurrentUser().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/users/me');
    req.flush({
      success: true,
      data: { id: '1', name: 'Alice', email: 'alice@example.test', currency: null },
      timestamp: '2026-01-01T00:00:00Z',
    });

    expect(service.currentUser()?.name).toBe('Alice');
  });

  it('clears local session on logout success', () => {
    tokenService.setTokens('access-token', 'refresh-token');

    service.logout().subscribe();

    const req = httpMock.expectOne('http://localhost:8080/api/auth/logout');
    expect(req.request.body).toEqual({ refreshToken: 'refresh-token' });
    req.flush({ success: true, data: null, timestamp: '2026-01-01T00:00:00Z' });

    expect(tokenService.getAccessToken()).toBeNull();
    expect(tokenService.getRefreshToken()).toBeNull();
  });
});
