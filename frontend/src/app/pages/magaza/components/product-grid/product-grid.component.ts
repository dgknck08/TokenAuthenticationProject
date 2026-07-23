import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Product } from '../../magaza.types';

@Component({
  selector: 'app-product-grid',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-grid.component.html',
  styleUrl: './product-grid.component.css'
})
export class ProductGridComponent {
  @Input() filteredCount = 0;
  @Input() products: Product[] = [];
  @Input() sortBy = 'default';
  @Input() currentPage = 1;
  @Input() pageSize = 9;
  @Input() totalPages = 1;
  @Input() pageNumbers: number[] = [];

  @Output() sortByChange = new EventEmitter<string>();
  @Output() productClick = new EventEmitter<Product>();
  @Output() addToCart = new EventEmitter<{ event: Event; product: Product }>();
  @Output() resetFilters = new EventEmitter<void>();
  @Output() prevPage = new EventEmitter<void>();
  @Output() nextPage = new EventEmitter<void>();
  @Output() goToPage = new EventEmitter<number>();

  min(a: number, b: number): number {
    return Math.min(a, b);
  }
}
