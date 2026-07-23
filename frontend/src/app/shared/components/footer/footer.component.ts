import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './footer.component.html',
  styleUrl: './footer.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class FooterComponent {
  readonly shopLinks = [
    { label: 'Magaza', href: '/magaza' },
    { label: 'Gitarlar', href: '/magaza' },
    { label: 'Davul', href: '/magaza' },
    { label: 'Studyo', href: '/magaza' }
  ];
  readonly helpLinks = [
    { label: 'Iletisim', href: '/iletisim' },
    { label: 'SSS', href: '/sss' }
  ];
  readonly companyLinks = [
    { label: 'Hakkimizda', href: '/hakkimizda' },
    { label: 'Markalar', href: '/markalar' }
  ];
}
