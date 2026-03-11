# Dmusic Frontend

Angular standalone storefront for the ecommerce backend. Legacy React/Next uygulamasi kaldirildi.

## Stack
- Angular 19 standalone components + router
- HttpClient interceptor based auth refresh flow
- Reactive Forms for auth, checkout, account, and admin actions
- EN/TR locale shell with light/dark theme toggle
- Nginx static serving for production containers

## Run
```bash
cd frontend
npm install
npm run dev
```

App default URL: `http://localhost:3000`

## Build
```bash
npm run build
```

## Notes
- Angular source lives under `frontend/src`.
- Legacy React/Next application files were removed.
- API base URL resolves to `${window.location.origin}/api` in browser runtime, with fallback `http://localhost:8080/api`.
- Frontend Docker image now builds Angular output and serves it via Nginx.
