import React, { useEffect, useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { useAuth } from '../auth/AuthContext';
import { Language, roleLabel, t } from '../i18n/authStrings';
import { inputStyle, revealButtonStyle } from './LoginPage';
import { labelStyle, primaryButton } from './ResetPasswordPage';

/**
 * The account page.
 *
 * Everything shown here is read from the authenticated profile: the person's
 * own name, email and businesses. There are no fallbacks such as an email
 * prefix, an organisation name or a previously signed-in person.
 */
export const ProfilePage: React.FC<{ language: Language }> = ({ language }) => {
  const { theme, effectiveMode } = useTheme();
  const { user, setUser, signOut } = useAuth();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const [firstName, setFirstName] = useState(user?.firstName ?? '');
  const [lastName, setLastName] = useState(user?.lastName ?? '');
  const [phone, setPhone] = useState(user?.phone ?? '');

  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [profileNotice, setProfileNotice] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newPasswordConfirmation, setNewPasswordConfirmation] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [passwordNotice, setPasswordNotice] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | null>(null);

  useEffect(() => {
    setFirstName(user?.firstName ?? '');
    setLastName(user?.lastName ?? '');
    setPhone(user?.phone ?? '');
  }, [user?.firstName, user?.lastName, user?.phone]);

  const handleSaveProfile = async (event: React.FormEvent) => {
    event.preventDefault();
    setProfileError(null);
    setProfileNotice(null);
    setIsSavingProfile(true);

    try {
      const updated = await apiClient.updateProfile({
        first_name: firstName.trim(),
        last_name: lastName.trim(),
        phone: phone.trim(),
      });
      setUser(updated);
      setProfileNotice(strings.profileSaved);
    } catch (caught) {
      setProfileError(caught instanceof ApiError ? caught.message : strings.genericError);
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleChangePassword = async (event: React.FormEvent) => {
    event.preventDefault();
    setPasswordError(null);
    setPasswordNotice(null);

    if (newPassword !== newPasswordConfirmation) {
      setPasswordError(strings.passwordsDoNotMatch);
      return;
    }

    setIsChangingPassword(true);

    try {
      const result = await apiClient.changePassword({
        current_password: currentPassword,
        new_password: newPassword,
        new_password_confirmation: newPasswordConfirmation,
      });
      setPasswordNotice(result.message || strings.changeDone);
      setCurrentPassword('');
      setNewPassword('');
      setNewPasswordConfirmation('');
    } catch (caught) {
      setPasswordError(caught instanceof ApiError ? caught.message : strings.genericError);
    } finally {
      setIsChangingPassword(false);
    }
  };

  const cardStyle = {
    padding: '20px',
    borderRadius: '12px',
    backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
    border: `1px solid ${isDark ? theme.colors.borderStrong : '#E2E8F0'}`,
    fontFamily: theme.typography.fontFamily,
    marginBottom: '20px',
  };

  return (
    <div style={{ maxWidth: '720px', margin: '0 auto', direction: isFa ? 'rtl' : 'ltr', textAlign: isFa ? 'right' : 'left' }}>
      <h1 style={{ margin: '0 0 4px 0', fontSize: '22px', color: theme.colors.textPrimary }}>{strings.profileTitle}</h1>
      <p style={{ margin: '0 0 24px 0', fontSize: '13px', color: theme.colors.textSecondary }}>{strings.profileSubtitle}</p>

      <section style={cardStyle} aria-label={strings.profileTitle}>
        <dl style={{ display: 'grid', gridTemplateColumns: 'auto 1fr', gap: '8px 16px', margin: 0, fontSize: '13px' }}>
          <dt style={{ color: theme.colors.textSecondary }}>{strings.email}</dt>
          <dd style={{ margin: 0, color: theme.colors.textPrimary, direction: 'ltr', textAlign: isFa ? 'right' : 'left' }}>
            {user?.email ?? ''}
          </dd>

          <dt style={{ color: theme.colors.textSecondary }}>{strings.profileTitle}</dt>
          <dd style={{ margin: 0, color: theme.colors.textPrimary }}>{user?.displayName ?? ''}</dd>

          <dt style={{ color: theme.colors.textSecondary }}>{strings.inviteRole}</dt>
          <dd style={{ margin: 0, color: theme.colors.textPrimary }}>{roleLabel(language, user?.role)}</dd>

          <dt style={{ color: theme.colors.textSecondary }}>{strings.accountStatus}</dt>
          <dd style={{ margin: 0, color: theme.colors.textPrimary }}>
            {user?.status === 'ACTIVE' ? strings.statusActive : strings.statusInactive}
          </dd>

          {user && user.memberships.length > 0 && (
            <>
              <dt style={{ color: theme.colors.textSecondary }}>{strings.yourBusinesses}</dt>
              <dd style={{ margin: 0, color: theme.colors.textPrimary }}>
                {user.memberships
                  .map((membership) => `${membership.name} — ${roleLabel(language, membership.role)}`)
                  .join(isFa ? '، ' : ', ')}
              </dd>
            </>
          )}
        </dl>
      </section>

      <form style={cardStyle} onSubmit={handleSaveProfile} noValidate>
        <h2 style={{ margin: '0 0 16px 0', fontSize: '16px', color: theme.colors.textPrimary }}>{strings.profileTitle}</h2>

        <label htmlFor="profile-first-name" style={labelStyle(theme)}>{strings.firstName}</label>
        <input
          id="profile-first-name"
          name="first_name"
          type="text"
          autoComplete="given-name"
          value={firstName}
          onChange={(event) => setFirstName(event.target.value)}
          style={inputStyle(theme, isDark)}
        />

        <label htmlFor="profile-last-name" style={{ ...labelStyle(theme), marginTop: '16px' }}>{strings.lastName}</label>
        <input
          id="profile-last-name"
          name="last_name"
          type="text"
          autoComplete="family-name"
          value={lastName}
          onChange={(event) => setLastName(event.target.value)}
          style={inputStyle(theme, isDark)}
        />

        <label htmlFor="profile-phone" style={{ ...labelStyle(theme), marginTop: '16px' }}>{strings.phone}</label>
        <input
          id="profile-phone"
          name="phone"
          type="tel"
          autoComplete="tel"
          dir="ltr"
          value={phone}
          onChange={(event) => setPhone(event.target.value)}
          style={inputStyle(theme, isDark)}
        />

        {profileError && <Notice theme={theme} isDark={isDark} tone="error">{profileError}</Notice>}
        {profileNotice && <Notice theme={theme} isDark={isDark} tone="success">{profileNotice}</Notice>}

        <button type="submit" disabled={isSavingProfile} style={{ ...primaryButton(theme), marginTop: '16px', opacity: isSavingProfile ? 0.7 : 1 }}>
          {isSavingProfile ? strings.saving : strings.saveProfile}
        </button>
      </form>

      <form style={cardStyle} onSubmit={handleChangePassword} noValidate>
        <h2 style={{ margin: '0 0 16px 0', fontSize: '16px', color: theme.colors.textPrimary }}>{strings.changeTitle}</h2>

        <label htmlFor="profile-current-password" style={labelStyle(theme)}>{strings.currentPassword}</label>
        <input
          id="profile-current-password"
          name="current_password"
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          dir="ltr"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          style={inputStyle(theme, isDark)}
        />

        <label htmlFor="profile-new-password" style={{ ...labelStyle(theme), marginTop: '16px' }}>{strings.newPassword}</label>
        <div style={{ position: 'relative' }}>
          <input
            id="profile-new-password"
            name="new_password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="new-password"
            dir="ltr"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
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

        <label htmlFor="profile-new-password-confirm" style={{ ...labelStyle(theme), marginTop: '16px' }}>
          {strings.confirmPassword}
        </label>
        <input
          id="profile-new-password-confirm"
          name="new_password_confirmation"
          type={showPassword ? 'text' : 'password'}
          autoComplete="new-password"
          dir="ltr"
          value={newPasswordConfirmation}
          onChange={(event) => setNewPasswordConfirmation(event.target.value)}
          style={inputStyle(theme, isDark)}
        />

        <p style={{ margin: '12px 0 0 0', fontSize: '12px', color: theme.colors.textSecondary }}>{strings.newPasswordHint}</p>

        {passwordError && <Notice theme={theme} isDark={isDark} tone="error">{passwordError}</Notice>}
        {passwordNotice && <Notice theme={theme} isDark={isDark} tone="success">{passwordNotice}</Notice>}

        <button type="submit" disabled={isChangingPassword} style={{ ...primaryButton(theme), marginTop: '16px', opacity: isChangingPassword ? 0.7 : 1 }}>
          {isChangingPassword ? strings.changing : strings.changePassword}
        </button>
      </form>

      <div style={cardStyle}>
        <button
          type="button"
          onClick={async () => {
            await apiClient.logoutEverywhere().catch(() => undefined);
            await signOut();
          }}
          style={{
            background: 'none',
            border: `1px solid ${isDark ? theme.colors.borderStrong : '#CBD5E1'}`,
            color: theme.colors.textPrimary,
            padding: '10px 16px',
            borderRadius: '8px',
            fontSize: '13px',
            fontWeight: 600,
            cursor: 'pointer',
            fontFamily: theme.typography.fontFamily,
          }}
        >
          {strings.signOutEverywhere}
        </button>
      </div>
    </div>
  );
};

const Notice: React.FC<{ theme: any; isDark: boolean; tone: 'error' | 'success'; children: React.ReactNode }> = ({
  theme,
  isDark,
  tone,
  children,
}) => (
  <div
    role="alert"
    style={{
      marginTop: '14px',
      padding: '10px 12px',
      borderRadius: '8px',
      fontSize: '13px',
      backgroundColor:
        tone === 'error'
          ? isDark
            ? theme.colors.errorLight
            : '#FEF2F2'
          : isDark
            ? theme.colors.successLight
            : '#F0FDF4',
      color:
        tone === 'error' ? (isDark ? theme.colors.error : '#B91C1C') : isDark ? theme.colors.success : '#166534',
    }}
  >
    {children}
  </div>
);

export default ProfilePage;
