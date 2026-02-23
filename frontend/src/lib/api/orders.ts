"use client";

import { apiClient } from "@/lib/http";
import { OrderSummary } from "@/types/api";

const useMockOrders = process.env.NEXT_PUBLIC_ENABLE_MOCK_ORDERS === "true";

const mockOrders: OrderSummary[] = [
  {
    id: 1001,
    createdAt: "2026-02-01T10:15:00Z",
    total: 2599,
    status: "PAID",
    paymentMethod: "CARD",
    paidAt: "2026-02-01T10:16:00Z",
    items: [{ id: 1, productId: 1, productName: "Aurora Strat X", quantity: 1, unitPrice: 2599, lineTotal: 2599 }]
  }
];

type BackendOrderItem = {
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
};

type BackendOrder = {
  id: number;
  createdAt: string;
  totalAmount: number;
  status: "CREATED" | "PAID" | "REFUNDED" | "CANCELLED";
  paymentMethod?: "CARD" | "COD";
  paidAt?: string;
  cancelledAt?: string;
  refundedAt?: string;
  items: BackendOrderItem[];
};

function mapOrder(order: BackendOrder): OrderSummary {
  return {
    id: order.id,
    createdAt: order.createdAt,
    total: Number(order.totalAmount),
    status: order.status,
    paymentMethod: order.paymentMethod,
    paidAt: order.paidAt,
    cancelledAt: order.cancelledAt,
    refundedAt: order.refundedAt,
    items: order.items.map((item, index) => ({
      id: index + 1,
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      unitPrice: Number(item.unitPrice),
      lineTotal: Number(item.lineTotal)
    }))
  };
}

export type CreateOrderPayload = {
  items: Array<{ productId: number; quantity: number }>;
};

export const ordersApi = {
  getAll: async () => {
    if (useMockOrders) return mockOrders;
    const { data } = await apiClient.get<BackendOrder[]>("/orders");
    return data.map(mapOrder);
  },
  getById: async (id: number) => {
    if (useMockOrders) return mockOrders.find((order) => order.id === id) || null;
    const { data } = await apiClient.get<BackendOrder>(`/orders/${id}`);
    return mapOrder(data);
  },
  create: async (payload: CreateOrderPayload) => {
    if (process.env.NEXT_PUBLIC_ENABLE_MOCK_CHECKOUT === "true") {
      return { id: Date.now() };
    }
    const { data } = await apiClient.post<BackendOrder>("/orders", payload);
    return mapOrder(data);
  },
  pay: async (id: number, paymentMethod: "CARD" | "COD") => {
    const { data } = await apiClient.post<BackendOrder>(`/orders/${id}/pay`, { paymentMethod });
    return mapOrder(data);
  },
  getAdminAll: async () => {
    const { data } = await apiClient.get<BackendOrder[]>("/admin/orders");
    return data.map(mapOrder);
  },
  adminCancel: async (id: number) => {
    const { data } = await apiClient.post<BackendOrder>(`/admin/orders/${id}/cancel`);
    return mapOrder(data);
  },
  adminRefund: async (id: number) => {
    const { data } = await apiClient.post<BackendOrder>(`/admin/orders/${id}/refund`);
    return mapOrder(data);
  }
};
