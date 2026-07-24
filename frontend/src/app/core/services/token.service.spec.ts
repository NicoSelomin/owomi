import { TestBed } from '@angular/core/testing';
import { TokenService } from './token.service';

function jwt(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ exp: expSeconds }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${payload}.signature`;
}

describe('TokenService', () => {
  let service: TokenService;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({});
    service = TestBed.inject(TokenService);
  });

  afterEach(() => sessionStorage.clear());

  it('stores access and refresh tokens in sessionStorage', () => {
    service.setTokens('access-token', 'refresh-token');

    expect(service.getAccessToken()).toBe('access-token');
    expect(service.getRefreshToken()).toBe('refresh-token');
    expect(localStorage.getItem('owomi_access')).toBeNull();
  });

  it('clears tokens on logout', () => {
    service.setTokens('access-token', 'refresh-token');

    service.clearTokens();

    expect(service.getAccessToken()).toBeNull();
    expect(service.getRefreshToken()).toBeNull();
  });

  it('detects token expiration without validating signature client-side', () => {
    service.setAccessToken(jwt(Math.floor(Date.now() / 1000) + 60));
    expect(service.isAuthenticated()).toBeTrue();

    service.setAccessToken(jwt(Math.floor(Date.now() / 1000) - 60));
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('returns false for malformed tokens', () => {
    service.setAccessToken('not-a-jwt');

    expect(service.isAuthenticated()).toBeFalse();
  });
});
