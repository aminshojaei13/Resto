# Resto SaaS Platform Monorepo

Resto is an enterprise-grade multi-tenant Point of Sale (POS), Inventory Management, Double-Entry Financial Accounting, and Sales Management SaaS platform.

## Monorepo Architecture

```text
/
├── android/          # Native Android Application (Kotlin, Jetpack Compose M3, Room, DataStore)
├── backend/          # Laravel 11 REST API Backend & Double-Entry Accounting Engine
├── web/              # React Native Web / Next.js Admin & POS Dashboard
├── ios/              # Native iOS Swift / SwiftUI Client
├── infrastructure/   # Docker, Kubernetes, Terraform, and CI/CD Pipeline configurations
├── docs/             # Technical specifications & API documentation
├── scripts/          # Automation & deployment scripts
└── tests/            # End-to-end and integration test suites
```

## Documentation

- [PROJECT_SPEC.md](./PROJECT_SPEC.md) - High-level project specifications, feature requirements, and SaaS tenant architecture.
- [ARCHITECTURE.md](./ARCHITECTURE.md) - System architecture, data flow, and cross-platform communication.
- [AGENTS.md](./AGENTS.md) - AI agent delegation guidelines and workflow directives.
- [DATABASE.md](./DATABASE.md) - PostgreSQL database schema, migrations, and transactional integrity design.
- [API.md](./API.md) - REST API specs, OpenAPI schema, and authentication flow.
- [SECURITY.md](./SECURITY.md) - Multi-tenant data isolation, JWT authentication, and security standards.
- [TESTING.md](./TESTING.md) - Test strategy for unit, integration, and UI automation across platforms.

## Getting Started

### Android App
```bash
cd android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
