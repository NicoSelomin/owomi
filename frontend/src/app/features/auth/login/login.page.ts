import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AuthFeedbackService } from '../../../core/services/auth-feedback.service';
import { UiError } from '../../../core/services/error.service';

const PENDING_EMAIL_KEY = 'owomi_pending_email';

/**
 * Page de connexion — reproduction de docs/mockups/owomi_login.html.
 * Formulaire réactif (email, mot de passe), validation email après saisie,
 * œil afficher/masquer, « se souvenir de moi », gestion d'erreur en français.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.page.html',
  styleUrl: './login.page.scss',
})
export class LoginPage {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private feedback = inject(AuthFeedbackService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  private readonly REMEMBER_KEY = 'owomi_remember_email';

  readonly showPassword = signal(false);
  readonly isLoading = signal(false);
  readonly loginError = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
    rememberMe: [false],
  });

  constructor() {
    // Pré-remplissage de l'email si « se souvenir de moi » avait été coché
    const remembered = localStorage.getItem(this.REMEMBER_KEY);
    if (remembered) {
      this.form.patchValue({ email: remembered, rememberMe: true });
    }
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  /** État du champ email pour l'affichage (valide / invalide / neutre). */
  emailState(): 'ok' | 'err' | '' {
    const c = this.form.controls.email;
    if (!c.touched && !c.dirty) return '';
    if (c.value.trim() === '') return '';
    return c.valid ? 'ok' : 'err';
  }

  async onSubmit(): Promise<void> {
    if (this.isLoading()) {
      return;
    }

    this.loginError.set(null);
    this.form.controls.email.setValue(this.form.controls.email.value.trim());

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, password, rememberMe } = this.form.getRawValue();
    const normalizedEmail = email;
    this.isLoading.set(true);

    try {
      await this.feedback.runWithLoading('Connexion en cours...', () =>
        this.authService.login({ email: normalizedEmail, password })
      );
      if (rememberMe) {
        localStorage.setItem(this.REMEMBER_KEY, normalizedEmail);
      } else {
        localStorage.removeItem(this.REMEMBER_KEY);
      }
      await firstValueFrom(this.authService.loadCurrentUser()).catch(() => undefined);
      const returnUrl =
        this.route.snapshot.queryParamMap.get('returnUrl') ?? '/app/dashboard';
      await this.router.navigateByUrl(returnUrl);
    } catch (error) {
      const err = error as UiError;
      if (err.code === 'EMAIL_NOT_VERIFIED') {
        sessionStorage.setItem(PENDING_EMAIL_KEY, normalizedEmail);
        this.loginError.set(err.message);
        await this.feedback.showToast(
          "Votre email n'est pas vérifié. Vous pouvez demander un nouveau lien.",
          'warning'
        );
      } else {
        this.loginError.set(
          err.code === 'INVALID_CREDENTIALS'
            ? 'Email ou mot de passe incorrect.'
            : err.message
        );
      }
    } finally {
      this.isLoading.set(false);
    }
  }

  async resendVerification(): Promise<void> {
    const email = this.form.controls.email.value.trim();
    if (!email || this.form.controls.email.invalid || this.isLoading()) {
      this.form.controls.email.markAsTouched();
      return;
    }

    this.isLoading.set(true);
    try {
      await this.feedback.runWithLoading('Envoi du lien...', () =>
        this.authService.resendVerification(email)
      );
      sessionStorage.setItem(PENDING_EMAIL_KEY, email);
      await this.feedback.showToast('Un nouveau lien de vérification a été envoyé.', 'success');
      await this.router.navigateByUrl('/auth/email-sent');
    } catch (error) {
      this.loginError.set((error as UiError).message);
    } finally {
      this.isLoading.set(false);
    }
  }
}
