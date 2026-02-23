"use client";

import Link from "next/link";
import { Card } from "@/components/ui/card";
import { useOrders } from "@/lib/hooks/use-account";
import { formatPrice } from "@/lib/utils";
import { useParams } from "next/navigation";

export default function OrdersPage() {
  const params = useParams<{ locale: string }>();
  const locale = params.locale;
  const ordersQuery = useOrders();

  if (ordersQuery.isLoading) return <p>Siparişler yükleniyor...</p>;

  if (ordersQuery.isError) {
    return <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Sipariş endpoint&apos;i henüz kullanılabilir değil.</p>;
  }

  if (!ordersQuery.data?.length) {
    return <Card className="p-6 text-sm">Henüz sipariş yok.</Card>;
  }

  const statusLabel: Record<string, string> = {
    CREATED: "Oluşturuldu",
    PAID: "Ödendi",
    REFUNDED: "Refund",
    CANCELLED: "İptal"
  };

  return (
    <div className="space-y-3">
      <h1 className="font-display text-3xl font-bold">Siparişlerim</h1>
      {ordersQuery.data.map((order) => (
        <Card key={order.id} className="flex items-center justify-between p-4">
          <div>
            <p className="font-semibold">#{order.id}</p>
            <p className="text-sm text-foreground/70">{new Date(order.createdAt).toLocaleString()}</p>
            <p className="text-xs text-foreground/70">Durum: {statusLabel[order.status] || order.status}</p>
          </div>
          <div className="text-right">
            <p className="font-semibold">{formatPrice(order.total)}</p>
            <Link href={`/${locale}/account/orders/${order.id}`} className="text-sm text-primary underline">
              Detay
            </Link>
          </div>
        </Card>
      ))}
    </div>
  );
}
