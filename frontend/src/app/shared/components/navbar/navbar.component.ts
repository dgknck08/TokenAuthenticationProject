import { ChangeDetectionStrategy, Component, HostListener, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
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

  private readonly isHome = signal(false);
  private readonly scrolled = signal(false);

  readonly isHidden = computed(() => this.isHome() && !this.scrolled());
  readonly isSolid = computed(() => !this.isHome() || this.scrolled());

  private scrollTicking = false;

  constructor() {
    this.cartService.loadCart().subscribe({
      error: () => {
        this.cartService.resetCart();
      }
    });

    this.isHome.set(this.isHomeUrl(this.router.url));
    this.scrolled.set(this.isPastThreshold());

    this.router.events
      .pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
        takeUntilDestroyed()
      )
      .subscribe((event) => {
        this.isHome.set(this.isHomeUrl(event.urlAfterRedirects));
      });
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (this.scrollTicking) {
      return;
    }
    this.scrollTicking = true;
    requestAnimationFrame(() => {
      this.scrolled.set(this.isPastThreshold());
      this.scrollTicking = false;
    });
  }

  private isHomeUrl(url: string): boolean {
    const path = url.split('?')[0].split('#')[0];
    return path === '/' || path === '';
  }

  private isPastThreshold(): boolean {
    return window.scrollY > window.innerHeight * 0.8;
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

  goToStore(): void {
    this.isCartOpen.set(false);
    this.isUserMenuOpen.set(false);
    this.router.navigateByUrl('/magaza');
  }

  goToAbout(): void {
    this.isCartOpen.set(false);
    this.isUserMenuOpen.set(false);
    this.router.navigateByUrl('/hakkimizda');
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
