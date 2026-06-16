import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, OnInit, TemplateRef, ViewChild, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { Observable, Subscription, catchError, filter, forkJoin, map, of, startWith } from 'rxjs';
import { EtfApiService } from './etf-api.service';
import { EtfService } from './etf.service';
import { ETF } from './types';
import { AuthResult, AuthService } from './auth.service';
import { PortfolioApiService } from './portfolio-api.service';
import { AllocationSliceResponse, DashboardResponse, EtfHistoryApiResponse, PortfolioAnalyticsResponse, PortfolioResponse, PositionResponse } from './api.types';

interface BestValues {
  minTER: number | null; maxFundSize: number | null;
  max1Y: number | null; max3Y: number | null; max5Y: number | null;
}

interface ChartPoint {
  x: number; y: number; val: number; period: '1Y' | '3Y' | '5Y';
}

interface ChartLine {
  ticker: string; color: string; points: ChartPoint[];
}

interface ChartData {
  lines: ChartLine[];
  xPositions: Record<string, number>;
  yMin: number; yMax: number; range: number;
  minVal: number; maxVal: number;
}

interface ManualInvestmentRow {
  id: number;
  etf: string;
  value: string;
}

type AuthMode = 'login' | 'register' | 'recover';
type PasswordResetStep = 'request' | 'confirm';
type AnalyticsSectionKey = 'industry' | 'sector' | 'country' | 'etf';
type HistoryRange = 'MAX' | '5Y' | '1Y' | '6M' | '3M' | '1M' | '1W' | '1D';

interface HistorySeriesPoint {
  date: string;
  close: number;
  percent: number;
  timestamp: number;
}

interface HistorySeries {
  ticker: string;
  name: string;
  color: string;
  startClose: number;
  latestClose: number;
  changePercent: number;
  points: HistorySeriesPoint[];
}

interface HistoryChartPoint extends HistorySeriesPoint {
  x: number;
  y: number;
  ticker: string;
  color: string;
}

interface HistoryChartLine {
  ticker: string;
  color: string;
  points: HistoryChartPoint[];
}

