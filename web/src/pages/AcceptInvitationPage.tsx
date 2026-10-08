import React, { useEffect, useMemo, useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { Language, roleLabel, t } from '../i18n/authStrings';
import { inputStyle, revealButtonStyle } from './LoginPage';
import { labelStyle, primaryButton } from './ResetPasswordPage';

/**
 * Accept a staff invitation and choose your own password.
 *
 * The link token is the only credential here. The screen shows the invited
 * person their own name and the business they are joining — never anybody
 * else's.
 */
export const AcceptInvitationPage: React.FC<{
  language: Language;
  navigate: (path: string) => void;
  search?: string;
}> = ({ language, navigate, search }) => {
  const { theme, effectiveMode } = useTheme();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const token = useMemo(() => {
    const raw = search ?? (typeof window !== 'undefined' ? window.location.search : '');
    return new URLSearchParams(raw).get('token') ?? '';
  }, [search]);

  const [invitation, setInvitation] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(Boolean(token));
  const [isInvalid, setIsInvalid] = useState(!token);
  const [password, setPassword] = useState('');
  const [confirmation, setConfirmation] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;

    let cancelled = false;

    apiClient
      .getInvitation(token)
      .then((data) => {
        if (!cancelled) setInvitation(data.invitation);
      })
      .catch(() => {
        if (!cancelled) setIsInvalid(true);
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [token]);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (password !== confirmation) {
      setError(strings.passwordsDoNotMatch);
      return;
    }

    setIsSubmitting(true);

    try {
      await apiClient.acceptInvitation({
        token,
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
        {done ? strings.acceptDone : strings.acceptTitle}
      </h1>
      <p style={{ margin: '0 0 20px 0', fontSize: '13px', color: theme.colors.textSecondary }}>
        {done ? '' : strings.acceptBody}
      </p>

      {isInvalid && (
        <div
          role="alert"
          style={{
            padding: '12px',
            borderRadius: '8px',
            fontSize: '13px',
            backgroundColor: isDark ? theme.colors.errorLight : '#FEF2F2',
            color: isDark ? theme.colors.error : '#B91C1C',
          }}
        >
          {strings.acceptInvalid}
        </div>
      )}

      {isLoading && <p style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{strings.sending}…</p>}

      {done && (
        <button type="button" onClick={() => navigate('/login')} style={primaryButton(theme)}>
          {strings.goToSignIn}
        </button>
      )}

      {!done && !isInvalid && !isLoading && (
        <>
          <div
            style={{
              padding: '12px 14px',
              borderRadius: '8px',
              marginBottom: '18px',
              backgroundColor: isDark ? theme.colors.infoLight : '#EFF6FF',
              color: isDark ? theme.colors.info : '#1E40AF',
              fontSize: '13px',
              lineHeight: 1.8,
            }}
          >
            <div>
              <strong>{strings.acceptFor}:</strong>{' '}
              {[invitation?.first_name, invitation?.last_name].filter(Boolean).join(' ') || invitation?.email}
            </div>
            <div>
              <strong>{strings.yourBusinesses}:</strong> {invitation?.organization_name}
            </div>
            <div>
              <strong>{strings.inviteRole}:</strong> {roleLabel(language, invitation?.role)}
            </div>
            {invitation?.expires_at && (
              <div>
                <strong>{strings.expiresOn}:</strong>{' '}
                {new Date(invitation.expires_at).toLocaleString(language === 'fa' ? 'fa-IR' : 'en-GB')}
              </div>
            )}
            <p style={{ margin: '8px 0 0 0', fontSize: '12px', opacity: 0.85 }}>{strings.acceptExpiryNote}</p>
          </div>

          <form onSubmit={handleSubmit} noValidate>
            <label htmlFor="accept-password" style={labelStyle(theme)}>
              {strings.newPassword}
            </label>
            <div style={{ position: 'relative' }}>
              <input
                id="accept-password"
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

            <label htmlFor="accept-password-confirm" style={{ ...labelStyle(theme), marginTop: '16px' }}>
              {strings.confirmPassword}
            </label>
            <input
              id="accept-password-confirm"
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
              {isSubmitting ? strings.sending2 : strings.setNewPassword}
            </button>
          </form>
        </>
      )}
    </div>
  );
};

export default AcceptInvitationPage;
