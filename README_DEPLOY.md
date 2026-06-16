# Deploy Guide

## Objetivo

Este proyecto queda preparado para:

- backend Spring Boot en Railway
- PostgreSQL gestionado por Railway
- frontend Angular en Vercel
- Swagger publico
- secretos solo por variables de entorno

No subas `.env`, claves API, tokens, passwords ni secretos JWT a GitHub.

## 1. GitHub

Antes de publicar, revisa siempre:

```bash
git status
git diff
```

No debe aparecer ninguno de estos archivos o directorios:

```text
.env
backend/.env
node_modules/
dist/
target/
.angular/cache/
```

## 2. Variables para Railway Backend

Variables minimas para Spring Boot en produccion:

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
DB_NAME=${{Postgres.PGDATABASE}}
JWT_SECRET=REEMPLAZAR_CON_UN_SECRET_REAL_Y_LARGO
MARKET_DATA_PROVIDER=finnhub
FINNHUB_API_KEY=REEMPLAZAR_EN_RAILWAY
FMP_API_KEY=
MARKET_DATA_ENRICH_ON_POSITION_CREATE=true
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://TU-FRONTEND.vercel.app
SPRINGDOC_API_DOCS_ENABLED=true
SPRINGDOC_SWAGGER_UI_ENABLED=true
```

Notas:

- `JWT_SECRET` debe existir solo en Railway.
- `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` deben venir del servicio PostgreSQL de Railway.
- No pongas claves reales en `application.yml`, `application-prod.yml` ni Angular.

## 3. Backend en Railway

1. Crea un proyecto en Railway.
2. Conecta el repositorio de GitHub.
3. Crea un servicio desde este repo con `Root Directory = backend`.
4. Railway debe usar `backend/Dockerfile`.
5. Añade las variables de entorno del bloque anterior.
6. Despliega el servicio.

Configuracion relevante ya preparada en el repo:

- `backend/src/main/resources/application-prod.yml`
- `backend/Dockerfile`
- `backend/.dockerignore`

## 4. PostgreSQL en Railway

1. Dentro del mismo proyecto, añade PostgreSQL.
2. Verifica que Railway exponga `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` y `PGPASSWORD`.
3. Construye estas variables en el backend:

```env
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
DB_NAME=${{Postgres.PGDATABASE}}
```

4. Arranca el backend y comprueba que Flyway corre correctamente.

## 5. Swagger publico

Swagger queda accesible publicamente y el resto de endpoints sigue protegido.

URLs esperadas:

```text
Swagger UI:
https://TU-BACKEND.up.railway.app/swagger-ui/index.html

OpenAPI JSON:
https://TU-BACKEND.up.railway.app/v3/api-docs
```

Rutas publicas configuradas:

```text
/api/auth/**
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
/actuator/health
GET /api/etfs/**
```

El resto requiere autenticacion JWT.

## 6. CORS

El backend lee origenes permitidos desde:

```env
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://TU-FRONTEND.vercel.app
```

No se usa `*` con credenciales activadas.

Metodos permitidos:

```text
GET
POST
PUT
PATCH
DELETE
OPTIONS
```

Headers permitidos:

```text
Authorization
Content-Type
Accept
Origin
X-Requested-With
```

## 7. Frontend Angular

El frontend esta preparado para usar:

- `src/environments/environment.ts` para local con `apiUrl: '/api'`
- `src/environments/environment.prod.ts` para produccion

Antes del deploy final en Vercel, sustituye el placeholder de produccion:

```ts
export const environment = {
  production: true,
  apiUrl: 'https://TU-BACKEND.up.railway.app/api'
};
```

Importante:

- Angular no debe contener API keys.
- Angular no debe llamar directamente a Finnhub o FMP.
- Todas las llamadas de mercado deben pasar por el backend.

## 8. Vercel

`vercel.json` ya esta preparado para SPA routing:

```json
{
  "buildCommand": "npm run build",
  "outputDirectory": "dist/etf-compass/browser",
  "rewrites": [
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

Pasos:

1. Importa el repo en Vercel.
2. Usa la raiz del proyecto.
3. Build command:

```bash
npm install
npm run build
```

4. Output directory:

```text
dist/etf-compass/browser
```

Variables de entorno en Vercel:

- ninguna obligatoria con la configuracion actual
- si cambias `environment.prod.ts`, haz commit antes de desplegar

## 9. Checklist final

1. `git status` limpio antes del push.
2. No hay `.env` ni secretos en Git.
3. `JWT_SECRET` solo existe en Railway.
4. `FINNHUB_API_KEY` solo existe en Railway.
5. Swagger abre publicamente.
6. Los endpoints privados responden `401/403` sin token.
7. El frontend llama al backend Railway.
8. `GET /actuator/health` responde correctamente.

## 10. Comandos utiles

Backend:

```bash
cd backend
mvn -DskipTests clean package
```

Frontend:

```bash
npm install
npm run build
```

## 11. URLs a sustituir

Reemplaza estos placeholders cuando tengas las URLs reales:

```text
https://TU-BACKEND.up.railway.app
https://TU-FRONTEND.vercel.app
```
