import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActiveFilters, Product } from '../../magaza.types';

@Component({
  selector: 'app-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './filter-panel.component.html',
  styleUrl: './filter-panel.component.css'
})
export class FilterPanelComponent implements OnInit, OnChanges {
  @Input() allProducts: Product[] = [];
  @Input() activeFilters: ActiveFilters = {
    categories: [],
    brands: [],
    priceRange: { min: 0, max: 0 },
    ratings: [],
    badges: [],
    inStockOnly: false,
    search: ''
  };

  @Output() filtersChange = new EventEmitter<ActiveFilters>();
  @Output() resetFilters = new EventEmitter<void>();

  categories: { name: string; count: number }[] = [];
  brands: { name: string; count: number }[] = [];
  badges: { name: string; count: number }[] = [];
  ratingOptions = [4, 3, 2, 1];
  priceMin = 0;
  priceMax = 0;
  sliderMin = 0;
  sliderMax = 0;
  collapsedSections: Record<string, boolean> = {};
  showAllBrands = false;
  readonly BRAND_LIMIT = 6;

  get visibleBrands() {
    return this.showAllBrands ? this.brands : this.brands.slice(0, this.BRAND_LIMIT);
  }

  get activeFilterCount(): number {
    const filters = this.activeFilters;
    let count = 0;

    if (filters.categories.length) {
      count++;
    }

    if (filters.brands.length) {
      count++;
    }

    if (filters.ratings.length) {
      count++;
    }

    if (filters.badges.length) {
      count++;
    }

    if (filters.inStockOnly) {
      count++;
    }

    if (filters.search) {
      count++;
    }

    if (filters.priceRange.min > this.priceMin || filters.priceRange.max < this.priceMax) {
      count++;
    }

    return count;
  }

  ngOnInit(): void {
    this.buildFacets();
    this.syncSliderWithFilters();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['allProducts']) {
      this.buildFacets();
    }

    if (changes['activeFilters'] || changes['allProducts']) {
      this.syncSliderWithFilters();
    }
  }

  toggleSection(key: string): void {
    this.collapsedSections[key] = !this.collapsedSections[key];
  }

  isCollapsed(key: string): boolean {
    return !!this.collapsedSections[key];
  }

  toggleCategory(category: string): void {
    const categories = this.activeFilters.categories.includes(category)
      ? this.activeFilters.categories.filter((item) => item !== category)
      : [...this.activeFilters.categories, category];

    this.emit({ categories });
  }

  toggleBrand(brand: string): void {
    const brands = this.activeFilters.brands.includes(brand)
      ? this.activeFilters.brands.filter((item) => item !== brand)
      : [...this.activeFilters.brands, brand];

    this.emit({ brands });
  }

  toggleRating(rating: number): void {
    const ratings = this.activeFilters.ratings.includes(rating)
      ? this.activeFilters.ratings.filter((item) => item !== rating)
      : [...this.activeFilters.ratings, rating];

    this.emit({ ratings });
  }

  toggleBadge(badge: string): void {
    const badges = this.activeFilters.badges.includes(badge)
      ? this.activeFilters.badges.filter((item) => item !== badge)
      : [...this.activeFilters.badges, badge];

    this.emit({ badges });
  }

  toggleStock(): void {
    this.emit({ inStockOnly: !this.activeFilters.inStockOnly });
  }

  onPriceMinChange(value: number): void {
    if (this.priceMax <= this.priceMin) {
      this.sliderMin = this.priceMin;
      this.sliderMax = this.priceMax;
      this.emit({ priceRange: { min: this.priceMin, max: this.priceMax } });
      return;
    }

    const nextValue = Number.isFinite(value) ? value : this.priceMin;
    const clamped = Math.max(this.priceMin, Math.min(nextValue, this.sliderMax - 1));
    this.sliderMin = clamped;
    this.emit({ priceRange: { min: clamped, max: this.sliderMax } });
  }

  onPriceMaxChange(value: number): void {
    if (this.priceMax <= this.priceMin) {
      this.sliderMin = this.priceMin;
      this.sliderMax = this.priceMax;
      this.emit({ priceRange: { min: this.priceMin, max: this.priceMax } });
      return;
    }

    const nextValue = Number.isFinite(value) ? value : this.priceMax;
    const clamped = Math.min(this.priceMax, Math.max(nextValue, this.sliderMin + 1));
    this.sliderMax = clamped;
    this.emit({ priceRange: { min: this.sliderMin, max: clamped } });
  }

  onSearchChange(value: string): void {
    this.emit({ search: value });
  }

  clearPriceRange(): void {
    this.sliderMin = this.priceMin;
    this.sliderMax = this.priceMax;
    this.emit({ priceRange: { min: this.priceMin, max: this.priceMax } });
  }

  stars(rating: number): string {
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  trackByName(_: number, item: { name: string }): string {
    return item.name;
  }

  rangeFillLeft(): number {
    const total = this.priceMax - this.priceMin;
    if (total <= 0) {
      return 0;
    }

    return ((this.sliderMin - this.priceMin) / total) * 100;
  }

  rangeFillRight(): number {
    const total = this.priceMax - this.priceMin;
    if (total <= 0) {
      return 0;
    }

    return (1 - (this.sliderMax - this.priceMin) / total) * 100;
  }

  private buildFacets(): void {
    const brandMap = new Map<string, number>();
    const categoryMap = new Map<string, number>();
    const badgeMap = new Map<string, number>();
    let minPrice = Infinity;
    let maxPrice = -Infinity;

    for (const product of this.allProducts) {
      categoryMap.set(product.category, (categoryMap.get(product.category) ?? 0) + 1);
      brandMap.set(product.brand, (brandMap.get(product.brand) ?? 0) + 1);

      if (product.badge) {
        badgeMap.set(product.badge, (badgeMap.get(product.badge) ?? 0) + 1);
      }

      minPrice = Math.min(minPrice, product.price);
      maxPrice = Math.max(maxPrice, product.price);
    }

    this.categories = Array.from(categoryMap.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => a.name.localeCompare(b.name, 'tr'));

    this.brands = Array.from(brandMap.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'tr'));

    this.badges = Array.from(badgeMap.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'tr'));

    this.priceMin = Number.isFinite(minPrice) ? minPrice : 0;
    this.priceMax = Number.isFinite(maxPrice) ? maxPrice : 0;
  }

  private syncSliderWithFilters(): void {
    this.sliderMin = Math.max(this.priceMin, Math.min(this.activeFilters.priceRange.min, this.priceMax));
    this.sliderMax = Math.max(this.sliderMin, Math.min(this.activeFilters.priceRange.max, this.priceMax));
  }

  private emit(partial: Partial<ActiveFilters>): void {
    this.filtersChange.emit({ ...this.activeFilters, ...partial });
  }
}
