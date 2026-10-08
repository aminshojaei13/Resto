import React from 'react';
import { useTheme } from '../theme/ThemeContext';

export interface ThemeToggleProps {
  language?: 'fa' | 'en';
  variant?: 'sidebar' | 'header' | 'inline';
}

export const ThemeToggle: React.FC<ThemeToggleProps> = ({ language, variant }) => {
  const { themeMode, setThemeMode, theme } = useTheme();

  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        backgroundColor: theme.colors.surfaceElevated,
        border: `1px solid ${theme.colors.border}`,
        borderRadius: theme.borderRadius.full,
        padding: '2px',
        gap: '2px',
      }}
    >
      <button
        type="button"
        onClick={() => setThemeMode('warmDark')}
        title="Warm Dark Theme"
        style={{
          backgroundColor: themeMode === 'warmDark' ? theme.colors.primary : 'transparent',
          color: themeMode === 'warmDark' ? '#FFF' : theme.colors.textSecondary,
          border: 'none',
          borderRadius: theme.borderRadius.full,
          padding: '4px 8px',
          fontSize: '12px',
          fontWeight: 600,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          transition: 'all 0.15s ease',
        }}
      >
        🌙
      </button>

      <button
        type="button"
        onClick={() => setThemeMode('light')}
        title="Light Theme"
        style={{
          backgroundColor: themeMode === 'light' ? theme.colors.primary : 'transparent',
          color: themeMode === 'light' ? '#FFF' : theme.colors.textSecondary,
          border: 'none',
          borderRadius: theme.borderRadius.full,
          padding: '4px 8px',
          fontSize: '12px',
          fontWeight: 600,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          transition: 'all 0.15s ease',
        }}
      >
        ☀️
      </button>

      <button
        type="button"
        onClick={() => setThemeMode('system')}
        title="System Preference"
        style={{
          backgroundColor: themeMode === 'system' ? theme.colors.primary : 'transparent',
          color: themeMode === 'system' ? '#FFF' : theme.colors.textSecondary,
          border: 'none',
          borderRadius: theme.borderRadius.full,
          padding: '4px 8px',
          fontSize: '12px',
          fontWeight: 600,
          cursor: 'pointer',
          display: 'flex',
          alignItems: 'center',
          gap: '4px',
          transition: 'all 0.15s ease',
        }}
      >
        💻
      </button>
    </div>
  );
};
