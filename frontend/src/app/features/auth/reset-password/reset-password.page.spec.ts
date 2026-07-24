import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';
import { ResetPasswordPage } from './reset-password.page';

describe('ResetPasswordPage', () => {
  let fixture: ComponentFixture<ResetPasswordPage>;
  let component: ResetPasswordPage;
  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['resetPassword']);
  const feedback = jasmine.createSpyObj<AuthFeedbackService>('AuthFeedbackService', [
    'runWithLoading',
    'showToast',
  ]);

  beforeEach(async () => {
    router.navigateByUrl.calls.reset();
    authService.resetPassword.calls.reset();
    feedback.runWithLoading.calls.reset();
    feedback.showToast.calls.reset();
    feedback.runWithLoading.and.callFake((_message: string, action: () => unknown) =>
      firstValueFrom(action() as never)
    );
    feedback.showToast.and.resolveTo();
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [ResetPasswordPage],
      providers: [
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => 'reset-token' } } },
        },
        { provide: AuthService, useValue: authService },
        { provide: AuthFeedbackService, useValue: feedback },
      ],
    })
      .overrideComponent(ResetPasswordPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(ResetPasswordPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('resets password with URL token and switches to success state', async () => {
    authService.resetPassword.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );
    component.form.setValue({ password: 'Password1!', confirmPassword: 'Password1!' });

    await component.onSubmit();

    expect(authService.resetPassword).toHaveBeenCalledWith('reset-token', 'Password1!');
    expect(component.state()).toBe('success');
  });

  it('maps invalid reset token', async () => {
    const error: UiError = {
      code: 'RESET_TOKEN_INVALID',
      message: 'Token invalide.',
      details: [],
      status: 400,
    };
    authService.resetPassword.and.returnValue(throwError(() => error));
    component.form.setValue({ password: 'Password1!', confirmPassword: 'Password1!' });

    await component.onSubmit();

    expect(component.resetError()).toContain('invalide');
  });
});
