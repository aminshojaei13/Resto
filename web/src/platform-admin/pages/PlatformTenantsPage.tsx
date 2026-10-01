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
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [orgName, setOrgName] = useState('');
  const [orgCode, setOrgCode] = useState('');
  const [currencySymbol, setCurrencySymbol] = useState('$');
  const [currencyCode, setCurrencyCode] = useState('USD');
  const [isSubmitting, setIsSubmitting] = useState(false);

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

  const handleCreateOrganization = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!orgName.trim()) return;

    setIsSubmitting(true);
    try {
      await apiClient.createOrganization({
        name: orgName.trim(),
        code: orgCode.trim() || undefined,
        currency_symbol: currencySymbol,
        currency_code: currencyCode,
      });

      setOrgName('');
      setOrgCode('');
      setIsModalOpen(false);
      setIsSubmitting(false);
      loadOrganizations();
    } catch (err: any) {
      alert(err.message || 'Error creating organization');
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
            {isFa ? 'مدیریت تننت‌ها و سازمان‌های فعال (Platform Tenants)' : 'Provisioned Tenant Organizations'}
          </h2>
          <p style={{ margin: 0, color: theme.colors.textSecondary, fontSize: '14px' }}>
            {isFa ? 'فهرست تمامی سازمان‌ها، شعب فروشگاه و انبارهای راه‌اندازی‌شده در پلتفرم ابری رستو' : 'Overview of all active tenants, stores, and warehouses provisioned on Resto SaaS'}
          </p>
        </div>

        <button
          onClick={() => setIsModalOpen(true)}
          style={{
            backgroundColor: theme.colors.primary,
            color: theme.colors.primaryTextOnBrand,
            border: 'none',
            borderRadius: '8px',
            padding: '10px 18px',
            fontSize: '14px',
            fontWeight: 'bold',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
          }}
        >
          ➕ {isFa ? 'ثبت سازمان جدید' : 'New Organization'}
        </button>
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

      {/* New Organization Modal */}
      {isModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, padding: '28px', borderRadius: '16px', width: '480px', maxWidth: '92%', color: theme.colors.textPrimary, border: `1px solid ${theme.colors.border}` }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              🏢 {isFa ? 'ایجاد مستقیم سازمان و شعب' : 'Create Organization'}
            </h3>

            <form onSubmit={handleCreateOrganization} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              <div>
                <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
                  {isFa ? 'نام سازمان / برند:' : 'Organization Name:'} *
                </label>
                <input
                  type="text"
                  required
                  value={orgName}
                  onChange={(e) => setOrgName(e.target.value)}
                  placeholder={isFa ? 'مثال: هلدینگ پارس / هایپرمارکت ملت' : 'e.g. Grand Supermarket'}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surface, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
                  {isFa ? 'کد اختصاصی سازمان (اختیاری):' : 'Organization Code (Optional):'}
                </label>
                <input
                  type="text"
                  value={orgCode}
                  onChange={(e) => setOrgCode(e.target.value)}
                  placeholder="ORG999"
                  style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surface, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
                    {isFa ? 'نماد واحد پول:' : 'Currency Symbol:'}
                  </label>
                  <input
                    type="text"
                    value={currencySymbol}
                    onChange={(e) => setCurrencySymbol(e.target.value)}
                    placeholder="تومان / $"
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surface, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
                    {isFa ? 'کد ارز:' : 'Currency Code:'}
                  </label>
                  <input
                    type="text"
                    value={currencyCode}
                    onChange={(e) => setCurrencyCode(e.target.value)}
                    placeholder="IRT / USD"
                    style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surface, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '12px' }}>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '8px', padding: '10px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '10px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ثبت و ایجاد' : 'Create Organization')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
