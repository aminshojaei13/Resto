import React, { useState, useEffect } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ThemeToggle } from './ThemeToggle';
import { AuthUser } from '../types';

interface PlatformAdminShellProps {
  currentPath: string;
  navigate: (path: string) => void;
  language: 'fa' | 'en';
  setLanguage: (lang: 'fa' | 'en') => void;
  authUser: AuthUser | null;
  onLogout: () => void;
  children: React.ReactNode;
}

export const PlatformAdminShell: React.FC<PlatformAdminShellProps> = ({
  currentPath,
  navigate,
  language,
  setLanguage,
  authUser,
  onLogout,
  children,
}) => {
  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isMobileScreen, setIsMobileScreen] = useState(() => typeof window !== 'undefined' && window.innerWidth < 768);

  useEffect(() => {
    const handleResize = () => {
      setIsMobileScreen(window.innerWidth < 768);
      if (window.innerWidth >= 768) {
        setIsMobileOpen(false);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const adminNavItems = [
    {
      path: '/applications',
      label: isFa ? 'درخواست‌های ثبت سازمان' : 'Business Applications',
      icon: '📝',
    },
    {
      path: '/tenants',
      label: isFa ? 'سازمان‌ها و تننت‌های فعال' : 'Provisioned Tenants',
      icon: '🏢',
    },
    {
      path: '/audit',
      label: isFa ? 'دفتر ثبت رویدادها' : 'Audit Trail',
      icon: '📋',
    },
  ];

  const handleNavClick = (path: string) => {
    navigate(path);
    if (isMobileScreen) {
      setIsMobileOpen(false);
    }
  };

  const sidebarContent = (
    <aside
      style={{
        width: '260px',
        backgroundColor: theme.colors.surface,
        color: theme.colors.textSecondary,
        display: 'flex',
        flexDirection: 'column',
        flexShrink: 0,
        boxShadow: theme.shadows.md,
        zIndex: 100,
        position: isMobileScreen ? 'fixed' : 'relative',
        top: 0,
        bottom: 0,
        [isFa ? 'right' : 'left']: isMobileScreen ? (isMobileOpen ? '0' : '-280px') : 'auto',
        transition: 'all 0.3s ease-in-out',
      }}
    >
      <div
        style={{
          padding: theme.spacing['2xl'],
          borderBottom: `1px solid ${theme.colors.border}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
          <div
            style={{
              width: '38px',
              height: '38px',
              borderRadius: theme.borderRadius.lg,
              backgroundColor: theme.colors.infoLight,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: '18px',
              color: theme.colors.info,
            }}
          >
            🛡️
          </div>
          <div>
            <div style={{ color: theme.colors.textPrimary, fontWeight: 700, fontSize: '15px' }}>
              {isFa ? 'مدیریت پلتفرم' : 'Platform Admin'}
            </div>
            <div style={{ fontSize: '11px', color: theme.colors.textMuted }}>
              {isFa ? 'کنسول راهبری Resto' : 'Resto SaaS Control Panel'}
            </div>
          </div>
        </div>

        {isMobileScreen && (
          <button
            onClick={() => setIsMobileOpen(false)}
            style={{ backgroundColor: 'transparent', border: 'none', color: theme.colors.textPrimary, fontSize: '20px', cursor: 'pointer' }}
          >
            ✕
          </button>
        )}
      </div>

      <nav style={{ flex: 1, padding: `${theme.spacing.lg} ${theme.spacing.md}` }}>
        {adminNavItems.map((item) => {
          const isActive = currentPath.startsWith(item.path);
          return (
            <button
              key={item.path}
              onClick={() => handleNavClick(item.path)}
              style={{
                width: '100%',
                display: 'flex',
                alignItems: 'center',
                gap: theme.spacing.md,
                padding: `${theme.spacing.md} ${theme.spacing.lg}`,
                borderRadius: theme.borderRadius.lg,
                border: 'none',
                backgroundColor: isActive ? theme.colors.infoLight : 'transparent',
                color: isActive ? theme.colors.info : theme.colors.textSecondary,
                fontWeight: isActive ? 700 : 500,
                fontSize: '14px',
                cursor: 'pointer',
                marginBottom: '6px',
                textAlign: isFa ? 'right' : 'left',
              }}
            >
              <span style={{ fontSize: '18px' }}>{item.icon}</span>
              <span style={{ flex: 1 }}>{item.label}</span>
            </button>
          );
        })}
      </nav>

      <div style={{ padding: theme.spacing.lg, borderTop: `1px solid ${theme.colors.border}` }}>
        <button
          onClick={() => setLanguage(isFa ? 'en' : 'fa')}
          style={{
            width: '100%',
            backgroundColor: theme.colors.surfaceHover,
            border: `1px solid ${theme.colors.border}`,
            borderRadius: theme.borderRadius.lg,
            padding: `${theme.spacing.md} ${theme.spacing.lg}`,
            color: theme.colors.textPrimary,
            fontWeight: 600,
            fontSize: '13px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: theme.spacing.sm,
          }}
        >
          🌐 {isFa ? 'English (LTR)' : 'فارسی (RTL)'}
        </button>
      </div>
    </aside>
  );

  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        backgroundColor: theme.colors.background,
        fontFamily: theme.typography.fontFamily,
        direction: isFa ? 'rtl' : 'ltr',
        position: 'relative',
        color: theme.colors.textPrimary,
      }}
    >
      {isMobileScreen && isMobileOpen && (
        <div
          onClick={() => setIsMobileOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: theme.colors.overlay,
            zIndex: 90,
          }}
        />
      )}

      {sidebarContent}

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflowX: 'hidden' }}>
        <header
          style={{
            backgroundColor: theme.colors.surfaceElevated,
            borderBottom: `1px solid ${theme.colors.border}`,
            padding: `${theme.spacing.lg} ${theme.spacing['2xl']}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: theme.shadows.sm,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
            {isMobileScreen && (
              <button
                onClick={() => setIsMobileOpen(!isMobileOpen)}
                style={{
                  backgroundColor: 'transparent',
                  border: `1px solid ${theme.colors.border}`,
                  borderRadius: theme.borderRadius.md,
                  padding: '6px 12px',
                  fontSize: '18px',
                  cursor: 'pointer',
                  color: theme.colors.textPrimary,
                }}
              >
                🍔
              </button>
            )}

            <span style={{ fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {isFa ? 'کنسول مدیریت پلتفرم (Platform Admin)' : 'Platform Admin Console'}
            </span>
            <span
              style={{
                fontSize: '11px',
                padding: '2px 10px',
                borderRadius: theme.borderRadius.full,
                backgroundColor: theme.colors.infoLight,
                color: theme.colors.info,
                fontWeight: 700,
              }}
            >
              {isFa ? 'سطح دسترسی راهبر کل' : 'Super Admin Mode'}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.lg }}>
            <ThemeToggle language={language} />
            <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
              👤 {authUser?.name || (isFa ? 'راهبر سیستم' : 'System Admin')} ({authUser?.email || 'admin@resto.com'})
            </div>
            <button
              onClick={onLogout}
              style={{
                backgroundColor: isDark ? theme.colors.errorLight : '#FEE2E2',
                color: isDark ? theme.colors.error : '#991B1B',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                padding: '6px 14px',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              🚪 {isFa ? 'خروج' : 'Logout'}
            </button>
          </div>
        </header>

        <main
          style={{
            flex: 1,
            padding: theme.spacing['2xl'],
            maxWidth: '1440px',
            width: '100%',
            margin: '0 auto',
            boxSizing: 'border-box',
          }}
        >
          {children}
        </main>
      </div>
    </div>
  );
};
