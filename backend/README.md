# ETF Compass Backend

Spring Boot 3 backend for the Angular ETF Compass app. It adds authenticated users, portfolio management, broker CSV imports, ETF intelligence, and allocation/exposure analytics for a Getquin-like investment dashboard.

## Stack

- Java 21
- Spring Boot 3.3
- Spring Data JPA
- Spring Security 6 with stateless JWT
- PostgreSQL
- Flyway migrations
- Lombok
- Jakarta Validation
- Springdoc OpenAPI / Swagger UI

## Run Locally

1. Start PostgreSQL:

```bash
docker compose up -d
```

2. Start the API:

```bash
mvn spring-boot:run
```

3. Open Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Default database settings are in `src/main/resources/application.yml`:

```text
DB_URL=jdbc:postgresql://localhost:5432/etf_compass
DB_USERNAME=etf_compass
DB_PASSWORD=etf_compass
JWT_SECRET=replace-with-a-strong-production-secret
FMP_API_KEY=your-financial-modeling-prep-api-key
```

## Password Recovery Email

The forgot-password flow sends a 6-digit verification code by email. Configure SMTP before using it in a real environment.

### Google Gmail Setup

1. **Activa 2FA** en tu cuenta Google:  
   https://myaccount.google.com/security

2. **Crea una App Password**:  
   https://myaccount.google.com/apppasswords  
   Selecciona "Correo" y "Dispositivo" → genera una password de 16 letras.

3. **Configura las variables de entorno** (`.env` o export):

```bash
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=tuemail@gmail.com
export SMTP_PASSWORD=la-app-password-de-16-letras
export SMTP_FROM=tuemail@gmail.com
export SMTP_SMTP_AUTH=true
export SMTP_SMTP_STARTTLS=true
export PASSWORD_RESET_CODE_EXPIRATION_MINUTES=15
```

El código generado es de **6 dígitos numéricos** (100000–999999), se guarda hasheado con BCrypt y caduca según `PASSWORD_RESET_CODE_EXPIRATION_MINUTES` (default: 15 min).

Si `SMTP_HOST` no está configurado, el backend **loguea el código por consola** en vez de enviar un email real:

## Real ETF Metadata

For realistic geography, sector, industry, and company exposure, configure a Financial Modeling Prep API key:

```bash
export FMP_API_KEY=your_key
export MARKET_DATA_PROVIDER=fmp
export MARKET_DATA_ENRICH_ON_POSITION_CREATE=true
mvn spring-boot:run
```

When a manual ETF position is created, the backend tries to enrich the ETF with:

- ETF sector weightings
- ETF country weightings
- ETF holdings
- Company profile data for holdings, used to derive industry exposure

If no API key is configured, the app uses the local Flyway seed data. That seed data is useful for development, but it is not a substitute for a live holdings/allocation provider.

## Authentication Flow

- Register: `POST /api/auth/register`
- Login: `POST /api/auth/login`
- Current user: `GET /api/auth/me`
- Use the returned token as `Authorization: Bearer <accessToken>` for all protected endpoints.
- Passwords are stored with BCrypt.
- Roles are persisted as `USER` and `ADMIN`; `/api/admin/**` is restricted to admins.

## Main API Surface

- ETF search: `GET /api/etfs?q=vwce`
- ETF details with holdings/exposures: `GET /api/etfs/{ticker}`
- ETF comparison: `GET /api/etfs/compare?tickers=VWCE&tickers=IWDA`
- Portfolio CRUD: `GET|POST|PUT|DELETE /api/portfolios`
- Manual positions: `POST /api/portfolios/{portfolioId}/positions`
- Position updates: `PUT|DELETE /api/portfolios/positions/{positionId}`
- Investment tracking: `GET /api/portfolios/{portfolioId}/transactions`, `POST /api/portfolios/positions/{positionId}/transactions`
- Portfolio analytics: `GET /api/portfolios/{portfolioId}/analytics`
- Broker list: `GET /api/imports/brokers`
- CSV import: `POST /api/portfolios/{portfolioId}/imports/csv?broker=TRADE_REPUBLIC`
- Dashboard: `GET /api/dashboard`

## CSV Import Format

The importer accepts common broker export column names. Required logical fields are:

```csv
ticker,name,quantity,averageCost,currentPrice,currency
VWCE,Vanguard FTSE All-World,12.5,102.40,128.40,EUR
```

Accepted aliases include `symbol`, `isin`, `shares`, `units`, `avgPrice`, `averagePrice`, `buyPrice`, `marketPrice`, and `price`. Direct broker APIs can be added later behind the same `ImportService` contract for Trade Republic, Interactive Brokers, Trading 212, Degiro, and Scalable Capital.

## Analytics Model

Portfolio analytics are calculated from positions and ETF metadata:

- Total portfolio value
- Total invested capital
- Profit/loss and profit/loss percentage
- ETF allocation
- Asset allocation
- Country exposure
- Sector exposure
- Industry exposure
- Currency exposure
- Top holdings exposure
- Best and worst performing positions

Flyway seeds several ETFs (`VWCE`, `IWDA`, `EIMI`, `VUSA`, `AGGH`, `IUSQ`, `CEBL`, `VGVF`, `EXXY`, `SPYD`, `AYEW`, `IUSN`, `NUKL`, `CBUK`) with realistic example exposure weights so the dashboard works immediately after migration. For production-grade accuracy, use `FMP_API_KEY` so holdings and allocations are refreshed from a market-data provider.

## Angular Integration

The Angular app now consumes this backend directly for ETF comparison and portfolio analytics:

- Configure the Angular proxy for `/api` so the browser talks only to Spring Boot.
- Use `GET /api/etfs/{ticker}` for on-demand ETF hydration from FMP plus holdings/exposure detail.
- Use `GET /api/etfs/compare?tickers=VWCE&tickers=IWDA` for lightweight comparison payloads.
- Use `GET /api/etfs/overlap?left=VWCE&right=IWDA` for holdings overlap calculated from persisted ETF constituents.
- Keep the auth service for protected portfolio endpoints; `GET /api/etfs/**` is public.
- Use `GET /api/dashboard` and `GET /api/portfolios/{id}/analytics` for the Getquin-style dashboard charts.
- Use `POST /api/portfolios/{id}/imports/csv` for Trade Republic or other broker export files until direct broker connectors are implemented.

The `EtfResponse` DTO intentionally exposes the same core fields used by the existing Angular `ETF` model: ticker, name, provider, TER, asset class, region, benchmark/index, distribution policy, fund size, currency, risk level, and 1Y/3Y/5Y performance.
