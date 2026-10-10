import React, { useState, useEffect } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ThemeToggle } from './ThemeToggle';

interface TenantAppShellProps {
  currentPath: string;
  navigate: (path: string) => void;
  language: 'fa' | 'en';
  setLanguage: (lang: 'fa' | 'en') => void;
  activeOrgName: string;
  activeStoreName: string;
  activeWarehouseName: string;
  onOpenBusinessModal: () => void;
  /** Display name of the signed-in person, exactly as the server reports it. */
  currentUserName: string;
  onLogout: () => void;
  children: React.ReactNode;
}

interface NavGroup {
  groupTitle: { fa: string; en: string };
  items: { path: string; labelFa: string; labelEn: string; icon: string }[];
}

export const TenantAppShell: React.FC<TenantAppShellProps> = ({
  currentPath,
  navigate,
  language,
  setLanguage,
  activeOrgName,
  activeStoreName,
  activeWarehouseName,
  onOpenBusinessModal,
  currentUserName,
  onLogout,
  children,
}) => {
  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';
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

  const navGroups: NavGroup[] = [
    {
      groupTitle: { fa: 'فروش', en: 'Sales' },
      items: [
        { path: '/app/dashboard', labelFa: 'نمای کلی کسب‌وکار', labelEn: 'Business Overview', icon: '📊' },
        { path: '/app/pos', labelFa: 'ثبت سفارش', labelEn: 'Register Order', icon: '🛒' },
        { path: '/app/messages', labelFa: 'ثبت سفارش از پیام', labelEn: 'Order From Message', icon: '📩' },
        { path: '/app/orders', labelFa: 'سفارش‌ها', labelEn: 'Orders', icon: '🧾' },
        { path: '/app/customers', labelFa: 'مشتریان', labelEn: 'Customers', icon: '👥' },
      ],
    },
    {
      groupTitle: { fa: 'خریدها', en: 'Purchasing' },
      items: [
        { path: '/app/purchases', labelFa: 'سفارش خرید', labelEn: 'Purchase Orders', icon: '🛍️' },
        { path: '/app/receiving', labelFa: 'دریافت کالا', labelEn: 'Receive Stock', icon: '📥' },
        { path: '/app/suppliers', labelFa: 'تأمین‌کنندگان', labelEn: 'Suppliers', icon: '🏢' },
      ],
    },
    {
      groupTitle: { fa: 'موجودی', en: 'Inventory' },
      items: [
        { path: '/app/inventory', labelFa: 'موجودی فعلی', labelEn: 'Current Stock', icon: '📦' },
        { path: '/app/products', labelFa: 'کالاها', labelEn: 'Products', icon: '🏷️' },
      ],
    },
    {
      groupTitle: { fa: 'مالی و تنظیمات', en: 'Finance & Settings' },
      items: [
        { path: '/app/expenses', labelFa: 'هزینه‌ها', labelEn: 'Expenses', icon: '💸' },
        { path: '/app/accounting', labelFa: 'دفتر کل و گزارش‌ها', labelEn: 'Ledger & Reports', icon: '⚖️' },
        { path: '/app/settings', labelFa: 'تنظیمات کسب‌وکار', labelEn: 'Business Settings', icon: '🔧' },
        { path: '/app/staff', labelFa: 'کارمندان', labelEn: 'Staff', icon: '👤' },
        { path: '/app/profile', labelFa: 'حساب کاربری من', labelEn: 'My account', icon: '⚙️' },
      ],
    },
  ];

  const allNavItems = navGroups.flatMap((g) => g.items);

  const handleNavClick = (path: string) => {
    navigate(path);
    if (isMobileScreen) {
      setIsMobileOpen(false);
    }
  };

  const currentItem = allNavItems.find((n) => n.path === currentPath);

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
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
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
              {isFa ? 'سیستم یکپارچه مدیریت کسب‌وکار' : 'Commerce Management OS'}
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

      {/* Business switcher */}
      <div style={{ padding: theme.spacing.lg }}>
        <button
          onClick={onOpenBusinessModal}
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
            <div style={{ fontWeight: 600, color: theme.colors.sidebarTextActive }}>{activeOrgName}</div>
            <div style={{ fontSize: '11px', color: theme.colors.sidebarText }}>
              {[activeStoreName, activeWarehouseName].filter(Boolean).join(' · ')}
            </div>
          </div>
          <span style={{ fontSize: '12px' }}>▼</span>
        </button>
      </div>

      {/* Grouped Navigation Items */}
      <nav style={{ flex: 1, padding: `${theme.spacing.sm} ${theme.spacing.lg}`, overflowY: 'auto' }}>
        {navGroups.map((group, groupIdx) => (
          <div key={groupIdx} style={{ marginBottom: theme.spacing.lg }}>
            <div
              style={{
                fontSize: '11px',
                fontWeight: 700,
                color: 'rgba(255,255,255,0.4)',
                textTransform: 'uppercase',
                padding: `0 ${theme.spacing.md} ${theme.spacing.xs}`,
                letterSpacing: '0.05em',
              }}
            >
              {isFa ? group.groupTitle.fa : group.groupTitle.en}
            </div>
            {group.items.map((item) => {
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
                    backgroundColor: isActive ? theme.colors.sidebarItemActiveBg : 'transparent',
                    color: isActive ? theme.colors.primary : theme.colors.sidebarText,
                    fontWeight: isActive ? 600 : 400,
                    fontSize: '13px',
                    cursor: 'pointer',
                    marginBottom: '3px',
                    transition: 'background-color 0.15s ease',
                    textAlign: isFa ? 'right' : 'left',
                  }}
                >
                  <span style={{ fontSize: '16px' }}>{item.icon}</span>
                  <span style={{ flex: 1 }}>{isFa ? item.labelFa : item.labelEn}</span>
                  {isActive && (
                    <span
                      style={{
                        width: '6px',
                        height: '6px',
                        borderRadius: '50%',
                        backgroundColor: theme.colors.primary,
                      }}
                    />
                  )}
                </button>
              );
            })}
          </div>
        ))}
      </nav>

      {/* Language Switcher Footer */}
      <div style={{ padding: theme.spacing.lg, borderTop: '1px solid rgba(255, 255, 255, 0.08)' }}>
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
            backgroundColor: theme.colors.overlay,
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
                }}
              >
                🍔
              </button>
            )}

            <span style={{ fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {currentItem ? (isFa ? currentItem.labelFa : currentItem.labelEn) : (isFa ? 'پنل کسب‌وکار' : 'Business OS')}
            </span>

          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
            <ThemeToggle language={language} />

            <button
              onClick={onOpenBusinessModal}
              style={{
                backgroundColor: theme.colors.surfaceHover,
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

            <button
              onClick={() => navigate('/app/profile')}
              style={{
                background: 'none',
                border: 'none',
                fontSize: '13px',
                fontWeight: 600,
                color: theme.colors.textSecondary,
                cursor: 'pointer',
                padding: '6px 4px',
              }}
            >
              👤 {currentUserName}
            </button>

            <button
              onClick={onLogout}
              style={{
                backgroundColor: isDark ? theme.colors.errorLight : '#FEE2E2',
                color: isDark ? theme.colors.error : '#991B1B',
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
