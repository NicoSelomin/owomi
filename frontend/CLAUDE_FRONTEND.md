# CLAUDE.md — OWOMI Frontend

> Ce fichier complète le `CLAUDE.md` global avec les règles spécifiques
> au projet Angular 19 + Ionic + Capacitor.
> Lire `CLAUDE.md` global avant ce fichier.

---

## 1. Stack & Versions

```json
{
  "angular": "19.x",
  "ionic": "8.x",
  "@capacitor/core": "6.x",
  "typescript": "5.x",
  "rxjs": "7.x",
  "zone.js": "0.14.x"
}
```

---

## 2. Structure du projet

```
frontend/
├── src/
│   ├── app/
│   │   ├── core/                   # Singletons (services, guards, interceptors)
│   │   │   ├── guards/
│   │   │   │   └── auth.guard.ts
│   │   │   ├── interceptors/
│   │   │   │   ├── jwt.interceptor.ts
│   │   │   │   └── error.interceptor.ts
│   │   │   ├── services/
│   │   │   │   ├── auth.service.ts
│   │   │   │   ├── token.service.ts
│   │   │   │   └── currency.service.ts
│   │   │   └── models/
│   │   │       ├── user.model.ts
│   │   │       ├── transaction.model.ts
│   │   │       ├── category.model.ts
│   │   │       └── api-response.model.ts
│   │   ├── features/               # Modules fonctionnels (lazy loaded)
│   │   │   ├── auth/
│   │   │   │   ├── login/
│   │   │   │   └── register/
│   │   │   ├── dashboard/
│   │   │   ├── transactions/
│   │   │   ├── categories/
│   │   │   ├── reports/
│   │   │   └── settings/
│   │   ├── shared/                 # Composants réutilisables
│   │   │   ├── components/
│   │   │   │   ├── balance-card/
│   │   │   │   ├── transaction-item/
│   │   │   │   └── empty-state/
│   │   │   ├── pipes/
│   │   │   │   └── owomi-currency.pipe.ts
│   │   │   └── directives/
│   │   ├── app.routes.ts
│   │   └── app.config.ts
│   ├── environments/
│   │   ├── environment.ts          # Dev
│   │   └── environment.prod.ts     # Production
│   └── theme/
│       ├── variables.css           # Tokens CSS OWOMI
│       └── global.scss
├── capacitor.config.ts
├── ionic.config.json
└── angular.json
```

---

## 3. Conventions Angular 19

### 3.1 Standalone Components — TOUJOURS
```typescript
// ✅ CORRECT — Toujours standalone
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, IonicModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {}

// ❌ INTERDIT — Pas de NgModule
@NgModule({ declarations: [DashboardComponent] })
export class DashboardModule {}
```

### 3.2 Signals — Préférer aux BehaviorSubject simples
```typescript
// ✅ CORRECT — Signals pour l'état local
export class DashboardComponent {
  balance = signal<number>(0);
  transactions = signal<Transaction[]>([]);
  isLoading = signal<boolean>(false);

  totalIncome = computed(() =>
    this.transactions().filter(t => t.type === 'INCOME')
      .reduce((sum, t) => sum + t.amount, 0)
  );
}

// Pour l'état partagé entre composants → service avec signal
@Injectable({ providedIn: 'root' })
export class TransactionService {
  private _transactions = signal<Transaction[]>([]);
  transactions = this._transactions.asReadonly();
}
```

### 3.3 Inject function — Préférer au constructeur
```typescript
// ✅ CORRECT
export class DashboardComponent {
  private authService = inject(AuthService);
  private router = inject(Router);
}

// ✅ Aussi acceptable pour les tests
export class DashboardComponent {
  constructor(private authService: AuthService) {}
}
```

### 3.4 TypeScript strict
```typescript
// tsconfig.json — Ces options sont obligatoires
{
  "compilerOptions": {
    "strict": true,
    "noImplicitAny": true,
    "strictNullChecks": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true
  }
}
```

