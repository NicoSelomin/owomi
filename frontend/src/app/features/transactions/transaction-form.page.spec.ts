import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoadingController, ToastController } from '@ionic/angular/standalone';
import { of, throwError } from 'rxjs';
import { Category } from '../../core/models/category.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';
import { Transaction } from '../../core/models/transaction.model';
import { TransactionService } from '../../core/services/transaction.service';
import { TransactionFormPage } from './transaction-form.page';

describe('TransactionFormPage', () => {
  let fixture: ComponentFixture<TransactionFormPage>;
  let component: TransactionFormPage;

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
  const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', ['findAll']);
  const tokenService = jasmine.createSpyObj<TokenService>('TokenService', ['isAuthenticated']);
  const transactionService = jasmine.createSpyObj<TransactionService>('TransactionService', [
    'findById',
    'create',
    'update',
  ]);
  const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
  const loading = jasmine.createSpyObj<HTMLIonLoadingElement>('HTMLIonLoadingElement', ['present', 'dismiss']);
  const loadingController = jasmine.createSpyObj<LoadingController>('LoadingController', ['create']);
  const toast = jasmine.createSpyObj<HTMLIonToastElement>('HTMLIonToastElement', ['present']);
  const toastController = jasmine.createSpyObj<ToastController>('ToastController', ['create']);

  async function createComponent(id: string | null = null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [TransactionFormPage],
      providers: [
        ErrorService,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => id } } },
        },
        { provide: CategoryService, useValue: categoryService },
        { provide: TransactionService, useValue: transactionService },
        { provide: TokenService, useValue: tokenService },
        { provide: Router, useValue: router },
        { provide: LoadingController, useValue: loadingController },
        { provide: ToastController, useValue: toastController },
      ],
    })
      .overrideComponent(TransactionFormPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(TransactionFormPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
    categoryService.findAll.calls.reset();
    tokenService.isAuthenticated.calls.reset();
    transactionService.findById.calls.reset();
    transactionService.create.calls.reset();
    transactionService.update.calls.reset();
    router.navigate.calls.reset();
    loadingController.create.calls.reset();
    toastController.create.calls.reset();
    loading.present.calls.reset();
    loading.dismiss.calls.reset();
    toast.present.calls.reset();

    tokenService.isAuthenticated.and.returnValue(true);
    categoryService.findAll.and.returnValue(
      of({ success: true, data: categories, timestamp: '2026-01-01T00:00:00Z' })
    );
    transactionService.findById.and.returnValue(
      of({ success: true, data: transaction, timestamp: '2026-01-01T00:00:00Z' })
    );
    transactionService.create.and.returnValue(
      of({ success: true, data: transaction, timestamp: '2026-01-01T00:00:00Z' })
    );
    transactionService.update.and.returnValue(
      of({ success: true, data: transaction, timestamp: '2026-01-01T00:00:00Z' })
    );
    router.navigate.and.resolveTo(true);
    loadingController.create.and.resolveTo(loading);
    loading.present.and.resolveTo();
    loading.dismiss.and.resolveTo(true);
    toastController.create.and.resolveTo(toast);
    toast.present.and.resolveTo();
  });

  it('does not load form data when session is invalid', async () => {
    tokenService.isAuthenticated.and.returnValue(false);

    await createComponent();

    expect(categoryService.findAll).not.toHaveBeenCalled();
    expect(transactionService.findById).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('creates a valid transaction', async () => {
    await createComponent();
    component.form.setValue({
      type: 'EXPENSE',
      amount: '12500.50',
      categoryId: '2',
      date: '2026-07-20',
      note: ' Marché ',
    });

    await component.submit();
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(transactionService.create).toHaveBeenCalledWith({
      type: 'EXPENSE',
      amount: 12500.5,
      categoryId: 2,
      date: '2026-07-20',
      note: 'Marché',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/app/transactions']);
  });

  it('blocks double submission', async () => {
    await createComponent();
    component.isSubmitting.set(true);

    await component.submit();

    expect(transactionService.create).not.toHaveBeenCalled();
  });

  it('validates amount, category, type and date', async () => {
    await createComponent();
    component.form.setValue({
      type: 'EXPENSE',
      amount: '0',
      categoryId: '',
      date: '2999-01-01',
      note: '',
    });

    await component.submit();

    expect(transactionService.create).not.toHaveBeenCalled();
    expect(component.form.invalid || component.errorMessage() !== null).toBeTrue();
  });

  it('preloads edit form from backend transaction', async () => {
    await createComponent('10');

    expect(transactionService.findById).toHaveBeenCalledWith(10);
    expect(component.form.getRawValue()).toEqual({
      type: 'EXPENSE',
      amount: '12500',
      categoryId: '2',
      date: '2026-07-20',
      note: 'Marché',
    });
  });

  it('updates an existing transaction', async () => {
    await createComponent('10');
    component.form.patchValue({ amount: '15000', note: ' Mise à jour ' });

    await component.submit();

    expect(transactionService.update).toHaveBeenCalledWith(10, {
      type: 'EXPENSE',
      amount: 15000,
      categoryId: 2,
      date: '2026-07-20',
      note: 'Mise à jour',
    });
  });

  it('shows deletion-safe resource errors on save failure', async () => {
    transactionService.create.and.returnValue(
      throwError(() => new HttpErrorResponse({
        error: {
          success: false,
          error: { code: 'RESOURCE_NOT_FOUND', message: 'Introuvable.', details: [] },
        },
        status: 404,
      }))
    );
    await createComponent();
    component.form.setValue({
      type: 'EXPENSE',
      amount: '12500',
      categoryId: '999',
      date: '2026-07-20',
      note: '',
    });

    await component.submit();
    await fixture.whenStable();

    expect(component.errorMessage()).toBe('Transaction introuvable ou catégorie non autorisée.');
  });
});
