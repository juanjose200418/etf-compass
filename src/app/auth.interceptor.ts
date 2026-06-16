import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { isApiUrl, isAuthUrl } from './api-url';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const isApiRequest = isApiUrl(req.url);
  const isAuthRequest = isAuthUrl(req.url);
  const needsAuth = isApiRequest && !isAuthRequest;
  const token = needsAuth ? auth.getValidToken() : null;

  const authReq = token && !req.headers.has('Authorization')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError(error => {
      if (needsAuth && (error.status === 401 || error.status === 403)) {
        auth.logout();
      }
      return throwError(() => error);
    })
  );
};
