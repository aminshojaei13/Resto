// ============================================================================
// Resto Design Tokens — Light Theme + Warm Dark Theme (P8.3)
// ============================================================================
// Single source of truth for the Resto design language across Business Web
// and Platform Admin. Android maps the same conceptual palette in
// android/.../ui/theme/Color.kt + Theme.kt (Material 3).
//
// Warm Dark is NOT pure black: warm neutral surfaces (brown-tinted grays),
// Resto teal identity, restrained semantic colors, high text contrast.
// ============================================================================

export type ThemeMode = 'warmDark' | 'light' | 'system';

/** Palette defined per-mode; consumed via createTheme(). */
export interface RestoColorPalette {
  // Canvas
  background: string;
  backgroundSecondary: string;
  // Surfaces
  surface: string;
  surfaceElevated: string;
  surfaceHover: string;
  surfaceSelected: string;
  // Borders
  border: string;
  borderStrong: string;
  // Text
  textPrimary: string;
  textSecondary: string;
  textMuted: string;
  // Identity
  primary: string;
  primaryHover: string;
  primaryPressed: string;
  primaryLight: string;      // subtle teal container (badges, icon tiles)
  primaryDark: string;       // readable teal text on light containers
  primaryTextOnBrand: string;
  // Sidebar aliases (sidebar stays warm-dark in BOTH modes)
  sidebarBg: string;
  sidebarText: string;
  sidebarTextActive: string;
  sidebarItemActiveBg: string;
  // Semantics
  success: string;
  successLight: string;
  warning: string;
  warningLight: string;
  error: string;
  errorLight: string;
  info: string;
  infoLight: string;
  // Elevation
  overlay: string;           // modal/drawer scrim
  shadow: string;            // shadow tint
}

// ---- Shared primitives ------------------------------------------------------

const FONT_FAMILY = 'Inter, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';

export const spacing = {
  xs: '4px',
  sm: '8px',
  md: '12px',
  lg: '16px',
  xl: '20px',
  '2xl': '24px',
  '3xl': '32px',
  '4xl': '40px',
} as const;

export const borderRadius = {
  sm: '6px',
  md: '8px',
  lg: '12px',
  xl: '16px',
  '2xl': '24px',
  full: '9999px',
} as const;

const LIGHT_SHADOWS = {
  sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  card: '0 1px 3px 0 rgba(0, 0, 0, 0.06), 0 1px 2px -1px rgba(0, 0, 0, 0.04)',
  md: '0 4px 6px -1px rgba(0, 0, 0, 0.08), 0 2px 4px -2px rgba(0, 0, 0, 0.04)',
  lg: '0 10px 15px -3px rgba(0, 0, 0, 0.10), 0 4px 6px -4px rgba(0, 0, 0, 0.04)',
};

/** Dark shadows stay subtle: surfaces carry elevation, not heavy black blobs. */
const WARM_DARK_SHADOWS = {
  sm: 'none',
  card: '0 1px 2px 0 rgba(0, 0, 0, 0.24)',
  md: '0 2px 8px 0 rgba(0, 0, 0, 0.28)',
  lg: '0 12px 32px 0 rgba(0, 0, 0, 0.40)',
};

// ---- Warm Dark palette (P8.3 canonical) -------------------------------------

export const warmDarkColors: RestoColorPalette = {
  background: '#171513',
  backgroundSecondary: '#1D1A17',
  surface: '#24211E',
  surfaceElevated: '#2B2723',
  surfaceHover: '#322D28',
  surfaceSelected: '#123337',
  border: '#3A342E',
  borderStrong: '#4D453E',
  textPrimary: '#F5F1EA',
  textSecondary: '#C4BDB3',
  textMuted: '#958D83',
  primary: '#18B7C7',
  primaryHover: '#25C7D4',
  primaryPressed: '#0E8E9C',
  primaryLight: '#123337',
  primaryDark: '#4FC3CF',
  primaryTextOnBrand: '#FFFFFF',
  sidebarBg: '#1D1A17',
  sidebarText: '#C4BDB3',
  sidebarTextActive: '#F5F1EA',
  sidebarItemActiveBg: '#123337',
  success: '#35C98A',
  successLight: '#123223',
  warning: '#E7B85A',
  warningLight: '#382C13',
  error: '#E36A6A',
  errorLight: '#3B1A1A',
  info: '#5CA9E6',
  infoLight: '#132B42',
  overlay: 'rgba(10, 8, 6, 0.62)',
  shadow: 'rgba(0, 0, 0, 0.40)',
};

// ---- Light palette (legacy values preserved) ---------------------------------

export const lightColors: RestoColorPalette = {
  background: '#F4F5F7',
  backgroundSecondary: '#EAEDF1',
  surface: '#FFFFFF',
  surfaceElevated: '#FFFFFF',
  surfaceHover: '#F1F3F6',
  surfaceSelected: '#E6F7F9',
  border: '#E5E7EB',
  borderStrong: '#D1D5DB',
  textPrimary: '#1F2937',
  textSecondary: '#6B7280',
  textMuted: '#9CA3AF',
  primary: '#12AFC0',
  primaryHover: '#0E9DAE',
  primaryPressed: '#087F8C',
  primaryLight: '#DDF7F9',
  primaryDark: '#087F8C',
  primaryTextOnBrand: '#FFFFFF',
  sidebarBg: '#1E293B',
  sidebarText: '#94A3B8',
  sidebarTextActive: '#FFFFFF',
  sidebarItemActiveBg: '#334155',
  success: '#10B981',
  successLight: '#ECFDF5',
  warning: '#F59E0B',
  warningLight: '#FFFBEB',
  error: '#EF4444',
  errorLight: '#FEF2F2',
  info: '#3B82F6',
  infoLight: '#EFF6FF',
  overlay: 'rgba(15, 23, 42, 0.5)',
  shadow: 'rgba(0, 0, 0, 0.06)',
};

/** Semantic theme: mode palette + shared spacing/radius/typography/shadows. */
export function createTheme(mode: 'warmDark' | 'light') {
  const colors = mode === 'warmDark' ? warmDarkColors : lightColors;
  return {
    mode,
    colors,
    spacing,
    borderRadius,
    shadows: mode === 'warmDark' ? WARM_DARK_SHADOWS : LIGHT_SHADOWS,
    typography: { fontFamily: FONT_FAMILY },
  };
}

export type RestoTheme = ReturnType<typeof createTheme>;

/** Legacy static export — Light. Kept for unmigrated consumers; new code should use useTheme(). */
export const theme = createTheme('light');

/** Resolve 'system' to a concrete mode. */
export function resolveSystemMode(): 'warmDark' | 'light' {
  if (typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'warmDark';
  }
  return 'light';
}
