import React, { useState, useEffect } from 'react';
import { theme } from '../theme/tokens';
import { AuthUser } from '../types';

interface TenantAppShellProps {
  currentPath: string;
  navigate: (path: string) => void;
  language: 'fa' | 'en';
  setLanguage: (lang: 'fa' | 'en') => void;
  activeOrgName: string;
  activeStoreName: string;
  onOpenTenantModal: () => void;
  authUser: AuthUser | null;
  onLogout: () => void;
  children: React.ReactNode;
}

export const TenantAppShell: React.FC<TenantAppShellProps> = ({
  currentPath,
  navigate,
  language,
  setLanguage,
  activeOrgName,
  activeStoreName,
  onOpenTenantModal,
  authUser,
  onLogout,
  children,
}) => {
  const isFa = language === 'fa';
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isMobileScreen, setIsMobileScreen] = useState(() => typeof window !== 'undefined' && window.innerWidth > 0 && window.innerWidth < 768);

  useEffect(() => {
    const handleResize = () => {
      setIsMobileScreen(window.innerWidth > 0 && window.innerWidth < 768);
      if (window.innerWidth >= 768) {
        setIsMobileOpen(false);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const tenantNavItems = [
    { path: '/app/dashboard', label: isFa ? 'داشبورد مدیریتی' : 'Dashboard', icon: '📊' },
    { path: '/app/pos', label: isFa ? 'فروشگاه و POS' : 'Point of Sale', icon: '🛒' },
    { path: '/app/orders', label: isFa ? 'سفارشات فروش' : 'Sales Orders', icon: '🧾' },
    { path: '/app/products', label: isFa ? 'کاتالوگ محصولات' : 'Product Catalog', icon: '📦' },
    { path: '/app/inventory', label: isFa ? 'انبار و موجودی' : 'Warehouse Inventory', icon: '🏭' },
    { path: '/app/purchases', label: isFa ? 'خرید و تامین' : 'Purchasing & POs', icon: '🛍️' },
    { path: '/app/suppliers', label: isFa ? 'تامین‌کنندگان' : 'Suppliers', icon: '🏢' },
    { path: '/app/customers', label: isFa ? 'مشتریان (CRM)' : 'Customers CRM', icon: '👥' },
    { path: '/app/expenses', label: isFa ? 'هزینه‌ها' : 'Expenses', icon: '💸' },
    { path: '/app/messages', label: isFa ? 'ورود پیام‌های سفارش' : 'Message Import', icon: '📩' },
    { path: '/app/accounting', label: isFa ? 'دفتر کل حسابداری' : 'Double-Entry Ledger', icon: '⚖️' },
    { path: '/app/reports', label: isFa ? 'گزارش‌های مالی' : 'Financial Reports', icon: '📈' },
    { path: '/app/settings', label: isFa ? 'تنظیمات کسب‌وکار' : 'Settings', icon: '⚙️' },
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
        backgroundColor: theme.colors.sidebarBg,
        color: theme.colors.sidebarText,
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
      {/* Brand / Logo Area */}
      <div
        style={{
          padding: theme.spacing['2xl'],
          borderBottom: `1px solid ${theme.colors.sidebarItemActiveBg}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
          <div
            style={{
              width: '36px',
              height: '36px',
              borderRadius: theme.borderRadius.lg,
              backgroundColor: theme.colors.primary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              color: '#FFF',
              fontSize: '18px',
            }}
          >
            R
          </div>
          <div>
            <div style={{ color: '#FFF', fontWeight: 700, fontSize: '16px', letterSpacing: '-0.01em' }}>
              {isFa ? 'رسـتو' : 'Resto'}
            </div>
            <div style={{ fontSize: '11px', color: theme.colors.sidebarText }}>
              {isFa ? 'مدیریت یکپارچه کسب‌وکار' : 'Commerce OS'}
            </div>
          </div>
        </div>

        {isMobileScreen && (
          <button
            onClick={() => setIsMobileOpen(false)}
            style={{ backgroundColor: 'transparent', border: 'none', color: '#FFF', fontSize: '20px', cursor: 'pointer' }}
          >
            ✕
          </button>
        )}
      </div>

      {/* Tenant Switcher Trigger */}
      <div style={{ padding: theme.spacing.lg }}>
        <button
          onClick={onOpenTenantModal}
          style={{
            width: '100%',
            backgroundColor: theme.colors.sidebarItemActiveBg,
            border: `1px solid rgba(255,255,255,0.1)`,
            borderRadius: theme.borderRadius.lg,
            padding: `${theme.spacing.md} ${theme.spacing.lg}`,
            color: '#FFF',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '13px',
            textAlign: isFa ? 'right' : 'left',
          }}
        >
          <div style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            <div style={{ fontWeight: 600, color: theme.colors.primaryLight }}>{activeOrgName}</div>
            <div style={{ fontSize: '11px', color: theme.colors.sidebarText }}>{activeStoreName}</div>
          </div>
          <span style={{ fontSize: '12px' }}>▼</span>
        </button>
      </div>

      {/* Navigation Items List */}
      <nav style={{ flex: 1, padding: `${theme.spacing.sm} ${theme.spacing.lg}`, overflowY: 'auto' }}>
        {tenantNavItems.map((item) => {
          const isActive = currentPath === item.path;
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
                backgroundColor: isActive ? theme.colors.primary : 'transparent',
                color: isActive ? theme.colors.sidebarTextActive : theme.colors.sidebarText,
                fontWeight: isActive ? 600 : 400,
                fontSize: '14px',
                cursor: 'pointer',
                marginBottom: '4px',
                transition: 'background-color 0.15s ease',
                textAlign: isFa ? 'right' : 'left',
              }}
            >
              <span style={{ fontSize: '16px' }}>{item.icon}</span>
              <span style={{ flex: 1 }}>{item.label}</span>
              {isActive && (
                <span
                  style={{
                    width: '6px',
                    height: '6px',
                    borderRadius: '50%',
                    backgroundColor: '#FFF',
                  }}
                />
              )}
            </button>
          );
        })}
      </nav>

      {/* Language Switcher Footer */}
      <div style={{ padding: theme.spacing.lg, borderTop: `1px solid ${theme.colors.sidebarItemActiveBg}` }}>
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
          🌐 {isFa ? 'تغییر به English (LTR)' : 'Switch to Persian (RTL)'}
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
      }}
    >
      {/* Sidebar Backdrop Overlay on Mobile */}
      {isMobileScreen && isMobileOpen && (
        <div
          onClick={() => setIsMobileOpen(false)}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            zIndex: 90,
          }}
        />
      )}

      {sidebarContent}

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
                }}
              >
                🍔
              </button>
            )}

            <span style={{ fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {tenantNavItems.find((n) => n.path === currentPath)?.label || (isFa ? 'پنل سازمان' : 'Business App')}
            </span>

            <span
              style={{
                fontSize: '12px',
                padding: '2px 10px',
                borderRadius: theme.borderRadius.full,
                backgroundColor: theme.colors.primaryLight,
                color: theme.colors.primaryDark,
                fontWeight: 600,
              }}
            >
              {isFa ? 'نسخه فعال (SaaS Tenant)' : 'Active Tenant Workspace'}
            </span>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
            <button
              onClick={onOpenTenantModal}
              style={{
                backgroundColor: theme.colors.background,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: '6px 12px',
                fontSize: '13px',
                fontWeight: 600,
                color: theme.colors.textPrimary,
                cursor: 'pointer',
              }}
            >
              🏢 {activeOrgName}
            </button>

            <span style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
              👤 {authUser?.name || (isFa ? 'مالک سازمان' : 'Tenant User')}
            </span>

            <button
              onClick={onLogout}
              style={{
                backgroundColor: '#FEE2E2',
                color: '#991B1B',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                padding: '6px 12px',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              🚪 {isFa ? 'خروج' : 'Logout'}
            </button>
          </div>
        </header>

        {/* Page Content Surface */}
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
