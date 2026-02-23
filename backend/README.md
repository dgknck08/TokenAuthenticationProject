# E-Commerce API Backend

Bu proje Spring Boot tabanli bir e-ticaret backend'idir. JWT kimlik dogrulama, refresh token, Redis tabanli guest cart ve token blacklist, Caffeine cache, audit log ve rol/yetki bazli erisim kontrolu icerir.

## Ozellikler
- JWT Access Token (`Bearer`) + Refresh Token akisi
- Spring Security filter chain ile endpoint korumasi
- Redis ile:
- guest cart saklama
- token blacklist
- login lockout verisi
- Caffeine ile JWT claims ve userDetails cache
- Audit log kayitlari
- Product, Cart, Profile ve Auth modulleri
- Order modulu (kullanici bazli siparis olusturma/listeleme)

## Mimari Katmanlar
- `controller`: HTTP endpoint'ler, request/response yonetimi
- `service`: is kurallari ve orkestrasyon
- `repository`: JPA/Redis veri erisimi
- `security`: filter, token provider, auth entry point
- `config`: Security, Redis, Caffeine, Async, Swagger, Cloudinary
- `exception`: merkezi hata yonetimi (`@RestControllerAdvice`)

## API Cagri Akisi (Genel)
1. Istek gelir ve `SecurityFilterChain` tarafindan yakalanir.
2. `JwtAuthenticationFilter` token'i okur ve dogrular.
3. Token gecerliyse `SecurityContext` icine `Authentication` konur.
4. Istek ilgili `@RestController` methoduna gider.
5. Controller, Service'i cagirir.
6. Service, gerekli Repository cagri(lar)i ile DB/Redis'e erisir.
7. Donus DTO olarak controller'dan response olur.
8. Hata varsa `GlobalExceptionHandler` veya `CartExceptionHandler` standart hata cevabi uretir.

## Ornek Walkthrough: `POST /api/auth/login`
1. Endpoint: `AuthController.login(...)`
2. Kilit kontrolu: `AccountLockoutService.isAccountLocked(username)`
3. Giris denemesi: `AuthServiceImpl.login(request)`
4. Kimlik dogrulama: `AuthenticationManager.authenticate(...)`
5. Access token uretimi: `JwtTokenProvider.generateToken(authentication)`
6. Refresh token uretimi: `RefreshTokenServiceImpl.createRefreshToken(userId)`
7. Basarili deneme kaydi: `AccountLockoutService.recordLoginAttempt(...)`
8. Token metadata: `JwtBlacklistService.storeTokenMetadata(...)`
9. Response: `AuthResponseHandler.handleLogin(...)`

## Projede Kullanilan Onemli Spring/Spring Boot Siniflari

### Bootstrapping
- `@SpringBootApplication`: `EcommerceApiApplication`
- `@EnableJpaAuditing`: JPA audit alanlari
- `@EnableScheduling`: periyodik cleanup isleri

### Web ve REST
- `@RestController`, `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- `ResponseEntity`, `HttpServletRequest`

### Validation
- `@Valid`, `@Validated`

### Security
- `SecurityFilterChain` (`SecurityConfig`)
- `OncePerRequestFilter` (`JwtAuthenticationFilter`)
- `AuthenticationManager`
- `UsernamePasswordAuthenticationToken`
- `AuthenticationEntryPoint` (`JwtAuthenticationEntryPoint`)
- `@EnableMethodSecurity`, `@PreAuthorize`
- `UserDetailsService` (`CustomUserDetailsService`)
- `PasswordEncoder` (`BCryptPasswordEncoder`)

### Data/JPA
- `JpaRepository` (User, Product, Cart, RefreshToken, Inventory, Audit vb.)
- `@Transactional`
- `@Query`, `@Modifying`

### Cache / Async / Scheduling
- `RedisTemplate`, `LettuceConnectionFactory`
- `Caffeine Cache`
- `@EnableAsync`, `TaskExecutor`, `@Async`
- `@Scheduled`

### Error Handling
- `@RestControllerAdvice`
- `@ExceptionHandler`

## Ana Controller ve Service Siniflari

### Auth
- Controller: `auth/controller/AuthController.java`
- Service: `auth/service/impl/AuthServiceImpl.java`
- Destek servisleri: `JwtValidationService`, `JwtBlacklistService`, `AccountLockoutService`, `RefreshTokenServiceImpl`

### Cart
- Controller: `cart/controller/CartController.java`
- Service: `cart/service/CartServiceImpl.java`
- Guest/Redis service: `cart/service/GuestCartService.java`

### Product
- Controller: `product/controller/ProductController.java`
- Upload controller: `product/controller/ProductUploadController.java`
- Service: `product/service/ProductService.java`
- Inventory service: `inventory/service/InventoryService.java`

### Order
- Controller: `order/controller/OrderController.java`
- Service: `order/service/OrderService.java`

### Profile
- Controller: `profile/controller/ProfileController.java`
- Service: `profile/service/ProfileService.java`

## Guvenlikte Public Endpoint Kurali (Ozet)
- Public:
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh-token`
- `GET /api/products/**`
- `GET /api/cart/**`
- Diger endpoint'ler: authentication gerekli

