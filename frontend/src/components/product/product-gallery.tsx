"use client";

import Image from "next/image";
import { useEffect, useMemo, useState } from "react";

export function ProductGallery({ images }: { images: string[] }) {
  const safeImages = useMemo(() => images.filter(Boolean), [images]);
  const [active, setActive] = useState(safeImages[0] || "");

  useEffect(() => {
    setActive(safeImages[0] || "");
  }, [safeImages]);

  if (!active) {
    return (
      <div className="flex h-[440px] items-center justify-center rounded-2xl border border-border bg-card">
        <p className="text-sm text-foreground/60">No image available.</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="relative h-[440px] overflow-hidden rounded-2xl border border-border bg-card">
        <Image src={active} alt="Guitar image" fill className="object-cover transition duration-300 hover:scale-110" />
      </div>
      {safeImages.length > 1 && (
        <div className="grid grid-cols-4 gap-3">
          {safeImages.map((src) => (
          <button
            type="button"
            key={src}
            onClick={() => setActive(src)}
            className={`relative h-20 overflow-hidden rounded-xl border ${src === active ? "border-primary" : "border-border"}`}
          >
            <Image src={src} alt="Thumbnail" fill className="object-cover" />
          </button>
          ))}
        </div>
      )}
    </div>
  );
}
