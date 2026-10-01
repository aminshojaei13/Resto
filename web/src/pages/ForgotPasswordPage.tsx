import React, { useState } from 'react';
import { apiClient } from '../api/apiClient';
import { useTheme } from '../theme/ThemeContext';

interface ForgotPasswordPageProps {
  language?: 'fa' | 'en';
  navigate: (path: string) => void;
}

export const ForgotPasswordPage: React.FC<ForgotPasswordPageProps> = ({
  language = 'fa',
  navigate,
}) => {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [infoMessage, setActionMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  // Step 1: Request Reset Code
  const handleRequestCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email) {
      setErrorMessage(isFa ? 'لطفا ایمیل کاری خود را وارد کنید.' : 'Please enter your email.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setActionMessage('');

    try {
      const res = await apiClient.forgotPassword(email);
      setIsSubmitting(false);
      if (res.reset_code) {
        setCode(res.reset_code); // Prefill demo reset code for ease of testing
      }
      setActionMessage(res.message || (isFa ? 'کد بازیابی صادر گردید.' : 'Reset code generated.'));
      setStep(2);
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در درخواست کد بازیابی' : 'Failed to request reset code'));
    }
  };

  // Step 2: Reset Password
  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!code || !newPassword || !confirmPassword) {
      setErrorMessage(isFa ? 'لطفا تمامی فیلدها را تکمیل نمایید.' : 'Please fill in all fields.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage(isFa ? 'رمز عبور جدید و تکرار آن یکسان نیستند.' : 'Passwords do not match.');
      return;
    }

    if (newPassword.length < 6) {
      setErrorMessage(isFa ? 'رمز عبور باید حداقل ۶ کاراکتر باشد.' : 'Password must be at least 6 characters.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const res = await apiClient.resetPassword({
        email,
        code,
        password: newPassword,
        password_confirmation: confirmPassword,
      });

      setIsSubmitting(false);
      setActionMessage(res.message || (isFa ? 'رمز عبور شما با موفقیت تغییر یافت.' : 'Password reset successfully.'));
      setStep(3);
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در تغییر رمز عبور.' : 'Failed to reset password.'));
    }
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
          color: theme.colors.textPrimary,
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: theme.spacing['2xl'] }}>
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: theme.borderRadius.xl,
              backgroundColor: theme.colors.warningLight,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: theme.colors.warning,
              fontSize: '28px',
              margin: '0 auto 16px auto',
            }}
          >
            🔓
          </div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: theme.colors.textPrimary, margin: '0 0 8px 0' }}>
            {isFa ? 'بازیابی و بازنشانی رمز عبور' : 'Reset Your Password'}
          </h2>
          <p style={{ color: theme.colors.textSecondary, fontSize: '14px', margin: 0 }}>
            {isFa ? 'بازیابی امن حساب کاربری مالک و پرسنل فروشگاه' : 'Enter your email to receive password reset instructions'}
          </p>
        </div>

        {errorMessage && (
          <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px' }}>
            {errorMessage}
          </div>
        )}

        {infoMessage && (
          <div style={{ backgroundColor: theme.colors.infoLight, color: theme.colors.info, padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontWeight: 'bold' }}>
            {infoMessage}
          </div>
        )}

        {/* Step 1: Request Code */}
        {step === 1 && (
          <form onSubmit={handleRequestCode} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg }}>
            <div>
              <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px' }}>
                {isFa ? 'ایمیل کاری (حساب کاربری):' : 'Work Email:'} *
              </label>
              <input
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="reza@grandcoffee.com"
                style={{ width: '100%', padding: theme.spacing.md, borderRadius: theme.borderRadius.lg, border: `1px solid ${theme.colors.border}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg, fontSize: '16px', fontWeight: 700, cursor: 'pointer', marginTop: '8px' }}
            >
              {isSubmitting ? (isFa ? 'در حال ارسال درخواست...' : 'Sending...') : (isFa ? '📩 دریافت کد بازیابی ۶ رقمی' : 'Send Reset Code')}
            </button>
          </form>
        )}

        {/* Step 2: Enter Code & New Password */}
        {step === 2 && (
          <form onSubmit={handleResetPassword} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg }}>
            <div>
              <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px' }}>
                {isFa ? 'کد بازیابی ۶ رقمی:' : '6-Digit Verification Code:'} *
              </label>
              <input
                type="text"
                required
                maxLength={6}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                placeholder="123456"
                style={{ width: '100%', padding: theme.spacing.md, borderRadius: theme.borderRadius.lg, border: `1px solid ${theme.colors.border}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '16px', letterSpacing: '4px', textAlign: 'center', fontWeight: 'bold', boxSizing: 'border-box' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px' }}>
                {isFa ? 'رمز عبور جدید:' : 'New Password:'} *
              </label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <input
                  type={showNewPassword ? 'text' : 'password'}
                  required
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{ width: '100%', padding: theme.spacing.md, paddingLeft: isFa ? '12px' : '40px', paddingRight: isFa ? '40px' : '12px', borderRadius: theme.borderRadius.lg, border: `1px solid ${theme.colors.border}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
                />
                <button
                  type="button"
                  onClick={() => setShowNewPassword(!showNewPassword)}
                  style={{ position: 'absolute', left: isFa ? '12px' : 'auto', right: isFa ? 'auto' : '12px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
                >
                  {showNewPassword ? '👁️' : '🙈'}
                </button>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px' }}>
                {isFa ? 'تکرار رمز عبور جدید:' : 'Confirm New Password:'} *
              </label>
              <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  placeholder="••••••••"
                  style={{ width: '100%', padding: theme.spacing.md, paddingLeft: isFa ? '12px' : '40px', paddingRight: isFa ? '40px' : '12px', borderRadius: theme.borderRadius.lg, border: `1px solid ${theme.colors.border}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', boxSizing: 'border-box' }}
                />
                <button
                  type="button"
                  onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                  style={{ position: 'absolute', left: isFa ? '12px' : 'auto', right: isFa ? 'auto' : '12px', background: 'none', border: 'none', cursor: 'pointer', fontSize: '18px' }}
                >
                  {showConfirmPassword ? '👁️' : '🙈'}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              style={{ backgroundColor: theme.colors.success, color: '#0A2E1E', border: 'none', borderRadius: theme.borderRadius.lg, padding: theme.spacing.lg, fontSize: '16px', fontWeight: 700, cursor: 'pointer', marginTop: '8px' }}
            >
              {isSubmitting ? (isFa ? 'در حال بروزرسانی...' : 'Updating...') : (isFa ? '🔑 ثبت رمز عبور جدید' : 'Set New Password')}
            </button>
          </form>
        )}

        {/* Step 3: Success Confirmation */}
        {step === 3 && (
          <div style={{ textAlign: 'center', padding: '16px 0' }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎉</div>
            <h3 style={{ color: theme.colors.success, fontSize: '18px', fontWeight: 'bold', margin: '0 0 12px 0' }}>
              {isFa ? 'رمز عبور شما با موفقیت تغییر یافت!' : 'Password Changed Successfully!'}
            </h3>
            <p style={{ color: theme.colors.textSecondary, fontSize: '14px', marginBottom: '24px' }}>
              {isFa ? 'اکنون می‌توانید با ایمیل و رمز عبور جدید وارد حساب کاربری خود شوید.' : 'You can now log in with your email and new password.'}
            </p>
            <button
              onClick={() => navigate('/login')}
              style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '12px 24px', fontSize: '15px', fontWeight: 'bold', cursor: 'pointer', width: '100%' }}
            >
              🔑 {isFa ? 'انتقال به صفحه ورود' : 'Go to Login'}
            </button>
          </div>
        )}

        {/* Footer Navigation */}
        <div style={{ marginTop: theme.spacing.xl, textAlign: 'center', fontSize: '13px', paddingTop: theme.spacing.lg, borderTop: `1px solid ${theme.colors.border}` }}>
          <button
            type="button"
            onClick={() => navigate('/login')}
            style={{ border: 'none', background: 'none', color: theme.colors.primary, cursor: 'pointer', fontWeight: 600 }}
          >
            {isFa ? 'بازگشت به صفحه ورود' : 'Back to Login'}
          </button>
        </div>
      </div>
    </div>
  );
};
