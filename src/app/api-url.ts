import { environment } from '../environments/environment';

const API_BASE_URL = environment.apiUrl.replace(/\/+$/, '');
const AUTH_BASE_URL = `${API_BASE_URL}/auth`;

export function buildApiUrl(path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  return `${API_BASE_URL}${normalizedPath}`;
}

export function isApiUrl(url: string): boolean {
  return url === API_BASE_URL || url.startsWith(`${API_BASE_URL}/`);
}

export function isAuthUrl(url: string): boolean {
  return url === AUTH_BASE_URL || url.startsWith(`${AUTH_BASE_URL}/`);
}
