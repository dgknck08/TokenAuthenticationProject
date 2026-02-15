"use client";

import Link from "next/link";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useMe } from "@/lib/hooks/use-auth";
import { useUpdateProfile } from "@/lib/hooks/use-account";
import { useParams } from "next/navigation";

export default function AccountPage() {
  const params = useParams<{ locale: string }>();
  const locale = params.locale;
  const meQuery = useMe();
  const updateProfile = useUpdateProfile();

  const form = useForm({
    values: {
      firstName: meQuery.data?.firstName || "",
      lastName: meQuery.data?.lastName || "",
      email: meQuery.data?.email || ""
    }
  });

  const onSubmit = form.handleSubmit((values) => {
    updateProfile.mutate(values, {
      onSuccess: () => toast.success("Profil güncellendi"),
      onError: () => toast.error("Profil güncellenemedi")
    });
  });

  if (meQuery.isLoading) return <p>Profil yükleniyor...</p>;

  if (meQuery.isError) {
    return (
      <Card className="p-6">
        <p className="text-sm text-danger">Önce giriş yapmanız gerekiyor.</p>
        <Link className="mt-2 inline-block text-primary underline" href={`/${locale}/auth/login`}>
          Giriş sayfasına git
        </Link>
      </Card>
    );
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_280px]">
      <Card className="space-y-4 p-6">
        <h1 className="font-display text-3xl font-bold">Profilim</h1>
        <form className="grid gap-3 sm:grid-cols-2" onSubmit={onSubmit}>
          <Input placeholder="Ad" {...form.register("firstName")} />
          <Input placeholder="Soyad" {...form.register("lastName")} />
          <Input className="sm:col-span-2" placeholder="E-posta" {...form.register("email")} />
          <Button type="submit" className="sm:col-span-2" loading={updateProfile.isPending}>
            Değişiklikleri kaydet
          </Button>
        </form>
      </Card>

      <Card className="space-y-3 p-5">
        <h2 className="font-semibold">Hesap paneli</h2>
        <Link className="text-sm text-primary underline" href={`/${locale}/account/orders`}>
          Siparişlerim
        </Link>
      </Card>
    </div>
  );
}
