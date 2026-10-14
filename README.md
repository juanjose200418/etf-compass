# ETF Compass

ETF Compass is an Angular 18 frontend with a Spring Boot 3 backend. Local development uses fixed ports:

- Angular: `http://localhost:4200`
- Spring Boot API: `http://localhost:8080`
- Angular API calls: `/api/**`
- Local proxy: `/api/** -> http://localhost:8080`

## Prerequisites

- Java 21
- Maven installed on the machine; this repo does not include `mvnw`
- Node.js compatible with Angular 18 and npm
- Docker with `docker-compose` for the local PostgreSQL container
- No global Angular CLI is required; use the local CLI through `npm` scripts

## Install Dependencies

From the repository root:

```bash
npm install
```

Maven dependencies are downloaded automatically when running backend Maven commands.

## Environment

The backend requires `JWT_SECRET` before it starts. Generate it locally instead of committing it:

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
```

Optional market-data variables:

```bash
export MARKET_DATA_PROVIDER=finnhub
export FINNHUB_API_KEY="your-local-key"
```

Optional Swagger variables:

```bash
export SPRINGDOC_API_DOCS_ENABLED=true
export SPRINGDOC_SWAGGER_UI_ENABLED=true
```

Local defaults for database URL, CORS, mail, and frontend URL are in `backend/src/main/resources/application.yml`. Do not commit real API keys, JWT secrets, tokens, or production credentials.

## Run Locally

Start services in this order.

1. Start PostgreSQL:

```bash
cd backend
docker-compose up -d
```

2. Start the backend:

```bash
JWT_SECRET="$(openssl rand -base64 48)" mvn spring-boot:run
```

The backend listens on `http://localhost:8080` from `server.port: 8080`.

3. In another terminal, start the frontend:

```bash
# from the repository root
npm start
```

`npm start` runs `scripts/start-frontend.cjs`. It only uses `localhost:4200`:

- If `4200` is free, it starts Angular with `--host localhost --port 4200 --proxy-config proxy.conf.json`.
- If ETF Compass is already running on `4200`, it reuses that server and does not start a duplicate.
- If another process owns `4200`, it exits with a conflict message instead of switching to a random port.

Open the app at:

```text
http://localhost:4200
```

## URLs

- Frontend: `http://localhost:4200`
- API base: `http://localhost:8080/api`
- Health: `http://localhost:8080/actuator/health`
- Swagger UI, only if enabled with env vars: `http://localhost:8080/swagger-ui.html`

## Verify Servers

Check listening ports:

```bash
ss -ltnp '( sport = :4200 or sport = :8080 )'
```

Check the backend:

```bash
curl -i http://localhost:8080/api/etfs
```

Check the frontend and proxy together:

```bash
curl -i http://localhost:4200/
curl -i http://localhost:4200/api/etfs
```

Register and login endpoints are public through the proxy:

```text
POST http://localhost:4200/api/auth/register
POST http://localhost:4200/api/auth/login
```

## Build And Tests

Frontend production build:

```bash
npm run build
```

Frontend unit tests:

```bash
npm test
```

Backend verification:

```bash
cd backend
JWT_SECRET="$(openssl rand -base64 48)" mvn test
```

## Troubleshooting

Port `4200` is already in use:

- Run `ss -ltnp '( sport = :4200 )'`.
- If it is ETF Compass, reuse `http://localhost:4200`; `npm start` will not launch another server.
- If it is another process, stop that process before running `npm start`.
- Do not accept Angular CLI's random fallback ports; CORS is configured for `http://localhost:4200`.

Port `8080` is already in use:

- Run `ss -ltnp '( sport = :8080 )'`.
- Stop the conflicting process or the existing backend before starting Spring Boot.
- Do not change the backend port unless you also update `proxy.conf.json` and CORS settings.

Frontend cannot reach the backend:

- Confirm Spring Boot is running on `8080`.
- Confirm `proxy.conf.json` forwards `/api` to `http://localhost:8080`.
- Use `http://localhost:4200`, not a random Angular port, to avoid CORS failures.

Backend fails on startup with `JWT_SECRET must be configured`:

- Set `JWT_SECRET` in the shell before `mvn spring-boot:run`.
- The value must be present and at least 32 characters.

`/actuator/health` can be `DOWN`:

- This can happen when optional mail configuration is missing.
- Verify API availability with `GET /api/etfs` when diagnosing local startup.

## Stop Servers

- Stop Angular and Spring Boot with `Ctrl+C` in their terminals.
- Stop PostgreSQL from `backend/`:

```bash
docker-compose down
```

If a process was started in the background, find it first:

```bash
ss -ltnp '( sport = :4200 or sport = :8080 )'
```
