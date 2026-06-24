import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  filter,
  switchMap,
  take,
  throwError,
} from 'rxjs';
import { AuthService } from '../services/auth.service';

// État partagé pour éviter plusieurs refresh simultanés (single-flight)
let isRefreshing = false;
const refreshedToken$ = new BehaviorSubject<string | null>(null);

/**
 * Gère les erreurs HTTP :
 * - sur 401 : tente un refresh automatique puis rejoue la requête ;
 * - si le refresh échoue : déconnexion + redirection vers /auth/login.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Les endpoints d'auth ne déclenchent pas de refresh (évite les boucles)
  const isAuthEndpoint = req.url.includes('/api/auth/');

  const addToken = (request: HttpRequest<unknown>, token: string) =>
    request.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status !== 401 || isAuthEndpoint) {
        return throwError(() => error);
      }

      // Un refresh est déjà en cours : on attend le nouveau token puis on rejoue
      if (isRefreshing) {
        return refreshedToken$.pipe(
          filter((token): token is string => token !== null),
          take(1),
          switchMap((token) => next(addToken(req, token)))
        );
      }

      isRefreshing = true;
      refreshedToken$.next(null);

      return authService.refreshToken().pipe(
        switchMap((res) => {
          isRefreshing = false;
          const newToken = res.data.accessToken;
          refreshedToken$.next(newToken);
          return next(addToken(req, newToken));
        }),
        catchError((refreshError) => {
          isRefreshing = false;
          authService.clearSession();
          router.navigate(['/auth/login']);
          return throwError(() => refreshError);
        })
      );
    })
  );
};
