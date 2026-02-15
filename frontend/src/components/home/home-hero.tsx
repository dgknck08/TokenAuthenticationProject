"use client";

import { motion } from "framer-motion";
import Link from "next/link";
import { Button } from "@/components/ui/button";

export function HomeHero({ locale, title, subtitle, cta }: { locale: string; title: string; subtitle: string; cta: string }) {
  return (
    <section className="relative overflow-hidden rounded-3xl border border-border bg-card px-6 py-14 sm:px-10 lg:px-14">
      <motion.div
        initial={{ opacity: 0, y: 24 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{ duration: 0.5 }}
        className="max-w-2xl"
      >
        <p className="mb-4 text-xs uppercase tracking-[0.25em] text-primary">Luthier Kalitesi Koleksiyon</p>
        <h1 className="font-display text-4xl font-bold leading-tight sm:text-5xl">{title}</h1>
        <p className="mt-4 text-base text-foreground/70 sm:text-lg">{subtitle}</p>
        <div className="mt-8">
          <Button className="px-7">
            <Link href={`/${locale}/shop`}>{cta}</Link>
          </Button>
        </div>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, scale: 1.1 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.8 }}
        className="pointer-events-none absolute -right-20 -top-16 h-72 w-72 rounded-full bg-primary/20 blur-3xl"
      />
    </section>
  );
}
