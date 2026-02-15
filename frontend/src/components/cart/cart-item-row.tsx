"use client";

import { CartItem } from "@/types/api";
import { Button } from "@/components/ui/button";
import { formatPrice } from "@/lib/utils";
import { Input } from "@/components/ui/input";

interface CartItemRowProps {
  item: CartItem;
  onUpdate: (quantity: number) => void;
  onRemove: () => void;
}

export function CartItemRow({ item, onUpdate, onRemove }: CartItemRowProps) {
  return (
    <div className="grid gap-3 rounded-xl border border-border bg-card p-4 md:grid-cols-[1fr_auto_auto] md:items-center">
      <div>
        <p className="font-semibold">{item.productName}</p>
        <p className="text-sm text-foreground/70">Birim: {formatPrice(Number(item.unitPrice))}</p>
      </div>
      <Input
        type="number"
        min={1}
        className="w-24"
        value={item.quantity}
        onChange={(event) => onUpdate(Number(event.target.value))}
      />
      <div className="flex items-center gap-3 md:justify-end">
        <p className="font-semibold">{formatPrice(Number(item.totalPrice))}</p>
        <Button variant="ghost" onClick={onRemove}>
          Kaldır
        </Button>
      </div>
    </div>
  );
}
