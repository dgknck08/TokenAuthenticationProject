import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  input,
  signal,
} from '@angular/core';

export interface Testimonial {
  readonly id: string | number;
  readonly name: string;
  readonly role?: string;
  readonly quote: string;
}

const MOCK_TESTIMONIALS: readonly Testimonial[] = [
  {
    id: 1,
    name: 'Mert K.',
    role: 'Ev Stüdyosu',
    quote:
      'Sipariş akışı tertemizdi, ürün paketlemesi beklediğimden iyiydi ve kurulum süreci çok hızlı ilerledi.',
  },
  {
    id: 2,
    name: 'Sena A.',
    role: 'Sahne Müzisyeni',
    quote:
      'Ekipman seçimi ve kategori yapısı güven veren bir düzende; aradığım ürüne dakikalar içinde ulaştım.',
  },
  {
    id: 3,
    name: 'Barış T.',
    role: 'Prodüktör',
    quote:
      'Stüdyo tarafında filtreleme ve genel deneyim gayet netti, ihtiyacım olan her şeyi tek akışta topladım.',
  },
];

@Component({
  selector: 'app-reviews',
  standalone: true,
  templateUrl: './reviews.component.html',
  styleUrl: './reviews.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewsComponent implements OnInit, OnDestroy {
  readonly testimonials = input<readonly Testimonial[]>(MOCK_TESTIMONIALS);

  protected readonly activeIndex = signal(0);
  protected readonly count = computed(() => this.testimonials().length);
  protected readonly hasItems = computed(() => this.count() > 0);
  protected readonly active = computed(
    () => this.testimonials()[this.activeIndex()] ?? null
  );

  private autoTimer: ReturnType<typeof setInterval> | null = null;
  private isPaused = false;
  private readonly INTERVAL = 6000;

  ngOnInit(): void {
    this.startAuto();
  }

  ngOnDestroy(): void {
    this.stopAuto();
  }

  protected next(): void {
    this.advance(1);
    this.restart();
  }

  protected previous(): void {
    this.advance(-1);
    this.restart();
  }

  protected goTo(index: number): void {
    const total = this.count();
    if (total === 0) {
      return;
    }
    this.activeIndex.set(((index % total) + total) % total);
    this.restart();
  }

  protected pause(): void {
    this.isPaused = true;
    this.stopAuto();
  }

  protected resume(): void {
    this.isPaused = false;
    this.startAuto();
  }

  private advance(direction: 1 | -1): void {
    const total = this.count();
    if (total === 0) {
      return;
    }
    this.activeIndex.update((i) => (i + direction + total) % total);
  }

  private startAuto(): void {
    if (this.autoTimer || this.isPaused || this.count() < 2) {
      return;
    }
    this.autoTimer = setInterval(() => this.advance(1), this.INTERVAL);
  }

  private stopAuto(): void {
    if (this.autoTimer) {
      clearInterval(this.autoTimer);
      this.autoTimer = null;
    }
  }

  private restart(): void {
    this.stopAuto();
    this.startAuto();
  }
}
