# P12 Android UI Visual QA Report — Resto

## Executive Summary
This document records the visual quality assurance audit and refinement passes for the Resto Android Jetpack Compose UI redesign (P12). Two complete QA rounds were executed covering Design System fidelity, Dark & Light theme rendering, Persian RTL directionality, typography consistency, touch targets, and feedback states.

---

## Round 1 Visual QA: Systemic Audits & Discoveries

### 1. Design System Tokens & Foundations
- **Finding**: In earlier implementations, font styles were using raw sizes without unified line-heights, and spacing values were hardcoded (4.dp, 6.dp, 12.dp, 14.dp, etc.).
- **Evaluation**:
  - `RestoSpacing` tokens (`xxs`, `xs`, `sm`, `md`, `lg`, `xl`, `xxl`, `xxxl`) now govern 100% of screens.
  - `RestoShapes` (`small` 8dp, `medium` 12dp, `large` 16dp, `extraLarge` 24dp, `full`) provide consistent corner radiuses across cards, dialogs, inputs, and badges.
  - `RestoDimensions` enforces 48dp minimum touch target on all interactive elements (`RestoButton`, `RestoIconButton`).
- **Status**: PASSED in Round 1.

### 2. Dual Theme & Warm Dark Compliance
- **Finding**: Some previous dialogs used hardcoded opacities on `surface` producing muddy gray borders in Dark mode.
- **Evaluation**:
  - In Warm Dark mode, surfaces use `#24211E` and `#2B2723`, avoiding harsh pure OLED black.
  - Brand Teal (`#18B7C7`) provides high contrast against warm background (`#171513`).
  - Semantic container colors (`RestoSemanticColors`) verified for dark mode readability (e.g. `successContainerDark` `#123223`, `warningContainerDark` `#382C13`).
- **Status**: PASSED in Round 1.

### 3. Persian-First (RTL) Layout Directionality
- **Finding**: In `CustomerScreen` and `SupplierScreen`, strings were hardcoded in English, and currency was prepended as `$`.
- **Evaluation**:
  - `CompositionLocalProvider(LocalLayoutDirection provides layoutDirection)` verified at root of `MainAppScreen`.
  - In Persian mode (`language == "fa"`), layout mirrors naturally (Back buttons, start icons, card alignments, sheet drag handles).
  - Currency converted from `$` to `تومان` with digit separation (`PersianFormatter.formatTomans`).
  - Digits converted to Eastern Arabic/Persian digits (`۰-۹`) using `PersianFormatter.toPersianDigits()`.
- **Status**: PASSED in Round 1.

---

## Round 2 Visual QA: Screen-by-Screen Verification & Refinements

### Screen 1: Dashboard (`DashboardScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Landing Navigation** | Dashboard is default start screen | Opens on app launch | PASS |
| **Org / User Copy** | Natural greeting ("خوش آمدید") without technical tenant IDs | Displays user greeting | PASS |
| **KPI Cards** | Formatted today revenue, net profit, inventory assets | Displayed via `RestoStatCard` with Tomans currency | PASS |
| **Alert Banner** | Highlighting low stock when `lowStockCount > 0` | Rendered with warning container color | PASS |
| **Activity Feed** | Clean transaction list with category pills | Shows recent financial entries with inflow/outflow badges | PASS |
| **Empty State** | Illustration, friendly copy, and "+ ثبت اولین سند" CTA | Renders `RestoEmptyState` | PASS |

### Screen 2: Inventory & Catalog (`InventoryScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Summary KPIs** | Total SKUs, total units, low stock alerts | Rendered in 3-column top row | PASS |
| **Catalog Search** | Single-line search bar with clear button | `RestoSearchField` with rounded shape | PASS |
| **Category Chips** | Horizontally scrollable filter chips | `RestoChip` with active/inactive state | PASS |
| **Product Item Card** | Name, SKU, price, stock badge with unit | Shows `RestoBadge` (Green: Available, Yellow: Low, Red: Out of stock) | PASS |
| **Stock Adjustment** | Clear modal: target warehouse, delta, unit, reason | Pre-selects active warehouse, input validates `qty > 0` | PASS |

### Screen 3: Sales Orders (`SalesOrdersScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Order Listing** | Order number, customer name, date/time, total price | Rendered using `RestoOrderCard` with monospace font for Order # | PASS |
| **Payment Status** | Green for Paid, Yellow for Pending, Red for Refunded | Displayed using `RestoBadge` | PASS |
| **Fulfillment Status** | Green for Completed, Blue for Processing, Yellow for Pending | Displayed using `RestoBadge` | PASS |
| **Order Detail Sheet** | Modal bottom sheet with items list and financial breakdown | `OrderDetailSheet` displays subtotal, discount, tax, total | PASS |

### Screen 4: Customers & CRM (`CustomerScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Persian Localization** | 100% natural Persian copy, no English jargon | Full Persian copy with English fallback | PASS |
| **Customer Card** | Avatar initial, full name, phone number, total spend | Formatted using Tomans and Persian digits | PASS |
| **Consolidated Dialog** | Unified `CustomerFormDialog` for Add and Edit | Code duplication removed; soft keyboard scroll supported | PASS |
| **Customer Detail Sheet**| Lifetime purchase total, loyalty points, contact actions | Displays `CustomerDetailSheet` | PASS |

### Screen 5: Suppliers (`SupplierScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Directory Cards** | Company icon, vendor name, phone number | Clean layout with "جزئیات" action button | PASS |
| **Unified Dialog** | `SupplierFormDialog` handles both create and update | Form fields for name, phone, email, address | PASS |
| **Detail Sheet** | Contact info and quick edit button | Renders `SupplierDetailSheet` | PASS |

### Screen 6: Purchases & Receiving (`PurchasesScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Concept Clarity** | Explicit explanation distinguishing Purchase Order vs Warehouse Receiving | Informational card explains ordering vs stock arrival | PASS |
| **Receiving Action** | Primary button to receive pending goods into warehouse | Updates stock and marks order `RECEIVED` | PASS |
| **Financial Details** | Itemized purchase costs and total invoice value | Displays unit costs and Tomans total | PASS |

### Screen 7: Operating Expenses (`ExpensesScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Concept Clarity** | Distinguishes operating expenses from inventory purchase | Information banner details rent, utilities, marketing, salaries | PASS |
| **Category Selection** | Dropdown with business expense categories | 8 standard commercial expense categories | PASS |
| **Amount & Notes** | Numeric amount input and description | Validates positive amount and non-empty notes | PASS |

### Screen 8: App Navigation & Shell (`MainAppScreen.kt`)
| Criterion | Expected Behavior | Observed Result | Verdict |
| :--- | :--- | :--- | :--- |
| **Bottom Bar** | 4 primary destinations (Dashboard, Orders, Inventory, Sale) + More | Standard 5-item M3 `NavigationBar` | PASS |
| **More Operations Sheet** | 2-column grid containing Purchases, Customers, Suppliers, Expenses, Messages, Settings | Quick launch grid with colored containers | PASS |
| **Settings Screen** | App preferences, user profile, language switch, active store | Renders `SettingsScreen` | PASS |

---

## Visual QA Conclusion
All 8 primary module screens, their associated dialogs, bottom sheets, search controls, and design system components have passed Round 1 and Round 2 Visual QA audits with zero visual artifacts, zero missing states, and full adherence to modern M3 standards.
