import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/apiClient';
import { Organization } from '../../types';
import { useTheme } from '../../theme/ThemeContext';

interface PlatformTenantsPageProps {
  language?: 'fa' | 'en';
}

export const PlatformTenantsPage: React.FC<PlatformTenantsPageProps> = ({ language = 'fa' }) => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const isFa = language === 'fa';
  const { theme } = useTheme();

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
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
          {isFa ? 'مدیریت تننت‌ها و سازمان‌های فعال (Platform Tenants)' : 'Provisioned Tenant Organizations'}
        </h2>
        <p style={{ margin: 0, color: theme.colors.textSecondary, fontSize: '14px' }}>
          {isFa ? 'فهرست تمامی سازمان‌ها، شعب فروشگاه و انبارهای راه‌اندازی‌شده در پلتفرم ابری رستو' : 'Overview of all active tenants, stores, and warehouses provisioned on Resto SaaS'}
        </p>
      </div>

      <div style={{ backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}`, overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری سازمان‌ها...' : 'Loading tenants...'}</div>
        ) : organizations.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: theme.colors.textMuted }}>{isFa ? 'هیچ سازمانی یافت نشد.' : 'No active organizations found.'}</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: theme.colors.backgroundSecondary, textAlign: isFa ? 'right' : 'left', color: theme.colors.textSecondary, fontSize: '12px', fontWeight: 700 }}>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'کد سازمان' : 'Tenant Code'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نام سازمان' : 'Organization Name'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'سطح اشتراک' : 'Subscription Tier'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'واحد پول' : 'Currency'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'تعداد شعب' : 'Stores Count'}</th>
              </tr>
            </thead>
            <tbody>
              {organizations.map((org) => (
                <tr key={org.id} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontWeight: 'bold', color: theme.colors.info }}>{org.code}</td>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{org.name}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span
                      style={{
                        padding: '4px 10px',
                        borderRadius: '12px',
                        fontSize: '12px',
                        fontWeight: 'bold',
                        backgroundColor: theme.colors.infoLight,
                        color: theme.colors.info,
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
