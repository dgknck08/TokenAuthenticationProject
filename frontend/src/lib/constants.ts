export const ROUTES = {
  home: "",
  shop: "shop",
  cart: "cart",
  checkout: "checkout",
  account: "account"
} as const;

export const SORT_OPTIONS = [
  { value: "featured", label: "Öne Çıkanlar" },
  { value: "price_asc", label: "Fiyat: Düşükten Yükseğe" },
  { value: "price_desc", label: "Fiyat: Yüksekten Düşüğe" }
] as const;

export const PRODUCT_TYPES = ["acoustic", "electric", "bass"] as const;
