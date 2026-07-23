import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subject, catchError, map, of, startWith, switchMap } from 'rxjs';
import { FeaturedProduct, ProductService } from '../../../../core/product.service';
import { ProductCardComponent } from '../../../../shared/components/product-card/product-card.component';

type LoadState =
  | { status: 'loading' }
  | { status: 'success'; data: FeaturedProduct[] }
  | { status: 'error' };

@Component({
  selector: 'app-featured-products',
  standalone: true,
  imports: [RouterLink, ProductCardComponent],
  templateUrl: './featured-products.component.html',
  styleUrl: './featured-products.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeaturedProductsComponent {
  private readonly productService = inject(ProductService);
  private readonly reload$ = new Subject<void>();

  protected readonly skeletons = Array.from({ length: 8 });

  private readonly state = toSignal(
    this.reload$.pipe(
      startWith(undefined),
      switchMap(() =>
        this.productService.getFeaturedProducts(8).pipe(
          map((data): LoadState => ({ status: 'success', data })),
          startWith<LoadState>({ status: 'loading' }),
          catchError(() => of<LoadState>({ status: 'error' }))
        )
      )
    ),
    { initialValue: { status: 'loading' } as LoadState }
  );

  protected readonly status = computed(() => this.state().status);
  protected readonly products = computed(() => {
    const value = this.state();
    return value.status === 'success' ? value.data : [];
  });

  protected retry(): void {
    this.reload$.next();
  }
}
