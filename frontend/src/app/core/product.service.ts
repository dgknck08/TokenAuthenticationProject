import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';

export type ProductApiDto = {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl: string | null;
  category: string;
  brand: string | null;
  sku?: string | null;
  color?: string | null;
  size?: string | null;
  attributesJson?: string | null;
  stock: number;
};

export type PageResponse<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
};

export interface FeaturedProduct {
  id: number;
  name: string;
  brand: string;
  category: string;
  price: number;
  image: string | null;
  badge: string | null;
  inStock: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);

  getProducts(size = 100): Observable<PageResponse<ProductApiDto>> {
    const params = new HttpParams().set('size', size);
    return this.http.get<PageResponse<ProductApiDto>>(`${API_BASE_URL}/products`, { params });
  }

  getFeaturedProducts(limit = 8): Observable<FeaturedProduct[]> {
    return this.getProducts(limit * 3).pipe(
      map((response) =>
        response.content
          .map((dto) => this.toFeatured(dto))
          .sort((a, b) => Number(b.inStock) - Number(a.inStock))
          .slice(0, limit)
      )
    );
  }

  private toFeatured(dto: ProductApiDto): FeaturedProduct {
    const inStock = dto.stock > 0;
    return {
      id: dto.id,
      name: dto.name,
      brand: dto.brand || 'Dmusic',
      category: dto.category,
      price: Number(dto.price),
      image: dto.imageUrl || null,
      badge: !inStock ? 'Tükendi' : dto.stock < 10 ? 'Son birkaç ürün' : null,
      inStock,
    };
  }
}
