import { Injectable } from '@angular/core';

/**
 * Stockage et lecture des JWT.
 * Les tokens sont conservés en sessionStorage (effacés à la fermeture du navigateur)
 * conformément aux règles de sécurité OWOMI — jamais en localStorage.
 */
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'owomi_access';
  private readonly REFRESH_TOKEN_KEY = 'owomi_refresh';

  setTokens(access: string, refresh: string): void {
    sessionStorage.setItem(this.ACCESS_TOKEN_KEY, access);
    sessionStorage.setItem(this.REFRESH_TOKEN_KEY, refresh);
  }

  /** Met à jour uniquement l'access token (après un refresh). */
  setAccessToken(access: string): void {
    sessionStorage.setItem(this.ACCESS_TOKEN_KEY, access);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  clearTokens(): void {
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Vrai si un access token est présent et non expiré (vérification côté client).
   * La source de vérité reste le backend.
   */
  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) {
      return false;
    }
    const payload = this.decodePayload(token);
    if (!payload || typeof payload.exp !== 'number') {
      return false;
    }
    return payload.exp * 1000 > Date.now();
  }

  /** Décode (sans vérifier la signature) le payload d'un JWT. */
  private decodePayload(token: string): { exp?: number } | null {
    try {
      const part = token.split('.')[1];
      const json = atob(part.replace(/-/g, '+').replace(/_/g, '/'));
      return JSON.parse(json);
    } catch {
      return null;
    }
  }
}
