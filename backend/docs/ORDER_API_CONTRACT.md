# Order API Contract

## Public Endpoints
- `POST /api/orders`
- `GET /api/orders`
- `GET /api/orders/my`
- `GET /api/orders/{id}`
- `POST /api/orders/{id}/pay`

## Admin Endpoints
- `GET /api/admin/orders`
- `POST /api/admin/orders/{id}/cancel`
- `POST /api/admin/orders/{id}/refund`

## Status Transition Matrix
- `CREATED -> PAID` via `POST /api/orders/{id}/pay`
- `CREATED -> CANCELLED` via `POST /api/admin/orders/{id}/cancel`
- `PAID -> REFUNDED` via `POST /api/admin/orders/{id}/refund`
- Terminal statuses: `CANCELLED`, `REFUNDED`

## Error Contract
All error responses use `ApiErrorResponse`:
- `code`
- `message`
- `timestamp`
- `path`

## Notes
- `cancel` and `refund` restore stock.
- Payment is simulation-only (no external PSP capture).
