import { Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import {
  passwordScore,
  passwordStrengthValidator,
} from '../../../shared/validators/password-strength.validator';

/**
 * Page de réinitialisation de mot de passe.
 * Lit le token depuis l'URL (?token=xxx), demande deux fois le nouveau mot de passe,
 * indique la force et soumet à POST /api/auth/reset-password.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.page.html',
  styleUrl: './reset-password.page.scss',
})
export class ResetPasswordPage implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  private token = '';

  readonly showPassword = signal(false);
  readonly isLoading = signal(false);
  readonly resetError = signal<string | null>(null);
  /** 'form' tant que le token est présent, 'success' après reset, 'invalid' si lien absent. */
  readonly state = signal<'form' | 'success' | 'invalid'>('form');

  readonly form = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: [this.passwordsMatchValidator] }
  );

  private readonly pwdValue = signal('');
  readonly pwdScore = computed(() => passwordScore(this.pwdValue()));

  readonly strengthLabel = computed(() => {
    const labels: Record<number, string> = {
      1: 'Mot de passe faible',
      2: 'Mot de passe moyen',
      3: 'Bon mot de passe',
      4: 'Mot de passe fort',
    };
    return this.pwdScore() ? labels[this.pwdScore()] : '';
  });

  readonly strengthColor = computed(() => {
    const colors: Record<number, string> = {
      1: '#E24B4A',
      2: '#D49E10',
      3: '#D49E10',
      4: '#1D9E75',
    };
    return colors[this.pwdScore()] ?? 'rgba(255,255,255,.08)';
  });

  readonly strengthLabelColor = computed(() => {
    const colors: Record<number, string> = {
      1: '#F09595',
      2: '#FAC775',
      3: '#FAC775',
      4: '#5DCAA5',
    };
    return colors[this.pwdScore()] ?? 'rgba(255,255,255,.3)';
  });

  readonly bars = [0, 1, 2, 3];

  constructor() {
    this.form.controls.password.valueChanges.subscribe((v) => this.pwdValue.set(v ?? ''));
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('invalid');
      return;
    }
    this.token = token;
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  barBackground(index: number): string {
    return index < this.pwdScore() ? this.strengthColor() : 'rgba(255,255,255,.08)';
  }

  /** Les deux champs de mot de passe doivent être identiques. */
  private passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
  }

  /** Vrai si l'utilisateur a saisi la confirmation et qu'elle diffère. */
  get showMismatch(): boolean {
    const confirm = this.form.controls.confirmPassword;
    return this.form.hasError('passwordMismatch') && (confirm.touched || confirm.dirty);
  }

  onSubmit(): void {
    this.resetError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.authService.resetPassword(this.token, this.form.controls.password.value).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.state.set('success');
        // Redirection automatique vers la connexion après quelques secondes
        setTimeout(() => this.router.navigateByUrl('/auth/login'), 4000);
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        const code = err.error?.error?.code;
        if (code === 'RESET_TOKEN_EXPIRED') {
          this.resetError.set(
            'Ce lien de réinitialisation a expiré. Veuillez en demander un nouveau.'
          );
        } else if (code === 'RESET_TOKEN_INVALID') {
          this.resetError.set(
            'Ce lien de réinitialisation est invalide ou a déjà été utilisé.'
          );
        } else if (code === 'VALIDATION_ERROR') {
          const details: string[] = err.error?.error?.details ?? [];
          this.resetError.set(details[0] ?? 'Mot de passe invalide.');
        } else {
          this.resetError.set('Une erreur est survenue. Veuillez réessayer.');
        }
      },
    });
  }
}
