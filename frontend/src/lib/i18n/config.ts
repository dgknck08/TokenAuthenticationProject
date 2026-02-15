import { Locale } from "@/types/api";

export const locales: Locale[] = ["en", "tr"];
export const defaultLocale: Locale =
  (process.env.NEXT_PUBLIC_DEFAULT_LOCALE as Locale) || "en";

export function isLocale(value: string): value is Locale {
  return locales.includes(value as Locale);
}
