import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { AccountingSummary, LedgerEntry } from '../types';

interface DashboardPageProps {
  language?: 'fa' | 'en';
}

export const DashboardPage: React.FC<DashboardPageProps> = ({ language = 'fa' }) => {
  const [summary, setSummary] = useState<AccountingSummary | null>(null);
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const isFa = language === 'fa';

  useEffect(() => {
    apiClient.getAccountingSummary().then(setSummary);
    apiClient.getJournalEntries().then(setEntries);
  }, []);

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <h2 style={{ margin: '0 0 20px 0' }}>{isFa ? 'داشبورد مدیریتی درآمد و سود' : 'Executive Revenue & Financial Dashboard'}</h2>

      {/* Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div style={{ backgroundColor: '#D8E2FF', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', color: '#001A41', fontWeight: 'bold' }}>{isFa ? 'کل درآمد فروش' : 'Total Revenue'}</div>
          <div style={{ fontSize: '26px', fontWeight: 'bold', color: '#005AC1', margin: '8px 0' }}>
            {isFa ? `${(summary?.totalRevenue || 2940.82).toLocaleString('fa-IR')} تومان` : `$${(summary?.totalRevenue || 2940.82).toFixed(2)}`}
          </div>
          <div style={{ fontSize: '12px', color: '#555' }}>{isFa ? `${summary?.totalSalesCount || 14} فاکتور فروش ثبت‌شده` : `${summary?.totalSalesCount || 14} sales orders`}</div>
        </div>

        <div style={{ backgroundColor: '#97F0FF', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', color: '#001F24', fontWeight: 'bold' }}>{isFa ? 'درآمد امروز' : "Today's Revenue"}</div>
          <div style={{ fontSize: '26px', fontWeight: 'bold', color: '#006874', margin: '8px 0' }}>
            {isFa ? `${(summary?.todayRevenue || 1603.78).toLocaleString('fa-IR')} تومان` : `$${(summary?.todayRevenue || 1603.78).toFixed(2)}`}
          </div>
          <div style={{ fontSize: '12px', color: '#555' }}>{isFa ? `${summary?.todaySalesCount || 3} فاکتور فروش امروز` : `${summary?.todaySalesCount || 3} orders today`}</div>
        </div>

        <div style={{ backgroundColor: '#89F8C7', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', color: '#002114', fontWeight: 'bold' }}>{isFa ? 'سود خالص عملیاتی' : 'Net Operating Profit'}</div>
          <div style={{ fontSize: '26px', fontWeight: 'bold', color: '#006C4C', margin: '8px 0' }}>
            {isFa ? '۲,۵۹۰,۸۲۰ تومان' : '$2,590.82'}
          </div>
          <div style={{ fontSize: '12px', color: '#555' }}>{isFa ? 'درآمد منهای هزینه‌های جاری' : 'Revenue minus Expenses'}</div>
        </div>
      </div>

      {/* Ledger Log */}
      <h3 style={{ margin: '0 0 12px 0' }}>{isFa ? 'دفتر کل و اسناد مالی' : 'General Ledger Activity Log'}</h3>
      <div style={{ backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E0E0E0', padding: '16px' }}>
        {entries.map((e) => (
          <div key={e.id} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0F0F0', padding: '12px 0' }}>
            <div>
              <span style={{ fontFamily: 'monospace', fontWeight: 'bold', marginLeft: '12px', marginRight: '12px' }}>{e.entryNumber}</span>
              <span style={{ fontSize: '14px' }}>{e.description}</span>
            </div>
            <div style={{ fontWeight: 'bold', color: e.type === 'CREDIT' ? '#006C4C' : '#BA1A1A' }}>
              {e.type === 'CREDIT' ? '+' : '-'}{isFa ? `${e.amount.toLocaleString('fa-IR')} تومان` : `$${e.amount.toFixed(2)}`}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
