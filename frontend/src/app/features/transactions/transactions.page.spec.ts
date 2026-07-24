import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { AlertController, LoadingController, ToastController } from '@ionic/angular/standalone';
import { AuthService } from '../../core/services/auth.service';
import { Category } from '../../core/models/category.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';
import { Transaction, TransactionPage } from '../../core/models/transaction.model';
import { TransactionService } from '../../core/services/transaction.service';
import { TransactionsPage } from './transactions.page';

describe('TransactionsPage', () => {
  let fixture: ComponentFixture<TransactionsPage>;
  let component: TransactionsPage;

  const categories: Category[] = [
    { id: 1, name: 'Salaire', icon: 'cash', color: '#1D9E75', type: 'INCOME', isDefault: true },
    { id: 2, name: 'Courses', icon: 'cart', color: '#D49E10', type: 'EXPENSE', isDefault: true },
  ];
  const transaction: Transaction = {
    id: 10,
    amount: 12500,
    type: 'EXPENSE',
    note: 'Marché',
    date: '2026-07-20',
    category: categories[1],
  };
  const page: TransactionPage = {
    content: [transaction],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1,
  };
  const currentUser = signal({
    id: '1',
    name: 'Alice',
    email: 'alice@example.test',
    currency: { code: 'XOF', name: 'Franc CFA', symbol: 'FCFA', locale: 'fr-BJ' },
  });
  const authService = jasmine.createSpyObj<AuthService>('AuthService', [], { currentUser });
  const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['findAll']);
  const transactionService = jasmine.createSpyObj<TransactionService>('TransactionService', [
    'findAll',
    'delete',
  ]);
  const tokenService = jasmine.createSpyObj<TokenService>('TokenService', ['isAuthenticated']);
  const alert = jasmine.createSpyObj<HTMLIonAlertElement>('HTMLIonAlertElement', ['present']);
  const alertController = jasmine.createSpyObj<AlertController>('AlertController', ['create']);
  const loading = jasmine.createSpyObj<HTMLIonLoadingElement>('HTMLIonLoadingElement', ['present', 'dismiss']);
  const loadingController = jasmine.createSpyObj<LoadingController>('LoadingController', ['create']);
  const toast = jasmine.createSpyObj<HTMLIonToastElement>('HTMLIonToastElement', ['present']);
  const toastController = jasmine.createSpyObj<ToastController>('ToastController', ['create']);

  beforeEach(async () => {
    categoryService.findAll.calls.reset();
    transactionService.findAll.calls.reset();
    transactionService.delete.calls.reset();
    tokenService.isAuthenticated.calls.reset();
    alertController.create.calls.reset();
    loadingController.create.calls.reset();
    toastController.create.calls.reset();
    alert.present.calls.reset();
    loading.present.calls.reset();
    loading.dismiss.calls.reset();
    toast.present.calls.reset();

    tokenService.isAuthenticated.and.returnValue(true);
    categoryService.findAll.and.returnValue(
      of({ success: true, data: categories, timestamp: '2026-01-01T00:00:00Z' })
    );
    transactionService.findAll.and.returnValue(
      of({ success: true, data: page, timestamp: '2026-01-01T00:00:00Z' })
    );
    transactionService.delete.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );
    alertController.create.and.resolveTo(alert);
    loadingController.create.and.resolveTo(loading);
    loading.present.and.resolveTo();
    loading.dismiss.and.resolveTo(true);
    toastController.create.and.resolveTo(toast);
    toast.present.and.resolveTo();

    await TestBed.configureTestingModule({
      imports: [TransactionsPage],
      providers: [
        ErrorService,
        { provide: AuthService, useValue: authService },
        { provide: CategoryService, useValue: categoryService },
        { provide: TransactionService, useValue: transactionService },
        { provide: TokenService, useValue: tokenService },
        { provide: AlertController, useValue: alertController },
        { provide: LoadingController, useValue: loadingController },
        { provide: ToastController, useValue: toastController },
      ],
    })
      .overrideComponent(TransactionsPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(TransactionsPage);
    component = fixture.componentInstance;
  });

  it('loads categories and first transaction page on init', () => {
    fixture.detectChanges();

    expect(categoryService.findAll).toHaveBeenCalledTimes(1);
    expect(transactionService.findAll).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(component.transactions()).toEqual([transaction]);
    expect(component.isLoading()).toBeFalse();
  });

  it('does not request data when session is invalid', () => {
    tokenService.isAuthenticated.and.returnValue(false);

    fixture.detectChanges();

    expect(categoryService.findAll).not.toHaveBeenCalled();
    expect(transactionService.findAll).not.toHaveBeenCalled();
  });

  it('applies supported filters and returns to first page', () => {
    fixture.detectChanges();
    transactionService.findAll.calls.reset();
    component.filterForm.setValue({
      type: 'EXPENSE',
      categoryId: '2',
      startDate: '2026-07-01',
      endDate: '2026-07-31',
    });

    component.applyFilters();

    expect(transactionService.findAll).toHaveBeenCalledWith({
      page: 0,
      size: 20,
      type: 'EXPENSE',
      categoryId: 2,
      startDate: '2026-07-01',
      endDate: '2026-07-31',
    });
  });

  it('does not request when date range is invalid', () => {
    fixture.detectChanges();
    transactionService.findAll.calls.reset();
    component.filterForm.patchValue({ startDate: '2026-07-31', endDate: '2026-07-01' });

    component.applyFilters();

    expect(transactionService.findAll).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('date de début');
  });

  it('resets filters without sending empty strings', () => {
    fixture.detectChanges();
    transactionService.findAll.calls.reset();
    component.filterForm.setValue({
      type: 'EXPENSE',
      categoryId: '2',
      startDate: '2026-07-01',
      endDate: '2026-07-31',
    });

    component.resetFilters();

    expect(transactionService.findAll).toHaveBeenCalledWith({ page: 0, size: 20 });
  });

  it('loads next page from backend pagination', () => {
    transactionService.findAll.and.returnValues(
      of({
        success: true,
        data: { ...page, totalPages: 2 },
        timestamp: '2026-01-01T00:00:00Z',
      }),
      of({
        success: true,
        data: { ...page, content: [{ ...transaction, id: 11 }], page: 1, totalPages: 2 },
        timestamp: '2026-01-01T00:00:00Z',
      })
    );
    fixture.detectChanges();

    component.loadMore();

    expect(transactionService.findAll).toHaveBeenCalledWith({ page: 1, size: 20 });
    expect(component.transactions().map((item) => item.id)).toEqual([10, 11]);
  });

  it('completes pull to refresh', () => {
    fixture.detectChanges();
    transactionService.findAll.calls.reset();
    const complete = jasmine.createSpy('complete');

    component.refresh({ target: { complete } } as unknown as CustomEvent);

    expect(transactionService.findAll).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(complete).toHaveBeenCalled();
  });

  it('shows API errors', () => {
    transactionService.findAll.and.returnValue(throwError(() => new Error('network')));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Une erreur inattendue est survenue.');
    expect(toastController.create).toHaveBeenCalled();
  });

  it('opens delete confirmation and handles cancel button', async () => {
    fixture.detectChanges();

    await component.confirmDelete(transaction);

    expect(alertController.create).toHaveBeenCalled();
    const options = alertController.create.calls.mostRecent().args[0]!;
    const buttons = options.buttons as { role?: string }[];
    expect(buttons.some((button) => button.role === 'cancel')).toBeTrue();
    expect(transactionService.delete).not.toHaveBeenCalled();
  });

  it('deletes a transaction and refreshes list', async () => {
    fixture.detectChanges();
    transactionService.findAll.calls.reset();

    await (component as unknown as { deleteTransaction: (id: number) => Promise<void> }).deleteTransaction(10);
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(transactionService.delete).toHaveBeenCalledWith(10);
    expect(transactionService.findAll).toHaveBeenCalledWith({ page: 0, size: 20 });
    expect(toastController.create).toHaveBeenCalled();
  });
});
