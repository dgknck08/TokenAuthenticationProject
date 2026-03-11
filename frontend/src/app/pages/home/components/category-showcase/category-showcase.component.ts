import { Component } from '@angular/core';
import { Router } from '@angular/router';

export type AccentVariant = 'primary' | 'warm' | 'soft';

export interface Category {
  title: string;
  subtitle: string;
  description: string;
  accent: AccentVariant;
  slug: string;
}

@Component({
  selector: 'app-category-showcase',
  templateUrl: './category-showcase.component.html',
  styleUrls: ['./category-showcase.component.css'],
})
export class CategoryShowcaseComponent {

  readonly categories: Category[] = [
    {
      title: 'Gitarlar &\nTeller',
      subtitle: 'Akustik Gitar · Elektro Gitar · Bass Gitar',
      description:
        'Yeni başlayanlar için entry-level modellerden profesyonel sahne gitarlarına uzanan geniş yelpaze.',
      accent: 'primary',
      slug: 'gitarlar',
    },
    {
      title: 'Davul &\nPerküsyon',
      subtitle: 'Akustik Davul · Elektronik Davul · Pad',
      description:
        'Akustik setlerden elektronik davullara, ritim dünyasına adım atın ya da sahnede daha güçlü vurun.',
      accent: 'warm',
      slug: 'davul',
    },
    {
      title: 'Stüdyo &\nSes \n',
      subtitle: 'Mikrofon · Interface · Monitör',
      description:
        'Ev stüdyonuzdan profesyonel kayıt ortamına — ses kalitesinden ödün vermeden üretmeye devam edin.',
      accent: 'soft',
      slug: 'studyo',
    },
  ];

  constructor(private router: Router) {}

  onCategoryClick(category: Category): void {
    this.router.navigate(['/kategoriler', category.slug]);
  }
}
