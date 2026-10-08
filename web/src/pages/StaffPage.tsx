import React, { useCallback, useEffect, useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ApiError, apiClient } from '../api/apiClient';
import { useAuth } from '../auth/AuthContext';
import { Language, roleLabel, t } from '../i18n/authStrings';
import { inputStyle } from './LoginPage';
import { labelStyle } from './ResetPasswordPage';

/**
 * Staff access.
 *
 * People are invited by email and set their own password. The list shows each
 * person's own name, never the owner's, and every action here is re-checked by
 * the server — hiding a button is not what keeps a staff member out.
 */
export const StaffPage: React.FC<{ language: Language }> = ({ language }) => {
  const { theme, effectiveMode } = useTheme();
  const { can } = useAuth();
  const strings = t(language);
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const [members, setMembers] = useState<any[]>([]);
  const [invitations, setInvitations] = useState<any[]>([]);
  const [canManage, setCanManage] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [isInviteOpen, setIsInviteOpen] = useState(false);
  const [email, setEmail] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [role, setRole] = useState<'MANAGER' | 'STAFF'>('STAFF');
  const [isInviting, setIsInviting] = useState(false);
  const [inviteError, setInviteError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setLoadError(null);

    try {
      const data = await apiClient.getStaff();
      setMembers(data.members ?? []);
      setInvitations(data.pending_invitations ?? []);
      setCanManage(Boolean(data.can_manage_staff));
    } catch (caught) {
      setLoadError(caught instanceof ApiError ? caught.message : strings.genericError);
    } finally {
      setIsLoading(false);
    }
  }, [strings.genericError]);

  useEffect(() => {
    void load();
  }, [load]);

  const run = async (action: () => Promise<{ message?: string }>, confirmText?: string) => {
    if (confirmText && !window.confirm(confirmText)) return;

    setNotice(null);

    try {
      const result = await action();
      setNotice(result?.message ?? '');
      await load();
    } catch (caught) {
      setLoadError(caught instanceof ApiError ? caught.message : strings.genericError);
    }
  };

  const handleInvite = async (event: React.FormEvent) => {
    event.preventDefault();
    setInviteError(null);
    setIsInviting(true);

    try {
      const result = await apiClient.inviteStaff({
        email: email.trim(),
        first_name: firstName.trim(),
        last_name: lastName.trim() || undefined,
        role,
        locale: language,
      });

      setNotice(result.message || strings.staffInviteSent);
      setEmail('');
      setFirstName('');
      setLastName('');
      setRole('STAFF');
      setIsInviteOpen(false);
      await load();
    } catch (caught) {
      setInviteError(caught instanceof ApiError ? caught.message : strings.genericError);
    } finally {
      setIsInviting(false);
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

  const smallButton = (tone: 'primary' | 'neutral' | 'danger') => ({
    padding: '8px 12px',
    borderRadius: '8px',
    fontSize: '12px',
    fontWeight: 600,
    cursor: 'pointer',
    fontFamily: theme.typography.fontFamily,
    border:
      tone === 'primary'
        ? 'none'
        : `1px solid ${tone === 'danger' ? (isDark ? theme.colors.error : '#DC2626') : isDark ? theme.colors.borderStrong : '#CBD5E1'}`,
    backgroundColor:
      tone === 'primary' ? theme.colors.primary : tone === 'danger' ? 'transparent' : 'transparent',
    color:
      tone === 'primary' ? '#FFFFFF' : tone === 'danger' ? (isDark ? theme.colors.error : '#DC2626') : theme.colors.textPrimary,
  });

  return (
    <div style={{ maxWidth: '860px', margin: '0 auto', direction: isFa ? 'rtl' : 'ltr', textAlign: isFa ? 'right' : 'left' }}>
      <h1 style={{ margin: '0 0 4px 0', fontSize: '22px', color: theme.colors.textPrimary }}>{strings.staffTitle}</h1>
      <p style={{ margin: '0 0 24px 0', fontSize: '13px', color: theme.colors.textSecondary }}>{strings.staffSubtitle}</p>

      {!canManage && !isLoading && (
        <p style={{ fontSize: '13px', color: theme.colors.textSecondary, marginBottom: '16px' }}>{strings.cannotManageStaff}</p>
      )}

      {canManage && !isInviteOpen && (
        <button type="button" onClick={() => setIsInviteOpen(true)} style={smallButton('primary')}>
          + {strings.inviteStaff}
        </button>
      )}

      {canManage && isInviteOpen && (
        <form style={{ ...cardStyle, marginTop: '16px' }} onSubmit={handleInvite} noValidate>
          <h2 style={{ margin: '0 0 4px 0', fontSize: '16px', color: theme.colors.textPrimary }}>{strings.inviteTitle}</h2>
          <p style={{ margin: '0 0 16px 0', fontSize: '12px', color: theme.colors.textSecondary }}>{strings.inviteBody}</p>

          <label htmlFor="invite-email" style={labelStyle(theme)}>{strings.inviteEmail}</label>
          <input
            id="invite-email"
            name="email"
            type="email"
            autoComplete="off"
            dir="ltr"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            style={inputStyle(theme, isDark)}
          />

          <label htmlFor="invite-first-name" style={{ ...labelStyle(theme), marginTop: '14px' }}>{strings.firstName}</label>
          <input
            id="invite-first-name"
            name="first_name"
            type="text"
            value={firstName}
            onChange={(event) => setFirstName(event.target.value)}
            style={inputStyle(theme, isDark)}
          />

          <label htmlFor="invite-last-name" style={{ ...labelStyle(theme), marginTop: '14px' }}>{strings.lastName}</label>
          <input
            id="invite-last-name"
            name="last_name"
            type="text"
            value={lastName}
            onChange={(event) => setLastName(event.target.value)}
            style={inputStyle(theme, isDark)}
          />

          <label htmlFor="invite-role" style={{ ...labelStyle(theme), marginTop: '14px' }}>{strings.inviteRole}</label>
          <select
            id="invite-role"
            name="role"
            value={role}
            onChange={(event) => setRole(event.target.value as 'MANAGER' | 'STAFF')}
            style={inputStyle(theme, isDark)}
          >
            <option value="STAFF">{roleLabel(language, 'STAFF')}</option>
            <option value="MANAGER">{roleLabel(language, 'MANAGER')}</option>
          </select>

          {inviteError && (
            <p role="alert" style={{ marginTop: '12px', fontSize: '12px', color: isDark ? theme.colors.error : '#B91C1C' }}>
              {inviteError}
            </p>
          )}

          <div style={{ display: 'flex', gap: '10px', marginTop: '16px' }}>
            <button type="submit" disabled={isInviting} style={smallButton('primary')}>
              {isInviting ? strings.sending2 : strings.sendInvitation}
            </button>
            <button type="button" onClick={() => setIsInviteOpen(false)} style={smallButton('neutral')}>
              {strings.revoke}
            </button>
          </div>
        </form>
      )}

      {notice && (
        <div
          role="status"
          style={{
            margin: '16px 0',
            padding: '10px 12px',
            borderRadius: '8px',
            fontSize: '13px',
            backgroundColor: isDark ? theme.colors.successLight : '#F0FDF4',
            color: isDark ? theme.colors.success : '#166534',
          }}
        >
          {notice}
        </div>
      )}

      {loadError && (
        <div
          role="alert"
          style={{
            margin: '16px 0',
            padding: '10px 12px',
            borderRadius: '8px',
            fontSize: '13px',
            backgroundColor: isDark ? theme.colors.errorLight : '#FEF2F2',
            color: isDark ? theme.colors.error : '#B91C1C',
          }}
        >
          {loadError}
        </div>
      )}

      <section style={cardStyle}>
        <h2 style={{ margin: '0 0 12px 0', fontSize: '16px', color: theme.colors.textPrimary }}>{strings.members}</h2>

        {isLoading && <p style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{strings.sending}…</p>}

        {!isLoading && members.length === 0 && (
          <p style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{strings.noMembers}</p>
        )}

        {!isLoading &&
          members.map((member) => (
            <div
              key={member.membership_id ?? member.id}
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: '10px',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 0',
                borderTop: `1px solid ${isDark ? theme.colors.border : '#F1F5F9'}`,
              }}
            >
              <div>
                <div style={{ fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                  {member.display_name}
                  {member.is_self ? ` (${strings.profileTitle})` : ''}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, direction: 'ltr', textAlign: isFa ? 'right' : 'left' }}>
                  {member.email}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                  {roleLabel(language, member.role)} —{' '}
                  {member.status === 'ACTIVE' ? strings.statusActive : strings.statusInactive}
                </div>
              </div>

              {canManage && !member.is_self && (
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <select
                    aria-label={strings.inviteRole}
                    value={member.role}
                    onChange={(event) =>
                      void run(() => apiClient.updateStaffRole(member.id, event.target.value))
                    }
                    style={{ ...inputStyle(theme, isDark), width: 'auto', padding: '6px 8px', fontSize: '12px' }}
                  >
                    <option value="OWNER">{roleLabel(language, 'OWNER')}</option>
                    <option value="MANAGER">{roleLabel(language, 'MANAGER')}</option>
                    <option value="STAFF">{roleLabel(language, 'STAFF')}</option>
                  </select>

                  <button
                    type="button"
                    onClick={() =>
                      void run(
                        () =>
                          apiClient.updateStaffStatus(
                            member.id,
                            member.status === 'ACTIVE' ? 'DEACTIVATED' : 'ACTIVE'
                          ),
                        member.status === 'ACTIVE' ? strings.confirmDeactivateStaff : undefined
                      )
                    }
                    style={smallButton('neutral')}
                  >
                    {member.status === 'ACTIVE' ? strings.deactivate : strings.reactivate}
                  </button>

                  <button
                    type="button"
                    onClick={() => void run(() => apiClient.removeStaffMember(member.id), strings.confirmRemoveStaff)}
                    style={smallButton('danger')}
                  >
                    {strings.remove}
                  </button>
                </div>
              )}
            </div>
          ))}
      </section>

      {canManage && invitations.length > 0 && (
        <section style={cardStyle}>
          <h2 style={{ margin: '0 0 12px 0', fontSize: '16px', color: theme.colors.textPrimary }}>
            {strings.pendingInvites}
          </h2>

          {invitations.map((invitation) => (
            <div
              key={invitation.id}
              style={{
                display: 'flex',
                flexWrap: 'wrap',
                gap: '10px',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '12px 0',
                borderTop: `1px solid ${isDark ? theme.colors.border : '#F1F5F9'}`,
              }}
            >
              <div>
                <div style={{ fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                  {[invitation.first_name, invitation.last_name].filter(Boolean).join(' ') || invitation.email}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, direction: 'ltr', textAlign: isFa ? 'right' : 'left' }}>
                  {invitation.email}
                </div>
                {invitation.expires_at && (
                  <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                    {strings.expiresOn}:{' '}
                    {new Date(invitation.expires_at).toLocaleString(language === 'fa' ? 'fa-IR' : 'en-GB')}
                  </div>
                )}
              </div>

              <div style={{ display: 'flex', gap: '8px' }}>
                <button
                  type="button"
                  onClick={() => void run(() => apiClient.resendInvitation(invitation.id))}
                  style={smallButton('neutral')}
                >
                  {strings.resend}
                </button>
                <button
                  type="button"
                  onClick={() => void run(() => apiClient.revokeInvitation(invitation.id))}
                  style={smallButton('danger')}
                >
                  {strings.revoke}
                </button>
              </div>
            </div>
          ))}
        </section>
      )}
    </div>
  );
};

export default StaffPage;
