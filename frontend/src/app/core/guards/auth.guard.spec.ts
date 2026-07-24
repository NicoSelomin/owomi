import { TestBed } from '@angular/core/testing';
import { CanActivateFn, Router } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { TokenService } from '../services/token.service';

describe('AuthGuard', () => {
  const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
  const tokenService = jasmine.createSpyObj<TokenService>('TokenService', ['isAuthenticated']);
  const executeGuard: CanActivateFn = (...guardParameters) =>
    TestBed.runInInjectionContext(() => AuthGuard(...guardParameters));

  beforeEach(() => {
    router.navigate.calls.reset();
    tokenService.isAuthenticated.calls.reset();
    TestBed.configureTestingModule({
      providers: [
        { provide: Router, useValue: router },
        { provide: TokenService, useValue: tokenService },
      ],
    });
  });

  it('allows authenticated users', () => {
    tokenService.isAuthenticated.and.returnValue(true);

    const result = executeGuard({} as never, { url: '/app/dashboard' } as never);

    expect(result).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects unauthenticated users with returnUrl', () => {
    tokenService.isAuthenticated.and.returnValue(false);

    const result = executeGuard({} as never, { url: '/app/dashboard' } as never);

    expect(result).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login'], {
      queryParams: { returnUrl: '/app/dashboard' },
    });
  });
});
