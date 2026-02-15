"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { useLogin } from "@/lib/hooks/use-auth";
import { loginSchema, LoginSchema } from "@/lib/schema/auth";
import { useParams } from "next/navigation";

export default function LoginPage() {
  const params = useParams<{ locale: string }>();
  const locale = params.locale;
  const router = useRouter();
  const login = useLogin();
  const form = useForm<LoginSchema>({ resolver: zodResolver(loginSchema) });

  const onSubmit = form.handleSubmit((values) => {
    login.mutate(values, {
      onSuccess: () => {
        toast.success("Tekrar hoş geldiniz");
        router.push(`/${locale}/account`);
      },
      onError: () => toast.error("Giriş başarısız")
    });
  });

  return (
    <Card className="mx-auto max-w-md space-y-4 p-6">
      <h1 className="font-display text-3xl font-bold">Giriş Yap</h1>
      <form className="space-y-3" onSubmit={onSubmit}>
        <Input placeholder="Kullanıcı adı" {...form.register("username")} />
        <Input placeholder="Şifre" type="password" {...form.register("password")} />
        <Button className="w-full" type="submit" loading={login.isPending}>
          Giriş yap
        </Button>
      </form>
      <div className="flex justify-between text-sm">
        <Link className="text-primary underline" href={`/${locale}/auth/register`}>
          Hesap oluştur
        </Link>
        <Link className="text-primary underline" href={`/${locale}/auth/forgot-password`}>
          Şifremi unuttum
        </Link>
      </div>
    </Card>
  );
}
