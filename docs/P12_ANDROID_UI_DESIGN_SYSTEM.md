# P12 Android UI Design System Specification — Resto

## 1. Overview & Visual Identity
Resto is a modern, professional, business-oriented commerce management application designed for entrepreneurs, online stores, and commerce operators. 

### Visual Brand Identity:
- **Brand Identity**: Teal / Cyan accent (`#18B7C7`), communicating precision, financial clarity, and calmness.
- **Dual Theme Support**:
  - **Light Theme**: Soft clean canvas (`#F4F5F7`), pure white cards (`#FFFFFF`), charcoal text (`#1F2937`).
  - **Warm Dark Theme**: Deep warm neutral canvas (`#171513`), elevated dark-brown surfaces (`#24211E`, `#2B2723`), warm text (`#F5F1EA`), Resto Teal identity. Strictly avoids harsh OLED pure black.
- **Persian First (RTL Native)**: True Right-to-Left layout directionality (`LayoutDirection.Rtl`), Persian digits (`۰-۹`), Tomans currency formatting, and natural Persian UX copy. Full English (LTR) localization support.

---

## 2. Centralized Color System

### 2.1 Brand Tokens
| Token | Hex (Light) | Hex (Warm Dark) | Role |
| :--- | :--- | :--- | :--- |
| `primary` | `#12AFC0` | `#18B7C7` | Primary brand accent, primary CTA buttons |
| `onPrimary` | `#FFFFFF` | `#003237` | Text/icons on primary surfaces |
| `primaryContainer` | `#DDF7F9` | `#123337` | Selected chip, active nav indicator |
| `onPrimaryContainer` | `#087F8C` | `#4FC3CF` | Highlights on container surfaces |

### 2.2 Neutral Surfaces
| Token | Light Hex | Warm Dark Hex | Role |
| :--- | :--- | :--- | :--- |
| `background` | `#F4F5F7` | `#171513` | Root screen canvas |
| `surface` | `#FFFFFF` | `#24211E` | Card surface, inputs, modal backgrounds |
| `surfaceContainer` | `#F8FAFC` | `#24211E` | Card surface default |
| `surfaceContainerHigh` | `#F1F5F9` | `#2B2723` | Dialogs, elevated sheets |
| `outline` | `#D1D5DB` | `#4D453E` | Input borders, card dividers |
| `outlineVariant` | `#E5E7EB` | `#3A342E` | Subtle borders, separators |

### 2.3 Semantic Tokens (`RestoTheme.colors`)
| Semantic | Light Color | Warm Dark Color | Meaning |
| :--- | :--- | :--- | :--- |
| `success` | `#10B981` | `#35C98A` | Paid orders, stock available, positive net profit |
| `successContainer` | `#ECFDF5` | `#123223` | Success badge background |
| `warning` | `#F59E0B` | `#E7B85A` | Low stock alert, pending orders, payables |
| `warningContainer` | `#FFFBEB` | `#382C13` | Warning badge background |
| `error` | `#EF4444` | `#E36A6A` | Out of stock, cancelled orders, debts, error states |
| `errorContainer` | `#FEE2E2` | `#3B1A1A` | Error badge background, error alert box |
| `info` | `#3B82F6` | `#5CA9E6` | Informational chips, social orders, sync status |
| `infoContainer` | `#EFF6FF` | `#132B42` | Info badge background |

---

## 3. Typography Hierarchy (`Type.kt`)
Standard Material 3 typography with Persian font compatibility and clear visual hierarchy:
- `displayLarge` (57sp, Normal, line-height 64sp)
- `displayMedium` (45sp, Normal, line-height 52sp)
- `displaySmall` (36sp, Normal, line-height 44sp)
- `headlineLarge` (32sp, Normal, line-height 40sp)
- `headlineMedium` (28sp, Normal, line-height 36sp) - Metric values
- `headlineSmall` (24sp, Normal, line-height 32sp) - Top level KPI values
- `titleLarge` (22sp, SemiBold, line-height 28sp) - Screen titles, modal headers
- `titleMedium` (16sp, SemiBold, line-height 24sp) - Section titles, card headlines
- `titleSmall` (14sp, SemiBold, line-height 20sp) - Subsections, list headers
- `bodyLarge` (16sp, Normal, line-height 24sp) - Primary readable body text
- `bodyMedium` (14sp, Normal, line-height 20sp) - Standard text, descriptions
- `bodySmall` (12sp, Normal, line-height 16sp) - Helper labels, timestamps
- `labelLarge` (14sp, Medium, line-height 20sp) - Primary button text
- `labelMedium` (12sp, Medium, line-height 16sp) - Filter chips, navigation labels
- `labelSmall` (11sp, Medium, line-height 16sp) - Status badges, micro metrics

