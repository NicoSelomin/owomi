import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '../services/token.service';

/**
 * Ajoute l'en-tête Authorization: Bearer <token> à toutes les requêtes
 * sauf les endpoints publics (/api/auth/**, /api/currencies).
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getAccessToken();

  const publicUrls = ['/api/auth/', '/api/currencies'];
  const isPublic = publicUrls.some((url) => req.url.includes(url));

  if (token && !isPublic) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(req);
};
