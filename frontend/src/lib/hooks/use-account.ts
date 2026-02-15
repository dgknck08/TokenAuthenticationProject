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
    queryFn: () => ordersApi.getById(id),
    enabled: Boolean(id)
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
  return useMutation({ mutationFn: ordersApi.create });
}
