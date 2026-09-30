import React, { useState } from 'react';
import { apiClient } from '../api/apiClient';
import { useTheme } from '../theme/ThemeContext';

interface PublicRegisterPageProps {
  language?: 'fa' | 'en';
  onRegistrationSubmitted?: (applicationId: string) => void;
  navigate?: (path: string) => void;
}

export const PublicRegisterPage: React.FC<PublicRegisterPageProps> = ({
  language = 'fa',
  onRegistrationSubmitted,
  navigate,
}) => {
  const [businessName, setBusinessName] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [businessType, setBusinessType] = useState('RETAIL');
  const [city, setCity] = useState('');
  const [address, setAddress] = useState('');
  const [notes, setNotes] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submittedApplication, setSubmittedApplication] = useState<any | null>(null);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const res = await apiClient.registerBusiness({
        business_name: businessName,
        owner_name: ownerName,
        email,
        phone,
        business_type: businessType,
        city,
        address,
        notes,
      });

      setIsSubmitting(false);
      setSubmittedApplication(res);
      if (onRegistrationSubmitted && res.application_id) {
        onRegistrationSubmitted(res.application_id);
      }
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ثبت درخواست آنلاین کسب‌وکار' : 'Failed to submit registration request'));
    }
  };

  return (
    <div style={{ maxWidth: '640px', margin: '40px auto', padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ textAlign: 'center', marginBottom: '32px' }}>
        <h1 style={{ color: theme.colors.primary, margin: '0 0 8px 0', fontSize: '28px', fontWeight: 'bold' }}>
          {isFa ? 'ثبت‌نام و راه‌اندازی کسب‌وکار در رستو (Resto)' : 'Register Your Business on Resto'}
        </h1>
        <p style={{ color: theme.colors.textSecondary, fontSize: '15px', margin: 0 }}>
          {isFa ? 'فرم درخواست اولیه برای ایجاد سازمان، مدیریت فروشگاه، انبار و حسابداری یکپارچه' : 'Submit your business registration request for platform approval and tenant setup'}
        </p>
      </div>

      {submittedApplication ? (
        <div style={{ backgroundColor: theme.colors.successLight, border: `1px solid ${theme.colors.success}`, padding: '32px', borderRadius: '16px', textAlign: 'center' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎉</div>
          <h2 style={{ color: theme.colors.success, margin: '0 0 12px 0' }}>
            {isFa ? 'درخواست شما با موفقیت ثبت گردید!' : 'Business Application Submitted Successfully!'}
          </h2>
          <p style={{ color: theme.colors.textSecondary, fontSize: '15px', marginBottom: '24px' }}>
            {isFa
              ? 'درخواست شما در وضعیت "در حال بررسی راهبر پلتفرم" قرار گرفت. پس از تایید، دسترسی به پنل مدیریت سازمان و حساب مالک فعال خواهد شد.'
              : 'Your application is now PENDING review by the Resto platform administrator. Once approved, tenant access will be provisioned.'}
          </p>

          <div style={{ backgroundColor: theme.colors.surfaceElevated, padding: '16px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, textAlign: isFa ? 'right' : 'left', fontSize: '14px', marginBottom: '24px' }}>
            <div><strong>{isFa ? 'شناسه پیگیری:' : 'Application ID:'}</strong> <code style={{ color: theme.colors.primary }}>{submittedApplication.application_id}</code></div>
            <div><strong>{isFa ? 'نام کسب‌وکار:' : 'Business:'}</strong> {businessName}</div>
            <div><strong>{isFa ? 'مالک:' : 'Owner:'}</strong> {ownerName} ({email})</div>
            <div><strong>{isFa ? 'وضعیت:' : 'Status:'}</strong> <span style={{ color: theme.colors.warning, fontWeight: 'bold' }}>{submittedApplication.status || 'PENDING'}</span></div>
          </div>

          <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>
            <button
              onClick={() => setSubmittedApplication(null)}
              style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '12px 20px', fontSize: '14px', fontWeight: 'bold', cursor: 'pointer' }}
            >
              {isFa ? 'ثبت درخواست جدید' : 'Submit Another Application'}
            </button>
            {navigate && (
              <button
                onClick={() => navigate('/login')}
                style={{ backgroundColor: theme.colors.success, color: '#0A2E1E', border: 'none', borderRadius: '8px', padding: '12px 20px', fontSize: '14px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                🔑 {isFa ? 'انتقال به صفحه ورود' : 'Go to Login'}
              </button>
            )}
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate style={{ backgroundColor: theme.colors.surface, border: `1px solid ${theme.colors.border}`, padding: '32px', borderRadius: '16px', boxShadow: theme.shadows.card, display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {errorMessage && (
            <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '12px 16px', borderRadius: '8px', fontSize: '14px' }}>
              {errorMessage}
            </div>
          )}

          <div>
            <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
              {isFa ? 'نام کسب‌وکار / برند:' : 'Business Name:'} *
            </label>
            <input
              type="text"
              required
              value={businessName}
              onChange={(e) => setBusinessName(e.target.value)}
              placeholder={isFa ? 'مثال: کافه رستوران گرند / فروشگاه مرکزی' : 'e.g. Grand Coffee Roasters'}
              style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
            />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
                {isFa ? 'نام و نام خانوادگی مالک:' : 'Owner Full Name:'} *
              </label>
              <input
                type="text"
                required
                value={ownerName}
                onChange={(e) => setOwnerName(e.target.value)}
                placeholder={isFa ? 'رضا علوی' : 'Reza Alavi'}
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
                {isFa ? 'نوع کسب‌وکار:' : 'Business Category:'}
              </label>
              <select
                value={businessType}
                onChange={(e) => setBusinessType(e.target.value)}
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              >
                <option value="RETAIL">{isFa ? 'فروشگاهی / خرده‌فروشی (Retail)' : 'Retail Store'}</option>
                <option value="RESTAURANT">{isFa ? 'رستوران و کافه (Restaurant)' : 'Restaurant / Cafe'}</option>
                <option value="GROCERY">{isFa ? 'سوپرمارکت و مواد غذایی' : 'Grocery / Supermarket'}</option>
                <option value="ELECTRONICS">{isFa ? 'کالای دیجیتال و الکترونیک' : 'Electronics'}</option>
                <option value="SERVICE">{isFa ? 'خدماتی / عمومی' : 'Services'}</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
                {isFa ? 'ایمیل کاری (نام کاربری ورود):' : 'Work Email:'} *
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="reza@grandcoffee.com"
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
                {isFa ? 'شماره همراه همراه:' : 'Phone Number:'}
              </label>
              <input
                type="text"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="+1 (555) 333-4444"
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
                {isFa ? 'شهر:' : 'City:'}
              </label>
              <input
                type="text"
                value={city}
                onChange={(e) => setCity(e.target.value)}
                placeholder={isFa ? 'تهران' : 'Tehran'}
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 'bold', marginBottom: '6px', fontSize: '14px' }}>
                {isFa ? 'آدرس فروشگاه / دفتر مرکزی:' : 'Address:'}
              </label>
              <input
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                placeholder={isFa ? 'خیابان ولیعصر، نرسیده به میدان ونک' : 'Valiasr St, Tehran'}
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '14px', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer', marginTop: '12px' }}
          >
            {isSubmitting ? (isFa ? 'در حال ثبت درخواست...' : 'Submitting...') : (isFa ? '🚀 ثبت درخواست ایجاد سازمان در رستو' : '🚀 Submit Business Registration')}
          </button>
        </form>
      )}
    </div>
  );
};