## Product Search / Pagination
- Endpoint: `GET /api/products/search`
- Query params (opsiyonel):
  - `category`
  - `brand`
  - `q` (name/description text search)
  - Spring pageable: `page`, `size`, `sort`

## Order API (Temel)
- `POST /api/orders` (yetki: `ORDER_WRITE`)
- `GET /api/orders/my` (yetki: `ORDER_READ`)
- `GET /api/orders` (yetki: `ORDER_READ`, `my` ile ayni response)
- `GET /api/orders/{id}` (yetki: `ORDER_READ`, sadece kendi siparisi)
- `POST /api/orders/{id}/pay` (yetki: `ORDER_WRITE`, body: `{ "paymentMethod": "CARD|COD" }`)

## Admin Order API (Simulasyon)
- `GET /api/admin/orders` (ROLE_ADMIN)
- `POST /api/admin/orders/{id}/cancel` (ROLE_ADMIN, sadece `CREATED`)
- `POST /api/admin/orders/{id}/refund` (ROLE_ADMIN, sadece `PAID`)

## Cloudinary Product Image Upload
- Endpoint: `POST /api/admin/uploads/product-image`
- Yetki: `PRODUCT_WRITE`
- Content type: `multipart/form-data`
- Form field: `file`

Gerekli env:
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`
- Opsiyonel: `CLOUDINARY_FOLDER` (varsayilan: `ecommerce/products`)

## Config Notlari
- CORS originleri env/property ile yonetilir: `APP_CORS_ALLOWED_ORIGINS`
- Flyway migration aktif: `SPRING_FLYWAY_ENABLED` (default: `true`)
- Order tablolari migration: `db/migration/V1__create_orders_tables.sql`
- Schema hardening migration: `db/migration/V4__order_schema_hardening.sql`

## Gozlemlenebilirlik
- Prometheus endpoint: `GET /actuator/prometheus`
- Metrikler:
  - `ecommerce.order.events{action,outcome}`
  - `ecommerce.order.action.duration{action,outcome}`
  - `ecommerce.order.payment.failed{reason}`
- Ornek alert kurallari: `docs/observability/alerts-prometheus.yml`
- Correlation ID header: `X-Correlation-Id` (request/response + audit details)

## API Sozlesmesi ve Yasam Dongusu
- Public/Admin endpoint ayrimi ve status matrix:
  - `docs/ORDER_API_CONTRACT.md`
- Flyway migration politikasi:
  - `docs/MIGRATION_POLICY.md`

## Test ve Coverage Gate
- JaCoCo quality gate aktif (`mvn verify`):
  - Line coverage min: `%40`
  - Branch coverage min: `%15`
- Yeni testler:
  - `security/OrderControllerSecurityIntegrationTest`
  - `security/AdminOrderControllerSecurityIntegrationTest`
  - `security/CorrelationIdFilterTest`
  - `service/AuditServiceTest`

## Kisa Kurulum
```bash
mvn clean install
mvn spring-boot:run
```
