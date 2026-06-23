# ============================================================
# OWOMI Frontend — Dockerfile multi-stage (build Angular + Nginx)
# ============================================================

# --- Étape 1 : build de l'application Angular ---
FROM node:20-alpine AS build

WORKDIR /build

# Cache des dépendances
COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# --- Étape 2 : service statique via Nginx ---
FROM nginx:1.27-alpine AS runtime

# Configuration SPA (fallback index.html)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Les fichiers buildés (Angular application builder → dossier browser)
COPY --from=build /build/dist/owomi-frontend/browser /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
