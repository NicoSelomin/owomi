import { Routes } from '@angular/router';

/**
 * Routes de l'application.
 * Les features (auth, dashboard, transactions, etc.) seront ajoutées en lazy loading
 * au fil des prochains jours (cf. CLAUDE_FRONTEND.md §4).
 */
export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  {
    path: 'home',
    loadComponent: () =>
      import('./features/home/home.page').then((m) => m.HomePage),
  },
  { path: '**', redirectTo: 'home' },
];
