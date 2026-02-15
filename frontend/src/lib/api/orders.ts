"use client";

import { apiClient } from "@/lib/http";
import { OrderSummary } from "@/types/api";

const useMockOrders = process.env.NEXT_PUBLIC_ENABLE_MOCK_ORDERS === "true";

const mockOrders: OrderSummary[] = [
  {
    id: "ORD-2026-001",
    createdAt: "2026-02-01T10:15:00Z",
    total: 2599,
    status: "shipped",
    items: [{ id: "1", productName: "Aurora Strat X", quantity: 1, unitPrice: 2599 }]
  }
];

export const ordersApi = {
  getAll: async () => {
    if (useMockOrders) return mockOrders;
    const { data } = await apiClient.get<OrderSummary[]>("/orders");
    return data;
  },
  getById: async (id: string) => {
    if (useMockOrders) return mockOrders.find((order) => order.id === id) || null;
    const { data } = await apiClient.get<OrderSummary>(`/orders/${id}`);
    return data;
  },
  create: async (payload: unknown) => {
    if (process.env.NEXT_PUBLIC_ENABLE_MOCK_CHECKOUT === "true") {
      return { id: `ORD-MOCK-${Date.now()}` };
    }
    const { data } = await apiClient.post<{ id: string }>("/orders", payload);
    return data;
  }
};
