import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout.component';
import { HomeComponent } from './pages/home/home.component';
import { LoginPageComponent } from './pages/login/login-page.component';
import { RegisterPageComponent } from './pages/register/register-page.component';
import { CartPageComponent } from './pages/cart/cart-page.component';
import { ContactPageComponent } from './pages/contact/contact-page.component';
import { FaqPageComponent } from './pages/faq/faq-page.component';
import { AboutPageComponent } from './pages/about/about-page.component';
import { BrandsPageComponent } from './pages/brands/brands-page.component';
import { MagazaComponent } from './pages/magaza/magaza.component';

export const appRoutes: Routes = [
  {
    path: '',
    component: MainLayoutComponent,
    children: [
      { path: '', component: HomeComponent, title: 'Dmusic — Müzik Aletleri ve Ekipmanları' },
      { path: 'login', component: LoginPageComponent, title: 'Giriş Yap · Dmusic' },
      { path: 'register', component: RegisterPageComponent, title: 'Üye Ol · Dmusic' },
      { path: 'cart', component: CartPageComponent, title: 'Sepetim · Dmusic' },
      { path: 'magaza', component: MagazaComponent, title: 'Mağaza · Dmusic' },
      { path: 'iletisim', component: ContactPageComponent, title: 'İletişim · Dmusic' },
      { path: 'sss', component: FaqPageComponent, title: 'Sıkça Sorulan Sorular · Dmusic' },
      { path: 'hakkimizda', component: AboutPageComponent, title: 'Hakkımızda · Dmusic' },
      { path: 'markalar', component: BrandsPageComponent, title: 'Markalar · Dmusic' },
    ],
  },
  { path: '**', redirectTo: '' },
];
