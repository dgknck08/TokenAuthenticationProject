import type { Metadata } from "next";
import { Manrope, Sora } from "next/font/google";
import "@/app/globals.css";
import { Providers } from "@/components/layout/providers";

const sora = Sora({
  subsets: ["latin"],
  variable: "--font-display"
});

const manrope = Manrope({
  subsets: ["latin"],
  variable: "--font-body"
});

export const metadata: Metadata = {
  metadataBase: new URL(process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000"),
  title: {
    default: "Dmusic",
    template: "%s | Dmusic"
  },
  description: "Premium guitar e-commerce experience powered by a modern frontend architecture.",
  openGraph: {
    title: "Dmusic",
    description: "Premium guitar e-commerce experience.",
    type: "website"
  }
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${sora.variable} ${manrope.variable}`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
