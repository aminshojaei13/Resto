import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { AccountingSummary, LedgerEntry } from '../types';

export const DashboardPage: React.FC = () => {
  const [summary, setSummary] = useState<AccountingSummary | null>(null);
  const [entries, setEntries] = useState<LedgerEntry[]>([]);

  useEffect(() => {
    apiClient.getAccountingSummary().then(setSummary);
    apiClient.getJournalEntries().then(setEntries);
  }, []);

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <h2 style={{ margin: '0 0 20px 0' }}>Executive Revenue & Financial Dashboard</h2>

      {/* Metric Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div style={{ backgroundColor: '#D8E2FF', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', color: '#001A41', fontWeight: 'bold' }}>Total Revenue</div>
          <div style={{ fontSize: '28px', fontWeight: 'bold', color: '#005AC1', margin: '8px 0' }}>${summary?.totalRevenue.toFixed(2) || '2,940.82'}</div>
          <div style={{ fontSize: '12px', color: '#555' }}>{summary?.totalSalesCount || 14} sales orders</div>
        </div>

        <div style={{ backgroundColor: '#97F0FF', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', color: '#001F24', fontWeight: 'bold' }}>Today's Revenue</div>
          <div style={{ fontSize: '28px', fontWeight: 'bold', color: '#006874', margin: '8px 0' }}>${summary?.todayRevenue.toFixed(2) || '1,603.78'}</div>
          <div style={{ fontSize: '12px', color: '#555' }}>{summary?.todaySalesCount || 3} orders today</div>
        </div>

        <div style={{ backgroundColor: '#89F8C7', borderRadius: '12px', padding: '20px' }}>
          <div style={{ fontSize: '13px', color: '#002114', fontWeight: 'bold' }}>Net Operating Profit</div>
          <div style={{ fontSize: '28px', fontWeight: 'bold', color: '#006C4C', margin: '8px 0' }}>$2,590.82</div>
          <div style={{ fontSize: '12px', color: '#555' }}>Revenue minus Expenses</div>
        </div>
      </div>

      {/* Ledger Log */}
      <h3 style={{ margin: '0 0 12px 0' }}>General Ledger Activity Log</h3>
      <div style={{ backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E0E0E0', padding: '16px' }}>
        {entries.map((e) => (
          <div key={e.id} style={{ display: 'flex', justifyContent: 'space-between', borderBottom: '1px solid #F0F0F0', padding: '12px 0' }}>
            <div>
              <span style={{ fontFamily: 'monospace', fontWeight: 'bold', marginRight: '12px' }}>{e.entryNumber}</span>
              <span style={{ fontSize: '14px' }}>{e.description}</span>
            </div>
            <div style={{ fontWeight: 'bold', color: e.type === 'CREDIT' ? '#006C4C' : '#BA1A1A' }}>
              {e.type === 'CREDIT' ? '+' : '-'}${e.amount.toFixed(2)}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
