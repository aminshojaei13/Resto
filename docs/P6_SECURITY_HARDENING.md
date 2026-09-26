# Resto SaaS Platform Security Hardening Report

## 1. Authentication & Token Security

- **Framework**: Laravel Sanctum Bearer Token Authentication.
- **Token Hashing**: Sanctum hashes API tokens in database using SHA-256 before storage. Plaintext tokens are returned only once upon initial `/auth/login` request.
- **Password Security**: Hashed via Bcrypt (`Hash::make()`) with cost factor 12. Plaintext passwords are never persisted or logged.
- **Login Rate Limiting**: Endpoint `/api/v1/auth/login` throttled to 5 attempts per minute per IP address.

---

## 2. Multi-Tenant Data Isolation (P0)

- **Source of Truth**: `Organization` model represents the Tenant (`Organization = Tenant / Business`).
- **Enforcement Mechanism**: `TenantMiddleware.php` intercepts all tenant-scoped routes:
  1. Inspects `X-Tenant-ID` header or `org_id` parameter.
  2. Verifies that the requested `organization_id` exists in `organizations` table.
  3. Validates that `OrganizationMembership` links `$request->user()->id` to `$organization_id`.
  4. Returns HTTP `403 Forbidden` if user attempts cross-tenant access.
- **Validation Result**: Direct ID tampering across tenants returns HTTP `403 Forbidden`.

---

## 3. Platform Admin Authorization Boundary

- **Platform Role**: Denoted by `is_platform_admin = true` on `users` table.
- **Isolation**:
  - Platform routes (`/api/v1/platform/*`) check `$user->is_platform_admin`.
  - Non-platform admin tenant users calling platform admin endpoints are immediately rejected with HTTP `403 Forbidden`.
  - Platform Admins manage platform business applications, but cannot query tenant business data through ordinary tenant APIs unless explicitly assigned membership.

---

## 4. CORS & Network Security

- **CORS Policy**:
  - Production allowed origins restricted to configured app origin (`APP_URL`). Wildcard wildcard origins (`*`) with credentials disabled.
  - Allowed Headers: `Content-Type`, `X-Requested-With`, `Authorization`, `X-Tenant-ID`, `X-Store-ID`, `X-Idempotency-Key`.
- **Security Headers**:
  - `X-Frame-Options: SAMEORIGIN` (prevents clickjacking)
  - `X-Content-Type-Options: nosniff` (prevents MIME sniffing)
  - `X-XSS-Protection: 1; mode=block`

---

## 5. API Rate Limiting & Input Validation

- **General API Limit**: Throttled to 60 requests per minute per authenticated user/IP.
- **Registration Limit**: Endpoint `POST /api/v1/business-applications` throttled to 5 requests per minute.
- **Input Validation**: Strictly validated via Laravel Form Request rules before processing.
- **Error Masking**: In production (`APP_DEBUG=false`), internal SQL exceptions and stack traces are masked, returning generic HTTP `500` or `422` validation responses.
