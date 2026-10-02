import React, { useCallback, useEffect, useState } from 'react';
import { PublicShell } from '../components/PublicShell';
import { TenantAppShell } from '../components/TenantAppShell';
import { TenantModal } from '../components/TenantModal';
import { AuthProvider, useAuth } from '../auth/AuthContext';
import { getBusinessContext } from '../auth/session';
import { setUnauthorizedHandler } from '../api/apiClient';
import { PublicRegisterPage } from '../pages/PublicRegisterPage';
import { LoginPage } from '../pages/LoginPage';
import { ForgotPasswordPage } from '../pages/ForgotPasswordPage';
import { ResetPasswordPage } from '../pages/ResetPasswordPage';
import { AcceptInvitationPage } from '../pages/AcceptInvitationPage';
import { ProfilePage } from '../pages/ProfilePage';
import { StaffPage } from '../pages/StaffPage';
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
import { useTheme } from '../theme/ThemeContext';
import { Language } from '../i18n/authStrings';

const BusinessAppRoutes: React.FC = () => {
  const [currentPath, setCurrentPath] = useState<string>(() => window.location.pathname || '/');
  const [language, setLanguage] = useState<Language>('fa');
  const [isBusinessModalOpen, setIsBusinessModalOpen] = useState(false);

  const { user, isRestoring, signOut, can } = useAuth();
  const { theme, effectiveMode } = useTheme();
  const isFa = language === 'fa';
  const isDark = effectiveMode === 'warmDark';

  // Re-read whenever the path changes so a switch made in the modal is
  // reflected immediately.
  const [context, setContext] = useState(() => getBusinessContext());
  useEffect(() => setContext(getBusinessContext()), [currentPath, user]);

  const navigate = useCallback((path: string) => {
    if (window.location.pathname !== path) {
      window.history.pushState(null, '', path);
    }
    setCurrentPath(path);
  }, []);

  useEffect(() => {
    const handlePopState = () => setCurrentPath(window.location.pathname || '/');
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  // An expired or revoked session sends the person back to the sign-in screen
  // with nothing of the previous person's identity left on display.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      void signOut();
      setCurrentPath('/login');
    });
    return () => setUnauthorizedHandler(null);
  }, [signOut]);

  // The person is signed in but their session ended: return to sign in.
  useEffect(() => {
    if (!isRestoring && !user && currentPath.startsWith('/app/')) {
      setCurrentPath('/login');
    }
  }, [isRestoring, user, currentPath]);

  const path = currentPath === '/' ? '' : currentPath;

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

  /* ------------------------------------------------------------- public */

  if (!user) {
    const renderPublic = () => {
      switch (path) {
        case '/login':
          return <LoginPage language={language} navigate={navigate} />;
        case '/forgot-password':
          return <ForgotPasswordPage language={language} navigate={navigate} />;
        case '/reset-password':
          return <ResetPasswordPage language={language} navigate={navigate} />;
        case '/accept-invitation':
          return <AcceptInvitationPage language={language} navigate={navigate} />;
        case '/activation':
          return <ActivationPage language={language} navigate={navigate} />;
        default:
          return <PublicRegisterPage language={language} navigate={navigate} />;
      }
    };

    return (
      <PublicShell
        currentPath={path || '/register'}
        navigate={navigate}
        language={language}
        setLanguage={setLanguage}
      >
        {renderPublic()}
      </PublicShell>
    );
  }

  /* ------------------------------------------------------------ signed in */

  const businessName = user.memberships.find((m) => m.id === context.organizationId)?.name ?? '';
  const store = user.memberships.flatMap((m) => m.stores).find((s) => s.id === context.storeId);
  const warehouse = store?.warehouses.find((w) => w.id === context.warehouseId);

  const renderModule = () => {
    switch (path) {
      case '/app/pos':
        return <PosPage language={language} />;
      case '/app/inventory':
      case '/app/products':
        return <InventoryPage language={language} />;
      case '/app/purchases':
        return <PurchasesPage language={language} />;
      case '/app/suppliers':
        return <SuppliersPage language={language} />;
      case '/app/customers':
        return <CustomersPage language={language} />;
      case '/app/expenses':
        return <ExpensesPage language={language} />;
      case '/app/messages':
        return <MessagesPage language={language} />;
      case '/app/accounting':
      case '/app/reports':
        return <AccountingPage language={language} />;
      case '/app/profile':
        return <ProfilePage language={language} />;
      case '/app/staff':
        return can('staff.view') ? (
          <StaffPage language={language} />
        ) : (
          <AccessDenied language={language} />
        );
      default:
        return <DashboardPage language={language} />;
    }
  };

  const handleSignOut = async () => {
    await signOut();
    navigate('/login');
  };

  return (
    <TenantAppShell
      currentPath={path || '/app/dashboard'}
      navigate={navigate}
      language={language}
      setLanguage={setLanguage}
      activeOrgName={businessName}
      activeStoreName={store?.name ?? ''}
      activeWarehouseName={warehouse?.name ?? ''}
      onOpenBusinessModal={() => setIsBusinessModalOpen(true)}
      currentUserName={user.displayName || user.email}
      onLogout={handleSignOut}
    >
      {renderModule()}

      {isBusinessModalOpen && (
        <TenantModal
          language={language}
          onClose={() => setIsBusinessModalOpen(false)}
          onSelected={() => {
            setIsBusinessModalOpen(false);
            setCurrentPath('/app/dashboard');
          }}
        />
      )}
    </TenantAppShell>
  );
};

const AccessDenied: React.FC<{ language: Language }> = ({ language }) => {
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  return (
    <div
      style={{
        padding: '32px',
        textAlign: 'center',
        borderRadius: '12px',
        backgroundColor: isDark ? theme.colors.errorLight : '#FEF2F2',
        color: isDark ? theme.colors.error : '#B91C1C',
        fontFamily: theme.typography.fontFamily,
        direction: language === 'fa' ? 'rtl' : 'ltr',
      }}
    >
      {language === 'fa'
        ? 'شما اجازه دسترسی به این بخش را ندارید.'
        : 'You do not have permission to view this section.'}
    </div>
  );
};

export const BusinessApp: React.FC = () => (
  <AuthProvider>
    <BusinessAppRoutes />
  </AuthProvider>
);

export default BusinessApp;
