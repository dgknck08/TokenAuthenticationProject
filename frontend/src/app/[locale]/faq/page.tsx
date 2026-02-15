import { Card } from "@/components/ui/card";

const faq = [
  { q: "Yurt dışına gönderim yapıyor musunuz?", a: "Evet, ödeme adımında kargo seçenekleri sunuluyor." },
  { q: "Gitar iadesi yapabilir miyim?", a: "İade talebi için 14 gün süreniz bulunuyor." },
  { q: "Ürünlerde kurulum yapılıyor mu?", a: "Her gitar gönderim öncesi temel ayardan geçer." }
];

export default function FaqPage() {
  return (
    <Card className="space-y-4 p-8">
      <h1 className="font-display text-4xl font-bold">Sık Sorulan Sorular</h1>
      <div className="space-y-3">
        {faq.map((item) => (
          <div key={item.q} className="rounded-xl border border-border p-4">
            <p className="font-semibold">{item.q}</p>
            <p className="text-sm text-foreground/75">{item.a}</p>
          </div>
        ))}
      </div>
    </Card>
  );
}
