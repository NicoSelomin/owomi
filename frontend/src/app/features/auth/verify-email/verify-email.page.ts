import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

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

  readonly state = signal<VerifyState>('loading');
  readonly errorMessage = signal<string>('');

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.state.set('error');
      this.errorMessage.set('Lien de vérification invalide ou incomplet.');
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: () => this.state.set('success'),
      error: (err: HttpErrorResponse) => {
        const code = err.error?.error?.code;
        this.state.set('error');
        if (code === 'VERIFICATION_TOKEN_EXPIRED') {
          this.errorMessage.set(
            'Ce lien de vérification a expiré. Veuillez vous reconnecter pour en recevoir un nouveau.'
          );
        } else if (code === 'VERIFICATION_TOKEN_INVALID') {
          this.errorMessage.set(
            'Ce lien de vérification est invalide ou a déjà été utilisé.'
          );
        } else {
          this.errorMessage.set('Une erreur est survenue. Veuillez réessayer plus tard.');
        }
      },
    });
  }
}
