# SECURITY.md - Security, Isolation & Compliance Policies

## 1. Multi-Tenant Data Isolation
- Strict database scoping by `org_id` on all database queries and DAOs.
- Backend middleware enforcing `X-Tenant-ID` verification against user membership tokens.

## 2. Authentication & Secrets
- OAuth2 / JWT authentication with token expiration and refresh token rotation.
- API keys stored strictly in `local.properties` (Android) or `.env` (Backend), never hardcoded in source control.

## 3. Local Mobile Storage Security
- Encrypted DataStore and Room DB encryption via SQLCipher where required for sensitive offline token persistence.
