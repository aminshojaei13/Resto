import React from 'react';
import { useTheme } from '../theme/ThemeContext';

interface ActivationPageProps {
  language?: 'fa' | 'en';
  navigate: (path: string) => void;
}

export const ActivationPage: React.FC<ActivationPageProps> = ({
  language = 'fa',
  navigate,
}) => {
  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  return (
    <div style={{ maxWidth: '640px', margin: '40px auto', fontFamily: theme.typography.fontFamily }}>
      <div
        style={{
          backgroundColor: theme.colors.surface,
          borderRadius: theme.borderRadius.xl,
          border: `1px solid ${theme.colors.border}`,
          padding: theme.spacing['2xl'],
          boxShadow: theme.shadows.md,
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: theme.spacing['2xl'] }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: theme.borderRadius.xl,
              backgroundColor: theme.colors.primaryLight,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: theme.colors.primaryDark,
              fontSize: '28px',
              margin: '0 auto 16px auto',
            }}
          >
            📲
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: theme.colors.textPrimary, margin: '0 0 8px 0' }}>
            {isFa ? 'راهنمای فعال‌سازی کسب‌وکار' : 'Business activation guide'}
          </h2>
          <p style={{ color: theme.colors.textSecondary, fontSize: '14px', margin: 0 }}>
            {isFa ? 'مراحل تایید درخواست و فعال‌سازی حساب کاربری شما' : 'How your request is reviewed and your account is activated'}
          </p>
        </div>

        {/* Status Callout */}
        <div
          style={{
            backgroundColor: isDark ? theme.colors.infoLight : '#EFF6FF',
            border: isDark ? `1px solid ${theme.colors.borderStrong}` : '1px solid #BFDBFE',
            borderRadius: theme.borderRadius.lg,
            padding: theme.spacing.lg,
            marginBottom: theme.spacing.xl,
          }}
        >
          <div style={{ fontWeight: 700, color: isDark ? theme.colors.info : '#1E40AF', fontSize: '14px', marginBottom: '6px' }}>
            📌 {isFa ? 'نحوه فعال‌سازی حساب' : 'How your account is activated'}
          </div>
          <p style={{ margin: 0, color: isDark ? theme.colors.textSecondary : '#1E3A8A', fontSize: '13px', lineHeight: 1.6 }}>
            {isFa
              ? 'پس از تایید درخواست، یک لینک فعال‌سازی برای ایمیل ثبت‌شده شما ارسال می‌شود. با باز کردن این لینک، خودتان رمز عبورتان را می‌سازید. هیچ رمز عبوری برای شما تعیین نمی‌شود.'
              : 'Once your request is approved, an activation link is emailed to the address you registered. Open it and choose your own password — no password is ever set on your behalf.'}
          </p>
        </div>

        {/* Flow Description */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg, marginBottom: theme.spacing['2xl'] }}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, color: theme.colors.textPrimary, margin: 0 }}>
            {isFa ? 'مراحل دریافت دسترسی:' : 'Steps to get access:'}
          </h3>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>1.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'ثبت درخواست کسب‌وکار' : 'Submit your business request'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'فرم ثبت‌نام آنلاین در صفحه /register را تکمیل کنید.' : 'Complete the registration form on the start page.'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>2.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'بررسی درخواست' : 'Request review'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'درخواست شما ثبت می‌شود و توسط مدیریت سامانه بررسی می‌گردد.' : 'Your request is registered and reviewed by the system administrator.'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>3.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'راه‌اندازی کسب‌وکار' : 'Setting up the business'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa
                  ? 'پس از تایید، کسب‌وکار، فروشگاه، انبار و حساب شما ساخته می‌شود.'
                  : 'On approval, your business, store, warehouse and account are created.'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>4.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'ورود به حساب کاربری' : 'Sign in'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa
                  ? 'پس از باز کردن لینک فعال‌سازی و تعیین رمز عبور، با ایمیل ثبت‌شده خود وارد شوید.'
                  : 'After opening the activation link and choosing a password, sign in with the email you registered.'}
              </div>
            </div>
          </div>
        </div>

        {/* Primary Action Button */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
          <button
            onClick={() => navigate('/login')}
            style={{
              width: '100%',
              backgroundColor: theme.colors.primary,
              color: '#FFF',
              border: 'none',
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.lg,
              fontSize: '16px',
              fontWeight: 700,
              cursor: 'pointer',
            }}
          >
            🔑 {isFa ? 'انتقال به صفحه ورود' : 'Go to sign in'}
          </button>

          <button
            onClick={() => navigate('/register')}
            style={{
              width: '100%',
              backgroundColor: theme.colors.background,
              color: theme.colors.textPrimary,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.md,
              fontSize: '14px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            🚀 {isFa ? 'ثبت درخواست کسب‌وکار جدید' : 'Submit a new business request'}
          </button>
        </div>
      </div>
    </div>
  );
};
