import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
import { API_BASE_URL } from './api.config';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.accessToken();
  const isApiRequest = req.url.startsWith(API_BASE_URL) || req.url.startsWith('/api/');

  if (!isApiRequest) {
    return next(req);
  }

  const headers = token ? req.headers.set('Authorization', `Bearer ${token}`) : req.headers;

  return next(
    req.clone({
      headers,
      withCredentials: true
    })
  );
};
