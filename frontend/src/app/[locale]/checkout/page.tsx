"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { checkoutSchema, CheckoutSchema } from "@/lib/schema/checkout";
import { useCreateOrder } from "@/lib/hooks/use-account";
import { useCart } from "@/lib/hooks/use-cart";
import { formatPrice } from "@/lib/utils";
import { useParams } from "next/navigation";

export default function CheckoutPage() {
  const params = useParams<{ locale: string }>();
  const locale = params.locale;
  const router = useRouter();
  const { cartQuery } = useCart();
  const createOrder = useCreateOrder();

  const form = useForm<CheckoutSchema>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: {
      shippingMethod: "standard",
      paymentMethod: "card"
    }
  });

  const payment = form.watch("paymentMethod");

  const onSubmit = form.handleSubmit((values) => {
    createOrder.mutate(
      {
        customer: values,
        items: cartQuery.data?.items || [],
        total: cartQuery.data?.totalAmount || 0
      },
      {
        onSuccess: (data) => {
          toast.success(`Sipariş oluşturuldu: ${data.id}`);
          router.push(`/${locale}/account/orders/${data.id}`);
        },
        onError: () => toast.error("Ödeme tamamlanamadı. Sipariş endpoint'ini yapılandırın veya mock checkout'u etkinleştirin.")
      }
    );
  });

  return (
    <form className="grid gap-6 lg:grid-cols-[1fr_360px]" onSubmit={onSubmit}>
      <section className="space-y-4">
        <Card className="grid gap-3 p-5 sm:grid-cols-2">
          <Input placeholder="Ad" {...form.register("firstName")} />
          <Input placeholder="Soyad" {...form.register("lastName")} />
          <Input className="sm:col-span-2" placeholder="E-posta" {...form.register("email")} />
          <Input className="sm:col-span-2" placeholder="Telefon" {...form.register("phone")} />
        </Card>

        <Card className="grid gap-3 p-5 sm:grid-cols-2">
          <Input className="sm:col-span-2" placeholder="Adres" {...form.register("address")} />
          <Input placeholder="Şehir" {...form.register("city")} />
          <Input placeholder="Posta kodu" {...form.register("postalCode")} />
        </Card>

        <Card className="grid gap-3 p-5 sm:grid-cols-2">
          <Select {...form.register("shippingMethod")}>
            <option value="standard">Standart kargo</option>
            <option value="express">Hızlı kargo</option>
          </Select>
          <Select {...form.register("paymentMethod")}>
            <option value="card">Kredi kartı</option>
            <option value="cod">Kapıda ödeme</option>
          </Select>

          {payment === "card" && (
            <>
              <Input className="sm:col-span-2" placeholder="Kart numarası" {...form.register("cardNumber")} />
              <Input className="sm:col-span-2" placeholder="Kart sahibi" {...form.register("cardHolder")} />
              <Input placeholder="MM/YY" {...form.register("expiry")} />
              <Input placeholder="CVC" {...form.register("cvc")} />
            </>
          )}
        </Card>
      </section>

      <aside>
        <Card className="space-y-4 p-5">
          <h2 className="font-display text-xl font-bold">Ödeme özeti</h2>
          <div className="flex justify-between text-sm">
            <span>Ürün adedi</span>
            <span>{cartQuery.data?.totalItems || 0}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span>Toplam</span>
            <span className="font-semibold">{formatPrice(Number(cartQuery.data?.totalAmount || 0))}</span>
          </div>
          <Button className="w-full" type="submit" loading={createOrder.isPending}>
            Siparişi tamamla
          </Button>
        </Card>
      </aside>
    </form>
  );
}
