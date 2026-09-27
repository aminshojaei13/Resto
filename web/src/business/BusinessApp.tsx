import React, { useState, useEffect } from 'react';
import { PublicShell } from '../components/PublicShell';
import { TenantAppShell } from '../components/TenantAppShell';
import { TenantModal } from '../components/TenantModal';
import { PublicRegisterPage } from '../pages/PublicRegisterPage';
import { LoginPage } from '../pages/LoginPage';
import { ActivationPage } from '../pages/ActivationPage';
import { DashboardPage } from '../pages/DashboardPage';
import { PosPage } from '../pages/PosPage';
import { InventoryPage } from '../pages/InventoryPage';
import { PurchasesPage } from '../pages/PurchasesPage';
import { SuppliersPage } from '../pages/SuppliersPage';
import { CustomersPage } from '../pages/CustomersPage';
import { ExpensesPage } from '../pages/ExpensesPage';
import { MessagesPage } from '../pages/MessagesPage';
import { AccountingPage } from '../pages/AccountingPage';
import { EmptyState } from '../components/EmptyState';
import { AuthUser } from '../types';

export const BusinessApp: React.FC = () => {
  const [currentPath, setCurrentPath] = useState<string>(() => {
    return window.location.pathname || '/';
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
  const [activeOrgName, setActiveOrgName] = useState('کسب‌وکار من');
  const [activeStoreName, setActiveStoreName] = useState('شعبه اصلی');
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);

  const isFa = language === 'fa';

  const navigate = (path: string) => {
    if (window.location.pathname !== path) {
      window.history.pushState(null, '', path);
    }
    setCurrentPath(path);
  };

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname || '/');
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

  const path = currentPath === '/' ? '' : currentPath;

  // Separation Notice for Legacy Platform Admin Routes on Business Web
  if (path.startsWith('/platform') || path.startsWith('/admin')) {
    return (
      <PublicShell currentPath="/login" navigate={navigate} language={language} setLanguage={setLanguage}>
        <div style={{ maxWidth: '600px', margin: '60px auto', padding: '32px', backgroundColor: '#EFF6FF', border: '1px solid #BFDBFE', borderRadius: '16px', color: '#1E40AF', fontFamily: 'system-ui, sans-serif', direction: isFa ? 'rtl' : 'ltr', textAlign: 'center' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>🛡️</div>
          <h2 style={{ margin: '0 0 12px 0', fontSize: '22px', fontWeight: 800 }}>
            {isFa ? 'انتقال بخش مدیریت پلتفرم به سامانه اپراتور (Operator Console)' : 'Platform Admin Console Moved to Port 3001'}
          </h2>
          <p style={{ margin: '0 0 24px 0', fontSize: '14px', lineHeight: 1.6 }}>
            {isFa
              ? 'این بخش (پورت ۳۰۰۰) منحصراً ویژه برنامه کسب‌وکار و تننت‌ها است. کنسول مدیریت پلتفرم و بررسی درخواست‌ها به سامانه مجزای راهبری انتقال یافته است.'
              : 'This site (port 3000) is the Business Web Application. Platform Administration & Application Review has been moved to a separate runtime on port 3001.'}
          </p>
          <a
            href="http://localhost:3001/login"
            style={{ backgroundColor: '#0284C7', color: '#FFF', textDecoration: 'none', padding: '12px 24px', borderRadius: '8px', fontWeight: 700, fontSize: '15px', display: 'inline-block' }}
          >
            🚀 {isFa ? 'ورود به پنل راهبری پلتفرم (localhost:3001)' : 'Launch Operator Console (localhost:3001)'}
          </a>
        </div>
      </PublicShell>
    );
  }

  // 1. Root Route '/' Resolver
  if (path === '' || path === '/') {
    if (!authUser) {
      return (
        <PublicShell currentPath="/register" navigate={navigate} language={language} setLanguage={setLanguage}>
          <PublicRegisterPage language={language} navigate={navigate} />
        </PublicShell>
      );
    }
    return (
      <TenantAppShell
        currentPath="/app/dashboard"
        navigate={navigate}
        language={language}
        setLanguage={setLanguage}
        activeOrgName={activeOrgName}
        activeStoreName={activeStoreName}
        onOpenTenantModal={() => setIsTenantModalOpen(true)}
        authUser={authUser}
        onLogout={handleLogout}
      >
        <DashboardPage language={language} />
        {isTenantModalOpen && (
          <TenantModal
            language={language}
            onClose={() => setIsTenantModalOpen(false)}
            onSelectTenant={(org, store) => {
              setActiveOrgName(org);
              setActiveStoreName(store);
            }}
          />
        )}
      </TenantAppShell>
    );
  }

  // 2. Public Experience Routes
  if (path === '/register' || path === '/login' || path === '/activation') {
    let pageContent = <PublicRegisterPage language={language} navigate={navigate} />;
    if (path === '/login') {
      pageContent = <LoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />;
    } else if (path === '/activation') {
      pageContent = <ActivationPage language={language} navigate={navigate} />;
    }

    return (
      <PublicShell currentPath={path} navigate={navigate} language={language} setLanguage={setLanguage}>
        {pageContent}
      </PublicShell>
    );
  }

  // 3. Tenant Experience Routes (/app/*)
  if (path.startsWith('/app/')) {
    if (!authUser) {
      return (
        <PublicShell currentPath="/login" navigate={navigate} language={language} setLanguage={setLanguage}>
          <LoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />;
        </PublicShell>
      );
    }

    let tenantContent = <DashboardPage language={language} />;
    if (path === '/app/pos') {
      tenantContent = <PosPage language={language} />;
    } else if (path === '/app/inventory' || path === '/app/products') {
      tenantContent = <InventoryPage language={language} />;
    } else if (path === '/app/purchases') {
      tenantContent = <PurchasesPage language={language} />;
    } else if (path === '/app/suppliers') {
      tenantContent = <SuppliersPage language={language} />;
    } else if (path === '/app/customers') {
      tenantContent = <CustomersPage language={language} />;
    } else if (path === '/app/expenses') {
      tenantContent = <ExpensesPage language={language} />;
    } else if (path === '/app/messages') {
      tenantContent = <MessagesPage language={language} />;
    } else if (path === '/app/accounting' || path === '/app/reports') {
      tenantContent = <AccountingPage language={language} />;
    } else if (path === '/app/settings') {
      tenantContent = (
        <EmptyState
          title={isFa ? 'تنظیمات کسب‌وکار' : 'Settings'}
          description={isFa ? 'پیکربندی حسابداری، ارز پایه و جزئیات فروشگاه' : 'Configure store currency, accounts, and profile'}
          actionText={isFa ? 'بازگشت به داشبورد' : 'Back to Dashboard'}
          onAction={() => navigate('/app/dashboard')}
          icon="⚙️"
        />
      );
    }

    return (
      <TenantAppShell
        currentPath={path}
        navigate={navigate}
        language={language}
        setLanguage={setLanguage}
        activeOrgName={activeOrgName}
        activeStoreName={activeStoreName}
        onOpenTenantModal={() => setIsTenantModalOpen(true)}
        authUser={authUser}
        onLogout={handleLogout}
      >
        {tenantContent}

        {isTenantModalOpen && (
          <TenantModal
            language={language}
            onClose={() => setIsTenantModalOpen(false)}
            onSelectTenant={(org, store) => {
              setActiveOrgName(org);
              setActiveStoreName(store);
            }}
          />
        )}
      </TenantAppShell>
    );
  }

  // 4. Fallback for Unknown Routes -> Register / Dashboard
  if (!authUser) {
    return (
      <PublicShell currentPath="/register" navigate={navigate} language={language} setLanguage={setLanguage}>
        <PublicRegisterPage language={language} navigate={navigate} />
      </PublicShell>
    );
  }

  return (
    <TenantAppShell
      currentPath="/app/dashboard"
      navigate={navigate}
      language={language}
      setLanguage={setLanguage}
      activeOrgName={activeOrgName}
      activeStoreName={activeStoreName}
      onOpenTenantModal={() => setIsTenantModalOpen(true)}
      authUser={authUser}
      onLogout={handleLogout}
    >
      <DashboardPage language={language} />
    </TenantAppShell>
  );
};

export default BusinessApp;
