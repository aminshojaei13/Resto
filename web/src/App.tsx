import React, { useState, useEffect } from 'react';
import { PublicShell } from './components/PublicShell';
import { PlatformAdminShell } from './components/PlatformAdminShell';
import { TenantAppShell } from './components/TenantAppShell';
import { TenantModal } from './components/TenantModal';
import { PublicRegisterPage } from './pages/PublicRegisterPage';
import { LoginPage } from './pages/LoginPage';
import { PlatformLoginPage } from './pages/PlatformLoginPage';
import { ActivationPage } from './pages/ActivationPage';
import { PlatformAdminPage } from './pages/PlatformAdminPage';
import { DashboardPage } from './pages/DashboardPage';
import { PosPage } from './pages/PosPage';
import { InventoryPage } from './pages/InventoryPage';
import { PurchasesPage } from './pages/PurchasesPage';
import { SuppliersPage } from './pages/SuppliersPage';
import { CustomersPage } from './pages/CustomersPage';
import { ExpensesPage } from './pages/ExpensesPage';
import { MessagesPage } from './pages/MessagesPage';
import { AccountingPage } from './pages/AccountingPage';
import { EmptyState } from './components/EmptyState';
import { AuthUser } from './types';

export const App: React.FC = () => {
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

  // Navigation Helper
  const navigate = (path: string) => {
    if (window.location.pathname !== path) {
      window.history.pushState(null, '', path);
    }
    setCurrentPath(path);
  };

  // Sync with browser back/forward history buttons
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

  // Route & Guard Resolver
  const path = currentPath === '/' ? '' : currentPath;

  // 1. Root Route '/' Resolver
  if (path === '' || path === '/') {
    if (!authUser) {
      return (
        <PublicShell currentPath="/register" navigate={navigate} language={language} setLanguage={setLanguage}>
          <PublicRegisterPage language={language} navigate={navigate} />
        </PublicShell>
      );
    }
    if (authUser.isPlatformAdmin) {
      return (
        <PlatformAdminShell
          currentPath="/platform/applications"
          navigate={navigate}
          language={language}
          setLanguage={setLanguage}
          authUser={authUser}
          onLogout={handleLogout}
        >
          <PlatformAdminPage language={language} navigate={navigate} />
        </PlatformAdminShell>
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
  if (path === '/register' || path === '/login' || path === '/platform/login' || path === '/activation') {
    let pageContent = <PublicRegisterPage language={language} navigate={navigate} />;
    if (path === '/login') {
      pageContent = <LoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />;
    } else if (path === '/platform/login') {
      pageContent = <PlatformLoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />;
    } else if (path === '/activation') {
      pageContent = <ActivationPage language={language} navigate={navigate} />;
    }

    return (
      <PublicShell currentPath={path} navigate={navigate} language={language} setLanguage={setLanguage}>
        {pageContent}
      </PublicShell>
    );
  }

  // 3. Platform Admin Experience Routes (/platform/*)
  if (path.startsWith('/platform/')) {
    // Route Guard 1: Unauthenticated -> Redirect to /platform/login
    if (!authUser) {
      return (
        <PublicShell currentPath="/platform/login" navigate={navigate} language={language} setLanguage={setLanguage}>
          <PlatformLoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />
        </PublicShell>
      );
    }

    // Route Guard 2: Tenant User -> Denied / Redirect to /app/dashboard
    if (!authUser.isPlatformAdmin) {
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
          <div style={{ backgroundColor: '#FEE2E2', border: '1px solid #FCA5A5', padding: '20px', borderRadius: '12px', color: '#991B1B', textAlign: 'center', margin: '40px auto', maxWidth: '600px' }}>
            <h3 style={{ margin: '0 0 8px 0', fontSize: '18px' }}>⛔ {isFa ? 'دسترسی غیرمجاز (Access Denied)' : 'Access Denied'}</h3>
            <p style={{ margin: '0 0 16px 0', fontSize: '14px' }}>
              {isFa
                ? 'شما مجاز به مشاهده پنل راهبری پلتفرم نیستید. به پنل سازمان هدایت شدید.'
                : 'You are not authorized to view the Platform Admin console. Redirected to your tenant dashboard.'}
            </p>
            <button
              onClick={() => navigate('/app/dashboard')}
              style={{ backgroundColor: '#991B1B', color: '#FFF', border: 'none', borderRadius: '8px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
            >
              {isFa ? 'بازگشت به داشبورد سازمان' : 'Return to Tenant Dashboard'}
            </button>
          </div>
          <DashboardPage language={language} />
        </TenantAppShell>
      );
    }

    // Authorized Platform Admin
    const appId = path.startsWith('/platform/applications/') ? path.replace('/platform/applications/', '') : undefined;

    return (
      <PlatformAdminShell
        currentPath={path}
        navigate={navigate}
        language={language}
        setLanguage={setLanguage}
        authUser={authUser}
        onLogout={handleLogout}
      >
        <PlatformAdminPage language={language} selectedApplicationId={appId} navigate={navigate} />
      </PlatformAdminShell>
    );
  }

  // 4. Tenant Experience Routes (/app/*)
  if (path.startsWith('/app/')) {
    // Route Guard 1: Unauthenticated -> Redirect to /login
    if (!authUser) {
      return (
        <PublicShell currentPath="/login" navigate={navigate} language={language} setLanguage={setLanguage}>
          <LoginPage language={language} onLoginSuccess={handleLoginSuccess} navigate={navigate} />
        </PublicShell>
      );
    }

    // Route Guard 2: Platform Admin -> Redirect to /platform/applications
    if (authUser.isPlatformAdmin) {
      return (
        <PlatformAdminShell
          currentPath="/platform/applications"
          navigate={navigate}
          language={language}
          setLanguage={setLanguage}
          authUser={authUser}
          onLogout={handleLogout}
        >
          <div style={{ backgroundColor: '#E0F2FE', border: '1px solid #7DD3FC', padding: '16px', borderRadius: '12px', color: '#0369A1', textAlign: 'center', marginBottom: '20px' }}>
            ℹ️ {isFa ? 'حساب شما از نوع راهبر پلتفرم است. به بخش مدیریت پلتفرم هدایت شدید.' : 'Logged in as Platform Admin. Redirected to Platform Applications.'}
          </div>
          <PlatformAdminPage language={language} navigate={navigate} />
        </PlatformAdminShell>
      );
    }

    // Render Tenant Business Module
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

  // 5. Fallback for Unknown Routes -> Route to Root Helper
  if (!authUser) {
    return (
      <PublicShell currentPath="/register" navigate={navigate} language={language} setLanguage={setLanguage}>
        <PublicRegisterPage language={language} navigate={navigate} />
      </PublicShell>
    );
  }
  if (authUser.isPlatformAdmin) {
    return (
      <PlatformAdminShell
        currentPath="/platform/applications"
        navigate={navigate}
        language={language}
        setLanguage={setLanguage}
        authUser={authUser}
        onLogout={handleLogout}
      >
        <PlatformAdminPage language={language} navigate={navigate} />
      </PlatformAdminShell>
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

export default App;
