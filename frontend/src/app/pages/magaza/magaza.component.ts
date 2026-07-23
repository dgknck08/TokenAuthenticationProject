import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProductApiDto, ProductService } from '../../core/product.service';
import { FilterPanelComponent } from './components/filter-panel/filter-panel.component';
import { ProductGridComponent } from './components/product-grid/product-grid.component';
import { ActiveFilters, DEFAULT_FILTERS, Product } from './magaza.types';

@Component({
  selector: 'app-magaza',
  standalone: true,
  imports: [CommonModule, FilterPanelComponent, ProductGridComponent],
  templateUrl: './magaza.component.html',
  styleUrl: './magaza.component.css'
})
export class MagazaComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly route = inject(ActivatedRoute);

  readonly products = signal<Product[]>([]);
  readonly isLoading = signal(true);
  readonly errorMessage = signal('');

  activeFilters: ActiveFilters = DEFAULT_FILTERS;
  sortBy = 'default';
  currentPage = 1;
  readonly pageSize = 9;

  ngOnInit(): void {
    this.productService.getProducts().subscribe({
      next: (response) => {
        const products = response.content.map((product) => this.mapProduct(product));
        this.products.set(products);
        this.activeFilters = this.applyQueryParams(this.buildDefaultFilters(products), products);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Urunler alinamadi.');
        this.isLoading.set(false);
      }
    });
  }

  filteredProducts(): Product[] {
    const filters = this.activeFilters;
    let list = this.products().filter((product) => {
      if (filters.categories.length > 0 && !filters.categories.includes(product.category)) {
        return false;
      }

      if (filters.brands.length > 0 && !filters.brands.includes(product.brand)) {
        return false;
      }

      if (product.price < filters.priceRange.min || product.price > filters.priceRange.max) {
        return false;
      }

      if (filters.ratings.length > 0 && !filters.ratings.some((rating) => (product.rating ?? 0) >= rating)) {
        return false;
      }

      if (filters.badges.length > 0 && (!product.badge || !filters.badges.includes(product.badge))) {
        return false;
      }

      if (filters.inStockOnly && !product.inStock) {
        return false;
      }

      if (filters.search) {
        const query = this.normalizeSearchValue(filters.search);
        const haystack = this.normalizeSearchValue(
          `${product.name} ${product.brand} ${product.description} ${product.category}`
        );

        if (!haystack.includes(query)) {
          return false;
        }
      }

      return true;
    });

    switch (this.sortBy) {
      case 'price-asc':
        list.sort((a, b) => a.price - b.price);
        break;
      case 'price-desc':
        list.sort((a, b) => b.price - a.price);
        break;
      case 'newest':
        list.sort((a, b) => Number(Boolean(b.badge)) - Number(Boolean(a.badge)));
        break;
    }

    return list;
  }

  totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredProducts().length / this.pageSize));
  }

  pagedProducts(): Product[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredProducts().slice(start, start + this.pageSize);
  }

  pageNumbers(): number[] {
    const total = this.totalPages();
    if (total <= 7) {
      return Array.from({ length: total }, (_, index) => index + 1);
    }

    const current = this.currentPage;
    const pages = new Set([1, 2, current - 1, current, current + 1, total - 1, total]);
    return [...pages].filter((page) => page >= 1 && page <= total).sort((a, b) => a - b);
  }

  resetFilters(): void {
    this.activeFilters = this.buildDefaultFilters(this.products());
    this.sortBy = 'default';
    this.currentPage = 1;
  }

  goToPage(page: number): void {
    this.currentPage = page;
  }

  prevPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages()) {
      this.currentPage++;
    }
  }

  updateFilters(value: ActiveFilters): void {
    this.activeFilters = this.normalizeFilters(value);
    this.currentPage = 1;
  }

  updateSortBy(value: string): void {
    this.sortBy = value;
    this.currentPage = 1;
  }

  onProductClick(product: Product): void {
    void product;
  }

  addToCart(event: Event, product: Product): void {
    event.stopPropagation();
    void product;
  }

  private mapProduct(product: ProductApiDto): Product {
    return {
      id: product.id,
      slug: product.sku?.toLowerCase().replace(/[^a-z0-9]+/g, '-') || `product-${product.id}`,
      brand: product.brand || 'Dmusic',
      name: product.name,
      description: product.description,
      price: Number(product.price),
      category: product.category,
      image: product.imageUrl || undefined,
      badge: product.stock > 0 ? (product.stock < 10 ? 'Sinirli Stok' : undefined) : 'Tukendi',
      rating: this.deriveRating(product),
      inStock: product.stock > 0
    };
  }

  private applyQueryParams(base: ActiveFilters, products: Product[]): ActiveFilters {
    const params = this.route.snapshot.queryParamMap;
    const search = params.get('search')?.trim() ?? '';
    const category = params.get('category')?.trim() ?? '';
    const categories =
      category && products.some((product) => product.category === category)
        ? [category]
        : base.categories;

    if (!search && categories === base.categories) {
      return base;
    }

    return this.normalizeFilters({ ...base, search, categories });
  }

  private buildDefaultFilters(products: Product[]): ActiveFilters {
    if (products.length === 0) {
      return {
        ...DEFAULT_FILTERS,
        priceRange: { ...DEFAULT_FILTERS.priceRange }
      };
    }

    const prices = products.map((product) => product.price);

    return this.normalizeFilters({
      ...DEFAULT_FILTERS,
      priceRange: {
        min: Math.min(...prices),
        max: Math.max(...prices)
      }
    });
  }

  private deriveRating(product: ProductApiDto): number {
    if (product.stock >= 20) {
      return 5;
    }

    if (product.stock >= 10) {
      return 4;
    }

    if (product.stock >= 5) {
      return 3;
    }

    return 2;
  }

  private normalizeFilters(filters: ActiveFilters): ActiveFilters {
    const prices = this.products().map((product) => product.price);
    const minPrice = prices.length > 0 ? Math.min(...prices) : 0;
    const maxPrice = prices.length > 0 ? Math.max(...prices) : 0;
    const nextMin = Math.max(minPrice, Math.min(filters.priceRange.min, maxPrice));
    const nextMax = Math.max(nextMin, Math.min(filters.priceRange.max, maxPrice));

    return {
      categories: [...new Set(filters.categories)].sort((a, b) => a.localeCompare(b, 'tr')),
      brands: [...new Set(filters.brands)],
      ratings: [...new Set(filters.ratings)].sort((a, b) => b - a),
      badges: [...new Set(filters.badges)],
      inStockOnly: filters.inStockOnly,
      search: filters.search.trim(),
      priceRange: {
        min: nextMin,
        max: nextMax
      }
    };
  }

  private normalizeSearchValue(value: string): string {
    return value
      .toLocaleLowerCase('tr')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/ı/g, 'i')
      .trim();
  }
}
