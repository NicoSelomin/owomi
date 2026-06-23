/**
 * Environnement de développement.
 * L'URL backend est servie via le proxy Angular (cf. proxy.conf.json) en `ng serve`.
 */
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  appVersion: '1.0.0',
};
