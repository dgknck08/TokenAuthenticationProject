"use client";

import { Card } from "@/components/ui/card";
import { useOrder } from "@/lib/hooks/use-account";
import { formatPrice } from "@/lib/utils";
import { useParams } from "next/navigation";

export default function OrderDetailPage() {
  const params = useParams<{ id: string }>();
  const orderQuery = useOrder(params.id);

  if (orderQuery.isLoading) return <p>Sipariş yükleniyor...</p>;
  if (orderQuery.isError || !orderQuery.data) {
    return <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Sipariş bulunamadı.</p>;
  }

  const statusLabel: Record<string, string> = {
    CREATED: "Oluşturuldu",
    PAID: "Ödendi",
    REFUNDED: "Refund",
    CANCELLED: "İptal"
  };

  return (
    <Card className="space-y-4 p-6">
      <h1 className="font-display text-3xl font-bold">Sipariş #{orderQuery.data.id}</h1>
      <p className="text-sm text-foreground/70">Durum: {statusLabel[orderQuery.data.status] || orderQuery.data.status}</p>
      {orderQuery.data.paymentMethod ? (
        <p className="text-sm text-foreground/70">Ödeme yöntemi: {orderQuery.data.paymentMethod}</p>
      ) : null}
      <div className="space-y-2">
        {orderQuery.data.items.map((item) => (
          <div key={`${item.productId || item.id}-${item.quantity}`} className="flex justify-between border-b border-border pb-2 text-sm">
            <span>
              {item.productName} x {item.quantity}
            </span>
            <span>{formatPrice(Number(item.lineTotal ?? item.unitPrice * item.quantity))}</span>
          </div>
        ))}
      </div>
      <p className="text-lg font-semibold">Toplam: {formatPrice(orderQuery.data.total)}</p>
    </Card>
  );
}
