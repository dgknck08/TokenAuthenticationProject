import Link from "next/link";

export default function NotFound() {
  return (
    <main className="mx-auto flex min-h-[70vh] max-w-xl flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="font-display text-4xl font-bold">404</h1>
      <p className="text-foreground/70">Sayfa bulunamadı.</p>
      <Link href="/en" className="text-primary underline">
        Ana sayfaya dön
      </Link>
    </main>
  );
}
