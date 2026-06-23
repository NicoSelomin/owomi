# CLAUDE.md — OWOMI Global

> Ce fichier est la référence principale pour Claude dans le projet OWOMI.
> Il définit le contexte, les conventions globales, les règles de sécurité et les standards
> qui s'appliquent à l'ensemble du projet (frontend + backend).

---

## 1. Contexte du projet

**OWOMI** signifie "mon argent" en Fon (langue du Bénin).
C'est une application de gestion de budget personnel, **100% gratuite**, sans publicité,
sans restriction de fonctionnalités, déployée publiquement pour tous.

| Attribut | Valeur |
|---|---|
| Auteur | VODOUNNOU Nicodème (Selonick) |
| Localisation | Cotonou, Bénin |
| Version | 1.0.0 |
| Licence | Propriétaire — code public GitHub, usage libre |
| Langue UI | Français |
| Public cible | Audience internationale, priorité Afrique francophone |

---

## 2. Stack technique

### Frontend
- Angular 19 (Standalone Components, Signals)
- Ionic 8
- Capacitor 6 (iOS + Android)
- TypeScript strict
- SCSS

### Backend
- Java 21
- Spring Boot 3
- Spring Security + JWT (jjwt 0.12)
- Spring Data JPA + Hibernate
- PostgreSQL 16
- Maven

### Infrastructure
- Docker + Docker Compose
- Render (backend) / Vercel (frontend PWA)

---

## 3. Structure du monorepo

```
owomi/
├── frontend/          # Angular 19 + Ionic + Capacitor
├── backend/           # Spring Boot 3
├── docs/              # Diagrammes, maquettes, CDC
├── docker-compose.yml # Orchestration locale
└── CLAUDE.md          # Ce fichier
```

---

## 4. Conventions de code globales

### 4.1 Langue
- **Code** (variables, fonctions, classes, méthodes) : **anglais**
- **Commentaires** : **français**
- **Messages utilisateur / UI** : **français**
- **Logs applicatifs** : **anglais**
- **Messages d'erreur API** : **français** (côté user), **anglais** (côté log)

### 4.2 Nommage
| Élément | Convention |
|---|---|
| Classes Java | PascalCase |
| Méthodes Java | camelCase |
| Variables Java | camelCase |
| Constantes Java | UPPER_SNAKE_CASE |
| Components Angular | PascalCase + suffix `Component` |
| Services Angular | PascalCase + suffix `Service` |
| Fichiers Angular | kebab-case |
| CSS classes | kebab-case |
| Variables SCSS | `$kebab-case` |
| Variables CSS | `--kebab-case` |

### 4.3 Format des montants
- Toujours utiliser `BigDecimal` côté Java, jamais `double` ou `float`
- Côté Angular : pipe `currency` ou service `CurrencyService` personnalisé
- Format d'affichage : `[symbole] [montant formaté]` — ex : `FCFA 12 500,00`
- Jamais afficher un float brut à l'écran

### 4.4 Dates
- Stockage BDD : `DATE` ou `TIMESTAMP WITH TIME ZONE`
- API : format ISO 8601 — `2026-06-22` / `2026-06-22T14:30:00Z`
- Affichage UI : `dd MMMM yyyy` — ex : `22 juin 2026`
- Pas de date dans le futur pour les transactions (validation côté back ET front)

---

## 5. Sécurité — Règles OWASP

### 5.1 Authentification & Sessions (OWASP A07)
- JWT Access Token : expiration **1 heure**
- JWT Refresh Token : expiration **7 jours**, stocké en BDD
- Secret JWT : minimum **256 bits**, chargé depuis variable d'environnement, **jamais hardcodé**
- Algorithme JWT : **HS256** minimum, préférer **RS256** en production
- Mots de passe hashés avec **BCrypt strength 12**
- Refresh tokens invalidés à la déconnexion et au changement de mot de passe

### 5.2 Contrôle d'accès (OWASP A01)
- Chaque endpoint protégé vérifie que la ressource appartient à l'utilisateur authentifié
- Pas de référence directe à un ID sans vérification de propriété (IDOR protection)
- Principe du moindre privilège : accès minimal par défaut
- Routes Angular protégées par `AuthGuard`

### 5.3 Injection (OWASP A03)
- **Jamais** de requêtes SQL natives — utiliser JPA/JPQL uniquement
- Si requête native nécessaire : `@Query` avec paramètres nommés, **jamais** de concaténation
- Validation de toutes les entrées utilisateur côté backend (`@Valid`, `@NotNull`, etc.)
- Sanitisation des sorties HTML côté Angular (Angular échappe par défaut — ne pas contourner)

### 5.4 Données sensibles (OWASP A02)
- Variables d'environnement pour : JWT secret, DB URL, DB password, API keys
- Fichier `.env` dans `.gitignore` — toujours
- Pas de données sensibles dans les logs (email, mot de passe, token)
- HTTPS obligatoire en production
- Pas de stack trace exposée à l'utilisateur final

### 5.5 Rate Limiting (OWASP A04)
- Endpoints auth (`/api/auth/**`) : **10 requêtes/minute par IP**
- Endpoints export PDF : **5 requêtes/minute par utilisateur**
- Autres endpoints : **100 requêtes/minute par utilisateur**
- Implémentation : Spring Boot + Bucket4j ou filtre custom

### 5.6 Headers de sécurité HTTP
Obligatoires en production :
```
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'
Referrer-Policy: strict-origin-when-cross-origin
```

