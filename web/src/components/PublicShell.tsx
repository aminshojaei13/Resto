import React from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ThemeToggle } from './ThemeToggle';

interface PublicShellProps {
  currentPath: string;
  navigate: (path: string) => void;
  language: 'fa' | 'en';
  setLanguage: (lang: 'fa' | 'en') => void;
  children: React.ReactNode;
}

export const PublicShell: React.FC<PublicShellProps> = ({
  currentPath,
  navigate,
  language,
  setLanguage,
  children,
}) => {
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const navLinks = [
    { path: '/register', label: isFa ? 'ثبت‌نام کسب‌وکار' : 'Register Business', icon: '🚀' },
    { path: '/login', label: isFa ? 'ورود به پنل' : 'Tenant Login', icon: '🔑' },
    { path: '/activation', label: isFa ? 'راهنمای فعال‌سازی' : 'Activation Info', icon: '📲' },
    { path: '/platform/login', label: isFa ? 'ورود راهبر پلتفرم' : 'Platform Admin', icon: '🛡️' },
  ];

  return (
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: theme.colors.background,
        color: theme.colors.textPrimary,
        fontFamily: theme.typography.fontFamily,
        direction: isFa ? 'rtl' : 'ltr',
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Public Header */}        <header
          style={{
            backgroundColor: theme.colors.surfaceElevated,
            borderBottom: `1px solid ${theme.colors.border}`,
          padding: `${theme.spacing.lg} ${theme.spacing['2xl']}`,
          boxShadow: theme.shadows.sm,
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}
      >
        <div
          style={{
            maxWidth: '1280px',
            margin: '0 auto',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: theme.spacing.lg,
          }}
        >
          {/* Logo / Brand */}
          <div
            onClick={() => navigate('/register')}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: theme.spacing.md,
              cursor: 'pointer',
            }}
          >
            <div
              style={{
                width: '40px',
                height: '40px',
                borderRadius: theme.borderRadius.lg,
                backgroundColor: theme.colors.primary,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 800,
                color: '#FFF',
                fontSize: '20px',
              }}
            >
              R
            </div>
            <div>
              <div style={{ color: theme.colors.textPrimary, fontWeight: 800, fontSize: '20px', letterSpacing: '-0.02em' }}>
                {isFa ? 'رسـتو' : 'Resto'}
              </div>
              <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                {isFa ? 'پلتفرم جامع مدیریت کسب‌وکار و فروش' : 'Unified Business Operations Platform'}
              </div>
            </div>
          </div>

          {/* Nav Navigation */}
          <nav style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
            {navLinks.map((link) => {
              const isActive = currentPath === link.path;
              return (
                <button
                  key={link.path}
                  onClick={() => navigate(link.path)}
                  style={{
                    backgroundColor: isActive ? theme.colors.surfaceSelected : 'transparent',
                    color: isActive ? theme.colors.primary : theme.colors.textPrimary,
                    border: 'none',
                    borderRadius: theme.borderRadius.md,
                    padding: `${theme.spacing.sm} ${theme.spacing.lg}`,
                    fontWeight: isActive ? 700 : 500,
                    fontSize: '14px',
                    cursor: 'pointer',
                    display: 'flex',
                    alignItems: 'center',
                    gap: theme.spacing.xs,
                    transition: 'all 0.15s ease',
                  }}
                >
                  <span>{link.icon}</span>
                  <span>{link.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Theme + Language Switcher */}
          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
            <ThemeToggle language={language} />
            <button
              onClick={() => setLanguage(isFa ? 'en' : 'fa')}
              style={{
                backgroundColor: theme.colors.surfaceHover,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: `${theme.spacing.sm} ${theme.spacing.md}`,
                fontSize: '13px',
                fontWeight: 600,
                color: theme.colors.textPrimary,
                cursor: 'pointer',
              }}
            >
              🌐 {isFa ? 'English' : 'فارسی'}
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main style={{ flex: 1, padding: `${theme.spacing['2xl']} ${theme.spacing.lg}` }}>
        <div style={{ maxWidth: '1280px', margin: '0 auto' }}>{children}</div>
      </main>

      {/* Public Footer */}
      <footer
        style={{
          backgroundColor: theme.colors.surface,
          borderTop: `1px solid ${theme.colors.border}`,
          padding: theme.spacing.xl,
          textAlign: 'center',
          color: theme.colors.textSecondary,
          fontSize: '13px',
        }}
      >
        {isFa
          ? '© ۲۰۲۶ رستو. تمامی حقوق محفوظ است.'
          : '© 2026 Resto. All rights reserved.'}
      </footer>
    </div>
  );
};
