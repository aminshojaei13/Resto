import React, { useState } from 'react';
import { AppShell } from './components/AppShell';
import { TenantModal } from './components/TenantModal';
import { PosPage } from './pages/PosPage';
import { InventoryPage } from './pages/InventoryPage';
import { DashboardPage } from './pages/DashboardPage';
import { CustomersPage } from './pages/CustomersPage';
import { SuppliersPage } from './pages/SuppliersPage';
import { PurchasesPage } from './pages/PurchasesPage';
import { ExpensesPage } from './pages/ExpensesPage';
import { MessagesPage } from './pages/MessagesPage';
import { AccountingPage } from './pages/AccountingPage';
import { EmptyState } from './components/EmptyState';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState('dashboard');
  const [language, setLanguage] = useState<'fa' | 'en'>('fa');
  const [activeOrgName, setActiveOrgName] = useState('گروه بازرگانی اپکس (Apex Retail)');
  const [activeStoreName, setActiveStoreName] = useState('شعبه مرکزی (Downtown)');
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);

  const isFa = language === 'fa';

  const renderContent = () => {
    switch (activeTab) {
      case 'dashboard':
        return <DashboardPage language={language} />;
      case 'pos':
        return <PosPage language={language} />;
      case 'inventory':
      case 'products':
        return <InventoryPage language={language} />;
      case 'purchases':
        return <PurchasesPage language={language} />;
      case 'suppliers':
        return <SuppliersPage language={language} />;
      case 'customers':
        return <CustomersPage language={language} />;
      case 'expenses':
        return <ExpensesPage language={language} />;
      case 'messages':
        return <MessagesPage language={language} />;
      case 'accounting':
      case 'reports':
        return <AccountingPage language={language} />;
      default:
        return (
          <EmptyState
            title={isFa ? 'ماژول در حال آماده‌سازی' : 'Module In Development'}
            description={isFa ? `بخش ${activeTab} به زودی فعال خواهد شد.` : `The ${activeTab} module will be active shortly.`}
            actionText={isFa ? 'بازگشت به داشبورد' : 'Back to Dashboard'}
            onAction={() => setActiveTab('dashboard')}
            icon="🚧"
          />
        );
    }
  };

  return (
    <AppShell
      activeTab={activeTab}
      setActiveTab={setActiveTab}
      language={language}
      setLanguage={setLanguage}
      activeOrgName={activeOrgName}
      activeStoreName={activeStoreName}
      onOpenTenantModal={() => setIsTenantModalOpen(true)}
    >
      {renderContent()}

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
    </AppShell>
  );
};

export default App;
