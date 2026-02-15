"use client";

import { useQuery } from "@tanstack/react-query";
import { productsApi } from "@/lib/api/products";
import { ProductFilters } from "@/types/api";
import { queryKeys } from "@/lib/query-keys";

export function useProducts(filters: ProductFilters) {
  return useQuery({
    queryKey: [...queryKeys.products, filters],
    queryFn: () => productsApi.listWithFilters(filters)
  });
}

export function useProduct(id: number) {
  return useQuery({
    queryKey: queryKeys.product(id),
    queryFn: () => productsApi.getById(id),
    enabled: Number.isFinite(id)
  });
}
