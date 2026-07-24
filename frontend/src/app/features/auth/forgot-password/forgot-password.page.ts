import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';

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
  private feedback = inject(AuthFeedbackService);

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

  async onSubmit(): Promise<void> {
    if (this.isLoading()) {
      return;
    }

    this.submitError.set(null);
    this.form.controls.email.setValue(this.form.controls.email.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    try {
      await this.feedback.runWithLoading('Envoi du lien...', () =>
        this.authService.forgotPassword(this.submittedEmail)
      );
      this.submitted.set(true);
      await this.feedback.showToast('Demande prise en compte.', 'success');
    } catch (error) {
      this.submitError.set((error as UiError).message);
    } finally {
      this.isLoading.set(false);
    }
  }
}
