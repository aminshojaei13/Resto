import React, { useMemo, useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { Language, t } from '../i18n/authStrings';
import { inputStyle, revealButtonStyle } from './LoginPage';

/**
 * Set a new password from the emailed link.
 *
 * The token comes from the link in the email and is submitted together with
 * the new password. There is no fixed code, and the screen says plainly when
 * the link is missing, expired or already used.
 */
export const ResetPasswordPage: React.FC<{
  language: Language;
  navigate: (path: string) => void;
  /** Read directly from the query string; injectable so tests can drive it. */
  search?: string;
}> = ({ language, navigate, search }) => {
  const { theme, effectiveMode } = useTheme();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const params = useMemo(() => {
    const raw = search ?? (typeof window !== 'undefined' ? window.location.search : '');
    return new URLSearchParams(raw);
  }, [search]);

  const emailFromLink = params.get('email') ?? '';
  const tokenFromLink = params.get('token') ?? '';

  const [token, setToken] = useState(tokenFromLink);
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!emailFromLink || !token.trim()) {
      setError(strings.resetLinkMissing);
      return;
    }

    if (password !== confirmation) {
      setError(strings.passwordsDoNotMatch);
      return;
    }

    setIsSubmitting(true);

    try {
      await apiClient.resetPassword({
        email: emailFromLink,
        token: token.trim(),
        password,
        password_confirmation: confirmation,
        locale: language,
      });
      setDone(true);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : strings.genericError);
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
      <h1 style={{ margin: '0 0 4px 0', fontSize: '20px', color: theme.colors.textPrimary }}>
        {done ? strings.resetDoneTitle : strings.resetTitle}
      </h1>
      <p style={{ margin: '0 0 24px 0', fontSize: '13px', color: theme.colors.textSecondary }}>
        {done ? strings.resetDoneBody : strings.resetSubtitle}
      </p>

      {done ? (
        <button
          type="button"
          onClick={() => navigate('/login')}
          style={primaryButton(theme)}
        >
          {strings.goToSignIn}
        </button>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          {!tokenFromLink && (
            <>
              <label
                htmlFor="reset-token"
                style={labelStyle(theme)}
              >
                {strings.resetToken}
              </label>
              <input
                id="reset-token"
                name="token"
                type="text"
                dir="ltr"
                autoComplete="one-time-code"
                value={token}
                onChange={(event) => setToken(event.target.value)}
                style={inputStyle(theme, isDark)}
              />
            </>
          )}

          <label htmlFor="reset-password" style={{ ...labelStyle(theme), marginTop: '16px' }}>
            {strings.newPassword}
          </label>
          <div style={{ position: 'relative' }}>
            <input
              id="reset-password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
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

          <label htmlFor="reset-password-confirm" style={{ ...labelStyle(theme), marginTop: '16px' }}>
            {strings.confirmPassword}
          </label>
          <input
            id="reset-password-confirm"
            name="password_confirmation"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            dir="ltr"
            value={confirmation}
            onChange={(event) => setConfirmation(event.target.value)}
            style={inputStyle(theme, isDark)}
          />

          <p style={{ margin: '12px 0 0 0', fontSize: '12px', color: theme.colors.textSecondary }}>
            {strings.newPasswordHint}
          </p>

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

          <button type="submit" disabled={isSubmitting} style={{ ...primaryButton(theme), marginTop: '20px', opacity: isSubmitting ? 0.7 : 1 }}>
            {isSubmitting ? strings.updating : strings.setNewPassword}
          </button>
        </form>
      )}
    </div>
  );
};

export const labelStyle = (theme: any) => ({
  display: 'block' as const,
  fontSize: '13px',
  fontWeight: 600,
  marginBottom: '6px',
  color: theme.colors.textPrimary,
});

export const primaryButton = (theme: any) => ({
  width: '100%',
  padding: '12px',
  borderRadius: '8px',
  border: 'none',
  backgroundColor: theme.colors.primary,
  color: '#FFFFFF',
  fontSize: '15px',
  fontWeight: 700,
  cursor: 'pointer',
  fontFamily: theme.typography.fontFamily,
});

export default ResetPasswordPage;
