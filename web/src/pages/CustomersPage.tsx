import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Customer } from '../types';

export const CustomersPage: React.FC = () => {
  const [customers, setCustomers] = useState<Customer[]>([]);

  useEffect(() => {
    apiClient.getCustomers().then(setCustomers);
  }, []);

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <h2 style={{ margin: '0 0 16px 0' }}>Customer Profiles & CRM</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
        {customers.map((c) => (
          <div key={c.id} style={{ backgroundColor: '#FFF', border: '1px solid #E0E0E0', borderRadius: '12px', padding: '20px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
            <h3 style={{ margin: '0 0 4px 0', fontSize: '18px', fontWeight: 'bold' }}>{c.name}</h3>
            <p style={{ margin: '0 0 12px 0', color: '#666', fontSize: '13px' }}>{c.phone} • {c.email}</p>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #EEE', paddingTop: '12px' }}>
              <div>
                <div style={{ fontSize: '11px', color: '#888' }}>Total Purchases</div>
                <div style={{ fontSize: '16px', fontWeight: 'bold' }}>${c.totalPurchases.toFixed(2)}</div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '11px', color: '#888' }}>Loyalty Points</div>
                <div style={{ fontSize: '16px', fontWeight: 'bold', color: '#005AC1' }}>★ {c.loyaltyPoints} pts</div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
