export const queryKeys = {
  products: ["products"] as const,
  product: (id: number) => ["product", id] as const,
  cart: ["cart"] as const,
  me: ["me"] as const,
  orders: ["orders"] as const,
  order: (id: string) => ["order", id] as const
};
