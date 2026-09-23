import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { LedgerEntry } from '../types';

export const AccountingPage: React.FC = () => {
  const [entries, setEntries] = useState<LedgerEntry[]>([]);

  useEffect(() => {
    apiClient.getJournalEntries().then(setEntries);
  }, []);

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <h2 style={{ margin: '0 0 16px 0' }}>Double-Entry General Ledger</h2>

      <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#FFF', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
        <thead>
          <tr style={{ backgroundColor: '#F5F5F5', textAlign: 'left', borderBottom: '2px solid #DDD' }}>
            <th style={{ padding: '12px 16px' }}>Entry #</th>
            <th style={{ padding: '12px 16px' }}>Category</th>
            <th style={{ padding: '12px 16px' }}>Description</th>
            <th style={{ padding: '12px 16px' }}>Type</th>
            <th style={{ padding: '12px 16px' }}>Amount</th>
          </tr>
        </thead>
        <tbody>
          {entries.map((e) => (
            <tr key={e.id} style={{ borderBottom: '1px solid #EEE' }}>
              <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontWeight: 'bold' }}>{e.entryNumber}</td>
              <td style={{ padding: '12px 16px' }}>{e.category}</td>
              <td style={{ padding: '12px 16px' }}>{e.description}</td>
              <td style={{ padding: '12px 16px', fontWeight: 'bold', color: e.type === 'CREDIT' ? '#006C4C' : '#BA1A1A' }}>{e.type}</td>
              <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>${e.amount.toFixed(2)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
