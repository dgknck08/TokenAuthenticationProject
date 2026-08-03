import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface Category {
  readonly title: string;
  readonly subtitle: string;
  readonly description: string;
  readonly query: string;
  readonly tone: 'amber' | 'violet' | 'teal';
}

@Component({
  selector: 'app-category-showcase',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './category-showcase.component.html',
  styleUrl: './category-showcase.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CategoryShowcaseComponent {
  protected readonly categories: readonly Category[] = [
    {
      title: 'Gitarlar & Teller',
      subtitle: 'Akustik · Elektro · Bas',
      description:
        'İlk gitarınız da olabilir, sahnedeki beşinciniz de — ikisini de aynı ciddiyetle seçiyoruz.',
      query: 'gitar',
      tone: 'amber',
    },
    {
      title: 'Davul & Perküsyon',
      subtitle: 'Akustik · Elektronik · Pad',
      description:
        'İlk baget tutuştan turne setine; ritmin her hâli burada.',
      query: 'davul',
      tone: 'violet',
    },
    {
      title: 'Stüdyo & Ses',
      subtitle: 'Mikrofon · Arabirim · Monitör',
      description:
        'Yatak odası stüdyosu da olsa kayıt kayıttır. Ses kalitesinden vazgeçmeyin.',
      query: 'stüdyo',
      tone: 'teal',
    },
  ];
}
