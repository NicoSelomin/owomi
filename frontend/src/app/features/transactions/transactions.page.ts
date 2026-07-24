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
import { finalize, forkJoin } from 'rxjs';
import { Category } from '../../core/models/category.model';
import { Transaction, TransactionListFilters, TransactionType } from '../../core/models/transaction.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService, UiError } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';
import { TransactionService } from '../../core/services/transaction.service';
import { AuthService } from '../../core/services/auth.service';

const PAGE_SIZE = 20;

@Component({
  selector: 'app-transactions',
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
  templateUrl: './transactions.page.html',
  styleUrl: './transactions.page.scss',
})
export class TransactionsPage implements OnInit {
  private readonly alertController = inject(AlertController);
  private readonly authService = inject(AuthService);
  private readonly categoryService = inject(CategoryService);
  private readonly errorService = inject(ErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly loadingController = inject(LoadingController);
  private readonly toastController = inject(ToastController);
  private readonly tokenService = inject(TokenService);
  private readonly transactionService = inject(TransactionService);

  readonly user = this.authService.currentUser;
  readonly categories = signal<Category[]>([]);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isLoadingMore = signal(false);
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly transactions = signal<Transaction[]>([]);

  readonly hasTransactions = computed(() => this.transactions().length > 0);
  readonly canLoadMore = computed(() => this.page() + 1 < this.totalPages());
  readonly filteredCategories = computed(() => {
    const type = this.filterForm.controls.type.value;
    return type ? this.categories().filter((category) => category.type === type) : this.categories();
  });

  readonly filterForm = this.fb.nonNullable.group({
    type: ['' as '' | TransactionType],
    categoryId: [''],
    startDate: [''],
    endDate: [''],
  });

  ngOnInit(): void {
    if (!this.tokenService.isAuthenticated()) {
      return;
    }
    this.loadInitial();
  }

  loadInitial(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      categories: this.categoryService.findAll(),
      transactions: this.transactionService.findAll(this.buildFilters(0)),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ categories, transactions }) => {
          this.categories.set(categories.data);
          this.applyPage(transactions.data, true);
        },
        error: (error) => this.handleLoadError(error),
      });
  }

  applyFilters(): void {
    if (!this.validateDateRange()) {
      return;
    }
    this.loadPage(0, true);
  }

  resetFilters(): void {
    this.filterForm.reset({
      type: '',
      categoryId: '',
      startDate: '',
      endDate: '',
    });
    this.loadPage(0, true);
  }

  onTypeChange(): void {
    const selectedCategoryId = Number(this.filterForm.controls.categoryId.value);
    if (!selectedCategoryId) {
      return;
    }
    const selected = this.categories().find((category) => category.id === selectedCategoryId);
    const type = this.filterForm.controls.type.value;
    if (selected && type && selected.type !== type) {
      this.filterForm.controls.categoryId.setValue('');
    }
  }

  refresh(event: CustomEvent): void {
    if (!this.tokenService.isAuthenticated()) {
      this.completeRefresh(event);
      return;
    }
    this.loadPage(0, true, event);
  }

  loadMore(): void {
    if (!this.canLoadMore() || this.isLoadingMore()) {
      return;
    }
    this.loadPage(this.page() + 1, false);
  }

  async confirmDelete(transaction: Transaction): Promise<void> {
    const alert = await this.alertController.create({
      header: 'Supprimer la transaction',
      message: 'Cette action est définitive.',
      buttons: [
        { text: 'Annuler', role: 'cancel' },
        {
          text: 'Supprimer',
          role: 'destructive',
          handler: () => {
            void this.deleteTransaction(transaction.id);
          },
        },
      ],
    });
    await alert.present();
  }

  formatAmount(transaction: Transaction): string {
    const amount = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(transaction.amount);
    const currency = this.user()?.currency;
    return `${currency?.symbol ?? currency?.code ?? ''} ${amount}`.trim();
  }

  typeLabel(type: TransactionType): string {
    return type === 'INCOME' ? 'Revenu' : 'Dépense';
  }

  private loadPage(page: number, replace: boolean, event?: CustomEvent): void {
    if (!this.tokenService.isAuthenticated()) {
      this.completeRefresh(event);
      return;
    }
    if (replace) {
      this.isLoading.set(true);
    } else {
      this.isLoadingMore.set(true);
    }
    this.errorMessage.set(null);
    this.transactionService
      .findAll(this.buildFilters(page))
      .pipe(
        finalize(() => {
          this.isLoading.set(false);
          this.isLoadingMore.set(false);
          this.completeRefresh(event);
        })
      )
      .subscribe({
        next: (response) => this.applyPage(response.data, replace),
        error: (error) => this.handleLoadError(error),
      });
  }

  private buildFilters(page: number): TransactionListFilters {
    const raw = this.filterForm.getRawValue();
    const filters: TransactionListFilters = {
      page,
      size: PAGE_SIZE,
    };
    if (raw.type) {
      filters.type = raw.type;
    }
    if (raw.categoryId) {
      filters.categoryId = Number(raw.categoryId);
    }
    if (raw.startDate) {
      filters.startDate = raw.startDate;
    }
    if (raw.endDate) {
      filters.endDate = raw.endDate;
    }
    return filters;
  }

  private applyPage(data: { content: Transaction[]; page: number; totalElements: number; totalPages: number }, replace: boolean): void {
    this.page.set(data.page);
    this.totalElements.set(data.totalElements);
    this.totalPages.set(data.totalPages);
    this.transactions.set(replace ? data.content : [...this.transactions(), ...data.content]);
  }

  private async deleteTransaction(id: number): Promise<void> {
    const loading = await this.loadingController.create({
      message: 'Suppression...',
      spinner: 'crescent',
      backdropDismiss: false,
    });
    await loading.present();
    this.transactionService
      .delete(id)
      .pipe(finalize(() => loading.dismiss().catch(() => undefined)))
      .subscribe({
        next: async () => {
          await this.showToast('Transaction supprimée.', 'success');
          const targetPage = this.transactions().length === 1 && this.page() > 0 ? this.page() - 1 : this.page();
          this.loadPage(targetPage, true);
        },
        error: async (error) => {
          const uiError = this.errorService.toUiError(error);
          await this.showToast(this.safeErrorMessage(uiError), 'danger');
        },
      });
  }

  private validateDateRange(): boolean {
    const { startDate, endDate } = this.filterForm.getRawValue();
    if (startDate && endDate && startDate > endDate) {
      this.errorMessage.set('La date de début doit être antérieure ou égale à la date de fin.');
      return false;
    }
    this.errorMessage.set(null);
    return true;
  }

  private handleLoadError(error: unknown): void {
    const uiError = this.errorService.toUiError(error);
    this.errorMessage.set(this.safeErrorMessage(uiError));
    void this.showToast(this.safeErrorMessage(uiError), 'danger');
  }

  private safeErrorMessage(error: UiError): string {
    if (error.code === 'RESOURCE_NOT_FOUND') {
      return 'Transaction introuvable.';
    }
    return error.details[0] ?? error.message;
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

  private completeRefresh(event?: CustomEvent): void {
    const target = event?.target as { complete?: () => void } | undefined;
    target?.complete?.();
  }
}
