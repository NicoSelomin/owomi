import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Calcule un score de force de mot de passe de 1 à 4.
 * Critères : longueur ≥ 8, majuscule, chiffre, caractère spécial.
 */
export function passwordScore(value: string): number {
  if (!value) {
    return 0;
  }
  let score = 0;
  if (value.length >= 8) score++;
  if (/[A-Z]/.test(value)) score++;
  if (/[0-9]/.test(value)) score++;
  if (/[^A-Za-z0-9]/.test(value)) score++;
  return Math.max(1, score);
}

/**
 * Validator : le mot de passe doit contenir au moins une majuscule,
 * une minuscule et un chiffre (cohérent avec la validation backend).
 */
export function passwordStrengthValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) {
      return null;
    }
    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumeric = /[0-9]/.test(value);
    const hasMinLength = value.length >= 8;

    const valid = hasUpperCase && hasLowerCase && hasNumeric && hasMinLength;
    return valid ? null : { weakPassword: true };
  };
}
