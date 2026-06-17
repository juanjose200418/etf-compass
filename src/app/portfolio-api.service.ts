import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { buildApiUrl } from './api-url';
import { ApiResponse, DashboardResponse, PortfolioAnalyticsResponse, PortfolioResponse, PositionResponse } from './api.types';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class PortfolioApiService {
  private http = inject(HttpClient);
  private auth = inject(AuthService);

  listPortfolios() {
    return this.http.get<ApiResponse<PortfolioResponse[]>>(buildApiUrl('/portfolios'), { headers: this.auth.authHeaders() }).pipe(
      map(res => res.data)
    );
  }

  createPortfolio(name: string, baseCurrency = 'EUR') {
    return this.http.post<ApiResponse<PortfolioResponse>>(buildApiUrl('/portfolios'), { name, baseCurrency }, { headers: this.auth.authHeaders() }).pipe(
      map(res => res.data)
    );
  }

  deletePortfolio(portfolioId: string) {
    return this.http.delete<void>(buildApiUrl(`/portfolios/${portfolioId}`), { headers: this.auth.authHeaders() });
  }

  deletePosition(positionId: string) {
    return this.http.delete<void>(buildApiUrl(`/portfolios/positions/${positionId}`), { headers: this.auth.authHeaders() });
  }

  updatePosition(positionId: string, ticker: string, value: number, currency: string) {
    return this.http.put<ApiResponse<PositionResponse>>(
      buildApiUrl(`/portfolios/positions/${positionId}`),
      this.buildManualPositionRequest(ticker, value, currency),
      { headers: this.auth.authHeaders() }
    ).pipe(map(res => res.data));
  }

  addManualPosition(portfolioId: string, ticker: string, value: number, currency = 'EUR') {
    return this.http.post<ApiResponse<unknown>>(
      buildApiUrl(`/portfolios/${portfolioId}/positions`),
      this.buildManualPositionRequest(ticker, value, currency),
      { headers: this.auth.authHeaders() }
    ).pipe(map(res => res.data));
  }

  addManualPositions(portfolioId: string, positions: Array<{ ticker: string; value: number; currency?: string }>) {
    return this.http.post<ApiResponse<PositionResponse[]>>(
      buildApiUrl(`/portfolios/${portfolioId}/positions/batch`),
      positions.map(position => this.buildManualPositionRequest(position.ticker, position.value, position.currency ?? 'EUR')),
      { headers: this.auth.authHeaders() }
    ).pipe(map(res => res.data));
  }

  dashboard() {
    return this.http.get<ApiResponse<DashboardResponse>>(buildApiUrl('/dashboard'), { headers: this.auth.authHeaders() }).pipe(
      map(res => res.data)
    );
  }

  analytics(portfolioId: string) {
    return this.http.get<ApiResponse<PortfolioAnalyticsResponse>>(buildApiUrl(`/portfolios/${portfolioId}/analytics`), { headers: this.auth.authHeaders() }).pipe(
      map(res => res.data)
    );
  }

  private buildManualPositionRequest(ticker: string, value: number, currency: string) {
    return {
      ticker,
      broker: 'MANUAL',
      quantity: 1,
      averageCost: value,
      currentPrice: value,
      currency
    };
  }

}
