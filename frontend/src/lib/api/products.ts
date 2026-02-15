"use client";

import { apiClient } from "@/lib/http";
import { Product, ProductFilters } from "@/types/api";

export const productsApi = {
  getAll: async () => {
    const { data } = await apiClient.get<Product[]>("/products");
    return data;
  },
  getById: async (id: number) => {
    const { data } = await apiClient.get<Product>(`/products/${id}`);
    return data;
  },
  getByBrand: async (brand: string) => {
    const { data } = await apiClient.get<Product[]>(`/products/brand/${brand}`);
    return data;
  },
  getByCategory: async (category: string) => {
    const { data } = await apiClient.get<Product[]>(`/products/category/${category}`);
    return data;
  },
  listWithFilters: async (filters: ProductFilters) => {
    const data = await productsApi.getAll();
    const query = filters.query?.trim().toLowerCase();
    const min = filters.minPrice ?? 0;
    const max = filters.maxPrice ?? Number.MAX_SAFE_INTEGER;

    const filtered = data.filter((item) => {
      const matchesQuery = !query || item.name.toLowerCase().includes(query) || item.description.toLowerCase().includes(query);
      const matchesBrand = !filters.brand || item.brand?.toLowerCase() === filters.brand.toLowerCase();
      const matchesCategory = !filters.category || item.category.toLowerCase() === filters.category.toLowerCase();
      const matchesType = !filters.type || item.category.toLowerCase().includes(filters.type.toLowerCase());
      const matchesPrice = Number(item.price) >= min && Number(item.price) <= max;
      return matchesQuery && matchesBrand && matchesCategory && matchesType && matchesPrice;
    });

    const sorted = [...filtered].sort((a, b) => {
      if (filters.sort === "price_asc") return Number(a.price) - Number(b.price);
      if (filters.sort === "price_desc") return Number(b.price) - Number(a.price);
      return b.stock - a.stock;
    });

    const page = filters.page ?? 1;
    const limit = filters.limit ?? 12;
    const start = (page - 1) * limit;
    const items = sorted.slice(start, start + limit);

    return {
      items,
      meta: {
        page,
        limit,
        totalItems: sorted.length,
        totalPages: Math.max(1, Math.ceil(sorted.length / limit))
      }
    };
  }
};
