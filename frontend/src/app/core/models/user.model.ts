/**
 * Devise (table de référence côté backend).
 */
export interface Currency {
  code: string;
  name: string;
  symbol: string;
  locale: string;
}

/**
 * Utilisateur authentifié (jamais le mot de passe).
 */
export interface User {
  id: string;
  name: string;
  email: string;
  currency: Currency | null;
}
