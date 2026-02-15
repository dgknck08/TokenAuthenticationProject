"use client";

import { HomeHero } from "@/components/home/home-hero";
import { ProductCard } from "@/components/product/product-card";
import { Skeleton } from "@/components/ui/skeleton";
import { useProducts } from "@/lib/hooks/use-products";
import { useCart } from "@/lib/hooks/use-cart";
import { getDictionary } from "@/lib/i18n/dictionaries";
import { toast } from "sonner";
import { useParams } from "next/navigation";

export default function HomePage() {
  const params = useParams<{ locale: "en" | "tr" }>();
  const locale = params.locale;
  const dict = getDictionary(locale);
  const { data, isLoading, isError } = useProducts({ page: 1, limit: 8, sort: "featured" });
  const { addMutation } = useCart();

  const handleAdd = (id: number) => {
    addMutation.mutate(
      { productId: id, quantity: 1 },
      {
        onSuccess: () => toast.success("Sepete eklendi"),
        onError: () => toast.error("Ürün sepete eklenemedi")
      }
    );
  };

  return (
    <div className="space-y-14">
      <HomeHero locale={locale} title={dict.home.title} subtitle={dict.home.subtitle} cta={dict.home.cta} />

      <section className="space-y-5">
        <div className="flex items-end justify-between">
          <h2 className="font-display text-2xl font-bold">En Çok Satanlar</h2>
          <p className="text-sm text-foreground/70">Stüdyo testinden geçen modeller</p>
        </div>

        {isLoading && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-80" />
            ))}
          </div>
        )}

        {isError && <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Ürünler yüklenemedi.</p>}

        {!isLoading && !isError && !data?.items.length && (
          <p className="rounded-xl border border-border bg-card p-4 text-sm">Henüz ürün yok.</p>
        )}

        {!!data?.items.length && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {data.items.map((product) => (
              <ProductCard key={product.id} locale={locale} product={product} onAddToCart={handleAdd} />
            ))}
          </div>
        )}
      </section>

      <section className="grid gap-4 md:grid-cols-3">
        {[
          { title: "Akustik", copy: "Şarkı yazarları için sıcak ve doğal ton." },
          { title: "Elektro", copy: "Solo ve ritim için modern ton karakteri." },
          { title: "Bas", copy: "Netlikten ödün vermeyen güçlü alt frekanslar." }
        ].map((category) => (
          <article key={category.title} className="rounded-2xl border border-border bg-card p-6">
            <p className="text-xs uppercase tracking-[0.2em] text-primary">{category.title}</p>
            <p className="mt-2 text-sm text-foreground/75">{category.copy}</p>
          </article>
        ))}
      </section>

      <section className="grid gap-4 rounded-3xl border border-border bg-card p-8 md:grid-cols-2 md:items-center">
        <div>
          <p className="text-xs uppercase tracking-[0.25em] text-primary">Kampanya</p>
          <h3 className="mt-2 font-display text-3xl font-bold">Takas Haftası: %18&apos;e varan ek takas desteği</h3>
          <p className="mt-3 text-foreground/70">Eski enstrümanını getir, özel kurulum hizmetiyle üst seri modellere geç.</p>
        </div>
        <div className="grid grid-cols-3 gap-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-20 rounded-xl bg-muted/70" />
          ))}
        </div>
      </section>

      <section className="grid gap-5 md:grid-cols-[1fr_1.2fr]">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h3 className="font-display text-2xl font-bold">Ton bültenine katıl</h3>
          <p className="mt-2 text-sm text-foreground/70">Aylık sanatçı seçimleri, bakım ipuçları ve özel ürün duyuruları.</p>
          <div className="mt-4 flex gap-2">
            <input
              className="h-11 flex-1 rounded-xl border border-border bg-background px-3 text-sm"
              placeholder="eposta@ornek.com"
            />
            <button className="rounded-xl bg-primary px-4 text-sm font-semibold text-white">Abone ol</button>
          </div>
        </div>
        <div className="grid grid-cols-3 gap-2 rounded-2xl border border-border bg-card p-4">
          {Array.from({ length: 9 }).map((_, i) => (
            <div key={i} className="aspect-square rounded-lg bg-muted/70" />
          ))}
        </div>
      </section>
    </div>
  );
}