---

## 4. Routing & Lazy Loading

```typescript
// app.routes.ts
export const routes: Routes = [
  { path: '', redirectTo: '/splash', pathMatch: 'full' },
  {
    path: 'splash',
    loadComponent: () => import('./features/auth/splash/splash.component')
      .then(m => m.SplashComponent)
  },
  {
    path: 'auth',
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component')
          .then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component')
          .then(m => m.RegisterComponent)
      }
    ]
  },
  {
    path: 'app',
    canActivate: [AuthGuard],           // Protection de toutes les routes protégées
    loadComponent: () => import('./features/shell/shell.component')
      .then(m => m.ShellComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component')
          .then(m => m.DashboardComponent)
      },
      {
        path: 'transactions',
        loadComponent: () => import('./features/transactions/transactions.component')
          .then(m => m.TransactionsComponent)
      },
      {
        path: 'categories',
        loadComponent: () => import('./features/categories/categories.component')
          .then(m => m.CategoriesComponent)
      },
      {
        path: 'reports',
        loadComponent: () => import('./features/reports/reports.component')
          .then(m => m.ReportsComponent)
      },
      {
        path: 'settings',
        loadComponent: () => import('./features/settings/settings.component')
          .then(m => m.SettingsComponent)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '/splash' }
];
```

---

## 5. Authentification & Sécurité Frontend

### 5.1 AuthGuard
```typescript
// core/guards/auth.guard.ts
export const AuthGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.isAuthenticated()) {
    return true;
  }

  // Sauvegarder l'URL cible pour redirection post-login
  router.navigate(['/auth/login'], {
    queryParams: { returnUrl: state.url }
  });
  return false;
};
```

### 5.2 JWT Interceptor
```typescript
// core/interceptors/jwt.interceptor.ts
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getAccessToken();

  // Ne pas ajouter le token aux endpoints publics
  const publicUrls = ['/api/auth/login', '/api/auth/register', '/api/currencies'];
  const isPublic = publicUrls.some(url => req.url.includes(url));

  if (token && !isPublic) {
    req = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }
  return next(req);
};
```

### 5.3 Error Interceptor — Gestion du 401
```typescript
// core/interceptors/error.interceptor.ts
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Tenter le refresh token
        return authService.refreshToken().pipe(
          switchMap(() => next(req)),
          catchError(() => {
            authService.logout();
            router.navigate(['/auth/login']);
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
```

### 5.4 Token Storage — Sécurisé
```typescript
// core/services/token.service.ts
// IMPORTANT : Sur mobile Capacitor → SecureStorage plugin
// Sur web → sessionStorage (pas localStorage pour les tokens)
@Injectable({ providedIn: 'root' })
export class TokenService {
  private readonly ACCESS_TOKEN_KEY = 'owomi_access';
  private readonly REFRESH_TOKEN_KEY = 'owomi_refresh';

  // ✅ sessionStorage : effacé à la fermeture du navigateur
  // ✅ SecureStorage sur mobile natif
  setTokens(access: string, refresh: string): void {
    sessionStorage.setItem(this.ACCESS_TOKEN_KEY, access);
    sessionStorage.setItem(this.REFRESH_TOKEN_KEY, refresh);
  }

  getAccessToken(): string | null {
    return sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  clearTokens(): void {
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
    // Vérifier l'expiration côté client (double sécurité)
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.exp * 1000 > Date.now();
    } catch {
      return false;
    }
  }
}
```

---

## 6. Services HTTP

