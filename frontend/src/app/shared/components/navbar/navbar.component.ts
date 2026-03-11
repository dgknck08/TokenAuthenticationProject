import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/auth.service';
import { CartService } from '../../../core/cart.service';
import { ThemeService } from '../../../core/theme.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NavbarComponent {
  private static readonly LOGO_URL = 'https://res.cloudinary.com/dbtoykeu4/image/upload/v1773243243/dmusiclatest_glclqd.svg';

  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);
  private readonly cartService = inject(CartService);
  private readonly themeService = inject(ThemeService);

  readonly isDarkMode = this.themeService.isDarkMode;
  readonly isFavoriteMode = signal(false);
  readonly isCartOpen = signal(false);
  readonly isCartLoading = signal(false);
  readonly cartErrorMessage = signal('');
  readonly isUserMenuOpen = signal(false);
  readonly isLoggedIn = this.authService.isLoggedIn;
  readonly currentUser = this.authService.user;
  readonly cart = this.cartService.cart;
  readonly cartCount = this.cartService.totalItems;
  readonly isCartActive = computed(() => this.router.url.startsWith('/cart'));
  readonly logoUrl = computed(() => NavbarComponent.LOGO_URL);

  constructor() {
    this.cartService.loadCart().subscribe({
      error: () => {
        this.cartService.resetCart();
      }
    });
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  toggleFavorites(): void {
    this.isFavoriteMode.update((value) => !value);
  }

  toggleCart(): void {
    const nextState = !this.isCartOpen();
    this.isCartOpen.set(nextState);
    this.isUserMenuOpen.set(false);

    if (nextState) {
      this.loadCartPreview();
    }
  }

  goToCartPage(): void {
    this.isCartOpen.set(false);
    this.router.navigateByUrl('/cart');
  }

  toggleUserMenu(): void {
    this.isCartOpen.set(false);
    this.isUserMenuOpen.update((value) => !value);
  }

  login(): void {
    this.isUserMenuOpen.set(false);
    this.router.navigateByUrl('/login');
  }

  register(): void {
    this.isUserMenuOpen.set(false);
    this.router.navigateByUrl('/register');
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.isUserMenuOpen.set(false);
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.authService.clearSession();
        this.isUserMenuOpen.set(false);
        this.router.navigateByUrl('/');
      }
    });
  }

  private loadCartPreview(): void {
    this.isCartLoading.set(true);
    this.cartErrorMessage.set('');

    this.cartService.loadCart().subscribe({
      next: () => {
        this.isCartLoading.set(false);
      },
      error: () => {
        this.isCartLoading.set(false);
        this.cartErrorMessage.set('Sepet bilgisi alinamadi.');
      }
    });
  }
}
