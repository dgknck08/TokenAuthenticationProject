import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-brands-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './brands-page.component.html',
  styleUrl: './brands-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BrandsPageComponent {
  readonly brands = [
    'Fender',
    'Gibson',
    'Roland',
    'Marshall',
    'Shure',
    'Yamaha',
    'Korg',
    'Focusrite'
  ];
}
