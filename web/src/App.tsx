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
  const [activeOrgName, setActiveOrgName] = useState('Apex Retail Group');
  const [activeStoreName, setActiveStoreName] = useState('Apex Flagship');
  const [isTenantModalOpen, setIsTenantModalOpen] = useState(false);

  return (
    <div style={{ backgroundColor: '#FEFBFD', minHeight: '100vh' }}>
      <Navbar
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        activeOrgName={activeOrgName}
        activeStoreName={activeStoreName}
        onOpenTenantModal={() => setIsTenantModalOpen(true)}
      />

      <main style={{ maxWidth: '1280px', margin: '0 auto' }}>
        {activeTab === 'pos' && <PosPage />}
        {activeTab === 'inventory' && <InventoryPage />}
        {activeTab === 'dashboard' && <DashboardPage />}
        {activeTab === 'customers' && <CustomersPage />}
        {activeTab === 'accounting' && <AccountingPage />}
      </main>

      {isTenantModalOpen && (
        <TenantModal
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
