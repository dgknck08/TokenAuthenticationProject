# Auralyn Guitars Frontend

Production-ready Next.js frontend for the ecommerce backend.

## Stack
- Next.js App Router + TypeScript
- TailwindCSS (headless component approach)
- TanStack Query + Axios interceptors
- RHF + Zod forms
- Framer Motion animations
- Light/Dark theme + EN/TR locale structure

## Run
```bash
cd frontend
npm install
npm run dev
```

## Environment
Copy `.env.example` to `.env.local`.

## Test
```bash
npm run test
npm run test:e2e
```

## Backend Contract Checklist (fill/confirm)
1. Base URL (`NEXT_PUBLIC_API_BASE_URL`) -> default `http://localhost:8080/api`
2. Auth model -> Access token in `localStorage`, refresh token in `httpOnly cookie` (detected from backend)
3. Products listing contract -> backend currently returns plain array from `GET /products`; no server pagination/filter endpoint yet
4. Categories endpoint -> backend currently has no `GET /categories`
5. Orders endpoints -> backend currently has no `/orders` endpoints
6. Payment flow endpoint -> backend currently has no `/payments/intent` endpoint
7. Forgot password endpoint -> backend currently has no reset endpoint
8. Error format -> `ApiErrorResponse { code, message, timestamp, path }` (confirmed)

## Notes
- Shop filters/pagination are handled client-side until backend filtering endpoints are added.
- Checkout and account orders support mock mode via env flags.
