import React from 'react';
import { theme } from '../theme/tokens';
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

  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        backgroundColor: theme.colors.background,
        fontFamily: theme.typography.fontFamily,
        direction: isFa ? 'rtl' : 'ltr',
      }}
    >
      {/* Platform Admin Dark Sidebar */}
      <aside
        style={{
          width: '260px',
          backgroundColor: '#1E293B', // Dark slate for admin
          color: '#F8FAFC',
          display: 'flex',
          flexDirection: 'column',
          flexShrink: 0,
          boxShadow: theme.shadows.md,
          zIndex: 20,
        }}
      >
        {/* Platform Admin Header Logo */}
        <div
          style={{
            padding: theme.spacing['2xl'],
            borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
            display: 'flex',
            alignItems: 'center',
            gap: theme.spacing.md,
          }}
        >
          <div
            style={{
              width: '38px',
              height: '38px',
              borderRadius: theme.borderRadius.lg,
              backgroundColor: '#38BDF8', // Cyan for admin badge
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              color: '#0F172A',
              fontSize: '18px',
            }}
          >
            🛡️
          </div>
          <div>
            <div style={{ color: '#FFF', fontWeight: 700, fontSize: '15px' }}>
              {isFa ? 'مدیریت پلتفرم' : 'Platform Admin'}
            </div>
            <div style={{ fontSize: '11px', color: '#94A3B8' }}>
              {isFa ? 'کنسول راهبری Resto' : 'Resto SaaS Control Panel'}
            </div>
          </div>
        </div>

        {/* Admin Navigation */}
        <nav style={{ flex: 1, padding: `${theme.spacing.lg} ${theme.spacing.md}` }}>
          {adminNavItems.map((item) => {
            const isActive = currentPath.startsWith(item.path);
            return (
              <button
                key={item.path}
                onClick={() => navigate(item.path)}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  gap: theme.spacing.md,
                  padding: `${theme.spacing.md} ${theme.spacing.lg}`,
                  borderRadius: theme.borderRadius.lg,
                  border: 'none',
                  backgroundColor: isActive ? '#0284C7' : 'transparent',
                  color: isActive ? '#FFF' : '#CBD5E1',
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

        {/* Language Switcher Footer */}
        <div style={{ padding: theme.spacing.lg, borderTop: '1px solid rgba(255, 255, 255, 0.1)' }}>
          <button
            onClick={() => setLanguage(isFa ? 'en' : 'fa')}
            style={{
              width: '100%',
              backgroundColor: 'rgba(255,255,255,0.05)',
              border: '1px solid rgba(255,255,255,0.1)',
              borderRadius: theme.borderRadius.lg,
              padding: `${theme.spacing.md} ${theme.spacing.lg}`,
              color: '#FFF',
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

      {/* Main Container Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0, overflowX: 'hidden' }}>
        {/* Top Header Bar */}
        <header
          style={{
            backgroundColor: theme.colors.surface,
            borderBottom: `1px solid ${theme.colors.border}`,
            padding: `${theme.spacing.lg} ${theme.spacing['2xl']}`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: theme.shadows.sm,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
            <span style={{ fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {isFa ? 'کنسول مدیریت پلتفرم (Platform Admin)' : 'Platform Admin Console'}
            </span>
            <span
              style={{
                fontSize: '11px',
                padding: '2px 10px',
                borderRadius: theme.borderRadius.full,
                backgroundColor: '#E0F2FE',
                color: '#0369A1',
                fontWeight: 700,
              }}
            >
              {isFa ? 'سطح دسترسی راهبر کل' : 'Super Admin Mode'}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.lg }}>
            <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
              👤 {authUser?.name || (isFa ? 'راهبر سیستم' : 'System Admin')} ({authUser?.email || 'admin@resto.com'})
            </div>
            <button
              onClick={onLogout}
              style={{
                backgroundColor: '#FEE2E2',
                color: '#991B1B',
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

        {/* Page Content */}
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
