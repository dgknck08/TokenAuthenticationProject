"use client";

import { Card } from "@/components/ui/card";
import { useMe } from "@/lib/hooks/use-auth";

export default function AdminPage() {
  const me = useMe();

  if (me.isLoading) return <p>Yükleniyor...</p>;
  if (me.data?.role !== "ADMIN") {
    return <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Sadece yönetici erişebilir.</p>;
  }

  return (
    <Card className="space-y-2 p-6">
      <h1 className="font-display text-3xl font-bold">Yönetim Paneli</h1>
      <p className="text-sm text-foreground/70">Backend admin/sipariş endpoint&apos;leri hazır olduğunda ürün CRUD ve sipariş yönetimi arayüzü bağlanmalıdır.</p>
    </Card>
  );
}
