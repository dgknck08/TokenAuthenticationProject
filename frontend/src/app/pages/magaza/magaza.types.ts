export interface Product {
  id: number;
  slug: string;
  brand: string;
  name: string;
  description: string;
  price: number;
  oldPrice?: number;
  badge?: string;
  category: string;
  image?: string;
  isNew?: boolean;
  rating?: number;
  inStock?: boolean;
}

export interface FilterOption {
  slug: string;
  label: string;
  count: number;
}

export interface PriceRange {
  min: number;
  max: number;
}

export interface ActiveFilters {
  categories: string[];
  brands: string[];
  priceRange: PriceRange;
  ratings: number[];
  badges: string[];
  inStockOnly: boolean;
  search: string;
}

export const DEFAULT_FILTERS: ActiveFilters = {
  categories: [],
  brands: [],
  priceRange: { min: 0, max: 0 },
  ratings: [],
  badges: [],
  inStockOnly: false,
  search: ''
};
