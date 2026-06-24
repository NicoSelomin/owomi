import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * Tableau de bord (protégé par AuthGuard).
 * Page volontairement simple pour J2-B : affiche le nom et la devise de
 * l'utilisateur connecté et permet la déconnexion.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss',
})
export class DashboardPage implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly user = this.authService.currentUser;
  readonly isLoggingOut = signal(false);

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
    // Au rechargement de page, currentUser est vide : on réhydrate via /api/users/me.
    if (!this.user()) {
      this.authService.loadCurrentUser().subscribe({
        error: () => {
          // En cas d'échec irrécupérable (l'intercepteur gère déjà le 401/refresh)
          this.router.navigate(['/auth/login']);
        },
      });
    }
  }

  onLogout(): void {
    this.isLoggingOut.set(true);
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/splash']),
      // Même en cas d'erreur réseau, la session locale est nettoyée → on quitte
      error: () => {
        this.authService.clearSession();
        this.router.navigate(['/splash']);
      },
    });
  }
}
