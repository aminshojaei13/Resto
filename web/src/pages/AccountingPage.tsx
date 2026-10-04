import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { LedgerEntry } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { PageHeader } from '../components/PageHeader';

interface AccountingPageProps {
  language?: 'fa' | 'en';
}

export const AccountingPage: React.FC<AccountingPageProps> = ({ language = 'fa' }) => {
  const [entries, setEntries] = useState<LedgerEntry[]>([]);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  useEffect(() => {
    apiClient.getJournalEntries().then(setEntries);
  }, []);

  return (
    <div style={{ fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <PageHeader
        title={isFa ? 'دفتر کل و اسناد دوطرفه حسابداری' : 'Double-Entry General Ledger'}
        description={isFa ? 'تمام اسناد بدهکار و بستانکار به‌صورت خودکار ثبت می‌شوند' : 'Automatic double-entry posting for every operation'}
      />

      <div
        style={{
          backgroundColor: theme.colors.surface,
          border: `1px solid ${theme.colors.border}`,
          borderRadius: theme.borderRadius.lg,
          overflow: 'hidden',
          boxShadow: theme.shadows.card,
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr
              style={{
                backgroundColor: theme.colors.backgroundSecondary,
                textAlign: isFa ? 'right' : 'left',
                borderBottom: `2px solid ${theme.colors.border}`,
                fontSize: '12px',
                fontWeight: 700,
                color: theme.colors.textSecondary,
              }}
            >
              <th style={{ padding: '12px 16px' }}>{isFa ? 'شماره سند' : 'Entry #'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'دسته‌بندی' : 'Category'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'شرح سند' : 'Description'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'نوع حساب' : 'Type'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'مبلغ' : 'Amount'}</th>
            </tr>
          </thead>
          <tbody>
            {entries.length === 0 ? (
              <tr>
                <td colSpan={5} style={{ padding: '40px', textAlign: 'center', color: theme.colors.textMuted, fontSize: '14px' }}>
                  {isFa ? 'هنوز سندی ثبت نشده است.' : 'No journal entries recorded yet.'}
                </td>
              </tr>
            ) : (
              entries.map((e) => (
                <tr key={e.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontWeight: 'bold', color: theme.colors.primary }}>
                    {e.entryNumber}
                  </td>
                  <td style={{ padding: '12px 16px' }}>{e.category}</td>
                  <td style={{ padding: '12px 16px', color: theme.colors.textPrimary }}>{e.description}</td>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold', color: e.type === 'CREDIT' ? theme.colors.success : theme.colors.error }}>
                    {isFa ? (e.type === 'CREDIT' ? 'بستانکار' : 'بدهکار') : e.type}
                  </td>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
                    {isFa ? `${e.amount.toLocaleString('fa-IR')} تومان` : `$${e.amount.toFixed(2)}`}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};