### 5.7 CORS
- Origins autorisées : uniquement le domaine frontend (variable d'environnement)
- En développement : `http://localhost:4200`
- En production : `https://owomi.selonick.dev` (ou domaine défini)
- Méthodes autorisées : `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`
- Headers autorisés : `Authorization`, `Content-Type`

### 5.8 Validation des données
- Validation côté **client** : UX rapide
- Validation côté **serveur** : source de vérité — toujours obligatoire, ne jamais faire confiance au client
- Double validation systématique pour : montants > 0, dates ≤ aujourd'hui, email format, password strength

---

## 6. Gestion des erreurs

### 6.1 Backend — Format de réponse uniforme
Toutes les réponses API suivent ce format :
```json
{
  "success": true,
  "data": { ... },
  "message": "Opération réussie",
  "timestamp": "2026-06-22T14:30:00Z"
}
```
En cas d'erreur :
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "Email ou mot de passe incorrect.",
    "details": []
  },
  "timestamp": "2026-06-22T14:30:00Z"
}
```

### 6.2 Codes d'erreur métier
| Code | Signification |
|---|---|
| `INVALID_CREDENTIALS` | Email ou mot de passe incorrect |
| `EMAIL_ALREADY_EXISTS` | Email déjà utilisé |
| `TOKEN_EXPIRED` | JWT expiré |
| `TOKEN_INVALID` | JWT invalide |
| `ACCESS_DENIED` | Accès refusé (403) |
| `RESOURCE_NOT_FOUND` | Ressource introuvable (404) |
| `VALIDATION_ERROR` | Erreur de validation des données |
| `AMOUNT_INVALID` | Montant invalide (≤ 0) |
| `FUTURE_DATE` | Date dans le futur |
| `CATEGORY_HAS_TRANSACTIONS` | Catégorie liée à des transactions |
| `CATEGORY_IS_DEFAULT` | Catégorie par défaut non modifiable |
| `RATE_LIMIT_EXCEEDED` | Trop de requêtes |

### 6.3 Frontend — Gestion des erreurs HTTP
- Intercepteur HTTP global pour capturer les erreurs
- Messages d'erreur affichés en français à l'utilisateur
- Logs d'erreur techniques en console (dev) / service de monitoring (prod)
- Retry automatique sur erreur réseau (max 2 tentatives)
- Redirection vers login sur 401 avec message explicatif

---

## 7. Tests

### 7.1 Backend
- **JUnit 5** + **Mockito** pour les tests unitaires
- **Spring Boot Test** pour les tests d'intégration
- **H2 in-memory** pour les tests (profil `test`)
- Coverage cible : **≥ 70%** sur les services
- Tests obligatoires pour : `AuthService`, `TransactionService`, `CategoryService`, `ReportService`

### 7.2 Frontend
- **Jest** pour les tests unitaires
- **Cypress** pour les tests E2E (optionnel pour le MVP)
- Tests obligatoires pour : services Angular, guards, intercepteurs

### 7.3 Commandes
```bash
# Backend
./mvnw test                    # Tous les tests
./mvnw test -pl backend        # Tests backend uniquement

# Frontend
cd frontend && ng test         # Tests unitaires
cd frontend && ng e2e          # Tests E2E
```

---

## 8. Git & Versioning

### 8.1 Branches
```
main          → production stable
develop       → intégration continue
feature/*     → nouvelles fonctionnalités
fix/*         → corrections de bugs
release/*     → préparation de release
```

### 8.2 Convention de commits (Conventional Commits)
```
feat(auth): ajouter l'inscription avec sélection de pays
fix(transaction): corriger le calcul du solde mensuel
docs(readme): mettre à jour les instructions de déploiement
test(auth): ajouter tests unitaires AuthService
refactor(category): extraire logique métier dans CategoryService
security(auth): renforcer la validation JWT
```

### 8.3 .gitignore obligatoire
```
# Secrets
.env
.env.*
*.env
application-secrets.yml

# Build
target/
dist/
.angular/
node_modules/

# IDE
.idea/
.vscode/
*.iml

# Logs
*.log
logs/
```

---

## 9. Variables d'environnement

### Backend (`backend/.env`)
```env
# Base de données
DB_URL=jdbc:postgresql://localhost:5432/owomi_db
DB_USERNAME=owomi_user
DB_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_256_bit_secret_key_here
JWT_ACCESS_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:4200

# App
APP_ENV=development
```

### Frontend (`frontend/.env`)
```env
API_BASE_URL=http://localhost:8080
APP_VERSION=1.0.0
```

---

## 10. Docker Compose local

```yaml
# docker-compose.yml (racine du projet)
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: owomi_db
      POSTGRES_USER: owomi_user
      POSTGRES_PASSWORD: owomi_dev_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    env_file: ./backend/.env
    depends_on:
      - postgres

  frontend:
    build: ./frontend
    ports:
      - "4200:80"
    depends_on:
      - backend

volumes:
  postgres_data:
```

---

## 11. Checklist avant chaque commit

- [ ] Aucune valeur sensible hardcodée (secret, password, token)
- [ ] Validation des entrées présente côté serveur
- [ ] Tests existants toujours verts
- [ ] Pas de `console.log` de données sensibles
- [ ] Format de réponse API uniforme respecté
- [ ] Messages d'erreur en français pour l'utilisateur
- [ ] Propriété des ressources vérifiée (anti-IDOR)

---

## 12. Contacts & Ressources

| Ressource | Lien |
|---|---|
| Maquettes HTML | `docs/mockups/` |
| Design System | `docs/OWOMI_DesignSystem_v1.0.docx` |
| Cahier des charges | `docs/CDC_BudgetTracker_Selonick_v1.1.docx` |
| Diagramme UC | `docs/OWOMI_UseCaseDiagram.html` |
| Diagramme Classes | `docs/OWOMI_ClassDiagram.puml` |
| Swagger UI (dev) | `http://localhost:8080/swagger-ui.html` |
| Auteur | nicodeme@selonick.dev |

