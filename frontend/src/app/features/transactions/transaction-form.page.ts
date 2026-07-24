import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { LoadingController, ToastController } from '@ionic/angular/standalone';
import { finalize, forkJoin, of } from 'rxjs';
import { Category } from '../../core/models/category.model';
import { Transaction, TransactionRequest, TransactionType } from '../../core/models/transaction.model';
import { CategoryService } from '../../core/services/category.service';
import { ErrorService, UiError } from '../../core/services/error.service';
import { TokenService } from '../../core/services/token.service';
import { TransactionService } from '../../core/services/transaction.service';

const MAX_NOTE_LENGTH = 1000;

@Component({
  selector: 'app-transaction-form',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './transaction-form.page.html',
  styleUrl: './transaction-form.page.scss',
})
export class TransactionFormPage implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly errorService = inject(ErrorService);
  private readonly fb = inject(FormBuilder);
  private readonly loadingController = inject(LoadingController);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly toastController = inject(ToastController);
  private readonly tokenService = inject(TokenService);
  private readonly transactionService = inject(TransactionService);

  private readonly transactionId = signal<number | null>(null);

  readonly categories = signal<Category[]>([]);
  readonly errorMessage = signal<string | null>(null);
  readonly isLoading = signal(true);
  readonly isSubmitting = signal(false);
  readonly isEditMode = computed(() => this.transactionId() !== null);
  readonly pageTitle = computed(() =>
    this.isEditMode() ? 'Modifier la transaction' : 'Nouvelle transaction'
  );
  readonly filteredCategories = computed(() => {
    const type = this.form.controls.type.value;
    return type ? this.categories().filter((category) => category.type === type) : this.categories();
  });

  readonly form = this.fb.nonNullable.group({
    type: ['EXPENSE' as TransactionType, [Validators.required]],
    amount: ['', [Validators.required, Validators.pattern(/^\d{1,13}([.,]\d{1,2})?$/)]],
    categoryId: ['', [Validators.required]],
    date: [this.today(), [Validators.required]],
    note: ['', [Validators.maxLength(MAX_NOTE_LENGTH)]],
  });

  ngOnInit(): void {
    if (!this.tokenService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    const idParam = this.route.snapshot.paramMap.get('id');
    const id = idParam ? Number(idParam) : null;
    this.transactionId.set(Number.isFinite(id) && id !== null ? id : null);
    this.loadFormData();
  }

  onTypeChange(): void {
    const selectedCategoryId = Number(this.form.controls.categoryId.value);
    if (!selectedCategoryId) {
      return;
    }
    const selected = this.categories().find((category) => category.id === selectedCategoryId);
    if (selected && selected.type !== this.form.controls.type.value) {
      this.form.controls.categoryId.setValue('');
    }
  }

  async submit(): Promise<void> {
    if (this.isSubmitting()) {
      return;
    }

    this.errorMessage.set(null);
    this.form.controls.note.setValue(this.form.controls.note.value.trim());

    if (this.form.invalid || !this.validateDate() || !this.validateAmount()) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.toRequest();
    const loading = await this.loadingController.create({
      message: this.isEditMode() ? 'Mise à jour...' : 'Création...',
      spinner: 'crescent',
      backdropDismiss: false,
    });
    this.isSubmitting.set(true);
    await loading.present();

    const save$ = this.transactionId()
      ? this.transactionService.update(this.transactionId() as number, request)
      : this.transactionService.create(request);

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
            this.isEditMode() ? 'Transaction mise à jour.' : 'Transaction créée.',
            'success'
          );
          await this.router.navigate(['/app/transactions']);
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
      return `La description ne peut pas dépasser ${MAX_NOTE_LENGTH} caractères.`;
    }
    if (control.hasError('pattern')) {
      return 'Le montant doit respecter le format 9999999999999.99.';
    }
    return 'Valeur invalide.';
  }

  private loadFormData(): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    const id = this.transactionId();
    forkJoin({
      categories: this.categoryService.findAll(),
      transaction: id ? this.transactionService.findById(id) : of(null),
    })
      .pipe(finalize(() => this.isLoading.set(false)))
      .subscribe({
        next: ({ categories, transaction }) => {
          this.categories.set(categories.data);
          if (transaction) {
            this.patchTransaction(transaction.data);
          }
        },
        error: (error) => {
          const uiError = this.errorService.toUiError(error);
          this.errorMessage.set(this.safeErrorMessage(uiError));
        },
      });
  }

  private patchTransaction(transaction: Transaction): void {
    this.form.setValue({
      type: transaction.type,
      amount: transaction.amount.toString(),
      categoryId: transaction.category.id.toString(),
      date: transaction.date,
      note: transaction.note ?? '',
    });
  }

  private toRequest(): TransactionRequest {
    const raw = this.form.getRawValue();
    return {
      amount: Number(raw.amount.replace(',', '.')),
      type: raw.type,
      categoryId: Number(raw.categoryId),
      date: raw.date,
      note: raw.note.trim() || null,
    };
  }

  private validateAmount(): boolean {
    const amount = Number(this.form.controls.amount.value.replace(',', '.'));
    if (!Number.isFinite(amount) || amount <= 0) {
      this.errorMessage.set('Le montant doit être supérieur à 0.');
      return false;
    }
    return true;
  }

  private validateDate(): boolean {
    if (this.form.controls.date.value > this.today()) {
      this.errorMessage.set('La date ne peut pas être dans le futur.');
      return false;
    }
    return true;
  }

  private today(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private safeErrorMessage(error: UiError): string {
    if (error.code === 'RESOURCE_NOT_FOUND') {
      return 'Transaction introuvable ou catégorie non autorisée.';
    }
    if (error.code === 'AMOUNT_INVALID') {
      return 'Le montant doit être supérieur à 0.';
    }
    if (error.code === 'FUTURE_DATE') {
      return 'La date ne peut pas être dans le futur.';
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
}
