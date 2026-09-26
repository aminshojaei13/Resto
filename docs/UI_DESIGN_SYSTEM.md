# Calcuapp UI/UX Design System Specification

**Date**: March 2025  
**Version**: 2.0  
**Repository**: Calcuapp Monorepo (`/Users/aminshojaei/AndroidStudioProjects/Calcuapp`)  
**Platforms**: Web Admin (React), Android Native (Jetpack Compose), iOS Prototype (SwiftUI)

---

## 1. Design Vision & Visual Brand Identity

Calcuapp's visual design is modeled as a **modern professional Business & Commerce Management SaaS**. It adopts a clean, calm, light-mode interface defined by generous whitespace, soft rounded content cards, dark charcoal navigation, and an energetic **Teal / Cyan primary brand accent**.

### Visual Characteristics:
- **Light Overall Interface**: Application background is a soft light gray (`#F4F5F7`).
- **Content Surfaces**: Pure white cards (`#FFFFFF`) with 12px–16px corner radius and subtle 1px border (`#E5E7EB`).
- **Brand Accent**: Teal/Cyan (`#12AFC0`) as the primary brand identity across buttons, active states, and metric indicators.
- **Dark Navigation**: Dark charcoal sidebar (`#1E293B`) for high-contrast, professional navigation.
- **RTL & Persian Native**: Primary UI language is Persian (fa) with full Right-to-Left (RTL) layout directionality, Persian digits (`۰-۹`), and Tomans currency (`تومان`).

---

## 2. Centralized Color Tokens

| Token Name | Hex Code | Usage / Context |
| :--- | :--- | :--- |
| `primary` | `#12AFC0` | Primary brand accent, primary CTA buttons, active state indicators |
| `primaryDark` | `#087F8C` | Hover states, dark brand text, active icon tints |
| `primaryLight` | `#DDF7F9` | Badge backgrounds, highlighted container surfaces, icon container backgrounds |
| `background` | `#F4F5F7` | Light gray application canvas background |
| `surface` | `#FFFFFF` | Card surfaces, modals, dropdowns, tables, drawers |
| `textPrimary` | `#1F2937` | High-contrast body text, card metric titles, primary headings |
| `textSecondary` | `#6B7280` | Subtitles, labels, table column headers, helper text |
| `textMuted` | `#9CA3AF` | Captions, placeholders, disabled text |
| `border` | `#E5E7EB` | Subtle card borders, divider lines, input field outlines |
| `sidebarBg` | `#1E293B` | Dark charcoal navigation sidebar background |
| `sidebarText` | `#94A3B8` | Navigation menu item text (inactive) |
| `sidebarItemActiveBg` | `#334155` | Selected store context trigger background |
| `success` | `#10B981` | Positive trends, paid order status, credit journal entries, profit |
| `successLight` | `#ECFDF5` | Green status badge background |
| `warning` | `#F59E0B` | Low stock alerts, pending payables (AP), overdue receivables |
| `warningLight` | `#FFFBEB` | Amber status badge background |
| `error` | `#EF4444` | Cancelled orders, debit entries, total expenses, error messages |
| `errorLight` | `#FEF2F2` | Red status badge background |
| `info` | `#3B82F6` | Informational badges, omnichannel indicators |
| `infoLight` | `#EFF6FF` | Blue status badge background |

---

## 3. Typography Scale & Hierarchy

| Type Role | Font Size | Weight | Context / Applied To |
| :--- | :--- | :--- | :--- |
| **Page Title** | `24px` | `700 (Bold)` | Top page title (`PageHeader.tsx`) |
| **Section Title** | `18px` | `700 (Bold)` | Section headers, modal titles, table headers |
| **Card Title / Metric Label**| `14px` | `600 (SemiBold)` | KPI card label, widget header |
| **Metric Large Value** | `28px` | `700 (Bold)` | Main financial KPI numbers |
| **Body Primary** | `14px` | `400 / 500` | Table cell text, form field text, order items |
| **Secondary Text** | `13px` | `400` | Subtitles, descriptions, date strings |
| **Caption / Badge** | `11px - 12px` | `600 / 700` | Status badges, trend percentage tags |

---

## 4. Spacing Scale & Hierarchy

All components must adhere strictly to a standardized 8-point base spacing grid:

$$\text{Spacing Grid} = [4\text{px}, 8\text{px}, 12\text{px}, 16\text{px}, 20\text{px}, 24\text{px}, 32\text{px}, 40\text{px}, 48\text{px}]$$

### Layout Padding Rules:
- **Outer Page Padding**: `24px` on desktop, `16px` on mobile.
- **Card Internal Padding**: `20px–24px` on desktop, `16px` on mobile.
- **Gap Between Cards**: `16px–24px`.
- **Card-to-Card Margin**: Minimum `16px`. No cards may touch each other.
- **Component Padding**: Text must maintain a minimum `12px` padding from card boundaries.

---

## 5. Component System Rules

### 5.1 StatCard (KPI Cards)
- **Background**: Pure white (`#FFFFFF`) with `16px` border radius and `1px solid #E5E7EB` border.
- **Internal Elements**:
  1. Icon box (`36x36px`, rounded `8px`, bg `#DDF7F9`) + Label (`14px` SemiBold `#6B7280`).
  2. Badge tag top right (e.g., `TOTAL`, `GROSS`, `NET`).
  3. Large metric value (`28px` Bold `#1F2937`).
  4. Trend tag (`12px` Bold Green/Red) + Subtitle period text.

### 5.2 AppShell (Desktop Sidebar + Mobile Drawer)
- **Desktop Sidebar**: Fixed `260px` width, dark charcoal (`#1E293B`), active item highlighted in Teal (`#12AFC0`).
- **Top Header Bar**: White surface (`#FFFFFF`), store context selector button, active module title, language toggle button.

### 5.3 DataTable
- **Header**: Light gray background (`#F8FAFC`), bold `12px` text.
- **Rows**: `1px solid #E5E7EB` border bottom, `14px` padding.
- **RTL Alignment**: Text aligns to the right for Persian, left for English.
- **Overflow**: Enclosed in a horizontally scrollable container (`overflowX: auto`).

---

## 6. Responsive Breakpoints & Rules

| Viewport Category | Width Threshold | Layout Adaptation Rules |
| :--- | :--- | :--- |
| **Large Desktop** | $> 1280\text{px}$ | 4-column KPI grid, fixed 260px dark sidebar, max-width 1440px page container |
| **Normal Desktop** | $1024\text{px} - 1280\text{px}$ | 4-column or 3-column KPI grid, responsive padding |
| **Tablet** | $640\text{px} - 1023\text{px}$ | 2-column KPI grid, collapsible sidebar, touch-optimized targets |
| **Mobile Portrait** | $< 640\text{px}$ | 1-column or 2-column compact KPI grid, bottom navigation or drawer menu |

---

## 7. RTL & Persian Formatting Rules

1. **Layout Direction**: Enforced dynamically via `dir="rtl"` in Web, `LocalLayoutDirection provides LayoutDirection.Rtl` in Android Compose, and `.environment(\.layoutDirection, .rightToLeft)` in SwiftUI.
2. **Typography & Digits**: ASCII digits (`0-9`) convert to Persian digits (`۰-۹`) using `PersianFormatter`.
3. **Currency Units**: Financial figures format with thousands separators and `تومان` currency text.
4. **Text Wrapping**: Long Persian text must wrap or ellipsize (`textOverflow: 'ellipsis'`); text must never clip or bleed outside card boundaries.
