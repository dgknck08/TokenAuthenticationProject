"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { ShoppingCart, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { useCart } from "@/lib/hooks/use-cart";
import { cn, formatPrice } from "@/lib/utils";
import { Locale } from "@/types/api";

interface MiniCartProps {
  locale: Locale;
}

export function MiniCart({ locale }: MiniCartProps) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const { cartQuery, updateMutation, removeMutation } = useCart();

  const cart = cartQuery.data;
  const cartCount = cart?.totalItems ?? 0;

  useEffect(() => {
    function handleOutsideClick(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    document.addEventListener("mousedown", handleOutsideClick);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleOutsideClick);
      document.removeEventListener("keydown", handleEscape);
    };
  }, []);

  return (
    <div className="relative" ref={containerRef}>
      <button
        type="button"
        aria-label="Sepet"
        className="relative rounded-xl p-2 hover:bg-muted/70"
        onClick={() => setIsOpen((prev) => !prev)}
      >
        <ShoppingCart size={18} />
        {cartCount > 0 ? (
          <span className="absolute -right-1 -top-1 inline-flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[11px] font-semibold text-white">
            {cartCount > 99 ? "99+" : cartCount}
          </span>
        ) : null}
      </button>

      <div
        className={cn(
          "absolute right-0 top-full z-50 mt-2 w-[min(92vw,380px)] rounded-2xl border border-border bg-card p-4 shadow-2xl",
          isOpen ? "block" : "hidden"
        )}
      >
        <div className="mb-3 flex items-center justify-between">
          <h3 className="font-display text-lg font-bold">Sepetiniz</h3>
          <span className="text-xs text-foreground/70">{cartCount} ürün</span>
        </div>

        {cartQuery.isLoading ? (
          <div className="space-y-2">
            <Skeleton className="h-16" />
            <Skeleton className="h-16" />
          </div>
        ) : null}

        {cartQuery.isError ? (
          <p className="rounded-xl border border-danger/40 bg-danger/10 p-3 text-sm">
            Sepet yüklenemedi.
          </p>
        ) : null}

        {!cartQuery.isLoading && !cartQuery.isError && !cart?.items?.length ? (
          <p className="rounded-xl bg-muted/60 p-3 text-sm text-foreground/80">Sepetiniz boş.</p>
        ) : null}

        {!cartQuery.isLoading && !cartQuery.isError && cart?.items?.length ? (
          <div className="max-h-72 space-y-2 overflow-auto pr-1">
            {cart.items.map((item) => (
              <div key={item.productId} className="rounded-xl border border-border/60 p-3">
                <div className="mb-2 flex items-start justify-between gap-2">
                  <p className="line-clamp-2 text-sm font-semibold">{item.productName}</p>
                  <button
                    type="button"
                    aria-label={`${item.productName} ürününü kaldır`}
                    onClick={() => removeMutation.mutate(item.productId)}
                    className="rounded-lg p-1 text-foreground/70 transition hover:bg-muted hover:text-foreground"
                  >
                    <Trash2 size={14} />
                  </button>
                </div>

                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-1">
                    <Button
                      type="button"
                      variant="outline"
                      className="h-7 w-7 rounded-lg px-0 py-0"
                      onClick={() =>
                        updateMutation.mutate({
                          productId: item.productId,
                          quantity: Math.max(1, item.quantity - 1)
                        })
                      }
                    >
                      -
                    </Button>
                    <span className="w-8 text-center text-sm">{item.quantity}</span>
                    <Button
                      type="button"
                      variant="outline"
                      className="h-7 w-7 rounded-lg px-0 py-0"
                      onClick={() =>
                        updateMutation.mutate({
                          productId: item.productId,
                          quantity: item.quantity + 1
                        })
                      }
                    >
                      +
                    </Button>
                  </div>
                  <p className="text-sm font-semibold">{formatPrice(Number(item.totalPrice))}</p>
                </div>
              </div>
            ))}
          </div>
        ) : null}

        <div className="mt-4 space-y-2 border-t border-border/60 pt-3">
          <div className="flex items-center justify-between text-sm">
            <span>Ara toplam</span>
            <span className="font-semibold">{formatPrice(Number(cart?.totalAmount ?? 0))}</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <Link
              href={`/${locale}/cart`}
              onClick={() => setIsOpen(false)}
              className="inline-flex items-center justify-center rounded-xl border border-border bg-transparent px-4 py-2 text-sm font-semibold transition hover:bg-muted/80"
            >
              Sepete Git
            </Link>
            <Link
              href={`/${locale}/checkout`}
              onClick={() => setIsOpen(false)}
              className={cn(
                "inline-flex items-center justify-center rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white transition hover:opacity-90"
              )}
            >
              Ödeme
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
