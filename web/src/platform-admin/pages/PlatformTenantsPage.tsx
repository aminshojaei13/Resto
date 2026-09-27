import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/apiClient';
import { Organization } from '../../types';

interface PlatformTenantsPageProps {
  language?: 'fa' | 'en';
}

export const PlatformTenantsPage: React.FC<PlatformTenantsPageProps> = ({ language = 'fa' }) => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const isFa = language === 'fa';

  useEffect(() => {
    loadOrganizations();
  }, []);

  const loadOrganizations = async () => {
    setIsLoading(true);
    try {
      const list = await apiClient.getOrganizations();
      setOrganizations(list || []);
      setIsLoading(false);
    } catch {
      setOrganizations([]);
      setIsLoading(false);
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold' }}>
          {isFa ? 'مدیریت تننت‌ها و سازمان‌های فعال (Platform Tenants)' : 'Provisioned Tenant Organizations'}
        </h2>
        <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>
          {isFa ? 'فهرست تمامی سازمان‌ها، شعب فروشگاه و انبارهای راه‌اندازی‌شده در پلتفرم ابری رستو' : 'Overview of all active tenants, stores, and warehouses provisioned on Resto SaaS'}
        </p>
      </div>

      <div style={{ backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E0E0E0', overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>{isFa ? 'در حال بارگذاری سازمان‌ها...' : 'Loading tenants...'}</div>
        ) : organizations.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>{isFa ? 'هیچ سازمانی یافت نشد.' : 'No active organizations found.'}</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: '#F5F5F5', textAlign: isFa ? 'right' : 'left' }}>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'کد سازمان' : 'Tenant Code'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نام سازمان' : 'Organization Name'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'سطح اشتراک' : 'Subscription Tier'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'واحد پول' : 'Currency'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'تعداد شعب' : 'Stores Count'}</th>
              </tr>
            </thead>
            <tbody>
              {organizations.map((org) => (
                <tr key={org.id} style={{ borderBottom: '1px solid #EEE' }}>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontWeight: 'bold', color: '#005AC1' }}>{org.code}</td>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{org.name}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span
                      style={{
                        padding: '4px 10px',
                        borderRadius: '12px',
                        fontSize: '12px',
                        fontWeight: 'bold',
                        backgroundColor: '#E0F2FE',
                        color: '#0369A1',
                      }}
                    >
                      {org.subscriptionTier || 'ENTERPRISE'}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px' }}>{org.currencyCode} ({org.currencySymbol})</td>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{(org.stores || []).length} {isFa ? 'شعبه' : 'store(s)'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
