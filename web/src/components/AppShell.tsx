import React, { useState } from 'react';
import { theme } from '../theme/tokens';

interface AppShellProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  language: 'fa' | 'en';
  setLanguage: (lang: 'fa' | 'en') => void;
  activeOrgName: string;
  activeStoreName: string;
  onOpenTenantModal: () => void;
  children: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({
  activeTab,
  setActiveTab,
  language,
  setLanguage,
  activeOrgName,
  activeStoreName,
  onOpenTenantModal,
  children,
}) => {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const isFa = language === 'fa';

  const navItems = [
    { id: 'dashboard', label: isFa ? 'داشبورد مدیریتی' : 'Dashboard', icon: '📊' },
    { id: 'pos', label: isFa ? 'فروشگاه و POS' : 'Point of Sale', icon: '🛒' },
    { id: 'orders', label: isFa ? 'سفارشات فروش' : 'Sales Orders', icon: '🧾' },
    { id: 'products', label: isFa ? 'کاتالوگ محصولات' : 'Product Catalog', icon: '📦' },
    { id: 'inventory', label: isFa ? 'انبار و موجودی' : 'Warehouse Inventory', icon: '🏭' },
    { id: 'purchases', label: isFa ? 'خرید و تامین' : 'Purchasing & POs', icon: '🛍️' },
    { id: 'suppliers', label: isFa ? 'تامین‌کنندگان' : 'Suppliers', icon: '🏢' },
    { id: 'customers', label: isFa ? 'مشتریان (CRM)' : 'Customers CRM', icon: '👥' },
    { id: 'expenses', label: isFa ? 'هزینه‌ها' : 'Expenses', icon: '💸' },
    { id: 'messages', label: isFa ? 'ورود پیام‌های سفارش' : 'Message Import', icon: '📩' },
    { id: 'public_register', label: isFa ? 'ثبت‌نام آنلاین سازمان' : 'SaaS Business Register', icon: '🌐' },
    { id: 'platform_admin', label: isFa ? 'مدیریت پلتفرم (Platform Admin)' : 'Platform Admin', icon: '🛡️' },
    { id: 'accounting', label: isFa ? 'دفتر کل حسابداری' : 'Double-Entry Ledger', icon: '⚖️' },
    { id: 'reports', label: isFa ? 'گزارش‌های مالی' : 'Financial Reports', icon: '📈' },
    { id: 'settings', label: isFa ? 'تنظیمات کسب‌وکار' : 'Settings', icon: '⚙️' },
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
      {/* Desktop Dark Charcoal Sidebar */}
      <aside
        style={{
          width: '260px',
          backgroundColor: theme.colors.sidebarBg,
          color: theme.colors.sidebarText,
          display: 'flex',
          flexDirection: 'column',
          flexShrink: 0,
          boxShadow: theme.shadows.md,
          zIndex: 20,
        }}
      >
        {/* Brand / Logo Area */}
        <div
          style={{
            padding: theme.spacing['2xl'],
            borderBottom: `1px solid ${theme.colors.sidebarItemActiveBg}`,
            display: 'flex',
            alignItems: 'center',
            gap: theme.spacing.md,
          }}
        >
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
          {navItems.map((item) => {
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => {
                  setActiveTab(item.id);
                  setIsMobileMenuOpen(false);
                }}
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
          <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.lg }}>
            <span style={{ fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {navItems.find((n) => n.id === activeTab)?.label || 'Resto'}
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
              {isFa ? 'نسخه فعال (SaaS)' : 'Active Workspace'}
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
