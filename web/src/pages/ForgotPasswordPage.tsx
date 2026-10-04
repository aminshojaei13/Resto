import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { Language, t } from '../i18n/authStrings';
import { inputStyle } from './LoginPage';

/**
 * Ask for a password reset link.
 *
 * The page deliberately shows no code, no link and no hint about whether the
 * address exists: the answer is the same in every case, which is what stops
 * this screen from being used to discover who has an account.
 */
export const ForgotPasswordPage: React.FC<{
  language: Language;
  navigate: (path: string) => void;
}> = ({ language, navigate }) => {
  const { theme, effectiveMode } = useTheme();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const [email, setEmail] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setError(null);

    if (!email.trim()) {
      setError(strings.required);
      return;
    }

    setIsSubmitting(true);

    try {
      await apiClient.forgotPassword(email.trim(), language);
      setSent(true);
    } catch (caught) {
      setError(
        caught instanceof ApiError
          ? caught.status === 429
            ? strings.tooManyAttempts
            : caught.message
          : strings.genericError
      );
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
        {strings.forgotTitle}
      </h1>
      <p style={{ margin: '0 0 24px 0', fontSize: '13px', color: theme.colors.textSecondary }}>
        {strings.forgotSubtitle}
      </p>

      {sent ? (
        <div>
          <div
            role="status"
            style={{
              padding: '14px',
              borderRadius: '8px',
              backgroundColor: isDark ? theme.colors.successLight : '#F0FDF4',
              color: isDark ? theme.colors.success : '#166534',
              fontSize: '14px',
              lineHeight: 1.7,
            }}
          >
            <strong style={{ display: 'block', marginBottom: '6px' }}>{strings.forgotSentTitle}</strong>
            {strings.forgotSentBody}
            <p style={{ margin: '10px 0 0 0', fontSize: '12px', opacity: 0.85 }}>{strings.forgotSentNote}</p>
          </div>
          <BackLink language={language} navigate={navigate} theme={theme} isDark={isDark} />
        </div>
      ) : (
        <form onSubmit={handleSubmit} noValidate>
          <label
            htmlFor="forgot-email"
            style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: theme.colors.textPrimary }}
          >
            {strings.email}
          </label>
          <input
            id="forgot-email"
            name="email"
            type="email"
            autoComplete="email"
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
            {isSubmitting ? strings.sending : strings.sendResetLink}
          </button>

          <BackLink language={language} navigate={navigate} theme={theme} isDark={isDark} />
        </form>
      )}
    </div>
  );
};

const BackLink: React.FC<{
  language: Language;
  navigate: (path: string) => void;
  theme: any;
  isDark: boolean;
}> = ({ language, navigate, theme }) => {
  const strings = t(language);

  return (
    <button
      type="button"
      onClick={() => navigate('/login')}
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
      {strings.backToSignIn}
    </button>
  );
};

export default ForgotPasswordPage;
