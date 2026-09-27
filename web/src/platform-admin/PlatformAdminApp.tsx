import React, { useState, useEffect } from 'react';
import { PlatformAdminShell } from '../components/PlatformAdminShell';
import { PlatformLoginPage } from '../pages/PlatformLoginPage';
import { PlatformAdminPage } from '../pages/PlatformAdminPage';
import { PlatformTenantsPage } from './pages/PlatformTenantsPage';
import { PlatformAuditPage } from './pages/PlatformAuditPage';
import { AuthUser } from '../types';

export const PlatformAdminApp: React.FC = () => {
  const [currentPath, setCurrentPath] = useState<string>(() => {
    return window.location.pathname || '/applications';
  });

  const [authUser, setAuthUser] = useState<AuthUser | null>(() => {
    const saved = localStorage.getItem('resto_auth_user');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return null;
      }
    }
    return null;
  });

  const [language, setLanguage] = useState<'fa' | 'en'>('fa');
  const isFa = language === 'fa';

  const navigate = (path: string) => {
    if (window.location.pathname !== path) {
      window.history.pushState(null, '', path);
    }
    setCurrentPath(path);
  };

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname || '/applications');
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const handleLoginSuccess = (user: AuthUser) => {
    setAuthUser(user);
    localStorage.setItem('resto_auth_user', JSON.stringify(user));
  };

  const handleLogout = () => {
    setAuthUser(null);
    localStorage.removeItem('resto_auth_user');
    navigate('/login');
  };

  // Unauthenticated Guard
  if (!authUser) {
    return <PlatformLoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />;
  }

  // Non-Platform Admin Guard (403 Authorization Boundary Check)
  if (!authUser.isPlatformAdmin) {
    return (
      <div style={{ maxWidth: '600px', margin: '80px auto', padding: '32px', backgroundColor: '#FEE2E2', border: '1px solid #FCA5A5', borderRadius: '16px', color: '#991B1B', fontFamily: 'system-ui, sans-serif', direction: isFa ? 'rtl' : 'ltr', textAlign: 'center' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>⛔</div>
        <h2 style={{ margin: '0 0 12px 0', fontSize: '22px', fontWeight: 800 }}>
          {isFa ? 'خطای دسترسی غیرمجاز راهبر پلتفرم (403 Access Denied)' : '403 Access Denied - Platform Admin Authorization Required'}
        </h2>
        <p style={{ margin: '0 0 24px 0', fontSize: '14px', lineHeight: 1.6 }}>
          {isFa
            ? 'پورت ۳۰۰۱ منحصراً ویژه راهبران پلتفرم ابری رستو است. حساب کاربری شما از نوع کاربر عادی سازمان/تننت بوده و فاقد سطح دسترسی is_platform_admin می‌باشد.'
            : 'Port 3001 is reserved exclusively for Resto Platform Operators. Your account is a tenant business user and lacks is_platform_admin authorization.'}
        </p>
        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <a
            href="http://localhost:3000/app/dashboard"
            style={{ backgroundColor: '#991B1B', color: '#FFF', textDecoration: 'none', padding: '10px 20px', borderRadius: '8px', fontWeight: 700, fontSize: '14px' }}
          >
            🏢 {isFa ? 'انتقال به پنل سازمان (localhost:3000)' : 'Go to Tenant Dashboard (localhost:3000)'}
          </a>
          <button
            onClick={handleLogout}
            style={{ backgroundColor: '#FFF', color: '#991B1B', border: '1px solid #991B1B', padding: '10px 20px', borderRadius: '8px', fontWeight: 700, fontSize: '14px', cursor: 'pointer' }}
          >
            🚪 {isFa ? 'خروج از حساب' : 'Logout'}
          </button>
        </div>
      </div>
    );
  }

  // Route Resolver for Platform Admin Application
  const path = currentPath === '/' ? '/applications' : currentPath;

  let pageContent = <PlatformAdminPage language={language} navigate={navigate} />;
  if (path === '/tenants') {
    pageContent = <PlatformTenantsPage language={language} />;
  } else if (path === '/audit') {
    pageContent = <PlatformAuditPage language={language} />;
  } else if (path === '/login') {
    pageContent = <PlatformLoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />;
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
      authUser={authUser}
      onLogout={handleLogout}
    >
      {pageContent}
    </PlatformAdminShell>
  );
};

export default PlatformAdminApp;
