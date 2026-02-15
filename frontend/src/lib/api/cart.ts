"use client";

import { apiClient } from "@/lib/http";
import { Cart } from "@/types/api";

export const cartApi = {
  get: async () => {
    const { data } = await apiClient.get<Cart>("/cart");
    return data;
  },
  add: async (productId: number, quantity = 1) => {
    const { data } = await apiClient.post<Cart>("/cart/items", { productId, quantity });
    return data;
  },
  update: async (productId: number, quantity: number) => {
    const { data } = await apiClient.put<Cart>(`/cart/items/${productId}`, { quantity });
    return data;
  },
  remove: async (productId: number) => {
    const { data } = await apiClient.delete<Cart>(`/cart/items/${productId}`);
    return data;
  },
  clear: async () => {
    await apiClient.delete("/cart");
  },
  merge: async () => {
    const { data } = await apiClient.post<Cart>("/cart/merge");
    return data;
  }
};
