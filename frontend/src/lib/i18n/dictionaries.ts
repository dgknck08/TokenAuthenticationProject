import { Locale } from "@/types/api";

const dictionaries = {
  en: {
    nav: {
      home: "Home",
      shop: "Shop",
      about: "About",
      contact: "Contact",
      faq: "FAQ",
      cart: "Cart",
      account: "Account",
      login: "Login"
    },
    home: {
      title: "Dmusic",
      subtitle: "Professional and beginner-level guitars, gear, and accessories. Quality sound, secure shopping, and fast delivery at D Music.",
      cta: "Explore Products"
    }
  },
  tr: {
    nav: {
      home: "Ana Sayfa",
      shop: "Mağaza",
      about: "Hakkımızda",
      contact: "İletişim",
      faq: "SSS",
      cart: "Sepet",
      account: "Hesabım",
      login: "Giriş"
    },
    home: {
      title: "Dmusic",
      subtitle: "Profesyonel ve başlangıç seviyesi gitarlar, ekipmanlar ve aksesuarlar. Kaliteli ses, güvenli alışveriş ve hızlı teslimat D Music'te.",
      cta: "Ürünleri Keşfet"
    }
  }
} as const;

export function getDictionary(locale: Locale) {
  return dictionaries[locale] ?? dictionaries.en;
}
