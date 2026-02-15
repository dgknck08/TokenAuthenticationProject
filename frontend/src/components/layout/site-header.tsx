"use client";

import Link from "next/link";
import { UserRound } from "lucide-react";
import { ThemeToggle } from "@/components/layout/theme-toggle";
import { LocaleSwitcher } from "@/components/layout/locale-switcher";
import { Locale } from "@/types/api";
import { getDictionary } from "@/lib/i18n/dictionaries";
import { MiniCart } from "@/components/cart/mini-cart";

const navItems = ["home", "shop", "about", "contact", "faq"] as const;

export function SiteHeader({ locale }: { locale: Locale }) {
  const dict = getDictionary(locale);

  return (
    <header className="sticky top-0 z-50 border-b border-border/60 bg-background/90 backdrop-blur-xl">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link href={`/${locale}`} className="font-display text-xl font-bold tracking-tight">
          Dmusic
        </Link>

        <nav className="hidden items-center gap-6 md:flex">
          {navItems.map((item) => (
            <Link key={item} href={`/${locale}${item === "home" ? "" : `/${item}`}`} className="text-sm text-foreground/80 transition hover:text-foreground">
              {dict.nav[item]}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <LocaleSwitcher locale={locale} />
          <ThemeToggle />
          <MiniCart locale={locale} />
          <Link className="rounded-xl p-2 hover:bg-muted/70" href={`/${locale}/account`} aria-label="Hesap">
            <UserRound size={18} />
          </Link>
        </div>
      </div>
    </header>
  );
}
