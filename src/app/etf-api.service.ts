import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { buildApiUrl } from './api-url';
import { ApiResponse, EtfDetailApiResponse, EtfHistoryApiResponse, EtfListApiResponse, EtfResponse, FinnhubEtfListItem } from './api.types';
import { ETF } from './types';

@Injectable({ providedIn: 'root' })
export class EtfApiService {
  private http = inject(HttpClient);

  search(query?: string) {
    const q = (query ?? '').trim();
    return this.http.get<ApiResponse<EtfResponse[]>>(buildApiUrl(`/etfs?q=${encodeURIComponent(q)}`)).pipe(
      map(res => res.data.map(etf => this.mapETF(etf)))
    );
  }

  listEtfs() {
    return this.http.get<ApiResponse<EtfListApiResponse>>(buildApiUrl('/market-data/etfs')).pipe(
      map(res => res.data)
    );
  }

  getDetail(ticker: string) {
    return this.http.get<ApiResponse<EtfDetailApiResponse>>(buildApiUrl(`/etfs/${encodeURIComponent(ticker)}`)).pipe(
      map(res => ({ etf: this.mapETF(res.data.etf), warnings: res.data.warnings ?? [] }))
    );
  }

  getHistory(ticker: string, range: string) {
    return this.http.get<ApiResponse<EtfHistoryApiResponse>>(buildApiUrl(`/etfs/${encodeURIComponent(ticker)}/history?range=${encodeURIComponent(range)}`)).pipe(
      map(res => res.data)
    );
  }

  private mapETF(etf: EtfResponse): ETF {
    return {
      ticker: etf.ticker,
      name: etf.name,
      provider: etf.provider ?? null,
      ter: etf.ter ?? null,
      assetClass: etf.assetClass ?? null,
      region: etf.region ?? null,
      indexTracked: etf.indexTracked ?? null,
      distributionPolicy: etf.distributionPolicy ?? null,
      fundSize: etf.fundSize ?? null,
      currency: etf.currency ?? null,
      riskLevel: etf.riskLevel ?? null,
      performance: {
        '1Y': etf.performance?.['1Y'] ?? null,
        '3Y': etf.performance?.['3Y'] ?? null,
        '5Y': etf.performance?.['5Y'] ?? null
      },
      quote: {
        currentPrice: etf.quote?.currentPrice ?? null,
        change: etf.quote?.change ?? null,
        changePercent: etf.quote?.changePercent ?? null,
        dayHigh: etf.quote?.dayHigh ?? null,
        dayLow: etf.quote?.dayLow ?? null,
        open: etf.quote?.open ?? null,
        previousClose: etf.quote?.previousClose ?? null
      }
    };
  }
}
