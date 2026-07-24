import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, finalize, tap } from 'rxjs';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { User } from '../models/user.model';
import { ApiService } from './api.service';
import { TokenService } from './token.service';
import { UserService } from './user.service';

/**
 * Service d'authentification : inscription, connexion, déconnexion, refresh.
 * Expose l'état de session via des signals (currentUser, isLoggedIn).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokenService = inject(TokenService);
  private readonly userService = inject(UserService);

  /** Utilisateur connecté (null si déconnecté). */
  private readonly _currentUser = signal<User | null>(null);
  readonly currentUser = this._currentUser.asReadonly();

  /** Vrai si un access token valide est présent. */
  readonly isLoggedIn = computed(() =>
    this._currentUser() !== null || this.tokenService.isAuthenticated()
  );

  register(request: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.api
      .post<ApiResponse<AuthResponse>, RegisterRequest>('/api/auth/register', request)
      .pipe(tap((res) => this.handleAuthSuccess(res.data)));
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.api
      .post<ApiResponse<AuthResponse>, LoginRequest>('/api/auth/login', request)
      .pipe(tap((res) => this.handleAuthSuccess(res.data)));
  }

  /** Rafraîchit l'access token à partir du refresh token stocké. */
  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.api
      .post<ApiResponse<AuthResponse>, { refreshToken: string | null }>('/api/auth/refresh', { refreshToken })
      .pipe(tap((res) => this.tokenService.setAccessToken(res.data.accessToken)));
  }

  /** Confirme l'adresse email à partir du token reçu par email (lien de vérification). */
  verifyEmail(token: string): Observable<ApiResponse<void>> {
    return this.api.get<ApiResponse<void>>('/api/auth/verify-email', { token });
  }

  /**
   * Renvoie l'email de vérification.
   * Le backend répond toujours 200 (pas de fuite sur l'existence du compte).
   */
  resendVerification(email: string): Observable<ApiResponse<void>> {
    return this.api.post<ApiResponse<void>, { email: string }>('/api/auth/resend-verification', { email });
  }

  /**
   * Demande de réinitialisation de mot de passe.
   * Le backend répond toujours 200 (pas de fuite sur l'existence du compte).
   */
  forgotPassword(email: string): Observable<ApiResponse<void>> {
    return this.api.post<ApiResponse<void>, { email: string }>('/api/auth/forgot-password', { email });
  }

  /** Applique un nouveau mot de passe à partir d'un token de réinitialisation. */
  resetPassword(token: string, newPassword: string): Observable<ApiResponse<void>> {
    return this.api.post<ApiResponse<void>, { token: string; newPassword: string }>('/api/auth/reset-password', {
      token,
      newPassword,
    });
  }

  /** Déconnexion : révoque le refresh token côté serveur puis nettoie l'état local. */
  logout(): Observable<ApiResponse<void>> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.api
      .post<ApiResponse<void>, { refreshToken: string | null }>('/api/auth/logout', { refreshToken })
      .pipe(finalize(() => this.clearSession()));
  }

  /** Nettoie l'état local sans appel réseau (ex : échec de refresh). */
  clearSession(): void {
    this.tokenService.clearTokens();
    this._currentUser.set(null);
  }

  /**
   * Charge le profil de l'utilisateur courant (GET /api/users/me).
   * Utilisé au rechargement de page pour réhydrater currentUser.
   */
  loadCurrentUser(): Observable<ApiResponse<User>> {
    return this.userService
      .getMe()
      .pipe(tap((res) => this._currentUser.set(res.data)));
  }

  private handleAuthSuccess(data: AuthResponse): void {
    this.tokenService.setTokens(data.accessToken, data.refreshToken);
    this._currentUser.set(data.user);
  }
}
