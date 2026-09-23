import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Customer } from '../types';

interface CustomersPageProps {
  language?: 'fa' | 'en';
}

export const CustomersPage: React.FC<CustomersPageProps> = ({ language = 'fa' }) => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const isFa = language === 'fa';

  useEffect(() => {
    apiClient.getCustomers().then(setCustomers);
  }, []);

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <h2 style={{ margin: '0 0 16px 0' }}>{isFa ? 'مدیریت مشتریان و باشگاه مشتریان' : 'Customer Profiles & CRM'}</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '16px' }}>
        {customers.map((c) => (
          <div key={c.id} style={{ backgroundColor: '#FFF', border: '1px solid #E0E0E0', borderRadius: '12px', padding: '20px', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
            <h3 style={{ margin: '0 0 4px 0', fontSize: '18px', fontWeight: 'bold' }}>{c.name}</h3>
            <p style={{ margin: '0 0 12px 0', color: '#666', fontSize: '13px' }}>{c.phone} • {c.email}</p>
            <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: '1px solid #EEE', paddingTop: '12px' }}>
              <div>
                <div style={{ fontSize: '11px', color: '#888' }}>{isFa ? 'مجموع خرید' : 'Total Purchases'}</div>
                <div style={{ fontSize: '15px', fontWeight: 'bold' }}>
                  {isFa ? `${c.totalPurchases.toLocaleString('fa-IR')} تومان` : `$${c.totalPurchases.toFixed(2)}`}
                </div>
              </div>
              <div style={{ textAlign: isFa ? 'left' : 'right' }}>
                <div style={{ fontSize: '11px', color: '#888' }}>{isFa ? 'امتیاز باشگاه' : 'Loyalty Points'}</div>
                <div style={{ fontSize: '15px', fontWeight: 'bold', color: '#005AC1' }}>
                  ★ {isFa ? c.loyaltyPoints.toLocaleString('fa-IR') : c.loyaltyPoints} {isFa ? 'امتیاز' : 'pts'}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
