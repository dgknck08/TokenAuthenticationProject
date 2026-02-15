export function RatingSummary({ rating = 4.8, reviews = 128 }: { rating?: number; reviews?: number }) {
  return (
    <div className="rounded-xl border border-border bg-card p-4">
      <p className="font-display text-3xl font-bold">{rating.toFixed(1)}</p>
      <p className="text-sm text-foreground/70">{reviews} doğrulanmış değerlendirme</p>
      <div className="mt-3 h-2 rounded-full bg-muted">
        <div className="h-full rounded-full bg-primary" style={{ width: `${(rating / 5) * 100}%` }} />
      </div>
    </div>
  );
}
