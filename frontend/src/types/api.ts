export type Locale = "en" | "tr";

export interface ApiErrorResponse {
  code: string;
  message: string;
  timestamp: string;
  path: string;
}

export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  category: string;
  brand?: string;
  sku?: string;
  color?: string;
  size?: string;
  attributesJson?: string;
  stock: number;
}

export interface ProductFilters {
  query?: string;
  brand?: string;
  category?: string;
  type?: string;
  minPrice?: number;
  maxPrice?: number;
  sort?: "featured" | "price_asc" | "price_desc";
  page?: number;
  limit?: number;
}

export interface CartItem {
  productId: number;
  productName: string;
  productImage?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  totalAmount: number;
  cartType: "authenticated" | "guest" | string;
}

export interface AuthResponse {
  accessToken: string;
  username: string;
  email: string;
}

export interface UserProfile {
  id: number;
  username: string;
  email: string;
  role: string;
  firstName: string;
  lastName: string;
}

export interface CheckoutFormValues {
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: string;
  city: string;
  postalCode: string;
  shippingMethod: "standard" | "express";
  paymentMethod: "card" | "cod";
  cardNumber?: string;
  cardHolder?: string;
  expiry?: string;
  cvc?: string;
}

export interface OrderItem {
  id?: number;
  productId?: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  lineTotal?: number;
}

export interface OrderSummary {
  id: number;
  createdAt: string;
  total: number;
  status: "CREATED" | "PAID" | "REFUNDED" | "CANCELLED";
  paymentMethod?: "CARD" | "COD";
  paidAt?: string;
  cancelledAt?: string;
  refundedAt?: string;
  items: OrderItem[];
}
