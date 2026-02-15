"use client";

import Link from "next/link";
import { CartItemRow } from "@/components/cart/cart-item-row";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { useCart } from "@/lib/hooks/use-cart";
import { formatPrice } from "@/lib/utils";
import { useParams } from "next/navigation";

export default function CartPage() {
  const params = useParams<{ locale: string }>();
  const locale = params.locale;
  const { cartQuery, updateMutation, removeMutation } = useCart();

  if (cartQuery.isLoading) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-24" />
        <Skeleton className="h-24" />
      </div>
    );
  }

  if (cartQuery.isError) {
    return <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Sepet yüklenemedi.</p>;
  }

  const cart = cartQuery.data;
  if (!cart?.items?.length) {
    return (
      <Card className="p-8 text-center">
        <h1 className="font-display text-3xl font-bold">Sepetiniz boş</h1>
        <p className="mt-2 text-foreground/70">Sıradaki enstrümanınızı seçin.</p>
        <Link className="mt-6 inline-block text-primary underline" href={`/${locale}/shop`}>
          Mağazaya git
        </Link>
      </Card>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
      <section className="space-y-3">
        {cart.items.map((item) => (
          <CartItemRow
            key={item.productId}
            item={item}
            onUpdate={(quantity) => updateMutation.mutate({ productId: item.productId, quantity })}
            onRemove={() => removeMutation.mutate(item.productId)}
          />
        ))}
      </section>

      <aside>
        <Card className="space-y-4 p-5">
          <h2 className="font-display text-xl font-bold">Sipariş özeti</h2>
          <div className="flex justify-between text-sm">
            <span>Ürün adedi</span>
            <span>{cart.totalItems}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span>Toplam</span>
            <span className="font-semibold">{formatPrice(Number(cart.totalAmount))}</span>
          </div>
          <Input placeholder="Kupon kodu" />
          <Input placeholder="Posta kodu" />
          <Button className="w-full">
            <Link href={`/${locale}/checkout`}>Ödemeye geç</Link>
          </Button>
        </Card>
      </aside>
    </div>
  );
}
