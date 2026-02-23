"use client";

import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { useMe } from "@/lib/hooks/use-auth";
import { productsApi, ProductUpsertPayload } from "@/lib/api/products";
import { ordersApi } from "@/lib/api/orders";
import { queryKeys } from "@/lib/query-keys";
import { Product } from "@/types/api";
import { formatPrice } from "@/lib/utils";
import { toast } from "sonner";

const EMPTY_FORM: ProductUpsertPayload = {
  name: "",
  description: "",
  price: 0,
  imageUrl: "",
  category: "",
  brand: "",
  sku: "",
  color: "",
  size: "",
  attributesJson: "",
  stock: 0
};

export default function AdminPage() {
  const me = useMe();
  const queryClient = useQueryClient();
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<ProductUpsertPayload>(EMPTY_FORM);

  const productsQuery = useQuery({
    queryKey: [...queryKeys.products, "admin"],
    queryFn: productsApi.getAdminList
  });

  const ordersQuery = useQuery({
    queryKey: [...queryKeys.orders, "admin"],
    queryFn: ordersApi.getAdminAll
  });

  const uploadMutation = useMutation({
    mutationFn: productsApi.uploadImage,
    onSuccess: (data) => {
      setForm((prev) => ({ ...prev, imageUrl: data.imageUrl }));
      toast.success("Görsel yüklendi.");
    },
    onError: () => toast.error("Görsel yüklenemedi.")
  });

  const createMutation = useMutation({
    mutationFn: productsApi.create,
    onSuccess: () => {
      toast.success("Ürün oluşturuldu.");
      resetForm();
      invalidateProducts(queryClient);
    },
    onError: () => toast.error("Ürün oluşturulamadı.")
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }: { id: number; payload: ProductUpsertPayload }) => productsApi.update(id, payload),
    onSuccess: () => {
      toast.success("Ürün güncellendi.");
      resetForm();
      invalidateProducts(queryClient);
    },
    onError: () => toast.error("Ürün güncellenemedi.")
  });

  const deleteMutation = useMutation({
    mutationFn: productsApi.remove,
    onSuccess: () => {
      toast.success("Ürün silindi.");
      invalidateProducts(queryClient);
    },
    onError: () => toast.error("Ürün silinemedi.")
  });

  const cancelOrderMutation = useMutation({
    mutationFn: ordersApi.adminCancel,
    onSuccess: () => {
      toast.success("Sipariş iptal edildi.");
      queryClient.invalidateQueries({ queryKey: [...queryKeys.orders, "admin"] });
    },
    onError: () => toast.error("Sipariş iptal edilemedi.")
  });

  const refundOrderMutation = useMutation({
    mutationFn: ordersApi.adminRefund,
    onSuccess: () => {
      toast.success("Sipariş refund edildi.");
      queryClient.invalidateQueries({ queryKey: [...queryKeys.orders, "admin"] });
    },
    onError: () => toast.error("Refund işlemi başarısız.")
  });

  const isBusy = uploadMutation.isPending || createMutation.isPending || updateMutation.isPending;
  const isAdmin = me.data?.role === "ROLE_ADMIN" || me.data?.role === "ADMIN";
  const products = useMemo(() => productsQuery.data || [], [productsQuery.data]);
  const orders = useMemo(() => ordersQuery.data || [], [ordersQuery.data]);

  function resetForm() {
    setForm(EMPTY_FORM);
    setEditingId(null);
  }

  function startEdit(product: Product) {
    setEditingId(product.id);
    setForm({
      name: product.name || "",
      description: product.description || "",
      price: Number(product.price || 0),
      imageUrl: product.imageUrl || "",
      category: product.category || "",
      brand: product.brand || "",
      sku: product.sku || "",
      color: product.color || "",
      size: product.size || "",
      attributesJson: product.attributesJson || "",
      stock: Number(product.stock || 0)
    });
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    const payload: ProductUpsertPayload = {
      ...form,
      price: Number(form.price),
      stock: Number(form.stock)
    };

    if (editingId) {
      await updateMutation.mutateAsync({ id: editingId, payload });
    } else {
      await createMutation.mutateAsync(payload);
    }
  }

  if (me.isLoading) return <p>Yükleniyor...</p>;
  if (!isAdmin) {
    return <p className="rounded-xl border border-danger/40 bg-danger/10 p-4 text-sm">Sadece yönetici erişebilir.</p>;
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
      <Card className="space-y-4 p-6">
        <h1 className="font-display text-2xl font-bold">{editingId ? "Ürün Düzenle" : "Yeni Ürün"}</h1>
        <form className="space-y-3" onSubmit={onSubmit}>
          <Input placeholder="Ürün adı" value={form.name} onChange={(e) => setForm((p) => ({ ...p, name: e.target.value }))} required />
          <Textarea
            placeholder="Açıklama"
            value={form.description}
            onChange={(e) => setForm((p) => ({ ...p, description: e.target.value }))}
            required
          />
          <div className="grid grid-cols-2 gap-3">
            <Input type="number" step="0.01" placeholder="Fiyat" value={String(form.price)} onChange={(e) => setForm((p) => ({ ...p, price: Number(e.target.value) }))} required />
            <Input type="number" placeholder="Stok" value={String(form.stock)} onChange={(e) => setForm((p) => ({ ...p, stock: Number(e.target.value) }))} required />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <Input placeholder="Kategori" value={form.category} onChange={(e) => setForm((p) => ({ ...p, category: e.target.value }))} required />
            <Input placeholder="Marka" value={form.brand || ""} onChange={(e) => setForm((p) => ({ ...p, brand: e.target.value }))} />
          </div>
          <Input placeholder="SKU" value={form.sku || ""} onChange={(e) => setForm((p) => ({ ...p, sku: e.target.value }))} />
          <Input placeholder="Görsel URL" value={form.imageUrl || ""} onChange={(e) => setForm((p) => ({ ...p, imageUrl: e.target.value }))} />
          <div className="space-y-2">
            <Input
              type="file"
              accept="image/*"
              onChange={(e) => {
                const file = e.target.files?.[0];
                if (file) uploadMutation.mutate(file);
              }}
            />
            <p className="text-xs text-foreground/70">Dosya seçince backend upload endpoint&apos;ine gönderilir ve URL alanı güncellenir.</p>
          </div>
          <div className="flex gap-2">
            <Button type="submit" loading={isBusy} className="flex-1">
              {editingId ? "Güncelle" : "Oluştur"}
            </Button>
            <Button type="button" variant="outline" onClick={resetForm}>
              Temizle
            </Button>
          </div>
        </form>
      </Card>

      <div className="space-y-6">
        <Card className="p-6">
          <h2 className="mb-4 font-display text-2xl font-bold">Ürünler</h2>
          {productsQuery.isLoading ? <p>Ürünler yükleniyor...</p> : null}
          {productsQuery.isError ? <p className="text-sm text-danger">Ürün listesi alınamadı.</p> : null}
          <div className="space-y-3">
            {products.map((product) => (
              <div key={product.id} className="flex items-start justify-between rounded-xl border border-border p-3">
                <div>
                  <p className="font-semibold">{product.name}</p>
                  <p className="text-xs text-foreground/70">
                    #{product.id} | {product.category} | {product.brand || "N/A"}
                  </p>
                  <p className="text-sm">{formatPrice(Number(product.price))}</p>
                </div>
                <div className="flex gap-2">
                  <Button variant="outline" onClick={() => startEdit(product)}>
                    Düzenle
                  </Button>
                  <Button variant="danger" onClick={() => deleteMutation.mutate(product.id)} loading={deleteMutation.isPending}>
                    Sil
                  </Button>
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card className="p-6">
          <h2 className="mb-4 font-display text-2xl font-bold">Siparişler</h2>
          {ordersQuery.isLoading ? <p>Siparişler yükleniyor...</p> : null}
          {ordersQuery.isError ? <p className="text-sm text-danger">Sipariş listesi alınamadı.</p> : null}
          <div className="space-y-3">
            {orders.map((order) => (
              <div key={order.id} className="rounded-xl border border-border p-3">
                <div className="flex items-center justify-between gap-3">
                  <div>
                    <p className="font-semibold">#{order.id}</p>
                    <p className="text-xs text-foreground/70">{new Date(order.createdAt).toLocaleString()}</p>
                    <p className="text-xs text-foreground/70">Durum: {order.status}</p>
                    <p className="text-sm">{formatPrice(order.total)}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      disabled={order.status !== "CREATED"}
                      loading={cancelOrderMutation.isPending}
                      onClick={() => cancelOrderMutation.mutate(order.id)}
                    >
                      Cancel
                    </Button>
                    <Button
                      variant="danger"
                      disabled={order.status !== "PAID"}
                      loading={refundOrderMutation.isPending}
                      onClick={() => refundOrderMutation.mutate(order.id)}
                    >
                      Refund
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    </div>
  );
}

function invalidateProducts(queryClient: ReturnType<typeof useQueryClient>) {
  queryClient.invalidateQueries({ queryKey: queryKeys.products });
}
