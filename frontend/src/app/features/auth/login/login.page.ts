import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';

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

  onSubmit(): void {
    this.loginError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const { email, password, rememberMe } = this.form.getRawValue();
    this.isLoading.set(true);

    this.authService.login({ email: email.trim(), password }).subscribe({
      next: () => {
        this.isLoading.set(false);
        if (rememberMe) {
          localStorage.setItem(this.REMEMBER_KEY, email.trim());
        } else {
          localStorage.removeItem(this.REMEMBER_KEY);
        }
        const returnUrl =
          this.route.snapshot.queryParamMap.get('returnUrl') ?? '/app/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const code = err.error?.error?.code;
        this.loginError.set(
          code === 'INVALID_CREDENTIALS'
            ? 'Email ou mot de passe incorrect.'
            : 'Une erreur est survenue. Veuillez réessayer.'
        );
      },
    });
  }
}
