import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { useAuth } from '../auth/AuthContext';
import { Language, t } from '../i18n/authStrings';
import { AuthUser } from '../types';

interface Props {
  language: Language;
  navigate: (path: string) => void;
  onLoginSuccess?: (user: AuthUser) => void;
}

/**
 * Business sign-in.
 *
 * Email and password only. There is no demo account, no quick login and no
 * client-side fabrication of a session: if the server refuses, the page says
 * so in one generic sentence that reveals nothing about the address.
 */
export const LoginPage: React.FC<Props> = ({ language, navigate, onLoginSuccess }) => {
  const { theme, effectiveMode } = useTheme();
  const { signIn } = useAuth();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string[]>>({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);
    setFieldErrors({});

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
        deviceName: 'business-web',
      });

      signIn(user);
      onLoginSuccess?.(user);
      navigate('/app/dashboard');
    } catch (caught) {
      if (caught instanceof ApiError) {
        setFieldErrors(caught.fieldErrors);
        // A single generic message: never "no such user" or "wrong password".
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
        maxWidth: '440px',
        margin: '40px auto',
        padding: '32px',
        backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
        border: `1px solid ${isDark ? theme.colors.borderStrong : '#E2E8F0'}`,
        borderRadius: '16px',
        direction: isFa ? 'rtl' : 'ltr',
        textAlign: isFa ? 'right' : 'left',
        fontFamily: theme.typography.fontFamily,
      }}
    >
      <h1 style={{ margin: '0 0 4px 0', fontSize: '22px', color: theme.colors.textPrimary }}>
        {strings.brandName}
      </h1>
      <p style={{ margin: '0 0 24px 0', fontSize: '13px', color: theme.colors.textSecondary }}>
        {strings.loginTitle}
      </p>

      <form onSubmit={handleSubmit} noValidate>
        <label
          htmlFor="login-email"
          style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: theme.colors.textPrimary }}
        >
          {strings.email}
        </label>
        <input
          id="login-email"
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

        <label
          htmlFor="login-password"
          style={{ display: 'block', fontSize: '13px', fontWeight: 600, margin: '16px 0 6px 0', color: theme.colors.textPrimary }}
        >
          {strings.password}
        </label>
        <div style={{ position: 'relative' }}>
          <input
            id="login-password"
            name="password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            dir="ltr"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={strings.passwordPlaceholder}
            style={{ ...inputStyle(theme, isDark), paddingInlineEnd: '44px' }}
          />
          <button
            type="button"
            onClick={() => setShowPassword((value) => !value)}
            aria-label={showPassword ? strings.hidePassword : strings.showPassword}
            title={showPassword ? strings.hidePassword : strings.showPassword}
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

        {Object.entries(fieldErrors).map(([field, messages]) => (
          <p key={field} style={{ margin: '8px 0 0 0', fontSize: '12px', color: isDark ? theme.colors.error : '#B91C1C' }}>
            {messages.join(' ')}
          </p>
        ))}

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
  );
};

export const inputStyle = (theme: any, isDark: boolean) => ({
  width: '100%',
  padding: '10px 12px',
  borderRadius: '8px',
  border: `1px solid ${isDark ? theme.colors.borderStrong : '#CBD5E1'}`,
  backgroundColor: isDark ? theme.colors.background : '#FFFFFF',
  color: theme.colors.textPrimary,
  fontSize: '14px',
  fontFamily: theme.typography.fontFamily,
  boxSizing: 'border-box' as const,
});

export const revealButtonStyle = (theme: any, isDark: boolean) => ({
  position: 'absolute' as const,
  insetInlineEnd: '6px',
  top: '50%',
  transform: 'translateY(-50%)',
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  fontSize: '16px',
  padding: '4px',
  lineHeight: 1,
  color: theme.colors.textSecondary,
  opacity: isDark ? 0.9 : 1,
});

export default LoginPage;
