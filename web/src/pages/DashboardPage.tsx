import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { AccountingSummary, LedgerEntry, Product } from '../types';
import { StatCard } from '../components/StatCard';
import { PageHeader } from '../components/PageHeader';
import { StatusBadge } from '../components/StatusBadge';
import { useTheme } from '../theme/ThemeContext';

interface DashboardPageProps {
  language?: 'fa' | 'en';
  navigate?: (path: string) => void;
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ language = 'fa', navigate }) => {
  const [summary, setSummary] = useState<AccountingSummary | null>(null);
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  useEffect(() => {
    const fetchData = async () => {
      try {
        const s = await apiClient.getAccountingSummary();
        if (s) setSummary(s);
      } catch {}
      try {
        const e = await apiClient.getJournalEntries();
        if (e) setEntries(e);
      } catch {}
      try {
        const p = await apiClient.getProducts();
        if (p) setProducts(p);
      } catch {}
    };
    fetchData();
  }, []);

  const totalRevenue = summary?.totalRevenue || 0;
  const todayRevenue = summary?.todayRevenue || 0;
  const totalExpenses = summary?.totalExpenses || 0;
  const netProfit = summary?.netProfit ?? (totalRevenue - totalExpenses);
  const totalSalesCount = summary?.totalSalesCount || 0;
  const todaySalesCount = summary?.todaySalesCount || 0;

  // Inventory Metrics derived from products
  const totalProductsCount = products.length;
  const lowStockProducts = products.filter((p) => {
    const totalQty = Object.values(p.stockQuantityByWarehouse || {}).reduce((acc, v) => acc + v, 0);
    return totalQty <= 5;
  });

  const handleNav = (path: string) => {
    if (navigate) {
      navigate(path);
    } else {
      window.location.pathname = path;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing['2xl'] }}>
      {/* Top Header Controls */}
      <PageHeader
        title={isFa ? 'مرکز مدیریت و عملیات کسب‌وکار' : 'Business Control Center'}
        description={isFa ? 'مدیریت متمرکز موجودی انبار، سفارش‌های آنلاین و جریان عملیاتی روزانه' : 'Centralized management of inventory, online orders, and daily business operations'}
      />

      {/* 5 Prioritized Quick Actions Banner */}
      <div
        style={{
          backgroundColor: theme.colors.surface,
          border: `1px solid ${theme.colors.border}`,
          borderRadius: theme.borderRadius.xl,
          padding: theme.spacing.xl,
          boxShadow: theme.shadows.card,
        }}
      >
        <div style={{ fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary, marginBottom: theme.spacing.lg, display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
          <span>⚡</span>
          <span>{isFa ? 'اقدامات سریع و عملیاتی روزانه' : 'Daily Quick Operations'}</span>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
            gap: theme.spacing.md,
          }}
        >
          <button
            onClick={() => handleNav('/app/pos')}
            style={{
              backgroundColor: theme.colors.primary,
              color: '#FFF',
              border: 'none',
              borderRadius: theme.borderRadius.lg,
              padding: `${theme.spacing.lg} ${theme.spacing.xl}`,
              fontSize: '14px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: theme.spacing.md,
              boxShadow: theme.shadows.sm,
            }}
          >
            <span>🛒</span>
            <span>{isFa ? 'ثبت سفارش جدید' : 'Register New Order'}</span>
          </button>

          <button
            onClick={() => handleNav('/app/messages')}
            style={{
              backgroundColor: theme.colors.surfaceSelected,
              color: theme.colors.primary,
              border: `1px solid ${theme.colors.borderStrong}`,
              borderRadius: theme.borderRadius.lg,
              padding: `${theme.spacing.lg} ${theme.spacing.xl}`,
              fontSize: '14px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: theme.spacing.md,
              boxShadow: theme.shadows.sm,
            }}
          >
            <span>📩</span>
            <span>{isFa ? 'ثبت سفارش از پیام' : 'Social Message Import'}</span>
          </button>

          <button
            onClick={() => handleNav('/app/inventory')}
            style={{
              backgroundColor: theme.colors.surface,
              color: theme.colors.textPrimary,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.lg,
              padding: `${theme.spacing.lg} ${theme.spacing.xl}`,
              fontSize: '14px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: theme.spacing.md,
            }}
          >
            <span>📦</span>
            <span>{isFa ? 'افزودن کالا' : 'Add Product'}</span>
          </button>

          <button
            onClick={() => handleNav('/app/purchases')}
            style={{
              backgroundColor: theme.colors.surface,
              color: theme.colors.textPrimary,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.lg,
              padding: `${theme.spacing.lg} ${theme.spacing.xl}`,
              fontSize: '14px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: theme.spacing.md,
            }}
          >
            <span>🛍️</span>
            <span>{isFa ? 'ثبت خرید و ورود انبار' : 'Purchasing & Receiving'}</span>
          </button>

          <button
            onClick={() => handleNav('/app/inventory')}
            style={{
              backgroundColor: theme.colors.surface,
              color: theme.colors.textPrimary,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.lg,
              padding: `${theme.spacing.lg} ${theme.spacing.xl}`,
              fontSize: '14px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: theme.spacing.md,
            }}
          >
            <span>🏭</span>
            <span>{isFa ? 'مدیریت موجودی انبار' : 'Manage Inventory'}</span>
          </button>
        </div>
      </div>

      {/* THREE CORE NEED CARDS */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(340px, 1fr))',
          gap: theme.spacing['2xl'],
        }}
      >
        {/* CORE NEED 1: INVENTORY & RESOURCE MANAGEMENT */}
        <div
          style={{
            backgroundColor: theme.colors.surface,
            borderRadius: theme.borderRadius.xl,
            padding: theme.spacing['2xl'],
            border: `2px solid ${theme.colors.primaryLight}`,
            boxShadow: theme.shadows.card,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.lg }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
                <div style={{ width: '40px', height: '40px', borderRadius: theme.borderRadius.lg, backgroundColor: theme.colors.primaryLight, color: theme.colors.primaryDark, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px' }}>
                  📦
                </div>
                <div>
                  <h3 style={{ fontSize: '17px', fontWeight: 800, margin: 0, color: theme.colors.textPrimary }}>
                    {isFa ? '۱. کنترل موجودی و کالاها' : '1. Inventory & Resource Control'}
                  </h3>
                  <span style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                    {isFa ? 'مدیریت متمرکز کالاها، انبارها و تامین' : 'Products, warehouses & stock levels'}
                  </span>
                </div>
              </div>
              <StatusBadge label={isFa ? 'رکن اصلی' : 'Core Need'} variant="info" />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md, marginBottom: theme.spacing.xl }}>
              <div style={{ backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg }}>
                <div style={{ fontSize: '12px', color: theme.colors.textMuted }}>{isFa ? 'تعداد کل کالاها' : 'Total Products'}</div>
                <div style={{ fontSize: '22px', fontWeight: 800, color: theme.colors.textPrimary }}>{totalProductsCount}</div>
              </div>
              <div style={{ backgroundColor: lowStockProducts.length > 0 ? (isDark ? theme.colors.errorLight : '#FEF2F2') : theme.colors.backgroundSecondary, borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg }}>
                <div style={{ fontSize: '12px', color: lowStockProducts.length > 0 ? (isDark ? theme.colors.error : '#991B1B') : theme.colors.textMuted }}>{isFa ? 'هشدار کمبود موجودی' : 'Low Stock Alert'}</div>
                <div style={{ fontSize: '22px', fontWeight: 800, color: lowStockProducts.length > 0 ? (isDark ? theme.colors.error : '#DC2626') : theme.colors.textPrimary }}>
                  {lowStockProducts.length}
                </div>
              </div>
            </div>

            <p style={{ fontSize: '13px', color: theme.colors.textSecondary, lineHeight: 1.6, margin: `0 0 ${theme.spacing.lg} 0` }}>
              {isFa
                ? 'ورودی و خروجی انبار، کسری موجودی و تامین کالاها به‌صورت آنی مدیریت و به‌روزرسانی می‌شوند.'
                : 'Real-time stock movements, low stock alerts, and receiving from suppliers.'}
            </p>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <button
              onClick={() => handleNav('/app/inventory')}
              style={{
                flex: 1,
                backgroundColor: theme.colors.primary,
                color: '#FFF',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              {isFa ? 'مدیریت موجودی' : 'Manage Inventory'}
            </button>
            <button
              onClick={() => handleNav('/app/purchases')}
              style={{
                backgroundColor: theme.colors.background,
                color: theme.colors.textPrimary,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              {isFa ? 'ثبت خرید' : 'PO Receiving'}
            </button>
          </div>
        </div>

        {/* CORE NEED 2: ONLINE / SOCIAL ORDERS */}
        <div
          style={{
            backgroundColor: theme.colors.surface,
            borderRadius: theme.borderRadius.xl,
            padding: theme.spacing['2xl'],
            border: isDark ? '2px solid #2C2545' : '2px solid #DDD6FE',
            boxShadow: theme.shadows.card,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.lg }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
                <div style={{ width: '40px', height: '40px', borderRadius: theme.borderRadius.lg, backgroundColor: isDark ? '#2A2340' : '#EDE9FE', color: isDark ? '#B49CF8' : '#7C3AED', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px' }}>
                  💬
                </div>
                <div>
                  <h3 style={{ fontSize: '17px', fontWeight: 800, margin: 0, color: theme.colors.textPrimary }}>
                    {isFa ? '۲. ثبت سریع سفارش‌های آنلاین' : '2. Fast Online & Social Orders'}
                  </h3>
                  <span style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                    {isFa ? 'ورود پیام سفارشات اینستاگرام، تلگرام و واتساپ' : 'Instagram, Telegram & WhatsApp order import'}
                  </span>
                </div>
              </div>
              <StatusBadge label={isFa ? 'سرعت بالا' : 'Fast Import'} variant="success" />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md, marginBottom: theme.spacing.xl }}>
              <div style={{ backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg }}>
                <div style={{ fontSize: '12px', color: theme.colors.textMuted }}>{isFa ? 'سفارش‌های امروز' : "Today's Orders"}</div>
                <div style={{ fontSize: '22px', fontWeight: 800, color: theme.colors.textPrimary }}>{todaySalesCount}</div>
              </div>
              <div style={{ backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg }}>
                <div style={{ fontSize: '12px', color: theme.colors.textMuted }}>{isFa ? 'درآمد امروز' : "Today's Revenue"}</div>
                <div style={{ fontSize: '22px', fontWeight: 800, color: theme.colors.success }}>
                  {isFa ? `${todayRevenue.toLocaleString('fa-IR')} تومان` : `$${todayRevenue}`}
                </div>
              </div>
            </div>

            <p style={{ fontSize: '13px', color: theme.colors.textSecondary, lineHeight: 1.6, margin: `0 0 ${theme.spacing.lg} 0` }}>
              {isFa
                ? 'متن سفارش‌های مشتریان را از شبکه‌های اجتماعی کپی و جای‌گذاری کنید تا اقلام، قیمت‌ها و مشتری بلافاصله استخراج و ثبت شوند.'
                : 'Copy and paste customer order messages from social apps to extract items, quantities, and customer details instantly.'}
            </p>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <button
              onClick={() => handleNav('/app/messages')}
              style={{
                flex: 1,
                backgroundColor: isDark ? '#5B21B6' : '#7C3AED',
                color: '#FFF',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 700,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              {isFa ? 'ثبت سفارش از پیام' : 'Import Order Message'}
            </button>
            <button
              onClick={() => handleNav('/app/pos')}
              style={{
                backgroundColor: theme.colors.background,
                color: theme.colors.textPrimary,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              {isFa ? 'ثبت دستی' : 'Manual POS'}
            </button>
          </div>
        </div>

        {/* CORE NEED 3: CENTRALIZED WORKSPACE (REDUCE EXCEL DEPENDENCY) */}
        <div
          style={{
            backgroundColor: theme.colors.surface,
            borderRadius: theme.borderRadius.xl,
            padding: theme.spacing['2xl'],
            border: `2px solid ${theme.colors.border}`,
            boxShadow: theme.shadows.card,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.lg }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
                <div style={{ width: '40px', height: '40px', borderRadius: theme.borderRadius.lg, backgroundColor: isDark ? theme.colors.successLight : '#D1FAE5', color: isDark ? theme.colors.success : '#065F46', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '20px' }}>
                  📊
                </div>
                <div>
                  <h3 style={{ fontSize: '17px', fontWeight: 800, margin: 0, color: theme.colors.textPrimary }}>
                    {isFa ? '۳. مرکز متمرکز داده‌های کسب‌وکار' : '3. Centralized Business Workspace'}
                  </h3>
                  <span style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                    {isFa ? 'جایگزینی متمرکز اکسل و فایل‌های پراکنده' : 'Unified product, customer & financial data'}
                  </span>
                </div>
              </div>
              <StatusBadge label={isFa ? 'یکپارچه' : 'Unified'} variant="success" />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md, marginBottom: theme.spacing.xl }}>
              <div style={{ backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg }}>
                <div style={{ fontSize: '12px', color: theme.colors.textMuted }}>{isFa ? 'کل فروش ثبت‌شده' : 'Total Revenue'}</div>
                <div style={{ fontSize: '18px', fontWeight: 800, color: theme.colors.textPrimary }}>
                  {isFa ? `${totalRevenue.toLocaleString('fa-IR')} تومان` : `$${totalRevenue}`}
                </div>
              </div>
              <div style={{ backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg }}>
                <div style={{ fontSize: '12px', color: theme.colors.textMuted }}>{isFa ? 'سود خالص عملیاتی' : 'Net Operating Profit'}</div>
                <div style={{ fontSize: '18px', fontWeight: 800, color: netProfit >= 0 ? theme.colors.success : theme.colors.error }}>
                  {isFa ? `${netProfit.toLocaleString('fa-IR')} تومان` : `$${netProfit}`}
                </div>
              </div>
            </div>

            <p style={{ fontSize: '13px', color: theme.colors.textSecondary, lineHeight: 1.6, margin: `0 0 ${theme.spacing.lg} 0` }}>
              {isFa
                ? 'همه داده‌های کالاها، انبار، سفارشات، مشتریان، هزینه‌ها و دفتر کل به‌صورت خودکار و یکپارچه در یک محیط متمرکز نگهداری می‌شوند.'
                : 'Products, inventory, purchases, customers, orders, expenses, and ledger entries centralized in one operational workspace.'}
            </p>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <button
              onClick={() => handleNav('/app/customers')}
              style={{
                flex: 1,
                backgroundColor: theme.colors.surface,
                color: theme.colors.textPrimary,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              👥 {isFa ? 'مشتریان' : 'Customers'}
            </button>
            <button
              onClick={() => handleNav('/app/expenses')}
              style={{
                flex: 1,
                backgroundColor: theme.colors.surface,
                color: theme.colors.textPrimary,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              💸 {isFa ? 'هزینه‌ها' : 'Expenses'}
            </button>
            <button
              onClick={() => handleNav('/app/accounting')}
              style={{
                flex: 1,
                backgroundColor: theme.colors.surface,
                color: theme.colors.textPrimary,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
                fontWeight: 600,
                fontSize: '13px',
                cursor: 'pointer',
              }}
            >
              ⚖️ {isFa ? 'دفتر کل' : 'Ledger'}
            </button>
          </div>
        </div>
      </div>

      {/* General Ledger Recent Activity Stream */}
      <div
        style={{
          backgroundColor: theme.colors.surface,
          borderRadius: theme.borderRadius.xl,
          padding: theme.spacing['2xl'],
          border: `1px solid ${theme.colors.border}`,
          boxShadow: theme.shadows.card,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.xl }}>
          <div>
            <h3 style={{ fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary, margin: 0 }}>
              {isFa ? 'تراکنش‌ها و فعالیت‌های اخیر سیستم' : 'Recent Operations Stream'}
            </h3>
            <p style={{ fontSize: '13px', color: theme.colors.textSecondary, margin: `${theme.spacing.xs} 0 0 0` }}>
              {isFa ? 'اسناد دوطرفه حسابداری و ثبت‌های اتوماتیک فروش و انبار' : 'Real-time double-entry posting and inventory movements'}
            </p>
          </div>
          <button
            onClick={() => handleNav('/app/accounting')}
            style={{
              backgroundColor: 'transparent',
              color: theme.colors.primary,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.md,
              padding: '6px 14px',
              fontWeight: 600,
              fontSize: '13px',
              cursor: 'pointer',
            }}
          >
            {isFa ? 'مشاهده دفتر کل ←' : 'View Full Ledger →'}
          </button>
        </div>

        <div style={{ overflowX: 'auto' }}>
          {entries.length === 0 ? (
            <div style={{ textAlign: 'center', padding: theme.spacing['2xl'], color: theme.colors.textSecondary, fontSize: '14px' }}>
              {isFa ? 'هنوز هیچ تراکنشی ثبت نشده است. اولین سفارش یا خرید خود را ثبت کنید.' : 'No transaction entries found. Register an order or purchase to start.'}
            </div>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left' }}>
              <thead>
                <tr style={{ borderBottom: `2px solid ${theme.colors.border}`, color: theme.colors.textSecondary, fontSize: '12px', fontWeight: 700 }}>
                  <th style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>{isFa ? 'شماره سند' : 'Entry #'}</th>
                  <th style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>{isFa ? 'دسته‌بندی' : 'Category'}</th>
                  <th style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>{isFa ? 'شرح حساب' : 'Description'}</th>
                  <th style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>{isFa ? 'نوع حساب' : 'Type'}</th>
                  <th style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>{isFa ? 'مبلغ' : 'Amount'}</th>
                </tr>
              </thead>
              <tbody>
                {entries.slice(0, 5).map((e) => (
                  <tr key={e.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                    <td style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}`, fontFamily: 'monospace', fontWeight: 700, color: theme.colors.primary }}>
                      {e.entryNumber}
                    </td>
                    <td style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>
                      <StatusBadge label={e.category} variant="neutral" />
                    </td>
                    <td style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}`, color: theme.colors.textPrimary, fontWeight: 500 }}>
                      {e.description}
                    </td>
                    <td style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}` }}>
                      <StatusBadge label={e.type === 'CREDIT' ? (isFa ? 'بستانکار' : 'CREDIT') : (isFa ? 'بدهکار' : 'DEBIT')} variant={e.type === 'CREDIT' ? 'success' : 'error'} />
                    </td>
                    <td style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}`, fontWeight: 700, color: theme.colors.textPrimary }}>
                      {isFa ? `${e.amount.toLocaleString('fa-IR')} تومان` : `$${e.amount.toFixed(2)}`}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
};
