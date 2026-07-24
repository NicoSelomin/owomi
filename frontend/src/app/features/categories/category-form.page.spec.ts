import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { LoadingController, ToastController } from '@ionic/angular/standalone';
import { of, throwError } from 'rxjs';
import { Category } from '../../core/models/category.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';
import { CategoryFormPage } from './category-form.page';

describe('CategoryFormPage', () => {
  let fixture: ComponentFixture<CategoryFormPage>;
  let component: CategoryFormPage;

  const personalCategory: Category = {
    id: 2,
    name: 'Freelance',
    icon: 'briefcase',
    color: '#1D9E75',
    type: 'INCOME',
    isDefault: false,
  };
  const defaultCategory: Category = {
    id: 1,
    name: 'Alimentation',
    icon: 'cart',
    color: '#D49E10',
    type: 'EXPENSE',
    isDefault: true,
  };
  const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', [
    'findById',
    'create',
    'update',
  ]);
  const tokenService = jasmine.createSpyObj<TokenService>('TokenService', ['isAuthenticated']);
  const router = jasmine.createSpyObj<Router>('Router', ['navigate']);
  const loading = jasmine.createSpyObj<HTMLIonLoadingElement>('HTMLIonLoadingElement', ['present', 'dismiss']);
  const loadingController = jasmine.createSpyObj<LoadingController>('LoadingController', ['create']);
  const toast = jasmine.createSpyObj<HTMLIonToastElement>('HTMLIonToastElement', ['present']);
  const toastController = jasmine.createSpyObj<ToastController>('ToastController', ['create']);

  async function createComponent(id: string | null = null): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [CategoryFormPage],
      providers: [
        ErrorService,
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => id } } },
        },
        { provide: CategoryService, useValue: categoryService },
        { provide: TokenService, useValue: tokenService },
        { provide: Router, useValue: router },
        { provide: LoadingController, useValue: loadingController },
        { provide: ToastController, useValue: toastController },
      ],
    })
      .overrideComponent(CategoryFormPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(CategoryFormPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(() => {
    TestBed.resetTestingModule();
    categoryService.findById.calls.reset();
    categoryService.create.calls.reset();
    categoryService.update.calls.reset();
    tokenService.isAuthenticated.calls.reset();
    router.navigate.calls.reset();
    loadingController.create.calls.reset();
    toastController.create.calls.reset();
    loading.present.calls.reset();
    loading.dismiss.calls.reset();
    toast.present.calls.reset();

    tokenService.isAuthenticated.and.returnValue(true);
    categoryService.findById.and.returnValue(
      of({ success: true, data: personalCategory, timestamp: '2026-01-01T00:00:00Z' })
    );
    categoryService.create.and.returnValue(
      of({ success: true, data: personalCategory, timestamp: '2026-01-01T00:00:00Z' })
    );
    categoryService.update.and.returnValue(
      of({ success: true, data: personalCategory, timestamp: '2026-01-01T00:00:00Z' })
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

    expect(categoryService.findById).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('rejects invalid route ids without calling the backend', async () => {
    await createComponent('bad');

    expect(categoryService.findById).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('Catégorie introuvable.');
  });

  it('creates a category with trimmed values', async () => {
    await createComponent();
    component.form.setValue({
      name: ' Freelance ',
      type: 'INCOME',
      icon: ' briefcase ',
      color: ' #1D9E75 ',
    });

    await component.submit();
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(categoryService.create).toHaveBeenCalledWith({
      name: 'Freelance',
      type: 'INCOME',
      icon: 'briefcase',
      color: '#1D9E75',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/app/categories']);
  });

  it('blocks double submission', async () => {
    await createComponent();
    component.isSubmitting.set(true);

    await component.submit();

    expect(categoryService.create).not.toHaveBeenCalled();
  });

  it('validates required fields, max length and type', async () => {
    await createComponent();
    component.form.setValue({
      name: 'x'.repeat(101),
      type: '' as never,
      icon: '',
      color: 'invalid',
    });

    await component.submit();

    expect(categoryService.create).not.toHaveBeenCalled();
    expect(component.form.invalid).toBeTrue();
  });

  it('preloads edit form from backend category', async () => {
    await createComponent('2');

    expect(categoryService.findById).toHaveBeenCalledWith(2);
    expect(component.form.getRawValue()).toEqual({
      name: 'Freelance',
      type: 'INCOME',
      icon: 'briefcase',
      color: '#1D9E75',
    });
  });

  it('updates a personal category', async () => {
    await createComponent('2');
    component.form.patchValue({ name: ' Consulting ' });

    await component.submit();
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(categoryService.update).toHaveBeenCalledWith(2, {
      name: 'Consulting',
      type: 'INCOME',
      icon: 'briefcase',
      color: '#1D9E75',
    });
    expect(router.navigate).toHaveBeenCalledWith(['/app/categories']);
  });

  it('disables edition of a default category in the UI', async () => {
    categoryService.findById.and.returnValue(
      of({ success: true, data: defaultCategory, timestamp: '2026-01-01T00:00:00Z' })
    );

    await createComponent('1');
    await component.submit();

    expect(component.isDefaultCategory()).toBeTrue();
    expect(categoryService.update).not.toHaveBeenCalled();
    expect(component.errorMessage()).toBe('Les catégories par défaut ne peuvent pas être modifiées.');
  });

  it('shows validation API errors without technical details', async () => {
    categoryService.create.and.returnValue(
      throwError(() => new HttpErrorResponse({
        error: {
          success: false,
          error: {
            code: 'VALIDATION_ERROR',
            message: 'Validation impossible.',
            details: ['Une catégorie avec ce nom existe déjà.'],
          },
        },
        status: 400,
      }))
    );
    await createComponent();
    component.form.setValue({
      name: 'Freelance',
      type: 'INCOME',
      icon: 'briefcase',
      color: '#1D9E75',
    });

    await component.submit();
    await fixture.whenStable();

    expect(component.errorMessage()).toBe('Une catégorie avec ce nom existe déjà.');
    expect(toastController.create).toHaveBeenCalledWith(jasmine.objectContaining({ color: 'danger' }));
  });

  it('shows not found errors on preload', async () => {
    categoryService.findById.and.returnValue(
      throwError(() => new HttpErrorResponse({
        error: {
          success: false,
          error: { code: 'RESOURCE_NOT_FOUND', message: 'Introuvable.', details: [] },
        },
        status: 404,
      }))
    );

    await createComponent('999');

    expect(component.errorMessage()).toBe('Catégorie introuvable.');
  });
});
