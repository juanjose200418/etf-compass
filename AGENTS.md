# ETF Compass — Angular

## Stack
- **Angular 18** (standalone components, no NgModules)
- **Signals** para estado reactivo (`signal`, `computed`)
- **TypeScript 5.5**
- **CSS** puro (sin librerías)

## Estructura del proyecto

```
src/
├── index.html                # Entry point HTML
├── main.ts                   # Bootstrap de Angular
├── styles.css                # Estilos globales (responsive, animaciones)
├── app/
│   ├── types.ts              # Interface: ETF
│   ├── popular-etfs.ts       # ~50 ETFs para búsqueda local instantánea
│   ├── yahoo-finance.service.ts  # API Yahoo Finance (chart, quote, fundamentals)
│   ├── etf.service.ts        # Estado global: señales, búsqueda local, favoritos
│   ├── app.component.ts      # Componente principal con toda la lógica
│   ├── app.component.html    # Template completo
│   └── app.component.css     # Estilos del componente (SVG chart, tabla)
├── proxy.conf.json           # Proxy para evitar CORS con Yahoo Finance
├── proxy.js                  # Alternativa crumb-based (no activa)
├── proxy-server.cjs          # Proxy intermedio con cookie de Yahoo (evita 429)
└── package.json              # start script con --proxy-config
```

## Arquitectura

### YahooFinanceService (`yahoo-finance.service.ts`)
- Cola de requests con `concatMap` + `timer(4s)` para evitar rate limiting
- Caché de 30 min para todos los endpoints
- `fetchCompleteETF(ticker)` — **fuente principal**: `v8/finance/chart` (siempre funciona)
  - Calcula performance 1Y/3Y/5Y desde precios históricos (adjclose)
  - Devuelve ETF con name, currency, performance
- `fetchQuote(tickers)` — mejora opcional via `v7/finance/quote` (puede fallar 429)
  - Aporta fundSize, mejora name/currency si v8 no trajo
- `fetchFundamentals(ticker)` — mejora opcional via `v10/finance/quoteSummary` (puede fallar)
  - Aporta TER, provider, assetClass, region, distributionPolicy
- `search(query)` — `v1/finance/search` (no usado en flujo principal; referencia)
- Sin reintentos — si falla, se ignora silenciosamente
- Inferencias: assetClass, region, distributionPolicy desde categoría Yahoo

### EtfService (`etf.service.ts`)
Servicio singleton con estado reactivo via Signals:
- `selectedETFs` — ETFs seleccionados para comparar (máx 4)
- `favorites` — tickers guardados en localStorage
- `searchQuery` / `searchResults` — búsqueda local sobre `popularETFs` (sin API calls)
- `isAddingETF` / `error` — estados de carga
- `selectETF(ticker)`:
  1. Llama a `fetchCompleteETF` (v8/chart) — el ETF aparece al instante con name, currency, performance
  2. En segundo plano: `fetchQuote` + `fetchFundamentals` para enriquecer datos (TER, fundSize, etc.)
- Helpers: `getETFColor`, `formatPerformance`, `formatFundSize`, `formatTER`, `formatRisk`

### AppComponent (`app.component.ts`)
- Inyecta `EtfService` via `inject()`
- Computed `bestValues` — mejores valores entre ETFs seleccionados
- Computed `comparisonRows` — filas de la tabla con highlight de mejores valores
- Computed `chartData` — líneas SVG con puntos 1Y/3Y/5Y + eje Y
- `getLinePoints()`, `getDots()`, `getZeroY()` — helpers SVG
- Handlers de búsqueda con teclado (flechas, Enter, Escape)
- `@HostListener('document:click')` para cerrar dropdown al hacer clic fuera

### Template (`app.component.html`)
- SVG `<polyline>` + `<circle>` + `<text>` para chart de performance
- Tabla de comparación con celdas destacadas en verde
- Cards de ETFs seleccionados con info básica
- Dropdown de búsqueda con resultados locales
- Sin FormsModule — inputs controlados via eventos

## Datos
- **No hay datos de mercado hardcodeados** — ni precios, ni performances, ni nombres reales
- `popular-etfs.ts` contiene ~50 tickers conocidos + nombres para búsqueda local instantánea
- La información de mercado se obtiene via API de Yahoo Finance (es.finance.yahoo.com)
- Fuente primaria: `v8/finance/chart/{ticker}?range=5y&interval=1mo` — devuelve meta (name, currency) + histórico de precios
- Mejora opcional: `v7/finance/quote?symbols=...` — datos básicos (fundSize, marketCap)
- Mejora opcional: `v10/finance/quoteSummary/{ticker}` — TER, assetProfile, performanceOverview
- Campos inferidos desde Yahoo: assetClass, region, distributionPolicy
- Campos no disponibles: indexTracked, riskLevel → se muestran como "—"

## Funcionalidades
| Feature | Implementación |
|---|---|
| Búsqueda de ETFs | Local sobre `popular-etfs.ts` (filtro instantáneo sin API). Enter para ticker directo |
| Comparación 2–4 ETFs | Cards de selección + tabla dinámica |
| Mejores valores destacados | Celda verde con ★ (TER, performance, tamaño) |
| Chart de performance | SVG `<polyline>` con círculos por período (1Y/3Y/5Y) |
| Watchlist | ★ en resultados, persistencia localStorage |
| Diseño responsive | 2 breakpoints (768px, 480px) |
| Navegación teclado | Flechas + Enter + Escape en búsqueda |
| Datos en vivo | Fetch desde Yahoo Finance API via proxy CORS |

## Comandos

```bash
npm start          # arranca proxy-server (puerto 3001) + Angular (puerto 4200)
npm run build      # build producción (output: dist/etf-compass/)
npm run watch      # build en modo watch
npm test           # ejecutar tests
```

## Notas clave
- El proyecto usa **Angular 18 standalone** — no hay `NgModule`.
- No requiere `FormsModule` — los controles se manejan con eventos nativos.
- Los estilos están en `src/styles.css` (globales) y `src/app/app.component.css` (componente).
- Favoritos guardados en localStorage key `etf-compass-favorites`.
- **Proxy CORS**: `proxy.conf.json` redirige `/api/yahoo` a `localhost:3001` (proxy-server).
  - `npm start` ya incluye `--proxy-config proxy.conf.json`.
- Si el proxy no está activo, la app muestra error: "Could not connect to Yahoo Finance."
- `proxy.js` (crumb-based) fue un experimento; no está activo porque Angular CLI no soporta el formato function-export con Vite.
- La app usa `popular-etfs.ts` (lista local) para resultados instantáneos de búsqueda.
- La app usa `popular-etfs.ts` (lista local) para resultados instantáneos de búsqueda.
- **v8/chart es la fuente principal** de datos — funciona siempre pero no trae TER, fundSize, assetClass.
- **v7/quote + v10/fundamentals** son mejoras opcionales en segundo plano — si fallan (429, crumb inválido), esos campos se muestran como "—".
- Cola de requests (4s entre llamadas) + caché de 30 min para no saturar Yahoo.
- Campos no disponibles via Yahoo: indexTracked, riskLevel → se muestran como "—".
- Disclaimer: datos ilustrativos, no es consejo financiero.
