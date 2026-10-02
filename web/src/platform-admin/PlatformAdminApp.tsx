import React, { useCallback, useEffect, useState } from 'react';
import { PlatformAdminShell } from '../components/PlatformAdminShell';
import { PlatformLoginPage } from '../pages/PlatformLoginPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { PlatformAdminPage } from '../pages/PlatformAdminPage';
import { PlatformTenantsPage } from './pages/PlatformTenantsPage';
import { PlatformAuditPage } from './pages/PlatformAuditPage';
import { AuthProvider, useAuth } from '../auth/AuthContext';
import { setUnauthorizedHandler } from '../api/apiClient';
import { useTheme } from '../theme/ThemeContext';
import { Language } from '../i18n/authStrings';

const PlatformRoutes: React.FC = () => {
  const [currentPath, setCurrentPath] = useState<string>(() => window.location.pathname || '/applications');
  const [language, setLanguage] = useState<Language>('fa');

  const { user, isRestoring, isPlatformAdmin, signOut } = useAuth();
  const { theme, effectiveMode } = useTheme();
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  const navigate = useCallback((path: string) => {
    if (window.location.pathname !== path) {
      window.history.pushState(null, '', path);
    }
    setCurrentPath(path);
  }, []);

  useEffect(() => {
    const handlePopState = () => setCurrentPath(window.location.pathname || '/applications');
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(() => {
      void signOut();
      setCurrentPath('/login');
    });
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  useEffect(() => {
    if (!isRestoring && !user && currentPath !== '/login' && currentPath !== '/forgot-password' && currentPath !== '/reset-password') {
      setCurrentPath('/login');
    }
  }, [isRestoring, user, currentPath]);

  if (isRestoring) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: theme.colors.background,
          color: theme.colors.textSecondary,
          fontFamily: theme.typography.fontFamily,
        }}
      >
        …
      </div>
    );
  }

  if (!user) {
    if (currentPath === '/forgot-password') {
      return <ForgotPasswordPage language={language} navigate={navigate} />;
    }

    if (currentPath === '/reset-password') {
      return <ResetPasswordPage language={language} navigate={navigate} />;
    }

    return <PlatformLoginPage language={language} onLoginSuccess={() => navigate('/applications')} navigate={navigate} />;
  }

  const handleLogout = async () => {
    await signOut();
    navigate('/login');
  };

  // The flag comes from the authenticated profile, so it reflects what the
  // server says about this account and nothing the browser can set itself.
  if (!isPlatformAdmin) {
    return (
      <div
        style={{
          maxWidth: '600px',
          margin: '80px auto',
          padding: '32px',
          backgroundColor: isDark ? theme.colors.errorLight : '#FEE2E2',
          border: `1px solid ${isDark ? theme.colors.error : '#FCA5A5'}`,
          borderRadius: '16px',
          color: isDark ? theme.colors.error : '#991B1B',
          fontFamily: theme.typography.fontFamily,
          direction: isFa ? 'rtl' : 'ltr',
          textAlign: 'center',
        }}
      >
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>⛔</div>
        <h2 style={{ margin: '0 0 12px 0', fontSize: '22px', fontWeight: 800 }}>
          {isFa ? 'دسترسی غیرمجاز' : 'Access denied'}
        </h2>
        <p style={{ margin: '0 0 24px 0', fontSize: '14px', lineHeight: 1.6 }}>
          {isFa
            ? 'این بخش فقط برای مدیران سامانه است. حساب کاربری شما دسترسی لازم را ندارد.'
            : 'This console is for system administrators only. Your account does not have the required access.'}
        </p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <a
            href="http://localhost:3000/app/dashboard"
            style={{
              backgroundColor: theme.colors.primary,
              color: '#FFF',
              textDecoration: 'none',
              padding: '10px 20px',
              borderRadius: '8px',
              fontWeight: 700,
              fontSize: '14px',
            }}
          >
            🏢 {isFa ? 'رفتن به پنل کسب‌وکار' : 'Go to the business app'}
          </a>
          <button
            type="button"
            onClick={handleLogout}
            style={{
              backgroundColor: 'transparent',
              color: isDark ? theme.colors.error : '#991B1B',
              border: `1px solid ${isDark ? theme.colors.error : '#991B1B'}`,
              padding: '10px 20px',
              borderRadius: '8px',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            🚪 {isFa ? 'خروج از حساب' : 'Sign out'}
          </button>
        </div>
      </div>
    );
  }

  const path = currentPath === '/' ? '/applications' : currentPath;

  let pageContent = <PlatformAdminPage language={language} navigate={navigate} />;
  if (path === '/tenants') {
    pageContent = <PlatformTenantsPage language={language} />;
  } else if (path === '/audit') {
    pageContent = <PlatformAuditPage language={language} />;
  } else if (path.startsWith('/applications/')) {
    const appId = path.replace('/applications/', '');
    pageContent = <PlatformAdminPage language={language} selectedApplicationId={appId} navigate={navigate} />;
  }

  return (
    <PlatformAdminShell
      currentPath={path}
      navigate={navigate}
      language={language}
      setLanguage={setLanguage}
      currentUserName={user.displayName || user.email}
      onLogout={handleLogout}
    >
      {pageContent}
    </PlatformAdminShell>
  );
};

export const PlatformAdminApp: React.FC = () => (
  <AuthProvider>
    <PlatformRoutes />
  </AuthProvider>
);

export default PlatformAdminApp;
