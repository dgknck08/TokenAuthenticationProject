"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Locale } from "@/types/api";

export function LocaleSwitcher({ locale }: { locale: Locale }) {
  const pathname = usePathname();
  const target = locale === "en" ? "tr" : "en";
  const swapped = pathname.replace(/^\/(en|tr)/, `/${target}`);

  return (
    <Link className="text-sm font-semibold text-foreground/80 hover:text-foreground" href={swapped}>
      {target.toUpperCase()}
    </Link>
  );
}
