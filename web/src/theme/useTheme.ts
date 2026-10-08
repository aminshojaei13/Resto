import { useTheme } from './ThemeContext';
import { ThemeMode } from './tokens';

export function useThemeMode() {
  const { themeMode, effectiveMode, setThemeMode } = useTheme();

  return {
    mode: effectiveMode,
    themeMode,
    toggle: () => setThemeMode(effectiveMode === 'warmDark' ? 'light' : 'warmDark'),
    setMode: (m: ThemeMode) => setThemeMode(m),
  };
}

export { useTheme };
