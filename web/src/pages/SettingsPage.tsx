import React, { useState } from 'react';
import { apiClient } from '../api/apiClient';
import { useTheme } from '../theme/ThemeContext';
import { AuthUser } from '../types';

interface SettingsPageProps {
  language?: 'fa' | 'en';
  authUser?: AuthUser | null;
  navigate: (path: string) => void;
}

export const SettingsPage: React.FC<SettingsPageProps> = ({
  language = 'fa',
  authUser,
  navigate,
}) => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currentPassword || !newPassword || !confirmPassword) {
      setErrorMessage(isFa ? 'لطفا تمامی فیلدهای مربوط به تغییر رمز عبور را وارد کنید.' : 'Please fill in all password fields.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage(isFa ? 'رمز عبور جدید و تکرار آن مطابقت ندارند.' : 'New passwords do not match.');
      return;
    }

    if (newPassword.length < 6) {
      setErrorMessage(isFa ? 'رمز عبور جدید باید حداقل ۶ کاراکتر باشد.' : 'New password must be at least 6 characters.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setSuccessMessage('');

    try {
      const res = await apiClient.changePassword({
        current_password: currentPassword,
        new_password: newPassword,
        new_password_confirmation: confirmPassword,
        email: authUser?.email,
      });

      setIsSubmitting(false);
      setSuccessMessage(res.message || (isFa ? 'رمز عبور شما با موفقیت بروزرسانی شد.' : 'Password changed successfully.'));
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در تغییر رمز عبور. رمز فعلی را بررسی کنید.' : 'Failed to change password.'));
    }
  };

  return (
    <div style={{ padding: '24px', maxWidth: '800px', margin: '0 auto', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold' }}>
          ⚙️ {isFa ? 'تنظیمات حساب و امنیت' : 'Account & Security Settings'}
        </h2>
        <p style={{ margin: 0, color: theme.colors.textSecondary, fontSize: '14px' }}>
          {isFa ? 'مدیریت اطلاعات کاربری، تنظیمات امنیتی و بروزرسانی رمز عبور' : 'Manage your user profile and change password'}
        </p>
      </div>

      {/* Account Profile Card */}
      <div style={{ backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}`, padding: '20px', marginBottom: '24px' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: 'bold', color: theme.colors.primary }}>
          👤 {isFa ? 'مشخصات حساب کاربر' : 'User Profile'}
        </h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', fontSize: '14px' }}>
          <div>
            <div style={{ color: theme.colors.textSecondary, fontSize: '12px', marginBottom: '2px' }}>{isFa ? 'نام و نام خانوادگی:' : 'Full Name:'}</div>
            <div style={{ fontWeight: 'bold' }}>{authUser?.name || (isFa ? 'کاربر رستو' : 'Resto User')}</div>
          </div>
          <div>
            <div style={{ color: theme.colors.textSecondary, fontSize: '12px', marginBottom: '2px' }}>{isFa ? 'ایمیل ورود:' : 'Login Email:'}</div>
            <div style={{ fontWeight: 'bold', fontFamily: 'monospace' }}>{authUser?.email || 'user@resto.com'}</div>
          </div>
          <div>
            <div style={{ color: theme.colors.textSecondary, fontSize: '12px', marginBottom: '2px' }}>{isFa ? 'نقش دسترسی:' : 'Account Role:'}</div>
            <div style={{ fontWeight: 'bold' }}>
              <span style={{ backgroundColor: theme.colors.infoLight, color: theme.colors.info, padding: '2px 8px', borderRadius: '12px', fontSize: '12px' }}>
                {authUser?.role || 'Owner'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Change Password Card */}
      <div style={{ backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}`, padding: '24px' }}>
        <h3 style={{ margin: '0 0 8px 0', fontSize: '16px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
          🔒 {isFa ? 'تغییر رمز عبور' : 'Change Password'}
        </h3>
        <p style={{ margin: '0 0 20px 0', color: theme.colors.textSecondary, fontSize: '13px' }}>
          {isFa ? 'جهت امنیت بیشتر حساب کاربری، رمز عبور خود را به صورت دوره‌ای بروزرسانی کنید.' : 'Update your account password for enhanced security.'}
        </p>

        {errorMessage && (
          <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px' }}>
            {errorMessage}
          </div>
        )}

        {successMessage && (
          <div style={{ backgroundColor: theme.colors.successLight, color: theme.colors.success, padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontWeight: 'bold' }}>
            {successMessage}
          </div>
        )}

        <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          <div>
            <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
              {isFa ? 'رمز عبور فعلی:' : 'Current Password:'} *
            </label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <input
                type={showCurrentPassword ? 'text' : 'password'}
                required
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                placeholder="••••••••"
                style={{ width: '100%', padding: '10px 12px', paddingLeft: isFa ? '12px' : '36px', paddingRight: isFa ? '36px' : '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
              />
              <button
                type="button"
                onClick={() => setShowCurrentPassword(!showCurrentPassword)}
                style={{ position: 'absolute', left: isFa ? '10px' : 'auto', right: isFa ? 'auto' : '10px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px' }}
              >
                {showCurrentPassword ? '👁️' : '🙈'}
              </button>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
                {isFa ? 'رمز عبور جدید:' : 'New Password:'} *
              </label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{ width: '100%', padding: '10px 12px', paddingLeft: isFa ? '12px' : '36px', paddingRight: isFa ? '36px' : '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  style={{ position: 'absolute', left: isFa ? '10px' : 'auto', right: isFa ? 'auto' : '10px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px' }}
                >
                  {showNewPassword ? '👁️' : '🙈'}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>
                {isFa ? 'تکرار رمز عبور جدید:' : 'Confirm New Password:'} *
              </label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{ width: '100%', padding: '10px 12px', paddingLeft: isFa ? '12px' : '36px', paddingRight: isFa ? '36px' : '12px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{ position: 'absolute', left: isFa ? '10px' : 'auto', right: isFa ? 'auto' : '10px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px' }}
                >
                  {showConfirmPassword ? '👁️' : '🙈'}
                </button>
              </div>
            </div>
          </div>

          <div style={{ marginTop: '8px' }}>
            <button
              type="submit"
              disabled={isSubmitting}
              style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '10px 20px', fontSize: '14px', fontWeight: 'bold', cursor: 'pointer' }}
            >
              {isSubmitting ? (isFa ? 'در حال ذخیره‌سازی...' : 'Saving...') : (isFa ? '💾 ذخیره رمز عبور جدید' : 'Update Password')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
