import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AlertController, LoadingController, ToastController } from '@ionic/angular/standalone';
import { of, throwError } from 'rxjs';
import { Category } from '../../core/models/category.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';
import { CategoriesPage } from './categories.page';

describe('CategoriesPage', () => {
  let fixture: ComponentFixture<CategoriesPage>;
  let component: CategoriesPage;

  const defaultCategory: Category = {
    id: 1,
    name: 'Alimentation',
    icon: 'cart',
    color: '#D49E10',
    type: 'EXPENSE',
    isDefault: true,
  };
  const personalCategory: Category = {
    id: 2,
    name: 'Freelance',
    icon: 'briefcase',
    color: '#1D9E75',
    type: 'INCOME',
    isDefault: false,
  };
  const categoryService = jasmine.createSpyObj<CategoryService>('CategoryService', [
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
    categoryService.delete.calls.reset();
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
      of({ success: true, data: [defaultCategory, personalCategory], timestamp: '2026-01-01T00:00:00Z' })
    );
    categoryService.delete.and.returnValue(
      of({ success: true, data: undefined, timestamp: '2026-01-01T00:00:00Z' })
    );
    alertController.create.and.resolveTo(alert);
    loadingController.create.and.resolveTo(loading);
    loading.present.and.resolveTo();
    loading.dismiss.and.resolveTo(true);
    toastController.create.and.resolveTo(toast);
    toast.present.and.resolveTo();

    await TestBed.configureTestingModule({
      imports: [CategoriesPage],
      providers: [
        ErrorService,
        { provide: CategoryService, useValue: categoryService },
        { provide: TokenService, useValue: tokenService },
        { provide: AlertController, useValue: alertController },
        { provide: LoadingController, useValue: loadingController },
        { provide: ToastController, useValue: toastController },
      ],
    })
      .overrideComponent(CategoriesPage, { set: { template: '' } })
      .compileComponents();

    fixture = TestBed.createComponent(CategoriesPage);
    component = fixture.componentInstance;
  });

  it('loads categories on init', () => {
    fixture.detectChanges();

    expect(categoryService.findAll).toHaveBeenCalledWith(undefined);
    expect(component.categories()).toEqual([defaultCategory, personalCategory]);
    expect(component.isLoading()).toBeFalse();
  });

  it('does not request categories when session is invalid', () => {
    tokenService.isAuthenticated.and.returnValue(false);

    fixture.detectChanges();

    expect(categoryService.findAll).not.toHaveBeenCalled();
    expect(component.isLoading()).toBeFalse();
  });

  it('applies backend type filter', () => {
    fixture.detectChanges();
    categoryService.findAll.calls.reset();
    component.filterForm.controls.type.setValue('EXPENSE');

    component.applyFilters();

    expect(categoryService.findAll).toHaveBeenCalledWith('EXPENSE');
  });

  it('resets filters without sending empty strings', () => {
    fixture.detectChanges();
    categoryService.findAll.calls.reset();
    component.filterForm.controls.type.setValue('INCOME');

    component.resetFilters();

    expect(categoryService.findAll).toHaveBeenCalledWith(undefined);
  });

  it('supports empty state', () => {
    categoryService.findAll.and.returnValue(
      of({ success: true, data: [], timestamp: '2026-01-01T00:00:00Z' })
    );

    fixture.detectChanges();

    expect(component.hasCategories()).toBeFalse();
    expect(component.errorMessage()).toBeNull();
  });

  it('shows loading API errors safely', () => {
    categoryService.findAll.and.returnValue(throwError(() => new Error('network')));

    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Une erreur inattendue est survenue.');
  });

  it('completes pull to refresh', () => {
    fixture.detectChanges();
    categoryService.findAll.calls.reset();
    const complete = jasmine.createSpy('complete');

    component.refresh({ target: { complete } } as unknown as CustomEvent);

    expect(categoryService.findAll).toHaveBeenCalledWith(undefined);
    expect(complete).toHaveBeenCalled();
  });

  it('hides management actions for default categories', () => {
    expect(component.canManage(defaultCategory)).toBeFalse();
    expect(component.canManage(personalCategory)).toBeTrue();
    expect(component.ownershipLabel(defaultCategory)).toBe('Par défaut');
    expect(component.ownershipLabel(personalCategory)).toBe('Personnelle');
  });

  it('does not call delete for a default category', async () => {
    await component.confirmDelete(defaultCategory);

    expect(alertController.create).not.toHaveBeenCalled();
    expect(categoryService.delete).not.toHaveBeenCalled();
    expect(toastController.create).toHaveBeenCalledWith(jasmine.objectContaining({ color: 'warning' }));
  });

  it('opens delete confirmation and handles cancel button for personal category', async () => {
    await component.confirmDelete(personalCategory);

    expect(alertController.create).toHaveBeenCalled();
    const options = alertController.create.calls.mostRecent().args[0]!;
    const buttons = options.buttons as { role?: string }[];
    expect(buttons.some((button) => button.role === 'cancel')).toBeTrue();
    expect(categoryService.delete).not.toHaveBeenCalled();
  });

  it('deletes a personal category and refreshes the list', async () => {
    fixture.detectChanges();
    categoryService.findAll.calls.reset();

    await (component as unknown as { deleteCategory: (id: number) => Promise<void> }).deleteCategory(2);
    await fixture.whenStable();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(categoryService.delete).toHaveBeenCalledWith(2);
    expect(categoryService.findAll).toHaveBeenCalledWith(undefined);
    expect(toastController.create).toHaveBeenCalledWith(jasmine.objectContaining({ color: 'success' }));
  });

  it('shows category business errors without technical details', async () => {
    categoryService.delete.and.returnValue(
      throwError(() => new HttpErrorResponse({
        error: {
          success: false,
          error: { code: 'CATEGORY_HAS_TRANSACTIONS', message: 'Erreur interne', details: [] },
        },
        status: 400,
      }))
    );

    await (component as unknown as { deleteCategory: (id: number) => Promise<void> }).deleteCategory(2);
    await fixture.whenStable();

    expect(toastController.create).toHaveBeenCalledWith(jasmine.objectContaining({
      message: 'Cette catégorie contient des transactions et ne peut pas être supprimée.',
      color: 'danger',
    }));
  });

  it('maps not found errors to a generic category message', async () => {
    categoryService.delete.and.returnValue(
      throwError(() => new HttpErrorResponse({
        error: {
          success: false,
          error: { code: 'RESOURCE_NOT_FOUND', message: 'Introuvable.', details: [] },
        },
        status: 404,
      }))
    );

    await (component as unknown as { deleteCategory: (id: number) => Promise<void> }).deleteCategory(2);
    await fixture.whenStable();

    expect(toastController.create).toHaveBeenCalledWith(jasmine.objectContaining({
      message: 'Catégorie introuvable.',
      color: 'danger',
    }));
  });
});
