import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { map, tap } from 'rxjs';
import { buildApiUrl } from './api-url';
import { ApiResponse, AuthResponse, UserResponse } from './api.types';

const TOKEN_KEY = 'etf-compass-access-token';
const USER_KEY = 'etf-compass-user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  readonly token = signal<string | null>(this.loadToken());
  readonly user = signal<UserResponse | null>(this.loadUser());
  readonly isAuthenticated = computed(() => !!this.token() && !!this.user() && !this.isTokenExpired(this.token()));

  login(email: string, password: string) {
    return this.http.post<ApiResponse<AuthResponse>>(buildApiUrl('/auth/login'), { email, password }).pipe(
      map(res => res.data),
      tap(auth => this.setSession(auth))
    );
  }

  register(email: string, password: string, displayName: string) {
    return this.http.post<ApiResponse<AuthResponse>>(buildApiUrl('/auth/register'), { email, password, displayName }).pipe(
      map(res => res.data),
      tap(auth => this.setSession(auth))
    );
  }

  requestPasswordResetCode(email: string) {
    return this.http.post<ApiResponse<null>>(buildApiUrl('/auth/forgot-password'), { email }).pipe(
      map(() => void 0)
    );
  }

  resetPasswordWithCode(email: string, code: string, newPassword: string) {
    return this.http.post<ApiResponse<null>>(buildApiUrl('/auth/reset-password'), { email, code, newPassword }).pipe(
      map(() => void 0)
    );
  }

  logout(): void {
    this.clearStoredSession();
    this.token.set(null);
    this.user.set(null);
  }

  authHeaders(): Record<string, string> {
    const token = this.token();
    if (!token || this.isTokenExpired(token)) {
      this.logout();
      return {};
    }
    return { Authorization: `Bearer ${token}` };
  }

  getValidToken(): string | null {
    const token = this.token();
    if (!token || this.isTokenExpired(token)) {
      this.logout();
      return null;
    }
    return token;
  }

  decodedTokenPayload(): Record<string, unknown> | null {
    const token = this.token();
    if (!token) return null;
    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      return JSON.parse(atob(padded)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private setSession(auth: AuthResponse): void {
    this.writeStoredValue(TOKEN_KEY, auth.accessToken);
    this.writeStoredValue(USER_KEY, JSON.stringify(auth.user));
    this.token.set(auth.accessToken);
    this.user.set(auth.user);
  }

  private loadUser(): UserResponse | null {
    const token = this.loadToken();
    if (!token) return null;
    const saved = this.readStoredValue(USER_KEY);
    if (!saved) return null;
    try {
      return JSON.parse(saved) as UserResponse;
    } catch {
      this.removeStoredValue(USER_KEY);
      return null;
    }
  }

  private loadToken(): string | null {
    const token = this.readStoredValue(TOKEN_KEY);
    if (!token || this.isTokenExpired(token)) {
      this.clearStoredSession();
      return null;
    }
    return token;
  }

  private readStoredValue(key: string): string | null {
    const session = this.sessionStorage();
    const sessionValue = session?.getItem(key) ?? null;
    if (sessionValue != null) {
      return sessionValue;
    }

    const legacy = this.legacyStorage();
    const legacyValue = legacy?.getItem(key) ?? null;
    if (legacyValue != null && session) {
      session.setItem(key, legacyValue);
      legacy?.removeItem(key);
    }
    return legacyValue;
  }

  private writeStoredValue(key: string, value: string): void {
    this.sessionStorage()?.setItem(key, value);
    this.legacyStorage()?.removeItem(key);
  }

  private removeStoredValue(key: string): void {
    this.sessionStorage()?.removeItem(key);
    this.legacyStorage()?.removeItem(key);
  }

  private clearStoredSession(): void {
    this.removeStoredValue(TOKEN_KEY);
    this.removeStoredValue(USER_KEY);
  }

  private sessionStorage(): Storage | null {
    return typeof window === 'undefined' ? null : window.sessionStorage;
  }

  private legacyStorage(): Storage | null {
    return typeof window === 'undefined' ? null : window.localStorage;
  }

  private isTokenExpired(token: string | null): boolean {
    if (!token) return true;
    try {
      const payload = token.split('.')[1];
      const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=');
      const decoded = JSON.parse(atob(padded)) as { exp?: number };
      if (!decoded.exp) return true;
      return decoded.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
