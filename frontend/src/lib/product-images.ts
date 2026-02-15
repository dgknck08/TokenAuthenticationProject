import { Product } from "@/types/api";

const FALLBACK_REMOTE_IMAGE = "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?q=80&w=1200&auto=format&fit=crop";

export function resolveProductImage(product: Pick<Product, "id" | "name" | "description" | "category" | "imageUrl" | "sku">) {
  return product.imageUrl?.trim() || FALLBACK_REMOTE_IMAGE;
}

export function buildProductGalleryImages(product: Pick<Product, "id" | "name" | "description" | "category" | "imageUrl" | "sku">) {
  const primary = resolveProductImage(product);
  return [primary];
}
