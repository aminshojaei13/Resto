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
            {isFa ? 'راهنمای فعال‌سازی و ورود سازمان' : 'Organization Activation & Access Guide'}
          </h2>
          <p style={{ color: theme.colors.textSecondary, fontSize: '14px', margin: 0 }}>
            {isFa ? 'اطلاعات مربوط به روند تایید و فعال‌سازی حساب کاربری در Resto SaaS' : 'Information regarding tenant provisioning and owner credential setup'}
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
            📌 {isFa ? 'وضعیت مکانیزم کدهای فعال‌سازی (Activation Code Status):' : 'Activation Code Mechanism Status:'}
          </div>
          <div style={{ fontFamily: 'monospace', fontWeight: 700, color: isDark ? theme.colors.primary : '#1D4ED8', fontSize: '13px', marginBottom: '8px' }}>
            ACTIVATION_CODE_STATUS: NOT_IMPLEMENTED
          </div>
          <p style={{ margin: 0, color: isDark ? theme.colors.textSecondary : '#1E3A8A', fontSize: '13px', lineHeight: 1.6 }}>
            {isFa
              ? 'در نسخه فعلی پلتفرم ابری رستو، نیازی به وارد کردن کد دعوت یا کد فعال‌سازی عددی نیست. فعال‌سازی سازمان‌ها مستقیماً پس از بررسی و تایید راهبر پلتفرم انجام می‌پذیرد.'
              : 'In the current version of Resto SaaS, explicit invitation/activation codes are not used. Tenant workspaces are activated directly upon review and approval by the Platform Administrator.'}
          </p>
        </div>

        {/* Flow Description */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg, marginBottom: theme.spacing['2xl'] }}>
          <h3 style={{ fontSize: '16px', fontWeight: 700, color: theme.colors.textPrimary, margin: 0 }}>
            {isFa ? 'مراحل دریافت دسترسی سازمان:' : 'Steps to Access Your Tenant Dashboard:'}
          </h3>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>1.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'ثبت درخواست اولیه سازمان' : 'Submit Business Application'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'فرم ثبت‌نام آنلاین در صفحه /register را تکمیل کنید.' : 'Complete the online registration form at /register.'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>2.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'بررسی توسط راهبر پلتفرم (Platform Admin)' : 'Review by Platform Admin'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'درخواست شما در وضعیت PENDING قرار گرفته و بررسی می‌شود.' : 'Your application enters PENDING status for administrative review.'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>3.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'راه‌اندازی اتوماتیک تننت و حساب مالک' : 'Automatic Provisioning'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa
                  ? 'پس از تایید، تننت سازمان، انبار، فروشگاه و حساب مالک با ایمیل ثبت‌شده ایجاد می‌شود.'
                  : 'Upon approval, your tenant workspace, stores, warehouses, and owner account are provisioned.'}
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: theme.spacing.md }}>
            <div style={{ fontWeight: 800, color: theme.colors.primary, fontSize: '18px' }}>4.</div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {isFa ? 'ورود مستقیم به داشبورد' : 'Login & Access'}
              </div>
              <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa
                  ? 'با استفاده از ایمیل ثبت‌شده خود در صفحه ورود (/login) وارد شوید.'
                  : 'Log in using your registered email at /login to manage your business.'}
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
            🔑 {isFa ? 'انتقال به صفحه ورود به حساب' : 'Proceed to Tenant Login'}
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
            🚀 {isFa ? 'ثبت درخواست سازمان جدید' : 'Submit New Business Application'}
          </button>
        </div>
      </div>
    </div>
  );
};
