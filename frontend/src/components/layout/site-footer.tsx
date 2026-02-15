import Link from "next/link";
import { Locale } from "@/types/api";

export function SiteFooter({ locale }: { locale: Locale }) {
  return (
    <footer className="mt-20 border-t border-border/60 py-10">
      <div className="mx-auto flex max-w-7xl flex-col gap-4 px-4 text-sm text-foreground/70 sm:px-6 lg:px-8 md:flex-row md:items-center md:justify-between">
        <p>2026 Dmusic. Tüm hakları saklıdır.</p>
        <div className="flex gap-4">
          <Link href={`/${locale}/terms`}>Şartlar</Link>
          <Link href={`/${locale}/privacy`}>Gizlilik</Link>
        </div>
      </div>
    </footer>
  );
}
