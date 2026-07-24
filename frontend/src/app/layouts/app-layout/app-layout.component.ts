import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';
import {
  IonContent,
  IonHeader,
  IonLabel,
  IonTabBar,
  IonTabButton,
  IonTitle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { AuthService } from '../../core/services/auth.service';
import { AuthFeedbackService } from '../../core/services/auth-feedback.service';

interface NavigationItem {
  label: string;
  route: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    IonContent,
    IonHeader,
    IonLabel,
    IonTabBar,
    IonTabButton,
    IonTitle,
    IonToolbar,
  ],
  templateUrl: './app-layout.component.html',
  styleUrl: './app-layout.component.scss',
})
export class AppLayoutComponent {
  private readonly authService = inject(AuthService);
  private readonly feedback = inject(AuthFeedbackService);
  private readonly router = inject(Router);

  readonly isLoggingOut = signal(false);

  readonly navigation: NavigationItem[] = [
    { label: 'Dashboard', route: '/app/dashboard' },
    { label: 'Transactions', route: '/app/transactions' },
    { label: 'Catégories', route: '/app/categories', disabled: true },
    { label: 'Rapports', route: '/app/reports', disabled: true },
    { label: 'Paramètres', route: '/app/settings', disabled: true },
  ];

  async logout(): Promise<void> {
    if (this.isLoggingOut()) {
      return;
    }

    this.isLoggingOut.set(true);
    try {
      await this.feedback.runWithLoading('Déconnexion...', () => this.authService.logout());
    } catch {
      await this.feedback.showToast('Session locale fermée.', 'warning');
    } finally {
      this.isLoggingOut.set(false);
      await this.router.navigateByUrl('/auth/login', { replaceUrl: true });
    }
  }
}
