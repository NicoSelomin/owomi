import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';
import { VerifyEmailPage } from './verify-email.page';

describe('VerifyEmailPage', () => {
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['verifyEmail']);
  const feedback = jasmine.createSpyObj<AuthFeedbackService>('AuthFeedbackService', [
    'runWithLoading',
    'showToast',
  ]);

  async function createComponent(token: string | null): Promise<ComponentFixture<VerifyEmailPage>> {
    await TestBed.configureTestingModule({
      imports: [VerifyEmailPage],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => token } } },
        },
        { provide: AuthService, useValue: authService },
        { provide: AuthFeedbackService, useValue: feedback },
      ],
    })
      .overrideComponent(VerifyEmailPage, { set: { template: '' } })
      .compileComponents();

    const fixture = TestBed.createComponent(VerifyEmailPage);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
    authService.verifyEmail.calls.reset();
    feedback.runWithLoading.calls.reset();
    feedback.showToast.calls.reset();
    feedback.runWithLoading.and.callFake((_message: string, action: () => unknown) =>
      firstValueFrom(action() as never)
    );
    feedback.showToast.and.resolveTo();
  });

  it('verifies email token from URL', async () => {
    authService.verifyEmail.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );

    const fixture = await createComponent('verify-token');

    expect(authService.verifyEmail).toHaveBeenCalledWith('verify-token');
    expect(fixture.componentInstance.state()).toBe('success');
  });

  it('shows invalid state when token is missing', async () => {
    const fixture = await createComponent(null);

    expect(authService.verifyEmail).not.toHaveBeenCalled();
    expect(fixture.componentInstance.state()).toBe('error');
  });

  it('maps expired verification token', async () => {
    const error: UiError = {
      code: 'VERIFICATION_TOKEN_EXPIRED',
      message: 'Token expiré.',
      details: [],
      status: 400,
    };
    authService.verifyEmail.and.returnValue(throwError(() => error));

    const fixture = await createComponent('expired-token');

    expect(fixture.componentInstance.errorMessage()).toContain('expiré');
  });
});
