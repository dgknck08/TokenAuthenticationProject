"use client";

import { Button } from "@/components/ui/button";

export default function GlobalError({ reset }: { error: Error; reset: () => void }) {
  return (
    <main className="mx-auto flex min-h-[70vh] max-w-xl flex-col items-center justify-center gap-4 px-4 text-center">
      <h2 className="font-display text-3xl font-bold">Bir hata oluştu.</h2>
      <p className="text-foreground/70">Sayfayı yenileyin veya daha sonra tekrar deneyin.</p>
      <Button onClick={reset}>Tekrar dene</Button>
    </main>
  );
}
