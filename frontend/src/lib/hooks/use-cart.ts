"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cartApi } from "@/lib/api/cart";
import { queryKeys } from "@/lib/query-keys";
import { Cart } from "@/types/api";
import { toast } from "sonner";

export function useCart() {
  const queryClient = useQueryClient();

  const cartQuery = useQuery({
    queryKey: queryKeys.cart,
    queryFn: cartApi.get
  });

  const addMutation = useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) =>
      cartApi.add(productId, quantity),
    onSuccess: (data) => queryClient.setQueryData(queryKeys.cart, data)
  });

  const updateMutation = useMutation({
    mutationFn: ({ productId, quantity }: { productId: number; quantity: number }) =>
      cartApi.update(productId, quantity),
    onMutate: async ({ productId, quantity }) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.cart });
      const previous = queryClient.getQueryData(queryKeys.cart);
      queryClient.setQueryData(queryKeys.cart, (old: Cart | undefined) => {
        if (!old) return old;
        return {
          ...old,
          items: old.items.map((item) =>
            item.productId === productId
              ? { ...item, quantity, totalPrice: Number(item.unitPrice) * quantity }
              : item
          )
        };
      });
      return { previous };
    },
    onError: (_, __, context) => {
      if (context?.previous) queryClient.setQueryData(queryKeys.cart, context.previous);
      toast.error("Cart could not be updated");
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.cart })
  });

  const removeMutation = useMutation({
    mutationFn: (productId: number) => cartApi.remove(productId),
    onSuccess: (data) => {
      queryClient.setQueryData(queryKeys.cart, data);
      toast.success("Item removed from cart");
    },
    onError: () => toast.error("Item could not be removed")
  });

  return { cartQuery, addMutation, updateMutation, removeMutation };
}
