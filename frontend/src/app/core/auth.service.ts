import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { CartService } from './cart.service';

type LoginPayload = {
  username: string;
  password: string;
};

type RegisterPayload = {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
};

export type AuthResponse = {
  accessToken: string;
  username: string;
  email: string;
};

export type AuthUser = {
  username: string;
  email: string;
};

const ACCESS_TOKEN_KEY = 'dmusic.access_token';
const USER_KEY = 'dmusic.user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly cartService = inject(CartService);

  private readonly accessTokenState = signal<string | null>(this.readAccessToken());
  private readonly userState = signal<AuthUser | null>(this.readUser());

  readonly accessToken = this.accessTokenState.asReadonly();
  readonly user = this.userState.asReadonly();
  readonly isLoggedIn = computed(() => !!this.accessTokenState());

  login(payload: LoginPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/login`, {
      username: payload.username.trim(),
      password: payload.password
    }).pipe(
      tap((response) => {
        this.persistSession(response);
        this.cartService.loadCart().subscribe();
      })
    );
  }

  register(payload: RegisterPayload): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_BASE_URL}/auth/register`, {
      username: payload.username.trim(),
      email: payload.email.trim(),
      password: payload.password,
      firstName: payload.firstName.trim(),
      lastName: payload.lastName.trim()
    }).pipe(
      tap((response) => {
        this.persistSession(response);
        this.cartService.loadCart().subscribe();
      })
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE_URL}/auth/logout`, {}).pipe(
      tap(() => this.clearSession())
    );
  }

  clearSession(): void {
    this.accessTokenState.set(null);
    this.userState.set(null);
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this.cartService.resetCart();
  }

  private persistSession(response: AuthResponse): void {
    const user = { username: response.username, email: response.email };
    this.accessTokenState.set(response.accessToken);
    this.userState.set(user);
    localStorage.setItem(ACCESS_TOKEN_KEY, response.accessToken);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  private readAccessToken(): string | null {
    return this.canUseStorage() ? localStorage.getItem(ACCESS_TOKEN_KEY) : null;
  }

  private readUser(): AuthUser | null {
    if (!this.canUseStorage()) {
      return null;
    }

    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      localStorage.removeItem(USER_KEY);
      return null;
    }
  }

  private canUseStorage(): boolean {
    return typeof localStorage !== 'undefined';
  }
}
