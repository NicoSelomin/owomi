import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.model';
import { User } from '../models/user.model';
import { TokenService } from './token.service';

/**
 * Service d'authentification : inscription, connexion, déconnexion, refresh.
 * Expose l'état de session via des signals (currentUser, isLoggedIn).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private tokenService = inject(TokenService);

  private readonly BASE_URL = `${environment.apiBaseUrl}/api/auth`;
  private readonly USERS_URL = `${environment.apiBaseUrl}/api/users`;

  /** Utilisateur connecté (null si déconnecté). */
  private readonly _currentUser = signal<User | null>(null);
  readonly currentUser = this._currentUser.asReadonly();

  /** Vrai si un access token valide est présent. */
  readonly isLoggedIn = computed(() =>
    this._currentUser() !== null || this.tokenService.isAuthenticated()
  );

  register(request: RegisterRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.BASE_URL}/register`, request)
      .pipe(tap((res) => this.handleAuthSuccess(res.data)));
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthResponse>> {
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.BASE_URL}/login`, request)
      .pipe(tap((res) => this.handleAuthSuccess(res.data)));
  }

  /** Rafraîchit l'access token à partir du refresh token stocké. */
  refreshToken(): Observable<ApiResponse<AuthResponse>> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http
      .post<ApiResponse<AuthResponse>>(`${this.BASE_URL}/refresh`, { refreshToken })
      .pipe(tap((res) => this.tokenService.setAccessToken(res.data.accessToken)));
  }

  /** Déconnexion : révoque le refresh token côté serveur puis nettoie l'état local. */
  logout(): Observable<ApiResponse<void>> {
    const refreshToken = this.tokenService.getRefreshToken();
    return this.http
      .post<ApiResponse<void>>(`${this.BASE_URL}/logout`, { refreshToken })
      .pipe(tap(() => this.clearSession()));
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
    return this.http
      .get<ApiResponse<User>>(`${this.USERS_URL}/me`)
      .pipe(tap((res) => this._currentUser.set(res.data)));
  }

  private handleAuthSuccess(data: AuthResponse): void {
    this.tokenService.setTokens(data.accessToken, data.refreshToken);
    this._currentUser.set(data.user);
  }
}
