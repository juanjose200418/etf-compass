import { Injectable, signal, computed, inject } from '@angular/core';
import { finalize } from 'rxjs';
import { ETF } from './types';
import { EtfApiService } from './etf-api.service';
import { POPULAR_ETFS } from './popular-etfs';

@Injectable({ providedIn: 'root' })
export class EtfService {
  private etfApi = inject(EtfApiService);
  private readonly pendingTickers = new Set<string>();

  readonly selectedETFs = signal<ETF[]>([]);
  readonly favorites = signal<string[]>([]);
  readonly isAddingETF = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = signal<string | null>(null);

  readonly isMaxReached = () => this.selectedETFs().length >= 4;

  // ── Local search ──

  readonly popularETFs = POPULAR_ETFS;
  searchQuery = signal('');
  readonly searchResults = computed(() => {
    const q = this.searchQuery().trim().toLowerCase();
    if (q.length < 1) return [];
    return this.popularETFs.filter(
      etf => etf.ticker.toLowerCase().includes(q) || etf.name.toLowerCase().includes(q)
    ).slice(0, 20);
  });

  constructor() {
    this.loadFavorites();
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  clearSearchResults(): void {
    this.searchQuery.set('');
  }

  selectETF(ticker: string): void {
    const symbol = ticker.trim().toUpperCase();
    if (!symbol || this.isMaxReached() || this.selectedETFs().some(e => e.ticker === symbol) || this.pendingTickers.has(symbol)) return;

    this.pendingTickers.add(symbol);
    this.isAddingETF.set(true);
    this.error.set(null);
    this.notice.set(null);

    this.etfApi.getDetail(symbol).pipe(
      finalize(() => {
        this.pendingTickers.delete(symbol);
        this.isAddingETF.set(this.pendingTickers.size > 0);
      })
    ).subscribe({
      next: ({ etf, warnings }) => {
        this.selectedETFs.update(list => {
          if (list.length >= 4 || list.some(item => item.ticker === symbol)) {
            return list;
          }
          return [...list, etf];
        });
        this.notice.set(this.mergeWarnings(warnings));
      },
      error: err => {
        this.error.set(this.getLoadErrorMessage(symbol, err));
      }
    });
  }

  isTickerPending(ticker: string): boolean {
    return this.pendingTickers.has(ticker.trim().toUpperCase());
  }

  removeETF(ticker: string): void {
    this.selectedETFs.update(list => list.filter(e => e.ticker !== ticker));
  }

  clearSelection(): void {
    this.selectedETFs.set([]);
    this.notice.set(null);
  }

  toggleFavorite(ticker: string): void {
    this.favorites.update(list => {
      const idx = list.indexOf(ticker);
      return idx === -1 ? [...list, ticker] : list.filter(t => t !== ticker);
    });
    this.saveFavorites();
  }

  isFavorite(ticker: string): boolean {
    return this.favorites().includes(ticker);
  }

  private loadFavorites(): void {
    try {
      const saved = localStorage.getItem('etf-compass-favorites');
      if (saved) {
        const parsed = JSON.parse(saved) as string[];
        this.favorites.set(parsed);
      }
    } catch { }
  }

  private saveFavorites(): void {
    localStorage.setItem('etf-compass-favorites', JSON.stringify(this.favorites()));
  }

  getETFColor(ticker: string): string {
    const colors = ['#2563eb', '#dc2626', '#059669', '#d97706', '#7c3aed', '#db2777', '#0891b2', '#ea580c'];
    let hash = 0;
    for (let i = 0; i < ticker.length; i++) {
      hash = ticker.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
  }

  formatPerformance(val: number | null): string {
    if (val == null) return 'No disponible';
    return (val >= 0 ? '+' : '') + val.toFixed(1) + '%';
  }

  formatFundSize(val: number | null): string {
    if (val == null) return 'No disponible';
    return '$' + val.toFixed(1) + 'B';
  }

  formatTER(val: number | null): string {
    if (val == null) return 'No disponible';
    return val.toFixed(2) + '%';
  }

  formatRisk(val: number | null): string {
    if (val == null || val <= 0) return 'No disponible';
    return '●'.repeat(val) + '○'.repeat(5 - val);
  }

  formatText(val: string | null | undefined): string {
    return val && val.trim().length > 0 ? val : 'No disponible';
  }

  formatPrice(val: number | null, currency: string | null): string {
    if (val == null) return 'No disponible';
    const prefix = currency ? `${currency} ` : '';
    return `${prefix}${val.toFixed(2)}`;
  }

  formatDayChange(change: number | null, percent: number | null): string {
    if (change == null && percent == null) return 'No disponible';
    const parts: string[] = [];
    if (change != null) {
      parts.push(`${change >= 0 ? '+' : ''}${change.toFixed(2)}`);
    }
    if (percent != null) {
      parts.push(`(${percent >= 0 ? '+' : ''}${percent.toFixed(2)}%)`);
    }
    return parts.join(' ');
  }

  formatRange(low: number | null, high: number | null, currency: string | null): string {
    if (low == null || high == null) return 'No disponible';
    const prefix = currency ? `${currency} ` : '';
    return `${prefix}${low.toFixed(2)} - ${high.toFixed(2)}`;
  }

  private getLoadErrorMessage(ticker: string, error: unknown): string {
    const status = typeof error === 'object' && error && 'status' in error
      ? (error as { status?: number }).status
      : undefined;

    if (status === 400 || status === 404) {
      return `No se pudieron cargar datos para ${ticker}. Revisa el ticker e intentalo otra vez.`;
    }

    return `No se pudieron cargar datos del backend para ${ticker}.`;
  }

  private mergeWarnings(warnings: string[]): string | null {
    const current = this.notice();
    const entries = new Set<string>([
      ...(current ? current.split(' | ').map(item => item.trim()).filter(Boolean) : []),
      ...warnings.map(item => this.sanitizeWarning(item)).filter(Boolean)
    ]);

    return entries.size > 0 ? [...entries].join(' | ') : null;
  }

  private sanitizeWarning(message: string): string {
    const normalized = message.trim();
    if (!normalized) return '';
    if (/api key|failed:|finnhub|fmp|plan gratuito|subscription/i.test(normalized)) {
      return 'Algunos datos avanzados del ETF no estan disponibles ahora mismo.';
    }
    return normalized;
  }
}
