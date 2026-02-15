"use client";

import { useMemo, useState } from "react";
import { notFound } from "next/navigation";
import { ProductGallery } from "@/components/product/product-gallery";
import { RatingSummary } from "@/components/product/rating-summary";
import { ProductCard } from "@/components/product/product-card";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { useProduct, useProducts } from "@/lib/hooks/use-products";
import { useCart } from "@/lib/hooks/use-cart";
import { formatPrice } from "@/lib/utils";
import { toast } from "sonner";
import { createBreadcrumbSchema, createProductSchema } from "@/lib/schema/seo";
import { useParams } from "next/navigation";
import { buildProductGalleryImages } from "@/lib/product-images";

export default function ProductDetailPage() {
  const params = useParams<{ locale: "en" | "tr"; id: string }>();
  const locale = params.locale;
  const id = Number(params.id);
  const { data: product, isLoading, isError } = useProduct(id);
  const { data: related } = useProducts({ page: 1, limit: 4, sort: "featured" });
  const { addMutation } = useCart();
  const [quantity, setQuantity] = useState(1);

  const images = useMemo(() => {
    if (!product) return [];
    return buildProductGalleryImages(product);
  }, [product]);

  if (!Number.isFinite(id)) notFound();

  if (isLoading) {
    return (
      <div className="grid gap-6 lg:grid-cols-2">
        <Skeleton className="h-[520px]" />
        <Skeleton className="h-[520px]" />
      </div>
    );
  }

  if (isError || !product) {
    return <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Ürün bulunamadı.</p>;
  }

  const appUrl = process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000";
  const productUrl = `${appUrl}/${locale}/product/${product.id}`;
  const productSchema = createProductSchema(product, productUrl);
  const breadcrumbSchema = createBreadcrumbSchema([
    { name: "Ana Sayfa", url: `${appUrl}/${locale}` },
    { name: "Mağaza", url: `${appUrl}/${locale}/shop` },
    { name: product.name, url: productUrl }
  ]);

  const addToCart = () => {
    addMutation.mutate(
      { productId: product.id, quantity },
      {
        onSuccess: () => toast.success("Sepete eklendi"),
        onError: () => toast.error("Sepete eklenemedi")
      }
    );
  };

  return (
    <div className="space-y-12">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(productSchema) }} />
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(breadcrumbSchema) }} />

      <section className="grid gap-8 lg:grid-cols-[1.1fr_1fr]">
        <ProductGallery images={images} />

        <div className="space-y-5">
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-xs uppercase tracking-[0.2em] text-primary">{product.brand || "Dmusic"}</p>
            <span className="rounded-full bg-muted px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-foreground/70">
              {product.category}
            </span>
            {product.sku ? (
              <span className="rounded-full border border-border px-2.5 py-1 text-[11px] font-semibold text-foreground/70">
                SKU: {product.sku}
              </span>
            ) : null}
          </div>
          <h1 className="font-display text-4xl font-bold">{product.name}</h1>
          <p className="text-foreground/75">{product.description}</p>
          <p className="font-display text-3xl font-bold">{formatPrice(Number(product.price))}</p>
          <p className={`text-sm font-semibold ${product.stock > 0 ? "text-emerald-700" : "text-rose-700"}`}>
            {product.stock > 0 ? `${product.stock} adet sevke hazır` : "Stokta yok"}
          </p>

          <div className="grid gap-3 sm:grid-cols-2">
            <Select value={String(quantity)} onChange={(e) => setQuantity(Number(e.target.value))}>
              {[1, 2, 3, 4].map((qty) => (
                <option key={qty} value={qty}>
                  Adet {qty}
                </option>
              ))}
            </Select>
            <Button onClick={addToCart} loading={addMutation.isPending} disabled={product.stock <= 0}>
              Sepete Ekle
            </Button>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-xl border border-border bg-card p-4 text-sm text-foreground/75">
              <p className="font-semibold text-foreground">Taksit</p>
              <p className="mt-1">3 taksit faizsiz ödeme seçeneği.</p>
            </div>
            <div className="rounded-xl border border-border bg-card p-4 text-sm text-foreground/75">
              <p className="font-semibold text-foreground">Kargolama</p>
              <p className="mt-1">16:00 öncesi siparişlerde aynı gün kargo.</p>
            </div>
          </div>

          <RatingSummary rating={4.9} reviews={312} />
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="font-display text-2xl font-bold">Benzer ürünler</h2>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {(related?.items || [])
            .filter((item) => item.id !== product.id)
            .slice(0, 4)
            .map((item) => (
              <ProductCard key={item.id} locale={locale} product={item} onAddToCart={(pid) => addMutation.mutate({ productId: pid, quantity: 1 })} />
            ))}
        </div>
      </section>
    </div>
  );
}