interface HistoryChartData {
  lines: HistoryChartLine[];
  minPercent: number;
  maxPercent: number;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit {
  private static readonly ANALYTICS_COLLAPSED_ROWS = 8;

  @ViewChild('etfExplorerTemplate', { static: true }) etfExplorerTemplate!: TemplateRef<unknown>;

  service = inject(EtfService);
  auth = inject(AuthService);
  private etfApi = inject(EtfApiService);
  private portfolioApi = inject(PortfolioApiService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  readonly periods = ['1Y', '3Y', '5Y'] as const;
  readonly historyRanges: HistoryRange[] = ['MAX', '5Y', '1Y', '6M', '3M', '1M', '1W', '1D'];
  readonly periodLabelMap: Record<'1Y' | '3Y' | '5Y', string> = {
    '1Y': '1 Year',
    '3Y': '3 Years',
    '5Y': '5 Years'
  };
  readonly MAX_COMPARE = 4;

  searchOpen = false;
  activeSearchIndex = -1;
  showPromo = false;

  authMode: AuthMode = 'login';
  authEmail = '';
  authPassword = '';
  authDisplayName = '';
  authResetCode = '';
  authResetPassword = '';
  authResetStep: PasswordResetStep = 'request';
  authLoading = false;
  authError: string | null = null;
  authMessage: string | null = null;
  authPasswordVisible = false;
  authResetPasswordVisible = false;
  analyticsExpanded: Record<AnalyticsSectionKey, boolean> = {
    industry: false,
    sector: false,
    country: false,
    etf: false
  };
  focusedSectionId: string | null = null;

  portfolioName = 'Mi cartera ETF';
  portfolioCurrency = 'EUR';
  importMessage: string | null = null;
  importError: string | null = null;
  manualRowsNotice: string | null = null;
  portfolioError: string | null = null;
  portfolioLoading = false;
  importLoading = false;

  readonly portfolios = signal<PortfolioResponse[]>([]);
  readonly activePortfolioId = signal<string | null>(null);
  readonly dashboard = signal<DashboardResponse | null>(null);
  readonly analytics = signal<PortfolioAnalyticsResponse | null>(null);
  readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.router.url),
      startWith(this.router.url)
    ),
    { initialValue: this.router.url }
  );
  readonly isAuthenticated = computed(() => this.auth.isAuthenticated());
  readonly currentUser = computed(() => this.auth.user());
  readonly isDashboardRoute = computed(() => this.currentUrl() === '/dashboard');
  readonly activePortfolio = computed(() => this.portfolios().find(p => p.id === this.activePortfolioId()) ?? null);
  readonly selectedTickers = computed(() => new Set(this.service.selectedETFs().map(etf => etf.ticker)));
  readonly comparisonHistoryRange = signal<HistoryRange>('1Y');
  readonly comparisonHistorySeries = signal<HistorySeries[]>([]);
  readonly comparisonHistoryWarnings = signal<string[]>([]);
  readonly comparisonHistoryLoading = signal(false);
  readonly comparisonHistoryError = signal<string | null>(null);
  readonly manualRows = signal<ManualInvestmentRow[]>([
    { id: 1, etf: '', value: '' }
  ]);
  private nextManualRowId = 2;
  private historyRequestId = 0;
  private historySubscription: Subscription | null = null;

  // ── Computed best values ──

  private bestValues = computed<BestValues | null>(() => {
    const etfs = this.service.selectedETFs();
    if (etfs.length < 2) return null;

    const numericValues = (values: Array<number | null>) => values.filter((value): value is number => value != null);
    const terValues = numericValues(etfs.map(e => e.ter));
    const fundSizeValues = numericValues(etfs.map(e => e.fundSize));
    const performanceValues = (period: '1Y' | '3Y' | '5Y') => numericValues(etfs.map(e => e.performance[period]));

    return {
      minTER: terValues.length > 0 ? Math.min(...terValues) : null,
      maxFundSize: fundSizeValues.length > 0 ? Math.max(...fundSizeValues) : null,
      max1Y: performanceValues('1Y').length > 0 ? Math.max(...performanceValues('1Y')) : null,
      max3Y: performanceValues('3Y').length > 0 ? Math.max(...performanceValues('3Y')) : null,
      max5Y: performanceValues('5Y').length > 0 ? Math.max(...performanceValues('5Y')) : null
    };
  });

  readonly chartPeriods = computed(() => {
    const etfs = this.service.selectedETFs();
    return this.periods.filter(period => etfs.some(etf => etf.performance[period] != null));
  });

  // ── Comparison rows ──

  readonly comparisonRows = computed(() => {
    const etfs = this.service.selectedETFs();
    const best = this.bestValues();
    if (etfs.length < 2 || !best) return [];

    const defs: {
      label: string;
      getValue: (e: ETF) => string;
      bestKey?: string;
      perfClass?: (e: ETF) => string;
    }[] = [
      { label: 'Name', getValue: e => e.name },
      { label: 'Ticker', getValue: e => e.ticker },
      { label: 'Provider', getValue: e => this.service.formatText(e.provider) },
      { label: 'TER (Expense Ratio)', getValue: e => this.service.formatTER(e.ter), bestKey: 'ter' },
      { label: 'Asset Class', getValue: e => this.service.formatText(e.assetClass) },
      { label: 'Region', getValue: e => this.service.formatText(e.region) },
      { label: 'Index Tracked', getValue: e => this.service.formatText(e.indexTracked) },
      { label: 'Distribution', getValue: e => this.service.formatText(e.distributionPolicy) },
      { label: 'Fund Size', getValue: e => this.service.formatFundSize(e.fundSize), bestKey: 'fundSize' },
      { label: 'Currency', getValue: e => this.service.formatText(e.currency) },
      { label: 'Risk Level', getValue: e => this.service.formatRisk(e.riskLevel) },
      { label: 'Current Price', getValue: e => this.service.formatPrice(e.quote.currentPrice, e.currency) },
      { label: 'Day Change', getValue: e => this.service.formatDayChange(e.quote.change, e.quote.changePercent), perfClass: e => e.quote.change == null ? '' : e.quote.change >= 0 ? 'perf-positive' : 'perf-negative' },
      { label: 'Day Range', getValue: e => this.service.formatRange(e.quote.dayLow, e.quote.dayHigh, e.currency) },
      { label: 'Open', getValue: e => this.service.formatPrice(e.quote.open, e.currency) },
      { label: 'Previous Close', getValue: e => this.service.formatPrice(e.quote.previousClose, e.currency) },
      { label: '1Y Performance', getValue: e => this.service.formatPerformance(e.performance['1Y']), bestKey: '1Y', perfClass: e => e.performance['1Y'] == null ? '' : e.performance['1Y'] >= 0 ? 'perf-positive' : 'perf-negative' },
      { label: '3Y Performance', getValue: e => this.service.formatPerformance(e.performance['3Y']), bestKey: '3Y', perfClass: e => e.performance['3Y'] == null ? '' : e.performance['3Y'] >= 0 ? 'perf-positive' : 'perf-negative' },
      { label: '5Y Performance', getValue: e => this.service.formatPerformance(e.performance['5Y']), bestKey: '5Y', perfClass: e => e.performance['5Y'] == null ? '' : e.performance['5Y'] >= 0 ? 'perf-positive' : 'perf-negative' },
    ];

    return defs.map(def => ({
      label: def.label,
      values: etfs.map(etf => ({
        text: def.getValue(etf),
        isBest: def.bestKey ? this.isBestValue(etf, best, def.bestKey) : false,
        perfClass: def.perfClass?.(etf) ?? ''
      }))
    }));
  });

  // ── SVG Line Chart ──

  readonly viewBox = '0 0 500 280';

  readonly chartData = computed<ChartData | null>(() => {
    const etfs = this.service.selectedETFs();
    if (etfs.length < 2) return null;

    const activePeriods = this.chartPeriods();
    if (activePeriods.length === 0) return null;

    const allVals = etfs.flatMap(e => activePeriods.map(period => e.performance[period]).filter((value): value is number => value != null));
    if (allVals.length === 0) return null;

    const maxVal = Math.max(...allVals);
    const minVal = Math.min(...allVals);
    const range = maxVal - minVal || 1;
    const pad = range * 0.15;

    const xPositions = this.getChartXPositions(activePeriods);
    const yTop = 25;
    const yBot = 235;

    const lines: ChartLine[] = etfs.map(etf => ({
      ticker: etf.ticker,
      color: this.service.getETFColor(etf.ticker),
      points: activePeriods.flatMap(period => {
        const val = etf.performance[period];
        if (val == null) return [];
        const x = xPositions[period];
        const y = yTop + ((maxVal + pad - val) / (range + 2 * pad)) * (yBot - yTop);
        return [{ x, y, val, period }];
      })
    }));

    return { lines, xPositions, yMin: yTop, yMax: yBot, range, minVal, maxVal };
  });

  readonly yAxisLabels = computed(() => {
    const data = this.chartData();
    if (!data) return [];
    const { minVal, maxVal } = data;
    const step = Math.max((maxVal - minVal) / 4, 1);
    const labels: { value: number; y: number }[] = [];
    for (let i = 0; i <= 4; i++) {
      const value = minVal + step * i;
      const y = data.yMin + ((maxVal - value) / (maxVal - minVal || 1)) * (data.yMax - data.yMin);
      labels.push({ value: Math.round(value * 10) / 10, y });
    }
    return labels;
  });

  readonly chartDots = computed(() => {
    const data = this.chartData();
    return data ? this.buildDots(data.lines) : [];
  });

  readonly zeroLineY = computed(() => {
    const data = this.chartData();
    if (!data || data.minVal >= 0 || data.maxVal <= 0) {
      return null;
    }

    const range = data.maxVal - data.minVal || 1;
    return data.yMin + ((data.maxVal - 0) / range) * (data.yMax - data.yMin);
  });

  private readonly moneyFormatters = new Map<string, Intl.NumberFormat>();

  readonly historyComparisonRows = computed(() => this.comparisonHistorySeries().map(series => ({
    ticker: series.ticker,
    startClose: series.startClose,
    latestClose: series.latestClose,
    changePercent: series.changePercent,
    color: series.color,
    points: series.points.length
  })));

  readonly historyChartData = computed<HistoryChartData | null>(() => {
    const series = this.comparisonHistorySeries();
    if (series.length === 0) return null;

    const allValues = series.flatMap(item => item.points.map(point => point.percent));
    if (allValues.length === 0) return null;

    const minPercent = Math.min(...allValues);
    const maxPercent = Math.max(...allValues);
    const spread = maxPercent - minPercent || 1;
    const pad = spread * 0.1;
    const allTimestamps = series.flatMap(item => item.points.map(point => point.timestamp));
    const minTs = Math.min(...allTimestamps);
    const maxTs = Math.max(...allTimestamps);
    const tsRange = maxTs - minTs || 1;
    const xStart = 68;
    const xEnd = 470;
    const yStart = 28;
    const yEnd = 232;

    const lines = series.map(item => ({
      ticker: item.ticker,
      color: item.color,
      points: item.points.map(point => ({
        ...point,
        ticker: item.ticker,
        color: item.color,
        x: xStart + ((point.timestamp - minTs) / tsRange) * (xEnd - xStart),
        y: yStart + ((maxPercent + pad - point.percent) / (spread + pad * 2)) * (yEnd - yStart)
      }))
    }));

    return { lines, minPercent, maxPercent };
  });

  readonly historyXAxisLabels = computed(() => {
    const series = this.comparisonHistorySeries();
    if (series.length === 0) return [];
    const timestamps = series.flatMap(item => item.points.map(point => point.timestamp));
    if (timestamps.length === 0) return [];
    const minTs = Math.min(...timestamps);
    const maxTs = Math.max(...timestamps);
    const total = 4;
    return Array.from({ length: total }, (_, index) => {
      const ratio = index / (total - 1);
      const current = minTs + (maxTs - minTs) * ratio;
      const x = 68 + ratio * (470 - 68);
      return {
        x,
        label: new Intl.DateTimeFormat('es-ES', { day: '2-digit', month: 'short' }).format(new Date(current))
      };
    });
  });

  readonly historyYAxisLabels = computed(() => {
    const data = this.historyChartData();
    if (!data) return [];
    const total = 5;
    const spread = data.maxPercent - data.minPercent || 1;
    return Array.from({ length: total }, (_, index) => {
      const ratio = index / (total - 1);
      const value = data.maxPercent - spread * ratio;
      const y = 28 + ratio * (232 - 28);
      return { value: Math.round(value * 10) / 10, y };
    });
  });

  readonly historyChartDots = computed(() => {
    const data = this.historyChartData();
    return data ? data.lines.flatMap(line => line.points.map(point => ({ ...point, color: line.color }))) : [];
  });

  ngOnInit(): void {
    if (this.isDashboardRoute() && this.isAuthenticated()) {
      this.refreshPortfolioData();
    }

    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe(() => {
      if (this.isDashboardRoute() && this.isAuthenticated() && this.portfolios().length === 0 && !this.portfolioLoading) {
        this.refreshPortfolioData();
      }
    });

    effect(() => {
      const selected = this.service.selectedETFs().map(etf => ({ ticker: etf.ticker, name: etf.name }));
      const range = this.comparisonHistoryRange();
      this.loadComparisonHistory(selected, range);
    });
  }

  setAuthMode(mode: AuthMode): void {
    this.authMode = mode;
    this.clearAuthFeedback();
    this.authPasswordVisible = false;
    this.authResetPasswordVisible = false;
    if (mode !== 'recover') {
      this.authResetStep = 'request';
      this.authResetCode = '';
      this.authResetPassword = '';
    }
  }

  submitAuth(): void {
    if (this.authMode === 'recover') {
      if (this.authResetStep === 'request') {
        this.requestPasswordResetCode();
      } else {
        this.confirmPasswordReset();
      }
      return;
    }

    const authValidationError = this.validateAuthSubmission();
    if (authValidationError) {
      this.authError = authValidationError;
      this.authMessage = null;
      return;
    }

    this.authLoading = true;
    this.clearAuthFeedback();
    const normalizedEmail = this.authEmail.trim();
    const request$: Observable<AuthResult> = this.authMode === 'login'
      ? this.auth.login(normalizedEmail, this.authPassword)
      : this.auth.register(normalizedEmail, this.authPassword, this.buildRegisterDisplayName(normalizedEmail));

    request$.subscribe({
      next: ({ authenticated }) => {
        this.authLoading = false;
        this.authPassword = '';

        if (!authenticated && this.authMode === 'register') {
          this.authDisplayName = '';
          this.authMode = 'login';
          this.authMessage = 'Cuenta creada correctamente. Ahora inicia sesion con tu email y password.';
          return;
        }

        this.authMessage = null;
        this.router.navigate(['/dashboard']).then(() => this.refreshPortfolioData());
      },
      error: (err: unknown) => {
        this.authLoading = false;
        this.authError = this.errorMessage(
          err,
          this.authMode === 'login'
            ? 'No se pudo iniciar sesion. Revisa tus credenciales.'
            : 'No se pudo crear la cuenta. Revisa email, nombre y password.'
        );
      }
    });
  }

  requestPasswordResetCode(): void {
    if (!this.authEmail.trim()) {
      this.authError = 'Escribe tu email para enviarte el codigo de recuperacion.';
      return;
    }

    this.authLoading = true;
    this.clearAuthFeedback();
    this.auth.requestPasswordResetCode(this.authEmail.trim()).subscribe({
      next: () => {
        this.authLoading = false;
        this.authResetStep = 'confirm';
        this.authResetCode = '';
        this.authResetPassword = '';
        this.authMessage = 'Si existe una cuenta con ese email, te hemos enviado un codigo de 6 digitos.';
      },
      error: err => {
        this.authLoading = false;
        this.authError = this.errorMessage(err, 'No se pudo enviar el codigo de recuperacion.');
      }
    });
  }

  confirmPasswordReset(): void {
    if (!this.authEmail.trim() || !this.authResetCode.trim() || !this.authResetPassword.trim()) {
      this.authError = 'Completa email, codigo y nueva password para continuar.';
      return;
    }

    this.authLoading = true;
    this.clearAuthFeedback();
    this.auth.resetPasswordWithCode(this.authEmail.trim(), this.authResetCode.trim(), this.authResetPassword).subscribe({
      next: () => {
        this.authLoading = false;
        this.authMode = 'login';
        this.authResetStep = 'request';
        this.authResetCode = '';
        this.authResetPassword = '';
        this.authPassword = '';
        this.authMessage = 'Contrasena actualizada. Ya puedes iniciar sesion con la nueva password.';
      },
      error: err => {
        this.authLoading = false;
        this.authError = this.errorMessage(err, 'No se pudo restablecer la password. Revisa el codigo e intentalo otra vez.');
      }
    });
  }

  toggleAuthPasswordVisibility(): void {
    this.authPasswordVisible = !this.authPasswordVisible;
  }

  toggleResetPasswordVisibility(): void {
    this.authResetPasswordVisible = !this.authResetPasswordVisible;
  }

  logout(): void {
    this.auth.logout();
    this.portfolios.set([]);
    this.activePortfolioId.set(null);
    this.dashboard.set(null);
    this.analytics.set(null);
    this.router.navigate(['/']);
  }

  refreshPortfolioData(): void {
    if (!this.isAuthenticated() || !this.isDashboardRoute()) {
      this.portfolioLoading = false;
      return;
    }

    this.portfolioLoading = true;
    this.portfolioError = null;
    this.portfolioApi.listPortfolios().subscribe({
      next: portfolios => {
        this.portfolios.set(portfolios);
        const activePortfolioId = this.activePortfolioId();
        const nextActivePortfolio = portfolios.find(portfolio => portfolio.id === activePortfolioId) ?? portfolios[0] ?? null;
        this.activePortfolioId.set(nextActivePortfolio?.id ?? null);
        this.syncManualRowsFromPortfolio(nextActivePortfolio);
        if (nextActivePortfolio) {
          this.portfolioName = nextActivePortfolio.name;
          this.portfolioCurrency = nextActivePortfolio.baseCurrency;
        }
        this.loadDashboard();
        this.loadAnalytics();
        this.portfolioLoading = false;
      },
      error: err => this.handleProtectedError(err, 'No se pudieron cargar tus carteras.')
    });
  }

  createPortfolio(): void {
    this.portfolioLoading = true;
    this.portfolioError = null;
    this.portfolioApi.createPortfolio(this.portfolioName.trim() || 'Mi cartera ETF', this.portfolioCurrency.trim().toUpperCase() || 'EUR').subscribe({
      next: portfolio => {
        this.portfolios.update(list => [portfolio, ...list]);
        this.activePortfolioId.set(portfolio.id);
        this.syncManualRowsFromPortfolio(portfolio);
        this.portfolioLoading = false;
        this.loadDashboard();
        this.loadAnalytics();
      },
      error: err => this.handleProtectedError(err, 'No se pudo crear la cartera.')
    });
  }

  deletePortfolio(portfolioId: string, event?: Event): void {
    event?.stopPropagation();
    const portfolio = this.portfolios().find(item => item.id === portfolioId);
    const name = portfolio?.name ?? 'esta cartera';
    if (!confirm(`Eliminar ${name}? Esta accion no se puede deshacer.`)) {
      return;
    }

    this.portfolioLoading = true;
    this.portfolioApi.deletePortfolio(portfolioId).subscribe({
      next: () => {
        const remaining = this.portfolios().filter(item => item.id !== portfolioId);
        this.portfolios.set(remaining);
        if (this.activePortfolioId() === portfolioId) {
          this.activePortfolioId.set(remaining[0]?.id ?? null);
        }
        this.syncManualRowsFromPortfolio(remaining.find(item => item.id === this.activePortfolioId()) ?? remaining[0] ?? null);
        this.portfolioLoading = false;
        this.loadDashboard();
        this.loadAnalytics();
      },
      error: err => this.handleProtectedError(err, 'No se pudo eliminar la cartera.')
    });
  }

  selectPortfolio(portfolioId: string): void {
    this.activePortfolioId.set(portfolioId);
    this.syncManualRowsFromPortfolio(this.portfolios().find(portfolio => portfolio.id === portfolioId) ?? null);
    this.loadAnalytics();
  }

  addManualRow(): void {
    this.manualRows.update(rows => [...rows, { id: this.nextManualRowId++, etf: '', value: '' }]);
  }

  removeManualRow(id: number): void {
    this.manualRows.update(rows => rows.length <= 1 ? rows : rows.filter(row => row.id !== id));
  }

  updateManualRow(id: number, field: 'etf' | 'value', value: string): void {
    this.manualRows.update(rows => rows.map(row => row.id === id ? { ...row, [field]: value } : row));
    this.manualRowsNotice = null;
    this.importMessage = null;
    this.importError = null;
  }

  importManualInvestments(): void {
    const portfolioId = this.activePortfolioId();
    const currency = this.activePortfolio()?.baseCurrency || this.portfolioCurrency || 'EUR';
    const rows = this.manualRows()
      .map(row => ({ etf: row.etf.trim(), value: Number(row.value.replace(',', '.')) }))
      .filter(row => row.etf.length > 0 && Number.isFinite(row.value) && row.value > 0);

    if (!portfolioId || rows.length === 0) {
      this.importError = 'Crea o selecciona una cartera y escribe al menos un ETF con un valor mayor que 0.';
      return;
    }
    this.importLoading = true;
    this.importMessage = null;
    this.importError = null;
    this.portfolioApi.addManualPositions(portfolioId, rows.map(row => ({ ticker: row.etf, value: row.value, currency }))).subscribe({
      next: createdPositions => {
        this.importLoading = false;
        this.importMessage = `Añadidas ${createdPositions.length} inversiones ETF a la cartera.`;
        this.refreshPortfolioData();
      },
      error: err => {
        this.importLoading = false;
        this.importError = this.errorMessage(err, 'No se pudieron añadir las inversiones. Revisa los ETFs y valores.');
      }
    });
  }

  loadDashboard(): void {
    if (!this.isAuthenticated() || !this.isDashboardRoute()) {
      this.dashboard.set(null);
      return;
    }

    this.portfolioApi.dashboard().subscribe({
      next: dashboard => this.dashboard.set(dashboard),
      error: err => this.handleProtectedError(err, 'No se pudo cargar el dashboard.')
    });
  }

  loadAnalytics(): void {
    if (!this.isAuthenticated() || !this.isDashboardRoute()) {
      this.analytics.set(null);
      return;
    }

    const portfolioId = this.activePortfolioId();
    if (!portfolioId) {
      this.analytics.set(null);
      return;
    }
    this.portfolioApi.analytics(portfolioId).subscribe({
      next: analytics => this.analytics.set(analytics),
      error: err => this.handleProtectedError(err, 'No se pudo cargar el analisis de la cartera.')
    });
  }

  formatMoney(value: number | null | undefined, currency = 'EUR'): string {
    const normalizedCurrency = currency.trim().toUpperCase() || 'EUR';
    let formatter = this.moneyFormatters.get(normalizedCurrency);
    if (!formatter) {
      formatter = new Intl.NumberFormat('es-ES', { style: 'currency', currency: normalizedCurrency, maximumFractionDigits: 0 });
      this.moneyFormatters.set(normalizedCurrency, formatter);
    }
    return formatter.format(value ?? 0);
  }

  formatPercentValue(value: number | null | undefined): string {
    const val = value ?? 0;
    return `${val >= 0 ? '+' : ''}${val.toFixed(2)}%`;
  }

  visibleSlices(section: AnalyticsSectionKey, slices: AllocationSliceResponse[] | null | undefined): AllocationSliceResponse[] {
    const items = section === 'sector' ? this.equitySectorSlices(slices) : this.classifiedSlices(slices);
    return this.analyticsExpanded[section] ? items : items.slice(0, AppComponent.ANALYTICS_COLLAPSED_ROWS);
  }

  classifiedSlices(slices: AllocationSliceResponse[] | null | undefined): AllocationSliceResponse[] {
    return (slices ?? []).filter(item => !this.isUnclassifiedLabel(item.label));
  }

  unclassifiedSlices(slices: AllocationSliceResponse[] | null | undefined): AllocationSliceResponse[] {
    return (slices ?? []).filter(item => this.isUnclassifiedLabel(item.label));
  }

  commoditySectorSlices(slices: AllocationSliceResponse[] | null | undefined): AllocationSliceResponse[] {
    return this.classifiedSlices(slices).filter(item => this.isCommodityLabel(item.label));
  }

  equitySectorSlices(slices: AllocationSliceResponse[] | null | undefined): AllocationSliceResponse[] {
    return this.classifiedSlices(slices).filter(item => !this.isCommodityLabel(item.label));
  }

  barWidth(percentage: number | null | undefined): string {
    return `${Math.max(2, Math.min(100, percentage ?? 0))}%`;
  }

  toggleAnalyticsSection(section: AnalyticsSectionKey): void {
    this.analyticsExpanded = {
      ...this.analyticsExpanded,
      [section]: !this.analyticsExpanded[section]
    };
  }

  shouldShowAnalyticsToggle(slices: AllocationSliceResponse[] | null | undefined): boolean {
    return this.classifiedSlices(slices).length > AppComponent.ANALYTICS_COLLAPSED_ROWS;
  }

  analyticsSectionCount(slices: AllocationSliceResponse[] | null | undefined): number {
    return this.classifiedSlices(slices).length;
  }

  populatedManualRowCount(): number {
    return this.manualRows().filter(row => row.etf.trim().length > 0).length;
  }

  focusSection(sectionId: string): void {
    this.focusedSectionId = sectionId;
    if (typeof document !== 'undefined') {
      document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
    if (typeof window !== 'undefined') {
      window.setTimeout(() => {
        if (this.focusedSectionId === sectionId) {
          this.focusedSectionId = null;
        }
      }, 1800);
    }
  }

  isFocusedSection(sectionId: string): boolean {
    return this.focusedSectionId === sectionId;
  }

  formatAllocationLabel(label: string, section: AnalyticsSectionKey): string {
    if (this.isUnclassifiedLabel(label)) {
      return this.unclassifiedLabel(section);
    }
    return label;
  }

  allocationTooltip(label: string, section: AnalyticsSectionKey): string {
    if (this.isUnclassifiedLabel(label)) {
      return `${this.unclassifiedLabel(section)}. This percentage includes holdings that could not be classified because provider metadata was incomplete or only a partial holdings snapshot was available.`;
    }
    return label;
  }

  isUnclassifiedLabel(label: string | null | undefined): boolean {
    const normalized = label?.trim().toLowerCase() ?? '';
    return normalized.startsWith('unclassified')
      || normalized === 'unknown'
      || normalized === 'other'
      || normalized === 'other industries'
      || normalized === 'other geography'
      || normalized === 'other commodities';
  }

  isCommodityLabel(label: string | null | undefined): boolean {
    const normalized = label?.trim().toLowerCase() ?? '';
    return normalized === 'energy commodities'
      || normalized === 'agriculture'
      || normalized === 'industrial metals'
      || normalized === 'precious metals'
      || normalized === 'livestock';
  }

  hasSignificantSectorGap(analytics: PortfolioAnalyticsResponse | null | undefined): boolean {
    return (analytics?.sectorDataQuality?.unclassifiedPercentage ?? 0) > 10;
  }

  sectorCoverageWarning(analytics: PortfolioAnalyticsResponse | null | undefined): string | null {
    const unclassified = analytics?.sectorDataQuality?.unclassifiedPercentage ?? 0;
    if (unclassified <= 10) {
      return null;
    }
    return `Warning: sector exposure may be underestimated because ${unclassified.toFixed(1)}% of the portfolio could not be mapped precisely.`;
  }

  dataQualityWarning(label: string, quality: { unclassifiedPercentage: number } | null | undefined): string | null {
    const unclassified = quality?.unclassifiedPercentage ?? 0;
    if (unclassified <= 10) {
      return null;
    }
    return `Warning: ${label} exposure may be underestimated because ${unclassified.toFixed(1)}% of the portfolio could not be mapped precisely.`;
  }

  private unclassifiedLabel(section: AnalyticsSectionKey): string {
    switch (section) {
      case 'industry':
        return 'Unclassified industries';
      case 'sector':
        return 'Unclassified sectors';
      case 'country':
        return 'Unclassified geography';
      case 'etf':
        return 'Unclassified ETF allocation';
    }
  }

  trackByLabel(_index: number, item: AllocationSliceResponse): string {
    return item.label;
  }

  trackByManualRow(_index: number, row: ManualInvestmentRow): number {
    return row.id;
  }

  private handleProtectedError(err: unknown, fallback: string): void {
    this.portfolioLoading = false;
    const status = (err as { status?: number }).status;
    if (status === 401 || status === 403) {
      this.logout();
      this.authError = 'Tu sesion ha caducado. Inicia sesion de nuevo.';
      return;
    }
    this.portfolioError = this.errorMessage(err, fallback);
  }

  private errorMessage(err: unknown, fallback: string): string {
    if (err instanceof Error && err.message === 'AUTH_RESPONSE_INVALID') {
      return 'La respuesta de autenticacion no incluye un token valido.';
    }

    const apiMessage = (err as { error?: { message?: string } }).error?.message;
    if (!apiMessage) {
      return fallback;
    }

    switch (apiMessage) {
      case 'Email is already registered':
        return 'Este email ya está registrado. Inicia sesión o usa otro email.';
      case 'Invalid email or password':
      case 'Bad credentials':
        return 'Email o contraseña incorrectos.';
      case 'Authentication required':
      case 'Authentication required or token expired':
        return 'Necesitas iniciar sesión para continuar.';
      default:
        return apiMessage;
    }
  }

  private validateAuthSubmission(): string | null {
    const email = this.authEmail.trim();
    if (!email) {
      return 'Escribe tu email para continuar.';
    }

    if (!this.isValidEmail(email)) {
      return 'Escribe un email valido.';
    }

    if (this.authPassword.trim().length < 8) {
      return 'La password debe tener al menos 8 caracteres.';
    }

    if (this.authMode === 'register') {
      const displayName = this.authDisplayName.trim();
      if (displayName.length > 120) {
        return 'El nombre no puede superar los 120 caracteres.';
      }
    }

    return null;
  }

  private buildRegisterDisplayName(email: string): string {
    const typedName = this.authDisplayName.trim();
    if (typedName) {
      return typedName.slice(0, 120);
    }

    const fallbackFromEmail = email.split('@')[0]?.replace(/[._-]+/g, ' ').trim();
    return (fallbackFromEmail || 'Investor').slice(0, 120);
  }

  private isValidEmail(value: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  private clearAuthFeedback(): void {
    this.authError = null;
    this.authMessage = null;
  }

  private syncManualRowsFromPortfolio(portfolio: PortfolioResponse | null): void {
    if (!portfolio) {
      this.manualRows.set([{ id: 1, etf: '', value: '' }]);
      this.nextManualRowId = 2;
      this.manualRowsNotice = 'Crea o selecciona una cartera para cargar posiciones guardadas.';
      return;
    }

    if (portfolio.positions.length === 0) {
      this.manualRows.set([{ id: 1, etf: '', value: '' }]);
      this.nextManualRowId = 2;
      this.manualRowsNotice = 'Esta cartera no tiene posiciones guardadas todavia. Anade ETFs manualmente para empezar.';
      return;
    }

    const rows = portfolio.positions.map((position, index) => ({
      id: index + 1,
      etf: this.manualRowLabel(position),
      value: this.formatManualRowValue(position.investedCapital ?? position.currentValue)
    }));

    this.manualRows.set(rows);
    this.nextManualRowId = rows.length + 1;
    this.manualRowsNotice = null;
  }

  private manualRowLabel(position: PositionResponse): string {
    switch (position.ticker) {
      case 'IUSQ': return 'MSCI ACWI';
      case 'EEMA': return 'MSCI EM Asia';
      case 'VGVF': return 'FTSE Developed World';
      case 'ICOM': return 'Diversified Commodities';
      case 'SPYD': return 'S&P U.S. Dividend Aristocrats';
      case 'XDWT': return 'MSCI World Information Technology';
      case 'IUSN': return 'MSCI World Small Cap';
      case 'NUKL': return 'Uranium and Nuclear Technologies';
      case 'CQQQ': return 'MSCI China Tech';
      default: return position.ticker || position.name;
    }
  }

  private formatManualRowValue(value: number | null | undefined): string {
    if (value == null || !Number.isFinite(value)) {
      return '';
    }

    const rounded = Math.round(value * 100) / 100;
    return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
  }

  // ── SVG helpers ──

  getLinePoints(line: ChartLine): string {
    return line.points.map(p => `${p.x},${p.y}`).join(' ');
  }

  private buildDots(lines: ChartLine[]): (ChartPoint & { color: string })[] {
    return lines.flatMap(line => line.points.map(p => ({ ...p, color: line.color })));
  }

  // ── Best value check ──

  private isBestValue(etf: ETF, best: BestValues, key: string): boolean {
    switch (key) {
      case 'ter': return etf.ter != null && best.minTER != null && Math.abs(etf.ter - best.minTER) < 0.001;
      case 'fundSize': return etf.fundSize != null && best.maxFundSize != null && Math.abs(etf.fundSize - best.maxFundSize) < 0.01;
      case '1Y': return etf.performance['1Y'] != null && best.max1Y != null && Math.abs(etf.performance['1Y'] - best.max1Y) < 0.01;
      case '3Y': return etf.performance['3Y'] != null && best.max3Y != null && Math.abs(etf.performance['3Y'] - best.max3Y) < 0.01;
      case '5Y': return etf.performance['5Y'] != null && best.max5Y != null && Math.abs(etf.performance['5Y'] - best.max5Y) < 0.01;
      default: return false;
    }
  }

  private getChartXPositions(periods: readonly ('1Y' | '3Y' | '5Y')[]): Record<string, number> {
    if (periods.length === 1) {
      return { [periods[0]]: 250 };
    }
    if (periods.length === 2) {
      return { [periods[0]]: 160, [periods[1]]: 340 };
    }
    return { [periods[0]]: 120, [periods[1]]: 250, [periods[2]]: 380 };
  }

  // ── Search handlers ──

  onSearchInput(value: string): void {
    this.service.setSearchQuery(value);
    this.searchOpen = true;
    this.activeSearchIndex = -1;
  }

  onSearchFocus(): void {
    const q = this.service.searchQuery();
    if (q.trim().length > 0) this.searchOpen = true;
  }

  onSearchKeydown(event: KeyboardEvent): void {
    const input = event.target as HTMLInputElement;
    const results = this.service.searchResults();

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeSearchIndex = Math.min(this.activeSearchIndex + 1, results.length - 1);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeSearchIndex = Math.max(this.activeSearchIndex - 1, -1);
    } else if (event.key === 'Enter') {
      if (this.activeSearchIndex >= 0 && results[this.activeSearchIndex]) {
        const r = results[this.activeSearchIndex];
        this.service.selectETF(r.ticker);
        this.closeSearch();
        input.value = '';
      } else if (results.length === 0) {
        const ticker = input.value.trim().toUpperCase();
        if (ticker.length >= 1 && ticker.length <= 5) {
          this.service.selectETF(ticker);
          this.closeSearch();
          input.value = '';
        }
      }
    } else if (event.key === 'Escape') {
      this.closeSearch();
      input.blur();
    }
  }

  selectETF(ticker: string): void {
    this.service.selectETF(ticker);
    this.closeSearch();
  }

  closeSearch(): void {
    if (!this.searchOpen && this.activeSearchIndex === -1 && this.service.searchQuery().length === 0) {
      return;
    }
    this.searchOpen = false;
    this.activeSearchIndex = -1;
    this.service.clearSearchResults();
  }

  openPromo(): void {
    this.showPromo = true;
    document.documentElement.requestFullscreen?.().catch(() => undefined);
  }

  closePromo(): void {
    this.showPromo = false;
    if (document.fullscreenElement) {
      document.exitFullscreen?.().catch(() => undefined);
    }
  }

  setComparisonHistoryRange(range: HistoryRange): void {
    if (this.comparisonHistoryRange() === range) {
      return;
    }

    this.comparisonHistoryRange.set(range);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.searchOpen) {
      return;
    }

    const target = event.target as HTMLElement;
    if (!target.closest('.search-container')) {
      this.closeSearch();
    }
  }

  trackByTicker(_index: number, etf: ETF): string {
    return etf.ticker;
  }

  isSelected(ticker: string): boolean {
    return this.selectedTickers().has(ticker);
  }

  isMaxedOut(): boolean {
    return this.service.isMaxReached();
  }

  getHistoryLinePoints(line: HistoryChartLine): string {
    return line.points.map(point => `${point.x},${point.y}`).join(' ');
  }

  private loadComparisonHistory(selected: Array<{ ticker: string; name: string }>, range: HistoryRange): void {
    this.historySubscription?.unsubscribe();

    if (selected.length === 0) {
      this.comparisonHistorySeries.set([]);
      this.comparisonHistoryWarnings.set([]);
      this.comparisonHistoryError.set(null);
      this.comparisonHistoryLoading.set(false);
      return;
    }

    this.comparisonHistoryLoading.set(true);
    this.comparisonHistoryError.set(null);
    this.comparisonHistoryWarnings.set([]);
    const requestId = ++this.historyRequestId;

    this.historySubscription = forkJoin(
      selected.map(etf => this.etfApi.getHistory(etf.ticker, range).pipe(
        map(history => ({ etf, history })),
        catchError(err => of({ etf, error: this.errorMessage(err, `No se pudo cargar el historico de ${etf.ticker}.`) }))
      ))
    ).subscribe(results => {
      if (requestId !== this.historyRequestId) {
        return;
      }

      const warnings: string[] = [];
      const series: HistorySeries[] = [];

      for (const result of results) {
        if ('error' in result) {
          warnings.push(result.error);
          continue;
        }

        const mapped = this.mapHistorySeries(result.etf.ticker, result.etf.name, result.history);
        if (!mapped) {
          warnings.push(`No hay historico suficiente para ${result.etf.ticker} en ${range}.`);
          continue;
        }

        series.push(mapped);
      }

      this.comparisonHistorySeries.set(series);
      this.comparisonHistoryWarnings.set(warnings);
      this.comparisonHistoryError.set(series.length === 0 ? 'No hay historico disponible para los ETFs seleccionados en este rango.' : null);
      this.comparisonHistoryLoading.set(false);
    });
  }

  private mapHistorySeries(ticker: string, name: string, history: EtfHistoryApiResponse): HistorySeries | null {
    const sortedPoints = [...history.points]
        .map(point => ({
          date: point.date,
          close: point.close,
          timestamp: new Date(point.date).getTime()
        }))
        .filter(point => Number.isFinite(point.close) && point.close > 0 && Number.isFinite(point.timestamp))
        .sort((left, right) => left.timestamp - right.timestamp);

    if (sortedPoints.length < 2) {
      return null;
    }

    const startClose = sortedPoints[0].close;
    const reduced = this.reduceHistoryPoints(sortedPoints.map(point => ({
      ...point,
      percent: ((point.close - startClose) / startClose) * 100
    })));
    const latest = reduced[reduced.length - 1];

    return {
      ticker,
      name,
      color: this.service.getETFColor(ticker),
      startClose,
      latestClose: latest.close,
      changePercent: latest.percent,
      points: reduced
    };
  }

  private reduceHistoryPoints(points: HistorySeriesPoint[]): HistorySeriesPoint[] {
    const maxPoints = 120;
    if (points.length <= maxPoints) {
      return points;
    }

    const step = Math.ceil(points.length / maxPoints);
    return points.filter((_, index) => index === 0 || index === points.length - 1 || index % step === 0);
  }
}
