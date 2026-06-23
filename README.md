# OWOMI

> **OWOMI** signifie « mon argent » en Fon (langue du Bénin).
> Application de gestion de budget personnel, **100% gratuite**, sans publicité ni
> restriction de fonctionnalités.

| Attribut | Valeur |
|---|---|
| Auteur | VODOUNNOU Nicodème (Selonick) |
| Localisation | Cotonou, Bénin |
| Version | 1.0.0 |
| Langue UI | Français |

---

## Stack technique

| Côté | Technologies |
|---|---|
| **Frontend** | Angular 19 (Standalone + Signals) · Ionic 8 · Capacitor 6 · TypeScript strict · SCSS |
| **Backend** | Java 21 · Spring Boot 3.5 · Spring Security + JWT · Spring Data JPA · PostgreSQL 16 · Flyway · Maven |
| **Infra** | Docker · Docker Compose · Render (back) · Vercel (front PWA) |

---

## Structure du monorepo

```
owomi/
├── frontend/          # Angular 19 + Ionic + Capacitor
├── backend/           # Spring Boot 3
├── docs/              # Diagrammes, maquettes, CDC
├── docker-compose.yml # Orchestration locale
├── .gitignore
└── README.md
```

---

## Démarrage rapide (développement)

### Prérequis
- Java 21, Maven (wrapper inclus)
- Node 20+, npm
- Docker + Docker Compose

### 1. Base de données

```bash
docker-compose up -d postgres
```

PostgreSQL démarre sur `localhost:5432` (db `owomi_db`, user `owomi_user`).

### 2. Backend

```bash
cd backend
cp .env.example .env        # puis renseigner les valeurs
./mvnw spring-boot:run
```

- API : http://localhost:8080
- Santé : http://localhost:8080/api/health → `{"status":"UP","app":"OWOMI","version":"1.0.0"}`
- Swagger UI (dev) : http://localhost:8080/swagger-ui.html

Les migrations **Flyway** (V1 → V6) s'exécutent automatiquement au démarrage.

### 3. Frontend

```bash
cd frontend
npm install
ng serve
```

- Application : http://localhost:4200
- Les appels `/api/**` sont relayés vers le backend via le proxy Angular (`proxy.conf.json`).

---

## Toute la stack via Docker

```bash
docker-compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| Backend | http://localhost:8080 |
| PostgreSQL | localhost:5432 |

---

## Conventions

Les règles de code, sécurité (OWASP) et conventions du projet sont décrites dans :

- [`CLAUDE.md`](./CLAUDE.md) — conventions globales
- [`frontend/CLAUDE_FRONTEND.md`](./frontend/CLAUDE_FRONTEND.md) — règles Angular/Ionic
- [`backend/CLAUDE_BACKEND.md`](./backend/CLAUDE_BACKEND.md) — règles Spring Boot

---

## Licence

Propriétaire — code public sur GitHub, usage libre. Auteur : nicodeme@selonick.dev
