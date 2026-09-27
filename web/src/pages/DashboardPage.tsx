import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { AccountingSummary, LedgerEntry } from '../types';
import { StatCard } from '../components/StatCard';
import { PageHeader } from '../components/PageHeader';
import { StatusBadge } from '../components/StatusBadge';
import { theme } from '../theme/tokens';

interface DashboardPageProps {
  language?: 'fa' | 'en';
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ language = 'fa' }) => {
  const [summary, setSummary] = useState<AccountingSummary | null>(null);
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const isFa = language === 'fa';

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
    };
    fetchData();
  }, []);

  const totalRevenue = summary?.totalRevenue || 0;
  const totalExpenses = summary?.totalExpenses || 0;
  const netProfit = summary?.netProfit ?? (totalRevenue - totalExpenses);
  const totalSalesCount = summary?.totalSalesCount || 0;

  return (
    <div>
      {/* Top Header Controls */}
      <PageHeader
        title={isFa ? 'داشبورد مدیریتی و هوش کسب‌وکار' : 'Executive Business Dashboard'}
        description={isFa ? 'نمای کلی از وضعیت فروش، خرید، سود عملیاتی، بدهکاران و بستانکاران' : 'Comprehensive performance, revenue, margins, and operational metrics'}
        actions={
          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <select
              style={{
                backgroundColor: theme.colors.surface,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: '8px 16px',
                fontSize: '13px',
                fontWeight: 600,
                color: theme.colors.textPrimary,
                cursor: 'pointer',
              }}
            >
              <option>{isFa ? '📅 ۳۰ روز گذشته' : '📅 Last 30 Days'}</option>
              <option>{isFa ? '📅 ۷ روز گذشته' : '📅 Last 7 Days'}</option>
              <option>{isFa ? '📅 ماه جاری' : '📅 This Month'}</option>
            </select>
            <button
              style={{
                backgroundColor: theme.colors.primary,
                color: '#FFF',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                padding: '8px 16px',
                fontSize: '13px',
                fontWeight: 600,
                cursor: 'pointer',
              }}
            >
              {isFa ? '⬇️ خروجی گزارش' : '⬇️ Export Report'}
            </button>
          </div>
        }
      />

      {/* 8 Core KPI Cards Grid */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: theme.spacing.xl,
          marginBottom: theme.spacing['3xl'],
        }}
      >
        <StatCard
          title={isFa ? 'کل فروش' : 'Sales Revenue'}
          value={isFa ? `${totalRevenue.toLocaleString('fa-IR')} تومان` : `$${totalRevenue.toFixed(2)}`}
          subtitle={isFa ? 'میزان فروش کل' : 'Total Revenue'}
          trend="0%"
          isPositive={true}
          icon="💳"
          badgeText={isFa ? 'فروش کل' : 'Total'}
          badgeColor="info"
        />

        <StatCard
          title={isFa ? 'خریدهای تامین' : 'Purchases & POs'}
          value={isFa ? '۰ تومان' : '$0.00'}
          subtitle={isFa ? 'تامین کالا' : '0 purchase orders'}
          trend="0%"
          isPositive={true}
          icon="🛍️"
          badgeText={isFa ? 'خرید' : 'Purchases'}
          badgeColor="warning"
        />

        <StatCard
          title={isFa ? 'سود ناخالص' : 'Gross Profit'}
          value={isFa ? `${totalRevenue.toLocaleString('fa-IR')} تومان` : `$${totalRevenue.toFixed(2)}`}
          subtitle={isFa ? 'حاشیه سود' : 'Margin'}
          trend="0%"
          isPositive={true}
          icon="📈"
          badgeText={isFa ? 'ناخالص' : 'Gross'}
          badgeColor="success"
        />

        <StatCard
          title={isFa ? 'سود خالص عملیاتی' : 'Net Profit'}
          value={isFa ? `${netProfit.toLocaleString('fa-IR')} تومان` : `$${netProfit.toFixed(2)}`}
          subtitle={isFa ? 'منهای هزینه‌ها' : 'After Expenses'}
          trend="0%"
          isPositive={true}
          icon="⚖️"
          badgeText={isFa ? 'خالص' : 'Net'}
          badgeColor="success"
        />

        <StatCard
          title={isFa ? 'تعداد سفارشات' : 'Total Orders'}
          value={isFa ? `${totalSalesCount.toLocaleString('fa-IR')} سفارش` : `${totalSalesCount} orders`}
          subtitle={isFa ? 'تعداد کل سفارش‌های ثبت شده' : 'Total registered orders'}
          trend="0%"
          isPositive={true}
          icon="🧾"
          badgeText={isFa ? 'سفارشات' : 'Orders'}
          badgeColor="info"
        />

        <StatCard
          title={isFa ? 'ارزش موجودی انبار' : 'Inventory Value'}
          value={isFa ? '۰ تومان' : '$0.00'}
          subtitle={isFa ? 'موجودی کل انبارها' : 'Total stock value'}
          icon="📦"
          badgeText={isFa ? 'انبار' : 'Stock'}
          badgeColor="info"
        />

        <StatCard
          title={isFa ? 'مطالبات مشتریان (AR)' : 'Receivables (AR)'}
          value={isFa ? '۰ تومان' : '$0.00'}
          subtitle={isFa ? 'مشتریان بدهکار' : 'Overdue accounts'}
          trend="0%"
          isPositive={true}
          icon="👥"
          badgeText={isFa ? 'بدهکاران' : 'AR'}
          badgeColor="warning"
        />

        <StatCard
          title={isFa ? 'بدهی به تامین‌کنندگان (AP)' : 'Payables (AP)'}
          value={isFa ? '۰ تومان' : '$0.00'}
          subtitle={isFa ? 'فاکتورهای معوق تامین‌کننده' : 'Pending bills'}
          icon="🏢"
          badgeText={isFa ? 'بستانکاران' : 'AP'}
          badgeColor="error"
        />
      </div>

      {/* Main Analytics Cards Layout */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: theme.spacing['2xl'],
          marginBottom: theme.spacing['3xl'],
        }}
      >
        {/* Sales Overview Card */}
        <div
          style={{
            backgroundColor: theme.colors.surface,
            borderRadius: theme.borderRadius.xl,
            padding: theme.spacing['2xl'],
            border: `1px solid ${theme.colors.border}`,
            boxShadow: theme.shadows.card,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.lg }}>
            <h3 style={{ fontSize: '16px', fontWeight: 700, color: theme.colors.textPrimary, margin: 0 }}>
              {isFa ? 'روند فروش و کانال‌های درآمدی' : 'Sales Channel Performance'}
            </h3>
            <StatusBadge label={isFa ? 'آنلاین / حضوری' : 'Omnichannel'} variant="info" />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
            {[
              { channel: isFa ? 'فروشگاه حضوری (POS)' : 'Physical POS Store', share: '0%', amount: isFa ? '۰ تومان' : '$0.00', color: theme.colors.primary },
              { channel: isFa ? 'وب‌سایت آنلاین' : 'Online Website', share: '0%', amount: isFa ? '۰ تومان' : '$0.00', color: theme.colors.info },
              { channel: isFa ? 'سفارش دستی / اینستاگرام' : 'Manual / Instagram', share: '0%', amount: isFa ? '۰ تومان' : '$0.00', color: theme.colors.success },
            ].map((c, i) => (
              <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xs }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                  <span style={{ fontWeight: 600, color: theme.colors.textPrimary }}>{c.channel}</span>
                  <span style={{ fontWeight: 700, color: theme.colors.textPrimary }}>{c.amount} ({c.share})</span>
                </div>
                <div style={{ width: '100%', height: '8px', backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.full, overflow: 'hidden' }}>
                  <div style={{ width: c.share, height: '100%', backgroundColor: c.color, borderRadius: theme.borderRadius.full }} />
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Expenses & Cost Structure Card */}
        <div
          style={{
            backgroundColor: theme.colors.surface,
            borderRadius: theme.borderRadius.xl,
            padding: theme.spacing['2xl'],
            border: `1px solid ${theme.colors.border}`,
            boxShadow: theme.shadows.card,
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.lg }}>
            <h3 style={{ fontSize: '16px', fontWeight: 700, color: theme.colors.textPrimary, margin: 0 }}>
              {isFa ? 'ساختار هزینه‌های جاری' : 'Operating Expenses'}
            </h3>
            <StatusBadge label={isFa ? 'هزینه‌ها' : 'Expenses'} variant="warning" />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
            {totalExpenses === 0 ? (
              <div style={{ textAlign: 'center', padding: theme.spacing.xl, color: theme.colors.textSecondary, fontSize: '13px' }}>
                {isFa ? 'هیچ هزینه‌ای ثبت نشده است' : 'No operating expenses recorded.'}
              </div>
            ) : (
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: theme.spacing.md, backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.lg }}>
                <span style={{ fontSize: '13px', fontWeight: 600, color: theme.colors.textPrimary }}>{isFa ? 'مجموع هزینه‌ها' : 'Total Expenses'}</span>
                <span style={{ fontSize: '13px', fontWeight: 700, color: theme.colors.error }}>
                  {isFa ? `${totalExpenses.toLocaleString('fa-IR')} تومان` : `$${totalExpenses.toFixed(2)}`}
                </span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* General Ledger Recent Activity Log Card */}
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
              {isFa ? 'آخرین اسناد دفتر کل و حسابداری' : 'General Ledger Activity Stream'}
            </h3>
            <p style={{ fontSize: '13px', color: theme.colors.textSecondary, margin: `${theme.spacing.xs} 0 0 0` }}>
              {isFa ? 'اسناد دوطرفه ثبت‌شده توسط سیستم و کاربران' : 'Real-time double-entry posting transactions'}
            </p>
          </div>
          <StatusBadge label={isFa ? 'تراکنش‌های تاییدشده' : 'Posted Ledger'} variant="success" />
        </div>

        <div style={{ overflowX: 'auto' }}>
          {entries.length === 0 ? (
            <div style={{ textAlign: 'center', padding: theme.spacing['2xl'], color: theme.colors.textSecondary, fontSize: '14px' }}>
              {isFa ? 'هیچ سند حسابداری ثبت نشده است' : 'No general ledger entries found.'}
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
                {entries.map((e) => (
                  <tr key={e.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                    <td style={{ padding: `${theme.spacing.md} ${theme.spacing.lg}`, fontFamily: 'monospace', fontWeight: 700, color: theme.colors.primaryDark }}>
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