### 6.1 Pattern de base
```typescript
// features/transactions/transaction.service.ts
@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private readonly BASE_URL = `${environment.apiBaseUrl}/api/transactions`;

  getAll(filters?: TransactionFilters): Observable<ApiResponse<PageResponse<Transaction>>> {
    const params = this.buildParams(filters);
    return this.http.get<ApiResponse<PageResponse<Transaction>>>(this.BASE_URL, { params });
  }

  create(request: TransactionRequest): Observable<ApiResponse<Transaction>> {
    return this.http.post<ApiResponse<Transaction>>(this.BASE_URL, request);
  }

  update(id: number, request: TransactionRequest): Observable<ApiResponse<Transaction>> {
    return this.http.put<ApiResponse<Transaction>>(`${this.BASE_URL}/${id}`, request);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.BASE_URL}/${id}`);
  }

  private buildParams(filters?: TransactionFilters): HttpParams {
    let params = new HttpParams();
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.categoryId) params = params.set('categoryId', filters.categoryId.toString());
    if (filters?.month) params = params.set('month', filters.month.toString());
    if (filters?.year) params = params.set('year', filters.year.toString());
    if (filters?.page) params = params.set('page', filters.page.toString());
    return params;
  }
}
```

### 6.2 Modèles TypeScript stricts
```typescript
// core/models/transaction.model.ts
export type TransactionType = 'INCOME' | 'EXPENSE';

export interface Transaction {
  id: number;
  amount: number;
  type: TransactionType;
  note: string | null;
  date: string;              // ISO 8601 : '2026-06-22'
  category: Category;
  createdAt: string;
}

export interface TransactionRequest {
  amount: number;
  type: TransactionType;
  categoryId: number;
  date: string;
  note?: string;
}

export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  timestamp: string;
  error?: ApiError;
}

export interface ApiError {
  code: string;
  message: string;
  details: string[];
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  size: number;
}
```

---

## 7. Formulaires Reactifs

### 7.1 Pattern standard
```typescript
// Toujours Reactive Forms — jamais Template-driven
export class RegisterComponent {
  private fb = inject(FormBuilder);

  form = this.fb.group({
    name:      ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
    email:     ['', [Validators.required, Validators.email]],
    password:  ['', [Validators.required, Validators.minLength(8), passwordStrengthValidator()]],
    country:   ['', Validators.required],
    currency:  ['XOF', Validators.required],
  });

  // Validation uniquement après que l'utilisateur a touché le champ
  getError(field: string): string | null {
    const control = this.form.get(field);
    if (!control || !control.touched || !control.errors) return null;

    if (control.errors['required'])   return 'Ce champ est obligatoire.';
    if (control.errors['email'])      return 'Format d\'email invalide.';
    if (control.errors['minlength'])  return `Minimum ${control.errors['minlength'].requiredLength} caractères.`;
    if (control.errors['maxlength'])  return `Maximum ${control.errors['maxlength'].requiredLength} caractères.`;
    return null;
  }
}
```

### 7.2 Validator personnalisé — Force du mot de passe
```typescript
// shared/validators/password-strength.validator.ts
export function passwordStrengthValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value as string;
    if (!value) return null;

    const hasUpperCase = /[A-Z]/.test(value);
    const hasLowerCase = /[a-z]/.test(value);
    const hasNumeric  = /[0-9]/.test(value);
    const hasMinLength = value.length >= 8;

    const score = [hasUpperCase, hasLowerCase, hasNumeric, hasMinLength]
      .filter(Boolean).length;

    return score < 3 ? { weakPassword: true } : null;
  };
}
```

---

## 8. Design System — Intégration Ionic

### 8.1 Variables CSS OWOMI (src/theme/variables.css)
```css
:root {
  /* Couleurs principales */
  --owomi-color-nuit:    #0C131E;
  --owomi-color-or:      #D49E10;
  --owomi-color-vert:    #1D9E75;
  --owomi-color-corail:  #D85A30;
  --owomi-color-ivoire:  #F7F5F0;
  --owomi-color-rouge:   #E24B4A;

  /* Sémantiques */
  --owomi-income-bg:     #E1F5EE;
  --owomi-income-text:   #085041;
  --owomi-expense-bg:    #FAECE7;
  --owomi-expense-text:  #4A1B0C;

  /* Typographie */
  --owomi-font-display:  'Sora', sans-serif;
  --owomi-font-body:     'Plus Jakarta Sans', sans-serif;
  --owomi-font-mono:     'IBM Plex Mono', monospace;

  /* Ionic overrides */
  --ion-color-primary:         var(--owomi-color-or);
  --ion-color-primary-contrast: var(--owomi-color-nuit);
  --ion-background-color:      var(--owomi-color-ivoire);
  --ion-text-color:            #1C1C1A;
  --ion-font-family:           var(--owomi-font-body);
}
```

### 8.2 Pipe devise personnalisé
```typescript
// shared/pipes/owomi-currency.pipe.ts
@Pipe({ name: 'owomiCurrency', standalone: true })
export class OwomiCurrencyPipe implements PipeTransform {
  private currencyService = inject(CurrencyService);