---

## 4. Spacing System (`RestoSpacing`)
Standardized 4px/8px-based spacing scale. Hardcoded random margins or paddings are prohibited:
- `xxs`: `4.dp` — Micro gaps, badge inner padding, inline icon margins
- `xs`: `8.dp` — Dense item spacing, chip horizontal padding
- `sm`: `12.dp` — Card inter-item spacing, compact padding
- `md`: `16.dp` — Standard screen margin, standard card content padding
- `lg`: `20.dp` — Large section dividers, dialog outer padding
- `xl`: `24.dp` — Major card margins, dialog headers
- `xxl`: `32.dp` — Empty state padding, screen top banner margins
- `xxxl`: `40.dp` — Hero section paddings
- `xxxxl`: `48.dp` — Major empty state top spacing

---

## 5. Shape System (`RestoShapes`)
- `small`: `RoundedCornerShape(8.dp)` — Badges, small tags, mini icon boxes
- `medium`: `RoundedCornerShape(12.dp)` — Buttons, input fields, stat cards
- `large`: `RoundedCornerShape(16.dp)` — Primary content cards, list cards
- `extraLarge`: `RoundedCornerShape(24.dp)` — Dialogs, Modal bottom sheets
- `full`: `RoundedCornerShape(50)` — Search bar, pills, filter chips

---

## 6. Dimension Standards (`RestoDimensions`)
- Minimum Touch Target: `48.dp`
- Standard Button Height: `48.dp`
- Standard Input Field Height: `56.dp`
- Top App Bar Height: `64.dp`
- Bottom Navigation Bar Height: `80.dp`
- Small Icon: `16.dp`, Medium Icon: `24.dp`, Large Icon: `32.dp`
- Avatar Sizes: Small `36.dp`, Medium `48.dp`, Large `64.dp`

---

## 7. Reusable Component Catalog

| Component | Responsibility & Variants |
| :--- | :--- |
| `RestoButton` | Primary, Secondary, Outlined, Destructive, Text with loading spinner, icons, touch target |
| `RestoIconButton` | 48dp touch target icon button with semantic tint |
| `RestoTextField` | Form input with floating label, helper text, inline error message, keyboard options |
| `RestoSearchField` | Rounded search bar with leading search icon and trailing clear button |
| `RestoCard` | Standard content card with Filled, Elevated, and Outlined variants |
| `RestoStatCard` | KPI widget with icon container, metric label, large formatted number, and badge |
| `RestoBadge` | Status pill with Success, Warning, Error, Info, and Neutral palettes |
| `RestoChip` | Filter/category selection chip with active/inactive states |
| `RestoSection` | Section title, optional subtitle, and trailing action button |
| `RestoTopBar` | App header with title, subtitle, navigation icon, and actions |
| `RestoBottomBar` | M3 bottom navigation bar with active indicators and labels |
| `RestoDialog` | M3 dialog with scrollable content, confirm, and dismiss buttons |
| `RestoBottomSheet` | Modal sheet with drag handle, title, and padded content area |
| `RestoEmptyState` | Empty placeholder with icon box, title, description, and primary CTA button |
| `RestoErrorState` | Actionable error banner with warning icon, message, and retry button |
| `RestoLoadingState` | Spinner or animated shimmer skeleton (`RestoSkeletonItem`) |
| `RestoProductCard` | Catalog product card with stock badge, SKU, unit, and quick add-to-cart |
| `RestoOrderCard` | Order summary card with order number, customer, timestamp, total, and status badges |
| `RestoListItem` | List row with leading avatar/icon, title, subtitle, and trailing action/price |

---

## 8. Persian & Localization Guidelines
1. **Layout Direction**: Automatically driven by `tenantState.language == "fa"` using `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`.
2. **Number Formatting**: All monetary values, quantities, and dates in Persian mode use `PersianFormatter.toPersianDigits()` and `PersianFormatter.formatTomans()`.
3. **No Technical Jargon**: Terms like `API`, `CRUD`, `Tenant`, `SaaS`, `DB` must never appear in customer UI. Replaced by "سازمان", "شعبه", "انبار", "کالاها", "سفارشات".
