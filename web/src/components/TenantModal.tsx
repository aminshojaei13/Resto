import React, { useState, useEffect } from 'react';
import { apiClient, setTenantContext } from '../api/apiClient';
import { Organization } from '../types';
import { useTheme } from '../theme/ThemeContext';

interface TenantModalProps {
  language: 'fa' | 'en';
  onClose: () => void;
  onSelectTenant: (orgName: string, storeName: string) => void;
}

export const TenantModal: React.FC<TenantModalProps> = ({ language, onClose, onSelectTenant }) => {
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [isCreating, setIsCreating] = useState(false);
  const [newOrgName, setNewOrgName] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isFa = language === 'fa';
  const { theme } = useTheme();

  useEffect(() => {
    loadOrganizations();
  }, []);

  const loadOrganizations = async () => {
    const list = await apiClient.getOrganizations();
    setOrganizations(list || []);
  };

  const handleSelect = (org: Organization, storeId: string, storeName: string) => {
    setTenantContext(org.id, storeId);
    onSelectTenant(org.name, storeName);
    onClose();
  };

  const handleCreateOrg = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newOrgName.trim()) return;

    setIsSubmitting(true);
    try {
      const newOrg = await apiClient.createOrganization({
        name: newOrgName.trim(),
      });
      setNewOrgName('');
      setIsCreating(false);
      setIsSubmitting(false);

      if (newOrg.stores && newOrg.stores.length > 0) {
        handleSelect(newOrg, newOrg.stores[0].id, newOrg.stores[0].name);
      } else {
        loadOrganizations();
      }
    } catch (err: any) {
      alert(err.message || 'Error creating organization');
      setIsSubmitting(false);
    }
  };

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: theme.colors.overlay,
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 1000,
        direction: isFa ? 'rtl' : 'ltr',
        fontFamily: theme.typography.fontFamily,
      }}
    >
      <div
        style={{
          backgroundColor: theme.colors.surfaceElevated,
          borderRadius: theme.borderRadius.xl,
          padding: theme.spacing['2xl'],
          width: '460px',
          maxWidth: '92%',
          boxShadow: theme.shadows.lg,
          border: `1px solid ${theme.colors.border}`,
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.xl }}>
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
            {isFa ? 'تغییر شعبه و سازمان فعال' : 'Switch Organization & Store Context'}
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              fontSize: '18px',
              color: theme.colors.textMuted,
              cursor: 'pointer',
            }}
          >
            ✕
          </button>
        </div>

        {isCreating ? (
          <form onSubmit={handleCreateOrg} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg }}>
            <div>
              <label style={{ display: 'block', fontWeight: 700, fontSize: '14px', marginBottom: '6px', color: theme.colors.textPrimary }}>
                {isFa ? 'نام سازمان جدید:' : 'New Organization Name:'} *
              </label>
              <input
                type="text"
                required
                value={newOrgName}
                onChange={(e) => setNewOrgName(e.target.value)}
                placeholder={isFa ? 'مثال: فروشگاه جدید من' : 'e.g. My New Store'}
                style={{
                  width: '100%',
                  padding: theme.spacing.md,
                  borderRadius: theme.borderRadius.lg,
                  border: `1px solid ${theme.colors.borderStrong}`,
                  backgroundColor: theme.colors.surface,
                  color: theme.colors.textPrimary,
                  fontSize: '14px',
                  boxSizing: 'border-box',
                }}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                onClick={() => setIsCreating(false)}
                style={{
                  flex: 1,
                  padding: theme.spacing.md,
                  borderRadius: theme.borderRadius.md,
                  border: `1px solid ${theme.colors.border}`,
                  backgroundColor: theme.colors.surfaceHover,
                  color: theme.colors.textPrimary,
                  cursor: 'pointer',
                  fontWeight: 600,
                  fontSize: '14px',
                }}
              >
                {isFa ? 'انصراف' : 'Cancel'}
              </button>
              <button
                type="submit"
                disabled={isSubmitting}
                style={{
                  flex: 1,
                  padding: theme.spacing.md,
                  borderRadius: theme.borderRadius.md,
                  border: 'none',
                  backgroundColor: theme.colors.primary,
                  color: theme.colors.primaryTextOnBrand,
                  cursor: 'pointer',
                  fontWeight: 700,
                  fontSize: '14px',
                }}
              >
                {isSubmitting ? (isFa ? 'در حال ایجاد...' : 'Creating...') : (isFa ? 'ایجاد و انتخاب' : 'Create & Select')}
              </button>
            </div>
          </form>
        ) : (
          <>
            <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg, maxHeight: '320px', overflowY: 'auto' }}>
              {organizations.map((org) => (
                <div
                  key={org.id}
                  style={{
                    border: `1px solid ${theme.colors.border}`,
                    borderRadius: theme.borderRadius.lg,
                    padding: theme.spacing.lg,
                    backgroundColor: theme.colors.background,
                  }}
                >
                  <div style={{ fontWeight: 700, fontSize: '15px', marginBottom: theme.spacing.md, color: theme.colors.primary }}>
                    🏢 {org.name}
                  </div>
                  {org.stores?.map((store) => (
                    <button
                      key={store.id}
                      onClick={() => handleSelect(org, store.id, store.name)}
                      style={{
                        width: '100%',
                        textAlign: isFa ? 'right' : 'left',
                        backgroundColor: theme.colors.surfaceHover,
                        border: `1px solid ${theme.colors.border}`,
                        borderRadius: theme.borderRadius.md,
                        padding: `${theme.spacing.md} ${theme.spacing.lg}`,
                        margin: `${theme.spacing.xs} 0`,
                        cursor: 'pointer',
                        fontWeight: 600,
                        fontSize: '13px',
                        color: theme.colors.textPrimary,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        transition: 'border-color 0.15s ease',
                      }}
                    >
                      <span>🏪 {store.name}</span>
                      <span style={{ fontSize: '12px', color: theme.colors.textMuted }}>{isFa ? 'انتخاب' : 'Select'}</span>
                    </button>
                  ))}
                </div>
              ))}
            </div>

            <div style={{ display: 'flex', gap: '10px', marginTop: theme.spacing.xl }}>
              <button
                onClick={() => setIsCreating(true)}
                style={{
                  flex: 1,
                  padding: theme.spacing.md,
                  borderRadius: theme.borderRadius.md,
                  border: `1px solid ${theme.colors.primary}`,
                  backgroundColor: theme.colors.surfaceElevated,
                  color: theme.colors.primary,
                  cursor: 'pointer',
                  fontWeight: 700,
                  fontSize: '14px',
                }}
              >
                ➕ {isFa ? 'ایجاد سازمان جدید' : 'New Organization'}
              </button>

              <button
                onClick={onClose}
                style={{
                  flex: 1,
                  padding: theme.spacing.md,
                  borderRadius: theme.borderRadius.md,
                  border: `1px solid ${theme.colors.border}`,
                  backgroundColor: theme.colors.surfaceHover,
                  color: theme.colors.textPrimary,
                  cursor: 'pointer',
                  fontWeight: 600,
                  fontSize: '14px',
                }}
              >
                {isFa ? 'بستن' : 'Close'}
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
};
