import { User } from './user.model';

/**
 * Requête d'inscription. countryCode et currencyCode au format ISO.
 */
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  countryCode: string;
  currencyCode: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

/**
 * Réponse d'authentification (register / login / refresh).
 */
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}
