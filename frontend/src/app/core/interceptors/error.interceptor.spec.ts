import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { errorInterceptor } from './error.interceptor';
import { AuthService } from '../services/auth.service';
import { ErrorService, UiError } from '../services/error.service';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', [
    'refreshToken',
    'clearSession',
  ]);

  beforeEach(() => {
    router.navigate.calls.reset();
    authService.refreshToken.calls.reset();
    authService.clearSession.calls.reset();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        ErrorService,
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('refreshes once on 401 and replays the protected request', () => {
    authService.refreshToken.and.returnValue(
      of({
        success: true,
        data: {
          accessToken: 'new-token',
          refreshToken: 'refresh-token',
          user: { id: '1', name: 'A', email: 'a@example.test', currency: null },
        },
        timestamp: '2026-01-01T00:00:00Z',
      })
    );

    http.get('/api/users/me').subscribe();

    const first = httpMock.expectOne('/api/users/me');
    first.flush(
      { success: false, error: { code: 'TOKEN_INVALID', message: 'Token invalide.', details: [] } },
      { status: 401, statusText: 'Unauthorized' }
    );

    const replay = httpMock.expectOne('/api/users/me');
    expect(replay.request.headers.get('Authorization')).toBe('Bearer new-token');
    replay.flush({});
    expect(authService.refreshToken).toHaveBeenCalledTimes(1);
  });

  it('does not refresh auth endpoint failures and returns UiError', () => {
    const uiErrors: UiError[] = [];

    http.post('/api/auth/login', {}).subscribe({
      error: (error: UiError) => {
        uiErrors.push(error);
      },
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush(
      { success: false, error: { code: 'INVALID_CREDENTIALS', message: 'Email ou mot de passe incorrect.', details: [] } },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(authService.refreshToken).not.toHaveBeenCalled();
    expect(uiErrors[0].code).toBe('INVALID_CREDENTIALS');
  });
});
