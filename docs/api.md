# API Overview

All backend APIs use `/api/**`.

- `GET /api/health`
- `POST /api/auth/wechat-login`
- `GET /api/catalog/home`
- `GET /api/products`
- `GET /api/products/page?page=1&size=10&keyword=`
- `GET /api/products/{id}`
- `GET /api/products/{id}/relations`
- `GET /api/solutions`
- `GET /api/cases`
- `POST /api/inquiries`
- `GET /api/admin/dashboard`
- `GET /api/admin/inquiries`
- `GET /api/admin/inquiries/page?page=1&size=10&keyword=`
- `POST /api/quotes`
- `GET /api/quotes/page?page=1&size=10`
- `GET /api/quotes/{id}`
- `POST /api/quotes/{id}/export`
- `GET /api/templates`
- `GET /api/ai/documents`
- `POST /api/ai/chat`
- `GET /api/ai/question-records`

AI and Word export are first-phase placeholders. File upload goes through the backend storage abstraction and returns a mock MinIO object record until real MinIO credentials are wired.
