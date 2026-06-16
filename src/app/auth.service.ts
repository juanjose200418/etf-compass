import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { map } from 'rxjs';
import { buildApiUrl } from './api-url';
import { ApiResponse, AuthResponse, UserResponse } from './api.types';

export interface AuthResult {
  authenticated: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  readonly token = signal<string | null>(null);
  readonly user = signal<UserResponse | null>(null);
  private readonly expiresAt = signal<number | null>(null);
  readonly isAuthenticated = computed(() => !!this.token() && !!this.user() && !this.isSessionExpired());

  login(email: string, password: string) {
    return this.http.post<ApiResponse<AuthResponse>>(buildApiUrl('/auth/login'), { email, password }).pipe(
      map(res => {
        const auth = this.requireAuthPayload(res);
        this.setSession(auth.accessToken, auth.user, auth.expiresInSeconds);
        return { authenticated: true } satisfies AuthResult;
      })
    );
  }

  register(email: string, password: string, displayName: string) {
    return this.http.post<ApiResponse<AuthResponse>>(buildApiUrl('/auth/register'), { email, password, displayName }).pipe(
      map(res => {
        const auth = this.readAuthPayload(res);
        if (!auth) {
          return { authenticated: false } satisfies AuthResult;
        }

        this.setSession(auth.accessToken, auth.user, auth.expiresInSeconds);
        return { authenticated: true } satisfies AuthResult;
      })
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
    this.token.set(null);
    this.user.set(null);
    this.expiresAt.set(null);
  }

  authHeaders(): Record<string, string> {
    const token = this.token();
    if (!token || this.isSessionExpired()) {
      this.logout();
      return {};
    }
    return { Authorization: `Bearer ${token}` };
  }

  getValidToken(): string | null {
    const token = this.token();
    if (!token || this.isSessionExpired()) {
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

  setSession(token: string, user: UserResponse, expiresInSeconds: number): void {
    if (!token?.trim()) {
      throw new Error('AUTH_RESPONSE_INVALID');
    }

    this.token.set(token);
    this.user.set(user);
    this.expiresAt.set(Date.now() + Math.max(1, expiresInSeconds) * 1000);
  }

  private requireAuthPayload(response: ApiResponse<AuthResponse>): AuthResponse {
    const auth = this.readAuthPayload(response);
    if (!auth) {
      throw new Error('AUTH_RESPONSE_INVALID');
    }
    return auth;
  }

  private readAuthPayload(response: ApiResponse<AuthResponse>): AuthResponse | null {
    const auth = response.data;
    if (!auth?.accessToken || !auth?.user) {
      return null;
    }
    return auth;
  }

  private isSessionExpired(): boolean {
    const expiresAt = this.expiresAt();
    return !expiresAt || expiresAt <= Date.now();
  }
}
