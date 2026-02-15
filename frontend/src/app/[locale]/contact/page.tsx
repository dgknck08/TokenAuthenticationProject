import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";

export default function ContactPage() {
  return (
    <Card className="space-y-4 p-8">
      <h1 className="font-display text-4xl font-bold">İletişim</h1>
      <form className="space-y-3">
        <Input placeholder="Adınız" />
        <Input placeholder="E-posta" />
        <Textarea rows={5} placeholder="Mesajınız" />
        <Button type="button">Mesaj gönder</Button>
      </form>
    </Card>
  );
}
