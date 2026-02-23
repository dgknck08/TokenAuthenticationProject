"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ordersApi } from "@/lib/api/orders";
import { profileApi } from "@/lib/api/profile";
import { queryKeys } from "@/lib/query-keys";

export function useOrders() {
  return useQuery({ queryKey: queryKeys.orders, queryFn: ordersApi.getAll });
}

export function useOrder(id: string) {
  return useQuery({
    queryKey: queryKeys.order(id),
    queryFn: () => ordersApi.getById(Number(id)),
    enabled: Boolean(id) && Number.isFinite(Number(id))
  });
}

export function useUpdateProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: profileApi.update,
    onSuccess: (data) => queryClient.setQueryData(queryKeys.me, data)
  });
}

export function useCreateOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ordersApi.create,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.orders })
  });
}

export function usePayOrder() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, paymentMethod }: { id: number; paymentMethod: "CARD" | "COD" }) =>
      ordersApi.pay(id, paymentMethod),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.orders });
      queryClient.invalidateQueries({ queryKey: queryKeys.order(String(data.id)) });
    }
  });
}
