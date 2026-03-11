import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ShippingBannerComponent } from './components/shipping-banner/shipping-banner.component';
import { CategoryShowcaseComponent } from './components/category-showcase/category-showcase.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ShippingBannerComponent, CategoryShowcaseComponent],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class HomeComponent {}
