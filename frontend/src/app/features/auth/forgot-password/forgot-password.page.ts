import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

/**
 * Page « Mot de passe oublié » : saisie de l'email et envoi du lien de réinitialisation.
 * Le backend répond toujours 200 (anti-énumération) ; l'écran de confirmation est donc
 * volontairement générique.
 */
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.page.html',
  styleUrl: './forgot-password.page.scss',
})
export class ForgotPasswordPage {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  readonly isLoading = signal(false);
  readonly submitted = signal(false);
  readonly submitError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  /** État du champ email pour l'affichage (valide / invalide / neutre). */
  emailState(): 'ok' | 'err' | '' {
    const c = this.form.controls.email;
    if (!c.touched && !c.dirty) return '';
    if (c.value.trim() === '') return '';
    return c.valid ? 'ok' : 'err';
  }

  /** Email saisi, pour rappel dans l'écran de confirmation. */
  get submittedEmail(): string {
    return this.form.controls.email.value.trim();
  }

  onSubmit(): void {
    this.submitError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.authService.forgotPassword(this.submittedEmail).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.submitted.set(true);
      },
      error: () => {
        this.isLoading.set(false);
        this.submitError.set('Une erreur réseau est survenue. Veuillez réessayer.');
      },
    });
  }
}
