import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { AuthUser } from '../types';

interface PlatformLoginPageProps {
  language?: 'fa' | 'en';
  onLoginSuccess: (user: AuthUser) => void;
  navigate: (path: string) => void;
}

export const PlatformLoginPage: React.FC<PlatformLoginPageProps> = ({
  language = 'fa',
  onLoginSuccess,
  navigate,
}) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');

  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setError(isFa ? 'لطفا مشخصات راهبر پلتفرم را وارد کنید.' : 'Please enter platform admin credentials.');
      return;
    }

    const user: AuthUser = {
      id: 'usr_admin',
      name: 'Resto Platform Administrator',
      email: email.toLowerCase(),
      role: 'PlatformAdmin',
      isPlatformAdmin: true,
    };
    onLoginSuccess(user);
    navigate('/platform/applications');
  };

  const handleQuickPlatformAdminLogin = () => {
    const user: AuthUser = {
      id: 'usr_admin',
      name: 'Resto Platform Administrator',
      email: 'admin@resto.com',
      role: 'PlatformAdmin',
      isPlatformAdmin: true,
    };
    onLoginSuccess(user);
    navigate('/platform/applications');
  };

  return (
    <div style={{ maxWidth: '480px', margin: '40px auto', fontFamily: theme.typography.fontFamily }}>
      <div
        style={{
          backgroundColor: theme.colors.surfaceElevated,
          color: theme.colors.textPrimary,
          borderRadius: theme.borderRadius.xl,
          padding: theme.spacing['2xl'],
          boxShadow: theme.shadows.lg,
          border: `1px solid ${theme.colors.border}`,
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: theme.spacing['2xl'] }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: theme.borderRadius.xl,
              backgroundColor: theme.colors.infoLight,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: theme.colors.info,
              fontSize: '28px',
              margin: '0 auto 16px auto',
            }}
          >
            🛡️
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: theme.colors.textPrimary, margin: '0 0 8px 0' }}>
            {isFa ? 'ورود به پنل راهبری پلتفرم (Platform Admin)' : 'Resto Platform Admin Portal'}
          </h2>
          <p style={{ color: theme.colors.textSecondary, fontSize: '14px', margin: 0 }}>
            {isFa ? 'ویژه بررسی درخواست‌های ثبت سازمان و راه‌اندازی تننت‌ها' : 'Administrative access for application reviews & tenant provisioning'}
          </p>
        </div>

        {error && (
          <div
            style={{
              backgroundColor: theme.colors.errorLight,
              color: theme.colors.error,
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
              {isFa ? 'ایمیل راهبر پلتفرم:' : 'Platform Admin Email:'} *
            </label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="admin@resto.com"
              style={{
                width: '100%',
                padding: theme.spacing.md,
                borderRadius: theme.borderRadius.lg,
                border: `1px solid ${theme.colors.borderStrong}`,
                backgroundColor: theme.colors.surface,
                color: theme.colors.textPrimary,
                fontSize: '15px',
                boxSizing: 'border-box',
              }}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px', color: theme.colors.textPrimary }}>
              {isFa ? 'رمز عبور راهبری:' : 'Admin Password:'} *
            </label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                style={{
                  width: '100%',
                  padding: theme.spacing.md,
                  paddingLeft: isFa ? '12px' : '40px',
                  paddingRight: isFa ? '40px' : '12px',
                  borderRadius: theme.borderRadius.lg,
                  border: `1px solid ${theme.colors.borderStrong}`,
                  backgroundColor: theme.colors.surface,
                  color: theme.colors.textPrimary,
                  fontSize: '15px',
                  boxSizing: 'border-box',
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                style={{ position: 'absolute', left: isFa ? '12px' : 'auto', right: isFa ? 'auto' : '12px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
              >
                {showPassword ? '👁️' : '🙈'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            style={{
              backgroundColor: theme.colors.info,
              color: '#FFFFFF',
              border: 'none',
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.lg,
              fontSize: '16px',
              fontWeight: 700,
              cursor: 'pointer',
              marginTop: '8px',
            }}
          >
            🚀 {isFa ? 'ورود به پنل راهبر' : 'Login to Admin Panel'}
          </button>
        </form>

        {/* Demo Quick Access */}
        <div style={{ marginTop: theme.spacing['2xl'], paddingTop: theme.spacing.xl, borderTop: `1px solid ${theme.colors.border}` }}>
          <button
            onClick={handleQuickPlatformAdminLogin}
            style={{
              width: '100%',
              backgroundColor: isDark ? theme.colors.surfaceHover : theme.colors.surfaceSelected,
              color: isDark ? theme.colors.info : theme.colors.info,
              border: 'none',
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.md,
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            🛡️ {isFa ? 'ورود مستقیم تست (Platform Admin Demo)' : 'Quick Demo Login as Platform Admin'}
          </button>
        </div>

        <div style={{ marginTop: theme.spacing.xl, textAlign: 'center', fontSize: '13px' }}>
          <button
            type="button"
            onClick={() => navigate('/login')}
            style={{ border: 'none', background: 'none', color: theme.colors.info, cursor: 'pointer', fontWeight: 600 }}
          >
            {isFa ? 'بازگشت به ورود کاربران معمولی (Tenant Login)' : 'Switch to Tenant User Login'}
          </button>
        </div>
      </div>
    </div>
  );
};
