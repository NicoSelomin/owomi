import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LoadingController, ToastController } from '@ionic/angular/standalone';
import { finalize } from 'rxjs';
import { Category, CategoryRequest } from '../../core/models/category.model';
import { TransactionType } from '../../core/models/transaction.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService, UiError } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';

const MAX_NAME_LENGTH = 100;
const MAX_ICON_LENGTH = 50;
const DEFAULT_COLOR = '#D49E10';
const DEFAULT_ICON = 'wallet';

@Component({
  selector: 'app-category-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './category-form.page.html',
  styleUrl: './category-form.page.scss',
})
export class CategoryFormPage implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly errorService = inject(ErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly loadingController = inject(LoadingController);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly tokenService = inject(TokenService);

  private readonly categoryId = signal<number | null>(null);
  readonly loadedCategory = signal<Category | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly isEditMode = computed(() => this.categoryId() !== null);
  readonly isDefaultCategory = computed(() => this.loadedCategory()?.isDefault === true);
  readonly pageTitle = computed(() =>
    this.isEditMode() ? 'Modifier la catégorie' : 'Nouvelle catégorie'
  );

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(MAX_NAME_LENGTH)]],
    type: ['EXPENSE' as TransactionType, [Validators.required]],
    icon: [DEFAULT_ICON, [Validators.required, Validators.maxLength(MAX_ICON_LENGTH)]],
    color: [DEFAULT_COLOR, [Validators.required, Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
  });

  ngOnInit(): void {
    if (!this.tokenService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) {
      this.isLoading.set(false);
      return;
    }

    const id = Number(idParam);
    if (!Number.isInteger(id) || id <= 0) {
      this.errorMessage.set('Catégorie introuvable.');
      this.isLoading.set(false);
      return;
    }

    this.categoryId.set(id);
    this.loadCategory(id);
  }

  async submit(): Promise<void> {
    if (this.isSubmitting()) {
      return;
    }

    this.errorMessage.set(null);
    this.trimTextControls();

    if (this.isDefaultCategory()) {
      this.errorMessage.set('Les catégories par défaut ne peuvent pas être modifiées.');
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const loading = await this.loadingController.create({
      message: this.isEditMode() ? 'Mise à jour...' : 'Création...',
      spinner: 'crescent',
      backdropDismiss: false,
    });
    this.isSubmitting.set(true);
    await loading.present();

    const request = this.toRequest();
    const save$ = this.categoryId()
      ? this.categoryService.update(this.categoryId() as number, request)
      : this.categoryService.create(request);

    save$
      .pipe(
        finalize(() => {
          this.isSubmitting.set(false);
          loading.dismiss().catch(() => undefined);
        })
      )
      .subscribe({
        next: async () => {
          await this.showToast(
            this.isEditMode() ? 'Catégorie mise à jour.' : 'Catégorie créée.',
            'success'
          );
          await this.router.navigate(['/app/categories']);
        },
        error: async (error) => {
          const uiError = this.errorService.toUiError(error);
          this.errorMessage.set(this.safeErrorMessage(uiError));
          await this.showToast(this.safeErrorMessage(uiError), 'danger');
        },
      });
  }

  fieldError(name: keyof typeof this.form.controls): string | null {
    const control = this.form.controls[name];
    if (!control.touched || control.valid) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Ce champ est obligatoire.';
    }
    if (control.hasError('maxlength')) {
      return name === 'name'
        ? `Le nom ne peut pas dépasser ${MAX_NAME_LENGTH} caractères.`
        : `L’icône ne peut pas dépasser ${MAX_ICON_LENGTH} caractères.`;
    }
    if (control.hasError('pattern')) {
      return 'La couleur doit respecter le format #RRGGBB.';
    }
    return 'Valeur invalide.';
  }

  private loadCategory(id: number): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.categoryService
      .findById(id)
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: (response) => this.patchCategory(response.data),
        error: (error) => {
          const uiError = this.errorService.toUiError(error);
          this.errorMessage.set(this.safeErrorMessage(uiError));
        },
      });
  }

  private patchCategory(category: Category): void {
    this.loadedCategory.set(category);
    this.form.setValue({
      name: category.name,
      type: category.type,
      icon: category.icon,
      color: category.color,
    });
    if (category.isDefault) {
      this.form.disable();
    }
  }

  private trimTextControls(): void {
    this.form.controls.name.setValue(this.form.controls.name.value.trim());
    this.form.controls.icon.setValue(this.form.controls.icon.value.trim());
    this.form.controls.color.setValue(this.form.controls.color.value.trim());
  }

  private toRequest(): CategoryRequest {
    const raw = this.form.getRawValue();
    return {
      name: raw.name,
      type: raw.type,
      icon: raw.icon,
      color: raw.color,
    };
  }

  private safeErrorMessage(error: UiError): string {
    if (error.code === 'CATEGORY_IS_DEFAULT') {
      return 'Les catégories par défaut ne peuvent pas être modifiées.';
    }
    if (error.code === 'RESOURCE_NOT_FOUND') {
      return 'Catégorie introuvable.';
    }
    if (error.code === 'VALIDATION_ERROR') {
      return error.details[0] ?? error.message;
    }
    return error.message;
  }

  private async showToast(message: string, color: 'success' | 'danger'): Promise<void> {
    const toast = await this.toastController.create({
      message,
      color,
      duration: 3000,
      position: 'top',
    });
    await toast.present();
  }
}
