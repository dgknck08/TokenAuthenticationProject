import { Card } from "@/components/ui/card";

export default function PrivacyPage() {
  return (
    <Card className="space-y-4 p-8 text-sm text-foreground/75">
      <h1 className="font-display text-4xl font-bold text-foreground">Gizlilik Politikası</h1>
      <p>Profil ve sipariş verilerini yalnızca hizmet sunumu için işleriz. Hassas ödeme verileri PCI uyumlu bir sağlayıcı tarafından işlenmelidir.</p>
    </Card>
  );
}
