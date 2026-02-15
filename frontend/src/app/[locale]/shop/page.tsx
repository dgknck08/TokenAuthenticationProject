"use client";

import { useMemo, useState } from "react";
import { ProductCard } from "@/components/product/product-card";
import { ShopFilters } from "@/components/shop/shop-filters";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useProducts } from "@/lib/hooks/use-products";
import { ProductFilters } from "@/types/api";
import { useCart } from "@/lib/hooks/use-cart";
import { toast } from "sonner";
import { useParams } from "next/navigation";

export default function ShopPage() {
  const params = useParams<{ locale: "en" | "tr" }>();
  const locale = params.locale;
  const [filters, setFilters] = useState<ProductFilters>({ page: 1, limit: 9, sort: "featured" });
  const { data, isLoading, isError } = useProducts(filters);
  const { addMutation } = useCart();

  const brands = useMemo(
    () => Array.from(new Set((data?.items || []).map((item) => item.brand).filter(Boolean) as string[])),
    [data?.items]
  );
  const categories = useMemo(
    () => Array.from(new Set((data?.items || []).map((item) => item.category).filter(Boolean) as string[])),
    [data?.items]
  );

  const addToCart = (productId: number) => {
    addMutation.mutate(
      { productId, quantity: 1 },
      {
        onSuccess: () => toast.success("Sepete eklendi"),
        onError: () => toast.error("Ürün sepete eklenemedi")
      }
    );
  };

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-border bg-card p-5 sm:p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-primary">Katalog</p>
        <h1 className="mt-2 font-display text-3xl font-bold">Gitar Mağazası</h1>
        <p className="mt-2 text-sm text-foreground/70">Güncel stok ve fiyatlarla en yeni akustik ve elektro modelleri inceleyin.</p>
      </section>

      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        <aside className="lg:sticky lg:top-24 lg:self-start">
          <ShopFilters filters={filters} brands={brands} categories={categories} onChange={setFilters} />
        </aside>

        <section className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-card p-3 text-sm">
            <p>
              {data?.items.length || 0} / {data?.meta.totalItems || 0} ürün gösteriliyor
            </p>
            <div className="flex items-center gap-2">
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs">Sayfa {data?.meta.page || 1}</span>
              <span className="rounded-full bg-muted px-2 py-0.5 text-xs">Sıralama: {filters.sort || "featured"}</span>
            </div>
          </div>

          {isLoading && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, i) => (
                <Skeleton key={i} className="h-80" />
              ))}
            </div>
          )}

          {isError && <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Ürünler yüklenemedi.</p>}

          {!isLoading && !isError && !data?.items.length && (
            <p className="rounded-xl border border-border bg-card p-6 text-center text-sm">Mevcut filtrelere uygun gitar bulunamadı.</p>
          )}

          {!!data?.items.length && (
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {data.items.map((product) => (
                <ProductCard key={product.id} locale={locale} product={product} onAddToCart={addToCart} />
              ))}
            </div>
          )}

          <div className="flex justify-end gap-2">
            <Button
              variant="outline"
              disabled={(data?.meta.page || 1) <= 1}
              onClick={() => setFilters((prev) => ({ ...prev, page: Math.max(1, (prev.page || 1) - 1) }))}
            >
              Önceki
            </Button>
            <Button
              variant="outline"
              disabled={(data?.meta.page || 1) >= (data?.meta.totalPages || 1)}
              onClick={() => setFilters((prev) => ({ ...prev, page: (prev.page || 1) + 1 }))}
            >
              Sonraki
            </Button>
          </div>
        </section>
      </div>
    </div>
  );
}
