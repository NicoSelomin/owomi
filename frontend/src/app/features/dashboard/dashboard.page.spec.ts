import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { TransactionService } from '../../core/services/transaction.service';
import { DashboardPage } from './dashboard.page';
import { ToastController } from '@ionic/angular/standalone';

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let component: DashboardPage;

  const user = {
    id: '1',
    name: 'Alice Dupont',
    email: 'alice@example.test',
    currency: { code: 'XOF', name: 'Franc CFA', symbol: 'FCFA', locale: 'fr-BJ' },
  };
  const currentUser = signal(user);
  const authService = jasmine.createSpyObj<AuthService>('AuthService', ['loadCurrentUser']);
  const dashboardService = jasmine.createSpyObj<DashboardService>('DashboardService', [
    'getSummary',
    'getMonthlyBalances',
    'getCategoryExpenses',
  ]);
  const tokenService = jasmine.createSpyObj<TokenService>('TokenService', ['isAuthenticated']);
  const transactionService = jasmine.createSpyObj<TransactionService>('TransactionService', ['findAll']);
  const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
  const toast = jasmine.createSpyObj<HTMLIonToastElement>('HTMLIonToastElement', ['present']);
  const toastController = jasmine.createSpyObj<ToastController>('ToastController', ['create']);

  beforeEach(async () => {
    currentUser.set(user);
    authService.loadCurrentUser.calls.reset();
    dashboardService.getSummary.calls.reset();
    dashboardService.getMonthlyBalances.calls.reset();
    dashboardService.getCategoryExpenses.calls.reset();
    tokenService.isAuthenticated.calls.reset();
    transactionService.findAll.calls.reset();
    router.navigate.calls.reset();
    toast.present.calls.reset();
    toastController.create.calls.reset();

    tokenService.isAuthenticated.and.returnValue(true);
    authService.loadCurrentUser.and.returnValue(
      of({ success: true, data: user, timestamp: '2026-01-01T00:00:00Z' })
    );
    dashboardService.getSummary.and.returnValue(
      of({
        success: true,
        data: {
          incomeTotal: 250000,
          expenseTotal: 125000,
          balance: 125000,
          transactionCount: 2,
          startDate: '2026-07-01',
          endDate: '2026-07-31',
        },
        timestamp: '2026-01-01T00:00:00Z',
      })
    );
    dashboardService.getMonthlyBalances.and.returnValue(
      of({
        success: true,
        data: [{ year: 2026, month: 7, incomeTotal: 250000, expenseTotal: 125000, balance: 125000 }],
        timestamp: '2026-01-01T00:00:00Z',
      })
    );
    dashboardService.getCategoryExpenses.and.returnValue(
      of({
        success: true,
        data: [{ categoryId: 1, categoryName: 'Courses', categoryColor: '#D49E10', totalAmount: 125000, transactionCount: 1 }],
        timestamp: '2026-01-01T00:00:00Z',
      })
    );
    transactionService.findAll.and.returnValue(
      of({
        success: true,
        data: {
          content: [
            {
              id: 1,
              amount: 125000,
              type: 'EXPENSE',
              note: 'Marché',
              date: '2026-07-10',
              category: {
                id: 1,
                name: 'Courses',
                type: 'EXPENSE',
                color: '#D49E10',
                icon: 'cart',
                isDefault: true,
              },
            },
          ],
          page: 0,
          size: 5,
          totalElements: 1,
          totalPages: 1,
        },
        timestamp: '2026-01-01T00:00:00Z',
      })
    );
    toastController.create.and.resolveTo(toast);

    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        { provide: AuthService, useValue: { ...authService, currentUser } },
        { provide: DashboardService, useValue: dashboardService },
        { provide: TokenService, useValue: tokenService },
        { provide: TransactionService, useValue: transactionService },
        { provide: Router, useValue: router },
        { provide: ToastController, useValue: toastController },
      ],
    })
      .overrideComponent(DashboardPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(DashboardPage);
    component = fixture.componentInstance;
  });

  it('loads dashboard data once on init', () => {
    fixture.detectChanges();

    expect(dashboardService.getSummary).toHaveBeenCalledTimes(1);
    expect(dashboardService.getMonthlyBalances).toHaveBeenCalledTimes(1);
    expect(dashboardService.getCategoryExpenses).toHaveBeenCalledTimes(1);
    expect(transactionService.findAll).toHaveBeenCalledOnceWith({ page: 0, size: 5 });
    expect(component.isLoading()).toBeFalse();
    expect(component.summary()?.balance).toBe(125000);
    expect(component.recentTransactions().length).toBe(1);
  });

  it('does not call dashboard endpoints when unauthenticated', () => {
    tokenService.isAuthenticated.and.returnValue(false);

    fixture.detectChanges();

    expect(dashboardService.getSummary).not.toHaveBeenCalled();
    expect(transactionService.findAll).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('shows an error and toast when loading fails', async () => {
    dashboardService.getSummary.and.returnValue(throwError(() => new Error('network')));

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.isLoading()).toBeFalse();
    expect(component.errorMessage()).toBe('Impossible de charger le tableau de bord.');
    expect(toastController.create).toHaveBeenCalled();
    expect(toast.present).toHaveBeenCalled();
  });

  it('refreshes data and completes refresher', () => {
    fixture.detectChanges();
    dashboardService.getSummary.calls.reset();
    const complete = jasmine.createSpy('complete');

    component.loadDashboard({ target: { complete } } as unknown as CustomEvent);

    expect(dashboardService.getSummary).toHaveBeenCalledTimes(1);
    expect(complete).toHaveBeenCalled();
  });
});
