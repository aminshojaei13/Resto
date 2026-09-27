import React, { useState } from 'react';
import { theme } from '../theme/tokens';
import { AuthUser } from '../types';

interface LoginPageProps {
  language?: 'fa' | 'en';
  onLoginSuccess: (user: AuthUser) => void;
  navigate: (path: string) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({
  language = 'fa',
  onLoginSuccess,
  navigate,
}) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isFa = language === 'fa';

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setError('');

    setTimeout(() => {
      setIsSubmitting(false);
      if (!email || !password) {
        setError(isFa ? 'لطفا ایمیل و رمز عبور را وارد کنید.' : 'Please enter email and password.');
        return;
      }

      // If user logs in with admin email
      if (email.toLowerCase().includes('admin')) {
        const user: AuthUser = {
          id: 'usr_admin',
          name: 'Resto Platform Admin',
          email: email.toLowerCase(),
          role: 'PlatformAdmin',
          isPlatformAdmin: true,
        };
        onLoginSuccess(user);
        navigate('/platform/applications');
      } else {
        const user: AuthUser = {
          id: 'usr_owner_1',
          name: 'Reza Alavi',
          email: email.toLowerCase(),
          role: 'Owner',
          isPlatformAdmin: false,
          tenantId: 'org_apex',
        };
        onLoginSuccess(user);
        navigate('/app/dashboard');
      }
    }, 300);
  };

  const handleQuickTenantLogin = () => {
    const user: AuthUser = {
      id: 'usr_owner_1',
      name: 'Reza Alavi',
      email: 'reza@grandcoffee.com',
      role: 'Owner',
      isPlatformAdmin: false,
      tenantId: 'org_apex',
    };
    onLoginSuccess(user);
    navigate('/app/dashboard');
  };

  return (
    <div style={{ maxWidth: '480px', margin: '40px auto', fontFamily: theme.typography.fontFamily }}>
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
              backgroundColor: theme.colors.primary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#FFF',
              fontSize: '28px',
              fontWeight: 800,
              margin: '0 auto 16px auto',
            }}
          >
            🔑
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: theme.colors.textPrimary, margin: '0 0 8px 0' }}>
            {isFa ? 'ورود به سامانه کسب‌وکار رستو' : 'Sign in to Resto Tenant Workspace'}
          </h2>
          <p style={{ color: theme.colors.textSecondary, fontSize: '14px', margin: 0 }}>
            {isFa ? 'وارد حساب مدیریت سازمان یا پنل فروشگاهی شوید' : 'Enter your organization credentials to access your dashboard'}
          </p>
        </div>

        {error && (
          <div
            style={{
              backgroundColor: '#FEE2E2',
              color: '#991B1B',
              padding: `${theme.spacing.md} ${theme.spacing.lg}`,
              borderRadius: theme.borderRadius.lg,
              fontSize: '14px',
              marginBottom: theme.spacing.lg,
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg }}>
          <div>
            <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px', color: theme.colors.textPrimary }}>
              {isFa ? 'ایمیل کاری (نام کاربری):' : 'Work Email:'} *
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="reza@grandcoffee.com"
              style={{
                width: '100%',
                padding: theme.spacing.md,
                borderRadius: theme.borderRadius.lg,
                border: `1px solid ${theme.colors.border}`,
                fontSize: '15px',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px', color: theme.colors.textPrimary }}>
              {isFa ? 'رمز عبور:' : 'Password:'} *
            </label>
            <input
              type="password"
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{
                width: '100%',
                padding: theme.spacing.md,
                borderRadius: theme.borderRadius.lg,
                border: `1px solid ${theme.colors.border}`,
                fontSize: '15px',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            style={{
              backgroundColor: theme.colors.primary,
              color: '#FFF',
              border: 'none',
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.lg,
              fontSize: '16px',
              fontWeight: 700,
              cursor: 'pointer',
              marginTop: '8px',
            }}
          >
            {isSubmitting ? (isFa ? 'در حال بررسی...' : 'Signing in...') : (isFa ? 'ورود به حساب کسب‌وکار' : 'Sign In')}
          </button>
        </form>

        {/* Demo Quick Login */}
        <div style={{ marginTop: theme.spacing['2xl'], paddingTop: theme.spacing.xl, borderTop: `1px solid ${theme.colors.border}` }}>
          <div style={{ fontSize: '12px', color: theme.colors.textSecondary, marginBottom: theme.spacing.md, textAlign: 'center' }}>
            {isFa ? 'دسترس آسان تست سریع (Quick Demo Access):' : 'Quick Demo Access:'}
          </div>
          <button
            onClick={handleQuickTenantLogin}
            style={{
              width: '100%',
              backgroundColor: theme.colors.primaryLight,
              color: theme.colors.primaryDark,
              border: 'none',
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.md,
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            🏢 {isFa ? 'ورود سریع به عنوان مالک سازمان (Tenant Owner Demo)' : 'Demo Login as Tenant Owner'}
          </button>
        </div>

        {/* Links */}
        <div style={{ marginTop: theme.spacing.xl, display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
          <button
            type="button"
            onClick={() => navigate('/register')}
            style={{ border: 'none', background: 'none', color: theme.colors.primary, cursor: 'pointer', fontWeight: 600 }}
          >
            {isFa ? 'ثبت کسب‌وکار جدید در رستو' : 'Register New Business'}
          </button>
          <button
            type="button"
            onClick={() => navigate('/activation')}
            style={{ border: 'none', background: 'none', color: theme.colors.textSecondary, cursor: 'pointer' }}
          >
            {isFa ? 'راهنمای فعال‌سازی' : 'Activation Info'}
          </button>
        </div>
      </div>
    </div>
  );
};
