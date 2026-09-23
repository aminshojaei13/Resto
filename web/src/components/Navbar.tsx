import React from 'react';

interface NavbarProps {
  activeTab: string;
  setActiveTab: (tab: string) => void;
  activeOrgName: string;
  activeStoreName: string;
  onOpenTenantModal: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  activeTab,
  setActiveTab,
  activeOrgName,
  activeStoreName,
  onOpenTenantModal,
}) => {
  const tabs = [
    { id: 'pos', label: 'Point of Sale (POS)' },
    { id: 'inventory', label: 'Inventory Catalog' },
    { id: 'dashboard', label: 'Revenue Dashboard' },
    { id: 'customers', label: 'Customers CRM' },
    { id: 'accounting', label: 'Double-Entry Ledger' },
  ];

  return (
    <header style={{ backgroundColor: '#005AC1', color: '#FFFFFF', padding: '12px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.15)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        <h1 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', fontFamily: 'system-ui, sans-serif' }}>Calcuapp SaaS</h1>
        <button
          onClick={onOpenTenantModal}
          style={{ backgroundColor: 'rgba(255,255,255,0.2)', color: '#FFFFFF', border: 'none', borderRadius: '20px', padding: '6px 14px', cursor: 'pointer', fontSize: '13px', fontWeight: '600' }}
        >
          🏢 {activeOrgName} • {activeStoreName} (Switch)
        </button>
      </div>

      <nav style={{ display: 'flex', gap: '8px' }}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            style={{
              backgroundColor: activeTab === tab.id ? 'rgba(255,255,255,0.25)' : 'transparent',
              color: '#FFFFFF',
              border: 'none',
              borderRadius: '8px',
              padding: '8px 16px',
              cursor: 'pointer',
              fontSize: '14px',
              fontWeight: activeTab === tab.id ? 'bold' : 'normal',
            }}
          >
            {tab.label}
          </button>
        ))}
      </nav>
    </header>
  );
};
