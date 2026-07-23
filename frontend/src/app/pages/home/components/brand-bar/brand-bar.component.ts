import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

export interface Brand {
  readonly short: string;
  readonly name: string;
  readonly category: string;
}

@Component({
  selector: 'app-brand-bar',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './brand-bar.component.html',
  styleUrl: './brand-bar.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandBarComponent {
  protected readonly brands: readonly Brand[] = [
    { short: 'Fender', name: 'Fender', category: 'Gitar · Bas' },
    { short: 'Gibson', name: 'Gibson', category: 'Gitar' },
    { short: 'Marshall', name: 'Marshall', category: 'Amfi' },
    { short: 'Roland', name: 'Roland', category: 'Elektronik' },
    { short: 'Yamaha', name: 'Yamaha', category: 'Çok Yönlü' },
    { short: 'Pearl', name: 'Pearl', category: 'Davul' },
    { short: 'Shure', name: 'Shure', category: 'Mikrofon' },
    { short: 'Boss', name: 'Boss', category: 'Efekt · Pedal' },
    { short: 'Ibanez', name: 'Ibanez', category: 'Gitar · Bas' },
    { short: 'Focusrite', name: 'Focusrite', category: 'Ses Kartı' },
    { short: 'PRS', name: 'Paul Reed Smith', category: 'Gitar' },
    { short: 'Korg', name: 'Korg', category: 'Synthesizer' },
    { short: 'Moog', name: 'Moog', category: 'Synthesizer' },
    { short: 'Neumann', name: 'Neumann', category: 'Stüdyo Mic' },
    { short: 'Zildjian', name: 'Zildjian', category: 'Zil' },
    { short: 'Ernie Ball', name: 'Ernie Ball', category: 'Tel · Aksesuar' },
  ];
}
