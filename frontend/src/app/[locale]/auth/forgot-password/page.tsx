"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { forgotSchema, ForgotSchema } from "@/lib/schema/auth";

export default function ForgotPasswordPage() {
  const form = useForm<ForgotSchema>({ resolver: zodResolver(forgotSchema) });

  const onSubmit = form.handleSubmit(() => {
    toast.info("Backend henüz şifre sıfırlama endpoint'i sunmuyor.");
  });

  return (
    <Card className="mx-auto max-w-md space-y-4 p-6">
      <h1 className="font-display text-3xl font-bold">Şifremi Unuttum</h1>
      <p className="text-sm text-foreground/70">E-posta adresinizi girin; backend desteklediğinde sıfırlama talimatlarını göndereceğiz.</p>
      <form className="space-y-3" onSubmit={onSubmit}>
        <Input placeholder="E-posta" {...form.register("email")} />
        <Button className="w-full" type="submit">
          Sıfırlama bağlantısı gönder
        </Button>
      </form>
    </Card>
  );
}
