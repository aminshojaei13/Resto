import React, { useState } from 'react';
import { Navbar } from './components/Navbar';
import { TenantModal } from './components/TenantModal';
import { PosPage } from './pages/PosPage';
import { InventoryPage } from './pages/InventoryPage';
import { DashboardPage } from './pages/DashboardPage';
import { CustomersPage } from './pages/CustomersPage';
import { AccountingPage } from './pages/AccountingPage';

export const App: React.FC = () => {
  const [activeTab, setActiveTab] = useState('pos');
  const [language, setLanguage] = useState<'fa' | 'en'>('fa');
  const [activeOrgName, setActiveOrgName] = useState('گروه بازرگانی اپکس (Apex Retail)');
  const [activeStoreName, setActiveStoreName] = useState('شعبه مرکزی (Downtown)');
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);

  const isRtl = language === 'fa';

  return (
    <div dir={isRtl ? 'rtl' : 'ltr'} style={{ backgroundColor: '#FEFBFD', minHeight: '100vh', direction: isRtl ? 'rtl' : 'ltr' }}>
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        language={language}
        setLanguage={setLanguage}
        activeOrgName={activeOrgName}
        activeStoreName={activeStoreName}
        onOpenTenantModal={() => setIsTenantModalOpen(true)}
      />

      <main style={{ maxWidth: '1280px', margin: '0 auto' }}>
        {activeTab === 'pos' && <PosPage language={language} />}
        {activeTab === 'inventory' && <InventoryPage language={language} />}
        {activeTab === 'dashboard' && <DashboardPage language={language} />}
        {activeTab === 'customers' && <CustomersPage language={language} />}
        {activeTab === 'accounting' && <AccountingPage language={language} />}
      </main>

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
    </div>
  );
};

export default App;
