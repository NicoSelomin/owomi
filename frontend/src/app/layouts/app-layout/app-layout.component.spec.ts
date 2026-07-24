import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { firstValueFrom, of, throwError } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { AuthFeedbackService } from '../../core/services/auth-feedback.service';
import { AppLayoutComponent } from './app-layout.component';

describe('AppLayoutComponent', () => {
  let fixture: ComponentFixture<AppLayoutComponent>;
  let component: AppLayoutComponent;
  const router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['logout']);
  const feedback = jasmine.createSpyObj<AuthFeedbackService>('AuthFeedbackService', [
    'runWithLoading',
    'showToast',
  ]);

  beforeEach(async () => {
    router.navigateByUrl.calls.reset();
    authService.logout.calls.reset();
    feedback.runWithLoading.calls.reset();
    feedback.showToast.calls.reset();
    feedback.runWithLoading.and.callFake((_message: string, action: () => unknown) =>
      firstValueFrom(action() as never)
    );
    feedback.showToast.and.resolveTo();
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [AppLayoutComponent],
      providers: [
        { provide: Router, useValue: router },
        { provide: AuthService, useValue: authService },
        { provide: AuthFeedbackService, useValue: feedback },
      ],
    })
      .overrideComponent(AppLayoutComponent, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(AppLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('logs out and redirects to login', async () => {
    authService.logout.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );

    await component.logout();

    expect(authService.logout).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/login', { replaceUrl: true });
  });

  it('redirects even when server logout fails', async () => {
    authService.logout.and.returnValue(throwError(() => new Error('network')));

    await component.logout();

    expect(feedback.showToast).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/auth/login', { replaceUrl: true });
  });
});
