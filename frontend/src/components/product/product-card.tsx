"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import Image from "next/image";
import { ShoppingBag } from "lucide-react";
import { Product } from "@/types/api";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/utils";
import { resolveProductImage } from "@/lib/product-images";

interface ProductCardProps {
  locale: "en" | "tr";
  product: Product;
  onAddToCart?: (id: number) => void;
}

export function ProductCard({ locale, product, onAddToCart }: ProductCardProps) {
  const imageSrc = resolveProductImage(product);
  const isInStock = product.stock > 0;

  return (
    <motion.div whileHover={{ y: -6 }} transition={{ duration: 0.2 }}>
      <Card className="group h-full overflow-hidden border-border/80">
        <Link href={`/${locale}/product/${product.id}`} className="block overflow-hidden">
          <div className="relative h-60 w-full bg-muted/35">
            <Image
              src={imageSrc}
              alt={product.name}
              fill
              className="object-cover transition duration-500 group-hover:scale-105"
            />
            <div className="absolute inset-x-0 top-0 flex items-start justify-between p-3">
              <span className="rounded-full border border-border/80 bg-background/90 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-foreground/70">
                {product.category}
              </span>
              <span
                className={`rounded-full px-2.5 py-1 text-[11px] font-semibold ${isInStock ? "bg-emerald-500/15 text-emerald-700" : "bg-rose-500/15 text-rose-700"}`}
              >
                {isInStock ? "Stokta" : "Tükendi"}
              </span>
            </div>
          </div>
        </Link>

        <div className="flex h-[calc(100%-15rem)] flex-col space-y-3 p-4">
          <div>
            <p className="text-xs uppercase tracking-wide text-foreground/60">{product.brand || "Dmusic"}</p>
            <Link href={`/${locale}/product/${product.id}`} className="line-clamp-2 font-semibold leading-tight hover:underline">
              {product.name}
            </Link>
          </div>

          <div className="mt-auto flex items-center justify-between">
            <p className="font-display text-lg font-bold">{formatPrice(Number(product.price))}</p>
            <Button
              variant="outline"
              className="gap-2"
              disabled={!isInStock}
              onClick={() => onAddToCart?.(product.id)}
              aria-label={`${product.name} ürününü sepete ekle`}
            >
              <ShoppingBag size={16} /> Ekle
            </Button>
          </div>
        </div>
      </Card>
    </motion.div>
  );
}
