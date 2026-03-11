import { ChangeDetectionStrategy, Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';

type ShippingCity = {
  name: string;
  pos: number;
  delay: number;
};

type ShippingChar = {
  value: string;
  highlight: boolean;
};

type MovingIcon = {
  kind: 'guitar' | 'drum';
  delay: number;
  top: number;
};

@Component({
  selector: 'app-shipping-banner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './shipping-banner.component.html',
  styleUrl: './shipping-banner.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ShippingBannerComponent {
  readonly text = '7 ilimizdeki mağazalarimizdan tüm Türkiyeye kargo imkani sağlıyoruz';
  readonly highlightLength = 13;

  readonly chars = computed<ShippingChar[]>(() =>
    this.text.split('').map((value, index) => ({
      value,
      highlight: index < this.highlightLength
    }))
  );

  readonly cities: ShippingCity[] = [
    { name: 'Istanbul', pos: 8, delay: 600 }, 
    { name: 'Ankara', pos: 22, delay: 900 },
    { name: 'Izmir', pos: 36, delay: 1200 },
    { name: 'Bursa', pos: 50, delay: 1500 },
    { name: 'Antalya', pos: 64, delay: 1800 },
    { name: 'Konya', pos: 78, delay: 2100 },
    { name: 'Adana', pos: 92, delay: 2400 }
  ];

  readonly movingIcons: MovingIcon[] = [
    { kind: 'guitar', delay: 0, top: 14 },
    { kind: 'drum', delay: 1400, top: 16 },
    { kind: 'guitar', delay: 2800, top: 14 }
  ];
}
