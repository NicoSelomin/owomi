import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import {
  passwordScore,
  passwordStrengthValidator,
} from '../../../shared/validators/password-strength.validator';

interface CountryOption {
  code: string;
  label: string;
  currency: string;
}

interface CurrencyOption {
  code: string;
  label: string;
}

/**
 * Page d'inscription — reproduction de docs/mockups/owomi_register.html.
 * Sélecteur pays → suggestion de devise, indicateur de force du mot de passe,
 * validation en temps réel, soumission réelle vers POST /api/auth/register.
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.page.html',
  styleUrl: './register.page.scss',
})
export class RegisterPage {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);

  readonly showPassword = signal(false);
  readonly isLoading = signal(false);
  readonly registerError = signal<string | null>(null);
  readonly currencySuggested = signal(false);

  /** Pays proposés (libellé avec drapeau) et devise associée. */
  readonly countries: CountryOption[] = [
    { code: 'BJ', label: '🇧🇯 Bénin', currency: 'XOF' },
    { code: 'CI', label: "🇨🇮 Côte d'Ivoire", currency: 'XOF' },
    { code: 'SN', label: '🇸🇳 Sénégal', currency: 'XOF' },
    { code: 'ML', label: '🇲🇱 Mali', currency: 'XOF' },
    { code: 'BF', label: '🇧🇫 Burkina Faso', currency: 'XOF' },
    { code: 'TG', label: '🇹🇬 Togo', currency: 'XOF' },
    { code: 'NE', label: '🇳🇪 Niger', currency: 'XOF' },
    { code: 'NG', label: '🇳🇬 Nigeria', currency: 'NGN' },
    { code: 'GH', label: '🇬🇭 Ghana', currency: 'GHS' },
    { code: 'KE', label: '🇰🇪 Kenya', currency: 'KES' },
    { code: 'CM', label: '🇨🇲 Cameroun', currency: 'XAF' },
    { code: 'MA', label: '🇲🇦 Maroc', currency: 'MAD' },
    { code: 'DZ', label: '🇩🇿 Algérie', currency: 'DZD' },
    { code: 'TN', label: '🇹🇳 Tunisie', currency: 'TND' },
    { code: 'FR', label: '🇫🇷 France', currency: 'EUR' },
    { code: 'BE', label: '🇧🇪 Belgique', currency: 'EUR' },
    { code: 'CH', label: '🇨🇭 Suisse', currency: 'CHF' },
    { code: 'CA', label: '🇨🇦 Canada', currency: 'CAD' },
    { code: 'US', label: '🇺🇸 États-Unis', currency: 'USD' },
    { code: 'GB', label: '🇬🇧 Royaume-Uni', currency: 'GBP' },
    { code: 'OTHER', label: '🌍 Autre pays', currency: 'USD' },
  ];

  /** Devises supportées (alignées sur la table currencies du backend). */
  readonly currencies: CurrencyOption[] = [
    { code: 'XOF', label: 'XOF — Franc CFA UEMOA (FCFA)' },
    { code: 'XAF', label: 'XAF — Franc CFA CEMAC (FCFA)' },
    { code: 'EUR', label: 'EUR — Euro (€)' },
    { code: 'USD', label: 'USD — Dollar américain ($)' },
    { code: 'GBP', label: 'GBP — Livre sterling (£)' },
    { code: 'MAD', label: 'MAD — Dirham marocain (DH)' },
    { code: 'DZD', label: 'DZD — Dinar algérien (DA)' },
    { code: 'TND', label: 'TND — Dinar tunisien (DT)' },
    { code: 'NGN', label: 'NGN — Naira (₦)' },
    { code: 'GHS', label: 'GHS — Cedi (₵)' },
    { code: 'KES', label: 'KES — Shilling kenyan (KSh)' },
    { code: 'CAD', label: 'CAD — Dollar canadien (CA$)' },
    { code: 'CHF', label: 'CHF — Franc suisse (CHF)' },
  ];

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]],
    countryCode: ['', [Validators.required]],
    currencyCode: ['', [Validators.required]],
  });

  /** Score de force du mot de passe (0–4), réactif à la saisie. */
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

  /** Couleur des barres de force (1 rouge, 2–3 or, 4 vert). */
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

  /** Indices des 4 barres de force. */
  readonly bars = [0, 1, 2, 3];

  barBackground(index: number): string {
    return index < this.pwdScore() ? this.strengthColor() : 'rgba(255,255,255,.08)';
  }

  constructor() {
    this.form.controls.password.valueChanges.subscribe((v) =>
      this.pwdValue.set(v ?? '')
    );
  }

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  /** Quand le pays change : suggère automatiquement la devise associée. */
  onCountryChange(): void {
    const country = this.countries.find(
      (c) => c.code === this.form.controls.countryCode.value
    );
    if (country) {
      this.form.controls.currencyCode.setValue(country.currency);
      this.currencySuggested.set(true);
    }
  }

  emailState(): 'ok' | 'err' | '' {
    const c = this.form.controls.email;
    if (!c.touched && !c.dirty) return '';
    if (c.value.trim() === '') return '';
    return c.valid ? 'ok' : 'err';
  }

  onSubmit(): void {
    this.registerError.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    // Le backend valide countryCode au format ISO 2 lettres ; « OTHER » est neutralisé.
    const countryCode = raw.countryCode === 'OTHER' ? 'XX' : raw.countryCode;

    this.isLoading.set(true);
    this.authService
      .register({
        name: raw.name.trim(),
        email: raw.email.trim(),
        password: raw.password,
        countryCode,
        currencyCode: raw.currencyCode,
      })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.router.navigateByUrl('/app/dashboard');
        },
        error: (err: HttpErrorResponse) => {
          this.isLoading.set(false);
          const code = err.error?.error?.code;
          if (code === 'EMAIL_ALREADY_EXISTS') {
            this.registerError.set('Cet email est déjà utilisé.');
          } else if (code === 'VALIDATION_ERROR') {
            const details: string[] = err.error?.error?.details ?? [];
            this.registerError.set(details[0] ?? 'Données invalides.');
          } else {
            this.registerError.set('Une erreur est survenue. Veuillez réessayer.');
          }
        },
      });
  }
}
