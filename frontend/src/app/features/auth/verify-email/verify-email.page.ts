import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';

type VerifyState = 'loading' | 'success' | 'error';

/**
 * Page de confirmation d'adresse email.
 * Lit le token depuis l'URL (?token=xxx), appelle le backend et affiche le résultat.
 */
@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './verify-email.page.html',
  styleUrl: './verify-email.page.scss',
})
export class VerifyEmailPage implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private feedback = inject(AuthFeedbackService);

  readonly state = signal<VerifyState>('loading');
  readonly errorMessage = signal<string>('');

  async ngOnInit(): Promise<void> {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.state.set('error');
      this.errorMessage.set('Lien de vérification invalide ou incomplet.');
      return;
    }

    try {
      await this.feedback.runWithLoading('Vérification en cours...', () =>
        this.authService.verifyEmail(token)
      );
      this.state.set('success');
      await this.feedback.showToast('Adresse email vérifiée.', 'success');
    } catch (error) {
      const err = error as UiError;
      this.state.set('error');
      if (err.code === 'VERIFICATION_TOKEN_EXPIRED') {
        this.errorMessage.set(
          'Ce lien de vérification a expiré. Veuillez vous reconnecter pour en recevoir un nouveau.'
        );
      } else if (err.code === 'VERIFICATION_TOKEN_INVALID') {
        this.errorMessage.set(
          'Ce lien de vérification est invalide ou a déjà été utilisé.'
        );
      } else {
        this.errorMessage.set(err.message);
      }
    }
  }
}
