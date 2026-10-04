import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Customer } from '../types';
import { useTheme } from '../theme/ThemeContext';

interface CustomersPageProps {
  language?: 'fa' | 'en';
}

export const CustomersPage: React.FC<CustomersPageProps> = ({ language = 'fa' }) => {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [editingCustomer, setEditingCustomer] = useState<Customer | null>(null);

  // Edit form pre-populated state
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  useEffect(() => {
    loadCustomers();
  }, []);

  const loadCustomers = () => {
    apiClient.getCustomers().then(setCustomers);
  };

  const openEditModal = (c: Customer) => {
    setEditingCustomer(c);
    setName(c.name);
    setEmail(c.email || '');
    setPhone(c.phone || '');
    setAddress(c.address || '');
    setErrorMessage('');
  };

  const closeEditModal = () => {
    setEditingCustomer(null);
    setErrorMessage('');
  };

  const handleUpdateCustomer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingCustomer) return;

    if (!name.trim()) {
      setErrorMessage(isFa ? 'نام مشتری الزامی است.' : 'Customer name is required.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await apiClient.updateCustomer(editingCustomer.id, {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
        address: address.trim(),
      });

      setIsSubmitting(false);
      closeEditModal();
      alert(isFa ? 'اطلاعات مشتری با موفقیت بروزرسانی شد.' : 'Customer profile updated successfully.');
      loadCustomers();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ویرایش اطلاعات مشتری' : 'Failed to update customer profile'));
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <h2 style={{ margin: '0 0 16px 0', color: theme.colors.textPrimary }}>{isFa ? 'مدیریت مشتریان و باشگاه مشتریان' : 'Customer Profiles & CRM'}</h2>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: '16px' }}>
        {customers.map((c) => {
          const totalPurchases = Number(c.totalPurchases ?? (c as any).total_purchases ?? 0);
          const loyaltyPoints = Number(c.loyaltyPoints ?? (c as any).loyalty_points ?? 0);

          return (
            <div key={c.id} style={{ backgroundColor: theme.colors.surface, border: `1px solid ${theme.colors.border}`, borderRadius: '12px', padding: '20px', boxShadow: theme.shadows.card, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <h3 style={{ margin: '0 0 4px 0', fontSize: '18px', fontWeight: 'bold', color: theme.colors.textPrimary }}>{c.name}</h3>
                  <button
                    onClick={() => openEditModal(c)}
                    style={{ backgroundColor: theme.colors.primaryLight, color: theme.colors.primaryDark, border: 'none', borderRadius: '6px', padding: '4px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '✏️ ویرایش' : '✏️ Edit'}
                  </button>
                </div>
                <p style={{ margin: '0 0 12px 0', color: theme.colors.textSecondary, fontSize: '13px' }}>
                  {c.phone ? c.phone : (isFa ? 'بدون شماره' : 'No phone')} • {c.email ? c.email : (isFa ? 'بدون ایمیل' : 'No email')}
                </p>
                {c.address && (
                  <p style={{ margin: '0 0 12px 0', color: theme.colors.textMuted, fontSize: '12px' }}>
                    📍 {c.address}
                  </p>
                )}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', borderTop: `1px solid ${theme.colors.border}`, paddingTop: '12px', marginTop: '12px' }}>
                <div>
                  <div style={{ fontSize: '11px', color: theme.colors.textMuted }}>{isFa ? 'مجموع خرید' : 'Total Purchases'}</div>
                  <div style={{ fontSize: '15px', fontWeight: 'bold' }}>
                    {isFa ? `${totalPurchases.toLocaleString('fa-IR')} تومان` : `$${totalPurchases.toFixed(2)}`}
                  </div>
                </div>
                <div style={{ textAlign: isFa ? 'left' : 'right' }}>
                  <div style={{ fontSize: '11px', color: theme.colors.textMuted }}>{isFa ? 'امتیاز باشگاه' : 'Loyalty Points'}</div>
                  <div style={{ fontSize: '15px', fontWeight: 'bold', color: theme.colors.primary }}>
                    ★ {isFa ? loyaltyPoints.toLocaleString('fa-IR') : loyaltyPoints} {isFa ? 'امتیاز' : 'pts'}
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Edit Customer Pre-Populated Modal */}
      {editingCustomer && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? `ویرایش پرونده مشتری (${editingCustomer.name})` : `Edit Customer Profile (${editingCustomer.name})`}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleUpdateCustomer} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'نام و نام خانوادگی *' : 'Full Name *'}</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'شماره تماس' : 'Phone Number'}</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'پست الکترونیک (ایمیل)' : 'Email Address'}</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'آدرس سکونت / تحویل' : 'Physical Address'}</label>
                <textarea
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  rows={2}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={closeEditModal}
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ثبت تغییرات' : 'Save Changes')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
