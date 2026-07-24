import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of, throwError, firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  let fixture: ComponentFixture<LoginPage>;
  let component: LoginPage;
  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', [
    'login',
    'loadCurrentUser',
    'resendVerification',
  ]);
  const feedback = jasmine.createSpyObj<AuthFeedbackService>('AuthFeedbackService', [
    'runWithLoading',
    'showToast',
  ]);

  beforeEach(async () => {
    sessionStorage.clear();
    localStorage.clear();
    router.navigateByUrl.calls.reset();
    authService.login.calls.reset();
    authService.loadCurrentUser.calls.reset();
    authService.resendVerification.calls.reset();
    feedback.showToast.calls.reset();
    feedback.runWithLoading.calls.reset();
    feedback.runWithLoading.and.callFake((_message: string, action: () => unknown) =>
      firstValueFrom(action() as never)
    );
    feedback.showToast.and.resolveTo();
    router.navigateByUrl.and.resolveTo(true);
    authService.loadCurrentUser.and.returnValue(
      of({
        success: true,
        data: { id: '1', name: 'Alice', email: 'alice@example.test', currency: null },
        timestamp: '2026-01-01T00:00:00Z',
      })
    );

    await TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => null } } },
        },
        { provide: AuthService, useValue: authService },
        { provide: AuthFeedbackService, useValue: feedback },
      ],
    })
      .overrideComponent(LoginPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(LoginPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('logs in, reloads user and redirects to dashboard', async () => {
    authService.login.and.returnValue(
      of({
        success: true,
        data: {
          accessToken: 'access-token',
          refreshToken: 'refresh-token',
          user: { id: '1', name: 'Alice', email: 'alice@example.test', currency: null },
        },
        timestamp: '2026-01-01T00:00:00Z',
      })
    );

    component.form.setValue({
      email: ' alice@example.test ',
      password: 'Password1!',
      rememberMe: false,
    });

    await component.onSubmit();

    expect(authService.login).toHaveBeenCalledWith({
      email: 'alice@example.test',
      password: 'Password1!',
    });
    expect(authService.loadCurrentUser).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/app/dashboard');
  });

  it('handles unverified email without generating navigation', async () => {
    const error: UiError = {
      code: 'EMAIL_NOT_VERIFIED',
      message:
        "Votre adresse email n'est pas encore vérifiée. Vous pouvez demander un nouveau lien de vérification.",
      details: [],
      status: 400,
    };
    authService.login.and.returnValue(throwError(() => error));

    component.form.setValue({
      email: ' alice@example.test ',
      password: 'Password1!',
      rememberMe: false,
    });

    await component.onSubmit();

    expect(sessionStorage.getItem('owomi_pending_email')).toBe('alice@example.test');
    expect(component.loginError()).toContain("n'est pas encore vérifiée");
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(feedback.showToast).toHaveBeenCalled();
  });

  it('resends verification link for a valid email', async () => {
    authService.resendVerification.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );
    component.form.patchValue({ email: 'alice@example.test' });

    await component.resendVerification();

    expect(authService.resendVerification).toHaveBeenCalledWith('alice@example.test');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/email-sent');
  });
});
