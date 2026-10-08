import React, { createContext, useContext, useState, useEffect, useMemo } from 'react';
import { createTheme, ThemeMode, warmDarkColors, lightColors, resolveSystemMode } from './tokens';

type EffectiveMode = 'warmDark' | 'light';

interface ThemeContextType {
  themeMode: ThemeMode;
  effectiveMode: EffectiveMode;
  setThemeMode: (mode: ThemeMode) => void;
  theme: ReturnType<typeof createTheme>;
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [themeMode, setThemeMode] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem('resto_theme_mode') as ThemeMode | null;
    return saved === 'warmDark' || saved === 'light' || saved === 'system' ? saved : 'warmDark';
  });

  const [systemPrefersDark, setSystemPrefersDark] = useState<boolean>(() => resolveSystemMode() === 'warmDark');

  // Follow OS changes while mode === 'system'
  useEffect(() => {
    if (!window.matchMedia) return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSystemPrefersDark(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  const effectiveMode: EffectiveMode = themeMode === 'system' ? (systemPrefersDark ? 'warmDark' : 'light') : themeMode;

  const currentTheme = useMemo(() => createTheme(effectiveMode), [effectiveMode]);

  useEffect(() => {
    localStorage.setItem('resto_theme_mode', themeMode);

    const root = document.documentElement;
    const colors = effectiveMode === 'warmDark' ? warmDarkColors : lightColors;

    root.style.setProperty('color-scheme', effectiveMode === 'warmDark' ? 'dark' : 'light');
    root.style.backgroundColor = colors.background;
    root.style.color = colors.textPrimary;

    const vars: Record<string, string> = {
      '--resto-bg': colors.background,
      '--resto-bg-secondary': colors.backgroundSecondary,
      '--resto-surface': colors.surface,
      '--resto-surface-elevated': colors.surfaceElevated,
      '--resto-surface-hover': colors.surfaceHover,
      '--resto-surface-selected': colors.surfaceSelected,
      '--resto-border': colors.border,
      '--resto-border-strong': colors.borderStrong,
      '--resto-text-primary': colors.textPrimary,
      '--resto-text-secondary': colors.textSecondary,
      '--resto-text-muted': colors.textMuted,
      '--resto-primary': colors.primary,
      '--resto-primary-hover': colors.primaryHover,
      '--resto-primary-pressed': colors.primaryPressed,
      '--resto-primary-container': colors.primaryLight,
      '--resto-on-primary-container': colors.primaryDark,
      '--resto-success': colors.success,
      '--resto-success-container': colors.successLight,
      '--resto-warning': colors.warning,
      '--resto-warning-container': colors.warningLight,
      '--resto-error': colors.error,
      '--resto-error-container': colors.errorLight,
      '--resto-info': colors.info,
      '--resto-info-container': colors.infoLight,
      '--resto-overlay': colors.overlay,
    };
    for (const [name, value] of Object.entries(vars)) {
      root.style.setProperty(name, value);
    }
  }, [themeMode, effectiveMode]);

  return (
    <ThemeContext.Provider value={{ themeMode, effectiveMode, setThemeMode, theme: currentTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext);
  if (!context) {
    // Fallback if rendered outside ThemeProvider
    return {
      themeMode: 'warmDark',
      effectiveMode: 'warmDark',
      setThemeMode: () => {},
      theme: createTheme('warmDark'),
    };
  }
  return context;
};
