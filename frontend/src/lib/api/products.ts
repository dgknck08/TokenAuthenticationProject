"use client";

import { apiClient } from "@/lib/http";
import { Product, ProductFilters } from "@/types/api";

type ProductSearchResponse = {
  content: Product[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductUpsertPayload = {
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  category: string;
  brand?: string;
  sku?: string;
  color?: string;
  size?: string;
  attributesJson?: string;
  stock: number;
};

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
    const page = Math.max((filters.page ?? 1) - 1, 0);
    const size = Math.max(filters.limit ?? 12, 1);
    const sort = filters.sort === "price_asc" ? "price,asc" : filters.sort === "price_desc" ? "price,desc" : "id,desc";

    const params = new URLSearchParams();
    params.set("page", String(page));
    params.set("size", String(size));
    params.set("sort", sort);
    if (filters.query?.trim()) params.set("q", filters.query.trim());
    if (filters.brand?.trim()) params.set("brand", filters.brand.trim());
    if (filters.category?.trim()) params.set("category", filters.category.trim());

    const { data } = await apiClient.get<ProductSearchResponse>(`/products/search?${params.toString()}`);

    let items = data.content;
    const min = filters.minPrice ?? 0;
    const max = filters.maxPrice ?? Number.MAX_SAFE_INTEGER;
    items = items.filter((item) => Number(item.price) >= min && Number(item.price) <= max);
    if (filters.type?.trim()) {
      const type = filters.type.trim().toLowerCase();
      items = items.filter((item) => item.category?.toLowerCase().includes(type));
    }

    return {
      items,
      meta: {
        page: data.number + 1,
        limit: data.size,
        totalItems: data.totalElements,
        totalPages: data.totalPages
      }
    };
  },
  create: async (payload: ProductUpsertPayload) => {
    const { data } = await apiClient.post<Product>("/products", payload);
    return data;
  },
  update: async (id: number, payload: ProductUpsertPayload) => {
    const { data } = await apiClient.put<Product>(`/products/${id}`, payload);
    return data;
  },
  remove: async (id: number) => {
    await apiClient.delete(`/products/${id}`);
  },
  uploadImage: async (file: File) => {
    const formData = new FormData();
    formData.append("file", file);
    const { data } = await apiClient.post<{ imageUrl: string; publicId?: string }>(
      "/admin/uploads/product-image",
      formData,
      { headers: { "Content-Type": "multipart/form-data" } }
    );
    return data;
  },
  getAdminList: async () => {
    const params = new URLSearchParams();
    params.set("page", "0");
    params.set("size", "100");
    params.set("sort", "id,desc");
    const { data } = await apiClient.get<ProductSearchResponse>(`/products/search?${params.toString()}`);
    return data.content;
  }
};
