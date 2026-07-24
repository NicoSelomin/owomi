import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { TokenService } from '../../../core/services/token.service';
import { SplashPage } from './splash.page';

describe('SplashPage', () => {
  let fixture: ComponentFixture<SplashPage>;
  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  const tokenService = jasmine.createSpyObj<TokenService>('TokenService', ['isAuthenticated']);

  beforeEach(async () => {
    router.navigateByUrl.calls.reset();
    tokenService.isAuthenticated.calls.reset();
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [SplashPage],
      providers: [
        { provide: Router, useValue: router },
        { provide: TokenService, useValue: tokenService },
      ],
    }).compileComponents();
  });

  it('redirects authenticated users to dashboard', async () => {
    tokenService.isAuthenticated.and.returnValue(true);

    fixture = TestBed.createComponent(SplashPage);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/dashboard', { replaceUrl: true });
  });

  it('redirects unauthenticated users to login', async () => {
    tokenService.isAuthenticated.and.returnValue(false);

    fixture = TestBed.createComponent(SplashPage);
    fixture.detectChanges();
    await fixture.whenStable();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/login', { replaceUrl: true });
  });
});
