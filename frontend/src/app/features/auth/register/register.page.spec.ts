import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';
import { RegisterPage } from './register.page';

describe('RegisterPage', () => {
  let fixture: ComponentFixture<RegisterPage>;
  let component: RegisterPage;
  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['register', 'clearSession']);
  const feedback = jasmine.createSpyObj<AuthFeedbackService>('AuthFeedbackService', [
    'runWithLoading',
    'showToast',
  ]);

  beforeEach(async () => {
    sessionStorage.clear();
    router.navigateByUrl.calls.reset();
    authService.register.calls.reset();
    authService.clearSession.calls.reset();
    feedback.runWithLoading.calls.reset();
    feedback.showToast.calls.reset();
    feedback.runWithLoading.and.callFake((_message: string, action: () => unknown) =>
      firstValueFrom(action() as never)
    );
    feedback.showToast.and.resolveTo();
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [RegisterPage],
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
        { provide: AuthFeedbackService, useValue: feedback },
      ],
    })
      .overrideComponent(RegisterPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(RegisterPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('registers, clears local auth session and redirects to email sent', async () => {
    authService.register.and.returnValue(
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
      name: ' Alice ',
      email: ' alice@example.test ',
      password: 'Password1!',
      countryCode: 'BJ',
      currencyCode: 'XOF',
    });

    await component.onSubmit();

    expect(authService.register).toHaveBeenCalledWith({
      name: 'Alice',
      email: 'alice@example.test',
      password: 'Password1!',
      countryCode: 'BJ',
      currencyCode: 'XOF',
    });
    expect(authService.clearSession).toHaveBeenCalled();
    expect(sessionStorage.getItem('owomi_pending_email')).toBe('alice@example.test');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/email-sent');
  });

  it('maps validation backend errors', async () => {
    const error: UiError = {
      code: 'VALIDATION_ERROR',
      message: 'Validation impossible.',
      details: ['La devise est inconnue.'],
      status: 400,
    };
    authService.register.and.returnValue(throwError(() => error));
    component.form.setValue({
      name: 'Alice',
      email: 'alice@example.test',
      password: 'Password1!',
      countryCode: 'BJ',
      currencyCode: 'XXX',
    });

    await component.onSubmit();

    expect(component.registerError()).toBe('La devise est inconnue.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
