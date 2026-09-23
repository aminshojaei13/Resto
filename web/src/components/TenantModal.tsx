import React, { useState, useEffect } from 'react';
import { apiClient, setTenantContext } from '../api/apiClient';
import { Organization } from '../types';

interface TenantModalProps {
  onClose: () => void;
  onSelectTenant: (orgName: string, storeName: string) => void;
}

export const TenantModal: React.FC<TenantModalProps> = ({ onClose, onSelectTenant }) => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);

  useEffect(() => {
    apiClient.getOrganizations().then(setOrganizations);
  }, []);

  const handleSelect = (org: Organization, storeId: string, storeName: string) => {
    setTenantContext(org.id, storeId);
    onSelectTenant(org.name, storeName);
    onClose();
  };

  return (
    <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
      <div style={{ backgroundColor: '#FFF', borderRadius: '16px', padding: '24px', width: '450px', maxWidth: '90%' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '20px', fontWeight: 'bold' }}>Switch Tenant & Store Branch</h3>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '350px', overflowY: 'auto' }}>
          {organizations.map((org) => (
            <div key={org.id} style={{ border: '1px solid #E0E0E0', borderRadius: '8px', padding: '12px' }}>
              <div style={{ fontWeight: 'bold', fontSize: '16px', marginBottom: '8px', color: '#005AC1' }}>{org.name}</div>
              {org.stores?.map((store) => (
                <button
                  key={store.id}
                  onClick={() => handleSelect(org, store.id, store.name)}
                  style={{ width: '100%', textAlign: 'left', backgroundColor: '#F8F9FA', border: '1px solid #DDD', borderRadius: '6px', padding: '8px 12px', margin: '4px 0', cursor: 'pointer', fontWeight: '500' }}
                >
                  🏪 {store.name}
                </button>
              ))}
            </div>
          ))}
        </div>

        <button onClick={onClose} style={{ marginTop: '16px', width: '100%', padding: '10px', borderRadius: '8px', border: 'none', backgroundColor: '#EEE', cursor: 'pointer', fontWeight: 'bold' }}>Close</button>
      </div>
    </div>
  );
};
