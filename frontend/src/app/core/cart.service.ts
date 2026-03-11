import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api.config';

export type CartItem = {
  productId: number;
  productName: string;
  productImage: string | null;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
};

export type Cart = {
  items: CartItem[];
  totalItems: number;
  totalAmount: number;
  cartType: string;
};

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly cartState = signal<Cart | null>(null);

  readonly cart = this.cartState.asReadonly();
  readonly totalItems = computed(() => this.cartState()?.totalItems ?? 0);

  loadCart(): Observable<Cart> {
    return this.http.get<Cart>(`${API_BASE_URL}/cart`).pipe(
      tap((cart) => this.cartState.set(cart))
    );
  }

  resetCart(): void {
    this.cartState.set(null);
  }
}
