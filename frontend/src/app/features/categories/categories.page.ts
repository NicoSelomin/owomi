import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import {
  AlertController,
  IonItem,
  IonLabel,
  IonList,
  IonRefresher,
  IonRefresherContent,
  IonSkeletonText,
  LoadingController,
  ToastController,
} from '@ionic/angular/standalone';
import { finalize } from 'rxjs';
import { Category } from '../../core/models/category.model';
import { TransactionType } from '../../core/models/transaction.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService, UiError } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    IonItem,
    IonLabel,
    IonList,
    IonRefresher,
    IonRefresherContent,
    IonSkeletonText,
  ],
  templateUrl: './categories.page.html',
  styleUrl: './categories.page.scss',
})
export class CategoriesPage implements OnInit {
  private readonly alertController = inject(AlertController);
  private readonly categoryService = inject(CategoryService);
  private readonly errorService = inject(ErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly loadingController = inject(LoadingController);
  private readonly toastController = inject(ToastController);
  private readonly tokenService = inject(TokenService);

  readonly categories = signal<Category[]>([]);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly hasCategories = computed(() => this.categories().length > 0);

  readonly filterForm = this.fb.nonNullable.group({
    type: ['' as '' | TransactionType],
  });

  ngOnInit(): void {
    if (!this.tokenService.isAuthenticated()) {
      this.isLoading.set(false);
      return;
    }
    this.loadCategories();
  }

  loadCategories(event?: CustomEvent): void {
    if (!this.tokenService.isAuthenticated()) {
      this.isLoading.set(false);
      this.completeRefresh(event);
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);
    const type = this.filterForm.controls.type.value || undefined;

    this.categoryService
      .findAll(type)
      .pipe(
        finalize(() => {
          this.isLoading.set(false);
          this.completeRefresh(event);
        })
      )
      .subscribe({
        next: (response) => this.categories.set(response.data),
        error: (error) => this.handleLoadError(error),
      });
  }

  applyFilters(): void {
    this.loadCategories();
  }

  resetFilters(): void {
    this.filterForm.reset({ type: '' });
    this.loadCategories();
  }

  refresh(event: CustomEvent): void {
    this.loadCategories(event);
  }

  canManage(category: Category): boolean {
    return !category.isDefault;
  }

  typeLabel(type: TransactionType): string {
    return type === 'INCOME' ? 'Revenu' : 'Dépense';
  }

  ownershipLabel(category: Category): string {
    return category.isDefault ? 'Par défaut' : 'Personnelle';
  }

  async confirmDelete(category: Category): Promise<void> {
    if (!this.canManage(category)) {
      await this.showToast('Les catégories par défaut ne peuvent pas être supprimées.', 'warning');
      return;
    }

    const alert = await this.alertController.create({
      header: 'Supprimer la catégorie',
      message: 'Cette action est possible uniquement si aucune transaction ne l’utilise.',
      buttons: [
        { text: 'Annuler', role: 'cancel' },
        {
          text: 'Supprimer',
          role: 'destructive',
          handler: () => {
            void this.deleteCategory(category.id);
          },
        },
      ],
    });
    await alert.present();
  }

  private async deleteCategory(id: number): Promise<void> {
    const loading = await this.loadingController.create({
      message: 'Suppression...',
      spinner: 'crescent',
      backdropDismiss: false,
    });
    await loading.present();

    this.categoryService
      .delete(id)
      .pipe(finalize(() => loading.dismiss().catch(() => undefined)))
      .subscribe({
        next: async () => {
          await this.showToast('Catégorie supprimée.', 'success');
          this.loadCategories();
        },
        error: async (error) => {
          const uiError = this.errorService.toUiError(error);
          await this.showToast(this.safeErrorMessage(uiError), 'danger');
        },
      });
  }

  private handleLoadError(error: unknown): void {
    const uiError = this.errorService.toUiError(error);
    this.errorMessage.set(this.safeErrorMessage(uiError));
  }

  private safeErrorMessage(error: UiError): string {
    if (error.code === 'CATEGORY_IS_DEFAULT') {
      return 'Les catégories par défaut ne peuvent pas être modifiées ou supprimées.';
    }
    if (error.code === 'CATEGORY_HAS_TRANSACTIONS') {
      return 'Cette catégorie contient des transactions et ne peut pas être supprimée.';
    }
    if (error.code === 'RESOURCE_NOT_FOUND') {
      return 'Catégorie introuvable.';
    }
    if (error.code === 'VALIDATION_ERROR') {
      return error.details[0] ?? error.message;
    }
    return error.message;
  }

  private completeRefresh(event?: CustomEvent): void {
    const target = event?.target as { complete?: () => void } | undefined;
    target?.complete?.();
  }

  private async showToast(message: string, color: 'success' | 'danger' | 'warning'): Promise<void> {
    const toast = await this.toastController.create({
      message,
      color,
      duration: 3000,
      position: 'top',
    });
    await toast.present();
  }
}
