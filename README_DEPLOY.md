# Despliegue Publico

## 1. Subir a GitHub

1. Revisa el estado final con `git status`.
2. Confirma que no aparezcan `.env`, `node_modules/`, `dist/`, `target/` ni otros artefactos locales.
3. Haz `git add` solo cuando estes conforme con los archivos versionados.
4. Crea el commit:

```bash
git commit -m "chore: prepare project for public deployment"
```

5. Haz `git push` manualmente cuando quieras publicar.

## 2. Variables para Railway

Backend Spring Boot:

```env
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=REEMPLAZAR_EN_RAILWAY
MARKET_DATA_PROVIDER=finnhub
FINNHUB_API_KEY=REEMPLAZAR_EN_RAILWAY
FMP_API_KEY=
MARKET_DATA_ENRICH_ON_POSITION_CREATE=true
CORS_ALLOWED_ORIGINS=http://localhost:4200,https://TU-FRONTEND.vercel.app
```

PostgreSQL en Railway suele exponer `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` y `PGPASSWORD` automaticamente.

Si tu plantilla de Railway no los inyecta en Spring como JDBC URL, añade tambien:

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
SPRING_DATASOURCE_USERNAME=${PGUSER}
SPRING_DATASOURCE_PASSWORD=${PGPASSWORD}
```

`DATABASE_URL` solo sirve si lo conviertes a formato JDBC. No asumas que `postgres://...` funcionara tal cual en `spring.datasource.url`.

## 3. Desplegar backend en Railway

1. Crea un nuevo proyecto en Railway.
2. Conecta el repo de GitHub.
3. Configura el servicio backend con `Root Directory = backend`.
4. Usa la opcion con `Dockerfile` para fijar Java 21 y un build reproducible.
5. Añade las variables de entorno anteriores.
6. Publica el servicio.

La opcion recomendada es usar `backend/Dockerfile`.

Alternativa sin Dockerfile:

```bash
mvn spring-boot:run
```

Para produccion es mejor el `jar` generado dentro del contenedor.

## 4. Desplegar PostgreSQL en Railway

1. En el mismo proyecto de Railway, añade una base PostgreSQL.
2. Verifica que el backend reciba `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER` y `PGPASSWORD`.
3. Si hace falta, define manualmente `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD`.
4. Comprueba que Flyway ejecute las migraciones al arrancar.

## 5. Desplegar frontend en Vercel

1. Conecta el repo en Vercel.
2. Usa el directorio raiz del proyecto.
3. El build debe ejecutarse con:

```bash
npm install
npm run build
```

4. El output esperado de Angular 18 es:

```text
dist/etf-compass/browser
```

5. `vercel.json` ya apunta a ese directorio y deja una rewrite SPA hacia `index.html`.
6. Antes del despliegue final, cambia `src/environments/environment.prod.ts` para apuntar al backend real:

```ts
export const environment = {
  production: true,
  apiUrl: 'https://TU-BACKEND.up.railway.app/api'
};
```

## 6. Configurar CORS

En produccion, el backend acepta por variable de entorno:

```text
http://localhost:4200
https://TU-FRONTEND.vercel.app
```

Headers permitidos:

```text
Authorization
Content-Type
Accept
Origin
X-Requested-With
```

Metodos permitidos:

```text
GET
POST
PUT
PATCH
DELETE
OPTIONS
```

No se usa `*` con credenciales activadas.

## 7. Swagger publico

Swagger queda publico en:

```text
https://TU-BACKEND.up.railway.app/swagger-ui/index.html
```

OpenAPI JSON:

```text
https://TU-BACKEND.up.railway.app/v3/api-docs
```

Rutas publicas:

```text
/api/auth/**
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
/actuator/health
GET /api/etfs/**
```

El resto de endpoints requiere JWT.

## 8. Pruebas finales

1. Abre el frontend:

```text
https://TU-FRONTEND.vercel.app
```

2. Verifica que el frontend llama al backend Railway y no directamente a Finnhub/FMP.
3. Comprueba login, registro y endpoints protegidos.
4. Abre Swagger y prueba un `POST /api/auth/login`.
5. Copia el token en Swagger Authorize y prueba un endpoint privado.
6. Comprueba `GET /actuator/health`.
7. Revisa logs de Railway para confirmar que no se muestran stacktraces al cliente ni secretos en respuestas.

## URLs esperadas

```text
Frontend:
https://TU-FRONTEND.vercel.app

Backend:
https://TU-BACKEND.up.railway.app

Swagger:
https://TU-BACKEND.up.railway.app/swagger-ui/index.html

OpenAPI JSON:
https://TU-BACKEND.up.railway.app/v3/api-docs
```
