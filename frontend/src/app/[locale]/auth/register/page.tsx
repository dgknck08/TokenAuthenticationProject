"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useRegister } from "@/lib/hooks/use-auth";
import { registerSchema, RegisterSchema } from "@/lib/schema/auth";
import { useParams } from "next/navigation";

export default function RegisterPage() {
  const params = useParams<{ locale: string }>();
  const locale = params.locale;
  const router = useRouter();
  const register = useRegister();
  const form = useForm<RegisterSchema>({ resolver: zodResolver(registerSchema) });

  const onSubmit = form.handleSubmit((values) => {
    register.mutate(values, {
      onSuccess: () => {
        toast.success("Kayıt tamamlandı");
        router.push(`/${locale}/account`);
      },
      onError: () => toast.error("Kayıt başarısız")
    });
  });

  return (
    <Card className="mx-auto max-w-md space-y-4 p-6">
      <h1 className="font-display text-3xl font-bold">Hesap Oluştur</h1>
      <form className="space-y-3" onSubmit={onSubmit}>
        <Input placeholder="Kullanıcı adı" {...form.register("username")} />
        <Input placeholder="E-posta" {...form.register("email")} />
        <Input placeholder="Ad" {...form.register("firstName")} />
        <Input placeholder="Soyad" {...form.register("lastName")} />
        <Input placeholder="Şifre" type="password" {...form.register("password")} />
        <Button className="w-full" type="submit" loading={register.isPending}>
          Kayıt ol
        </Button>
      </form>
      <Link className="text-sm text-primary underline" href={`/${locale}/auth/login`}>
        Zaten hesabın var mı?
      </Link>
    </Card>
  );
}
