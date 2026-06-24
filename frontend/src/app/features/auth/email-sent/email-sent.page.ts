import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

const PENDING_EMAIL_KEY = 'owomi_pending_email';
const RESEND_COOLDOWN_SECONDS = 30;

/**
 * Page « Email envoyé » affichée après une inscription réussie.
 * Lit l'email en attente depuis sessionStorage, propose de renvoyer le lien de vérification
 * avec un minuteur anti-spam de 30 secondes.
 */
@Component({
  selector: 'app-email-sent',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './email-sent.page.html',
  styleUrl: './email-sent.page.scss',
})
export class EmailSentPage implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);

  private intervalId: ReturnType<typeof setInterval> | null = null;

  readonly email = signal('');
  readonly secondsLeft = signal(RESEND_COOLDOWN_SECONDS);
  readonly isResending = signal(false);
  readonly resendSuccess = signal(false);

  /** Le bouton n'est actif qu'une fois le décompte terminé et hors envoi en cours. */
  readonly canResend = computed(() => this.secondsLeft() === 0 && !this.isResending());

  /** Email partiellement masqué : « v***@gmail.com » (1er caractère + domaine). */
  readonly maskedEmail = computed(() => this.maskEmail(this.email()));

  /** Libellé dynamique du bouton de renvoi. */
  readonly resendLabel = computed(() => {
    if (this.isResending()) return 'Envoi en cours…';
    if (this.secondsLeft() > 0) return `Renvoyer (${this.secondsLeft()}s)`;
    return "Renvoyer l'email";
  });

  ngOnInit(): void {
    const pending = sessionStorage.getItem(PENDING_EMAIL_KEY);

    // Sans email en attente (accès direct / refresh sans contexte) → retour à l'inscription
    if (!pending) {
      this.router.navigateByUrl('/auth/register');
      return;
    }

    this.email.set(pending);
    this.startTimer();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  onResend(): void {
    if (!this.canResend()) {
      return;
    }

    this.isResending.set(true);
    this.resendSuccess.set(false);

    this.authService.resendVerification(this.email()).subscribe({
      next: () => {
        this.isResending.set(false);
        this.resendSuccess.set(true);
        this.startTimer();
      },
      error: () => {
        // Le backend répond toujours 200 ; en cas d'erreur réseau on réarme simplement le minuteur.
        this.isResending.set(false);
        this.startTimer();
      },
    });
  }

  /** (Re)démarre le décompte de 30 secondes. */
  private startTimer(): void {
    this.clearTimer();
    this.secondsLeft.set(RESEND_COOLDOWN_SECONDS);
    this.intervalId = setInterval(() => {
      const current = this.secondsLeft();
      if (current <= 1) {
        this.secondsLeft.set(0);
        this.clearTimer();
      } else {
        this.secondsLeft.set(current - 1);
      }
    }, 1000);
  }

  private clearTimer(): void {
    if (this.intervalId !== null) {
      clearInterval(this.intervalId);
      this.intervalId = null;
    }
  }

  /** Garde le premier caractère du nom local et le domaine complet. */
  private maskEmail(email: string): string {
    const at = email.indexOf('@');
    if (at < 1) {
      return email;
    }
    return `${email[0]}***${email.slice(at)}`;
  }
}
