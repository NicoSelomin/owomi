import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  IonItem,
  IonLabel,
  IonList,
  IonRefresher,
  IonRefresherContent,
  IonSkeletonText,
  ToastController,
} from '@ionic/angular/standalone';
import { forkJoin, map } from 'rxjs';
import { CategoryExpense, DashboardSummary, MonthlyBalance } from '../../core/models/dashboard.model';
import { Transaction } from '../../core/models/transaction.model';
import { DashboardService } from '../../core/services/dashboard.service';
import { AuthService } from '../../core/services/auth.service';
import { TokenService } from '../../core/services/token.service';
import { TransactionService } from '../../core/services/transaction.service';

interface DashboardData {
  summary: DashboardSummary;
  monthlyBalances: MonthlyBalance[];
  categoryExpenses: CategoryExpense[];
  recentTransactions: Transaction[];
}

/**
 * Tableau de bord (protégé par AuthGuard).
 * Page volontairement simple pour J2-B : affiche le nom et la devise de
 * l'utilisateur connecté et permet la déconnexion.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    IonItem,
    IonLabel,
    IonList,
    IonRefresher,
    IonRefresherContent,
    IonSkeletonText,
  ],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage implements OnInit {
  private authService = inject(AuthService);
  private dashboardService = inject(DashboardService);
  private tokenService = inject(TokenService);
  private toastController = inject(ToastController);
  private transactionService = inject(TransactionService);
  private router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly isLoading = signal(true);
  readonly errorMessage = signal<string | null>(null);
  readonly data = signal<DashboardData | null>(null);

  readonly summary = computed(() => this.data()?.summary ?? null);
  readonly monthlyBalances = computed(() => this.data()?.monthlyBalances ?? []);
  readonly categoryExpenses = computed(() => this.data()?.categoryExpenses ?? []);
  readonly recentTransactions = computed(() => this.data()?.recentTransactions ?? []);
  readonly hasNoActivity = computed(() => (this.summary()?.transactionCount ?? 0) === 0);

  /** Initiales de l'utilisateur pour l'avatar. */
  readonly initials = computed(() => {
    const name = this.user()?.name?.trim();
    if (!name) {
      return '?';
    }
    return name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('');
  });

  ngOnInit(): void {
    if (!this.tokenService.isAuthenticated()) {
      this.router.navigate(['/auth/login']);
      return;
    }

    // Au rechargement de page, currentUser est vide : on réhydrate via /api/users/me.
    if (!this.user()) {
      this.authService.loadCurrentUser().subscribe({
        error: () => {
          // En cas d'échec irrécupérable (l'intercepteur gère déjà le 401/refresh)
          this.router.navigate(['/auth/login']);
        },
      });
    }

    this.loadDashboard();
  }

  loadDashboard(event?: CustomEvent): void {
    if (!this.tokenService.isAuthenticated()) {
      event?.target && this.completeRefresh(event);
      this.router.navigate(['/auth/login']);
      return;
    }

    this.isLoading.set(!event);
    this.errorMessage.set(null);

    forkJoin({
      summary: this.dashboardService.getSummary().pipe(map((response) => response.data)),
      monthlyBalances: this.dashboardService
        .getMonthlyBalances()
        .pipe(map((response) => response.data)),
      categoryExpenses: this.dashboardService
        .getCategoryExpenses()
        .pipe(map((response) => response.data)),
      recentTransactions: this.transactionService
        .findAll({ page: 0, size: 5 })
        .pipe(map((response) => response.data.content)),
    }).subscribe({
      next: (data) => {
        this.data.set(data);
        this.isLoading.set(false);
        this.completeRefresh(event);
      },
      error: async () => {
        this.isLoading.set(false);
        this.errorMessage.set('Impossible de charger le tableau de bord.');
        this.completeRefresh(event);
        await this.showErrorToast();
      },
    });
  }

  formatAmount(value: number | null | undefined): string {
    const currency = this.user()?.currency;
    const amount = value ?? 0;
    const formatted = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(amount);
    return `${currency?.symbol ?? currency?.code ?? ''} ${formatted}`.trim();
  }

  monthLabel(balance: MonthlyBalance): string {
    const date = new Date(balance.year, balance.month - 1, 1);
    return new Intl.DateTimeFormat('fr-FR', {
      month: 'short',
      year: 'numeric',
    }).format(date);
  }

  categoryPercent(category: CategoryExpense): number {
    const total = this.summary()?.expenseTotal ?? 0;
    if (total <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((category.totalAmount / total) * 100));
  }

  maxMonthlyAbsBalance(): number {
    return Math.max(
      1,
      ...this.monthlyBalances().map((item) => Math.abs(item.balance))
    );
  }

  monthlyBarHeight(balance: MonthlyBalance): number {
    return Math.max(8, Math.round((Math.abs(balance.balance) / this.maxMonthlyAbsBalance()) * 88));
  }

  private completeRefresh(event?: CustomEvent): void {
    const target = event?.target as { complete?: () => void } | undefined;
    target?.complete?.();
  }

  private async showErrorToast(): Promise<void> {
    const toast = await this.toastController.create({
      message: 'Impossible de charger vos statistiques. Réessayez.',
      color: 'danger',
      duration: 3200,
      position: 'top',
    });
    await toast.present();
  }
}
