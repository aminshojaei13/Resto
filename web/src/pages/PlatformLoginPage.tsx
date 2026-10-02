import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { AuthUser } from '../types';
import { inputStyle, revealButtonStyle } from './LoginPage';
import { Language, t } from '../i18n/authStrings';

interface PlatformLoginPageProps {
  language?: Language;
  onLoginSuccess: (user: AuthUser) => void;
  navigate: (path: string) => void;
}

/**
 * Sign-in for the system administration console.
 *
 * The console is a technical interface, so "system administration" wording is
 * appropriate here. What is not appropriate — and is not present — is a way to
 * get in without the server agreeing: no demo account, no quick login and no
 * session invented in the browser.
 */
export const PlatformLoginPage: React.FC<PlatformLoginPageProps> = ({
  language = 'fa',
  onLoginSuccess,
  navigate,
}) => {
  const { theme, effectiveMode } = useTheme();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!email.trim() || !password) {
      setError(strings.required);
      return;
    }

    setIsSubmitting(true);

    try {
      const user = await apiClient.login({
        email: email.trim(),
        password,
        locale: language,
        deviceName: 'system-console',
      });

      onLoginSuccess(user);
      navigate('/applications');
    } catch (caught) {
      if (caught instanceof ApiError) {
        setError(
          caught.status === 429
            ? strings.tooManyAttempts
            : caught.status === 0
              ? strings.offline
              : caught.message
        );
      } else {
        setError(strings.genericError);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: theme.colors.background,
        fontFamily: theme.typography.fontFamily,
      }}
    >
      <div
        style={{
          width: '440px',
          maxWidth: '92%',
          backgroundColor: theme.colors.surfaceElevated,
          borderRadius: theme.borderRadius.xl,
          padding: theme.spacing['2xl'],
          boxShadow: theme.shadows.lg,
          border: `1px solid ${theme.colors.border}`,
          direction: isFa ? 'rtl' : 'ltr',
          textAlign: isFa ? 'right' : 'left',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: theme.spacing['2xl'] }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              margin: '0 auto 12px',
              borderRadius: theme.borderRadius.lg,
              backgroundColor: theme.colors.primary,
              color: '#FFF',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '26px',
              fontWeight: 800,
            }}
          >
            R
          </div>
          <h1 style={{ margin: 0, fontSize: '20px', color: theme.colors.textPrimary }}>
            {isFa ? 'ورود به مدیریت سامانه' : 'System administration sign in'}
          </h1>
          <p style={{ margin: '6px 0 0 0', fontSize: '13px', color: theme.colors.textSecondary }}>
            {isFa ? strings.loginSubtitle : 'Enter your email and password to continue.'}
          </p>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="platform-email" style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: theme.colors.textPrimary }}>
            {strings.email}
          </label>
          <input
            id="platform-email"
            name="email"
            type="email"
            autoComplete="username"
            inputMode="email"
            autoCapitalize="off"
            autoCorrect="off"
            spellCheck={false}
            dir="ltr"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder={strings.emailPlaceholder}
            style={inputStyle(theme, isDark)}
          />

          <label htmlFor="platform-password" style={{ display: 'block', fontSize: '13px', fontWeight: 600, margin: '16px 0 6px 0', color: theme.colors.textPrimary }}>
            {strings.password}
          </label>
          <div style={{ position: 'relative' }}>
            <input
              id="platform-password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              dir="ltr"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              style={{ ...inputStyle(theme, isDark), paddingInlineEnd: '44px' }}
            />
            <button
              type="button"
              onClick={() => setShowPassword((value) => !value)}
              aria-label={showPassword ? strings.hidePassword : strings.showPassword}
              style={revealButtonStyle(theme, isDark)}
            >
              {showPassword ? '🙈' : '👁'}
            </button>
          </div>

          {error && (
            <div
              role="alert"
              style={{
                marginTop: '16px',
                padding: '10px 12px',
                borderRadius: '8px',
                fontSize: '13px',
                backgroundColor: isDark ? theme.colors.errorLight : '#FEF2F2',
                color: isDark ? theme.colors.error : '#B91C1C',
              }}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={isSubmitting}
            style={{
              width: '100%',
              marginTop: '20px',
              padding: '12px',
              borderRadius: '8px',
              border: 'none',
              backgroundColor: theme.colors.primary,
              color: '#FFFFFF',
              fontSize: '15px',
              fontWeight: 700,
              cursor: isSubmitting ? 'default' : 'pointer',
              opacity: isSubmitting ? 0.7 : 1,
              fontFamily: theme.typography.fontFamily,
            }}
          >
            {isSubmitting ? strings.signingIn : strings.signIn}
          </button>

          <button
            type="button"
            onClick={() => navigate('/forgot-password')}
            style={{
              display: 'block',
              width: '100%',
              marginTop: '12px',
              background: 'none',
              border: 'none',
              color: theme.colors.info,
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
              fontFamily: theme.typography.fontFamily,
            }}
          >
            {strings.forgotPassword}
          </button>
        </form>
      </div>
    </div>
  );
};

export default PlatformLoginPage;
