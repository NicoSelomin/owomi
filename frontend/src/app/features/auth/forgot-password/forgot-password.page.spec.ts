import { ComponentFixture, TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';
import { ForgotPasswordPage } from './forgot-password.page';

describe('ForgotPasswordPage', () => {
  let fixture: ComponentFixture<ForgotPasswordPage>;
  let component: ForgotPasswordPage;
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['forgotPassword']);
  const feedback = jasmine.createSpyObj<AuthFeedbackService>('AuthFeedbackService', [
    'runWithLoading',
    'showToast',
  ]);

  beforeEach(async () => {
    authService.forgotPassword.calls.reset();
    feedback.runWithLoading.calls.reset();
    feedback.showToast.calls.reset();
    feedback.runWithLoading.and.callFake((_message: string, action: () => unknown) =>
      firstValueFrom(action() as never)
    );
    feedback.showToast.and.resolveTo();

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordPage],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: AuthFeedbackService, useValue: feedback },
      ],
    })
      .overrideComponent(ForgotPasswordPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('submits forgot password request and confirms generically', async () => {
    authService.forgotPassword.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );
    component.form.setValue({ email: ' alice@example.test ' });

    await component.onSubmit();

    expect(authService.forgotPassword).toHaveBeenCalledWith('alice@example.test');
    expect(component.submitted()).toBeTrue();
  });

  it('shows mapped backend error', async () => {
    const error: UiError = {
      code: 'NETWORK_OR_SERVER_ERROR',
      message: 'Impossible de joindre le serveur.',
      details: [],
      status: 0,
    };
    authService.forgotPassword.and.returnValue(throwError(() => error));
    component.form.setValue({ email: 'alice@example.test' });

    await component.onSubmit();

    expect(component.submitError()).toBe('Impossible de joindre le serveur.');
  });
});
