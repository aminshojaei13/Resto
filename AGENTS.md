# AGENTS.md - AI Development Agent Operating Guidelines

## 1. Principles
- **Minimal Human Intervention**: Autonomously perform audits, file writes, dependencies, builds, and tests using available tools.
- **Strict Build Verification**: Always execute build commands (`./gradlew assembleDebug`, `./gradlew testDebugUnitTest`) after editing files.
- **Dynamic Color & Edge-to-Edge**: Android UI must strictly enforce Material 3 Dynamic Color and `enableEdgeToEdge()` display standards.
- **Full Path File Integrity**: Always reference absolute file paths. Never invent non-existent file locations.

## 2. Platform Guidelines
- **Android**: Pure Jetpack Compose (no XML layouts except adaptive launcher icons), StateFlow, Clean Architecture, Room DB, CameraX, Material 3.
- **Backend**: Clean Laravel REST API conventions, Form Requests, Resources, Database Migrations, Pest / PHPUnit tests.
- **Web**: React / Next.js with TypeScript and Tailwind CSS.
- **iOS**: Swift 5.10, SwiftUI, MVVM with `@Observable`.

## 3. Communication
- Conclude each task with a structured final summary outlining key actions, outcomes, and unresolved issues (if any).