  transform(amount: number, showSign = false): string {
    const currency = this.currencyService.currentCurrency();
    const sign = showSign ? (amount >= 0 ? '+' : '') : '';
    const formatted = new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(Math.abs(amount));

    return `${sign}${formatted} ${currency.symbol}`;
  }
}

// Usage dans le template :
// {{ transaction.amount | owomiCurrency:true }}
// → "+200 000 FCFA" ou "−18 500 FCFA"
```

---

## 9. Responsive & Breakpoints

```scss
// theme/_breakpoints.scss
$breakpoint-mobile:  639px;
$breakpoint-tablet:  1023px;
$breakpoint-desktop: 1024px;

@mixin mobile-only {
  @media (max-width: #{$breakpoint-mobile}) { @content; }
}
@mixin tablet-only {
  @media (min-width: 640px) and (max-width: #{$breakpoint-tablet}) { @content; }
}
@mixin desktop-only {
  @media (min-width: #{$breakpoint-desktop}) { @content; }
}

// Usage :
.balance-amount {
  font-size: 28px;

  @include tablet-only { font-size: 32px; }
  @include desktop-only { font-size: 40px; }
}
```

---

## 10. Capacitor — Mobile natif

### 10.1 Configuration (capacitor.config.ts)
```typescript
import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'dev.selonick.owomi',
  appName: 'OWOMI',
  webDir: 'dist/frontend/browser',
  server: {
    androidScheme: 'https'
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: '#0C131E',
      androidSplashResourceName: 'splash',
      showSpinner: false
    }
  }
};
export default config;
```

### 10.2 Détection de plateforme
```typescript
// Utiliser Platform d'Ionic pour adapter le comportement
export class AppComponent {
  private platform = inject(Platform);

  isMobile = this.platform.is('capacitor');
  isIos    = this.platform.is('ios');
  isAndroid = this.platform.is('android');
}
```

---

## 11. Commandes utiles

```bash
# Installation
npm install

# Serveur de développement
ng serve

# Build production
ng build --configuration production

# Tests
ng test
ng test --code-coverage

# Lint
ng lint

# Capacitor — Android
npx cap add android
npx cap sync
npx cap open android

# Capacitor — iOS
npx cap add ios
npx cap sync
npx cap open ios

# Générer un composant standalone
ng generate component features/dashboard/dashboard --standalone

# Générer un service
ng generate service core/services/auth
```

---

## 12. Checklist sécurité Frontend

- [ ] Aucun secret ou clé API dans le code source
- [ ] Tokens JWT stockés en `sessionStorage` (web) ou `SecureStorage` (mobile)
- [ ] Toutes les routes protégées ont le `AuthGuard`
- [ ] Intercepteur JWT attaché à toutes les requêtes protégées
- [ ] Gestion du 401 avec refresh automatique
- [ ] Validation formulaires côté client (UX) ET rappel que le backend valide toujours
- [ ] `innerHTML` jamais utilisé directement (risque XSS) — utiliser `DomSanitizer` si nécessaire
- [ ] `environment.prod.ts` sans URLs de dev ni debug activé
- [ ] HTTPS forcé en production dans `capacitor.config.ts`
