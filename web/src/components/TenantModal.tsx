import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { useAuth } from '../auth/AuthContext';
import { setActiveOrganization, setActiveStore, setActiveWarehouse } from '../auth/session';
import { roleLabel } from '../i18n/authStrings';

interface BusinessModalProps {
  language: 'fa' | 'en';
  onClose: () => void;
  onSelected: () => void;
}

/**
 * Choose which of *your own* businesses, stores and warehouses to work in.
 *
 * The list comes from the authenticated profile, so it can never contain a
 * business the person does not belong to, and it can never be empty-because-
 * the-request-failed: there is a real error state instead.
 */
export const TenantModal: React.FC<BusinessModalProps> = ({ language, onClose, onSelected }) => {
  const { user } = useAuth();
  const { theme } = useTheme();
  const isFa = language === 'fa';

  const [selection, setSelection] = useState<{
    organizationId: string;
    organizationName: string;
    role: string;
    storeId: string;
    storeName: string;
    warehouseId: string;
    warehouseName: string;
  } | null>(null);

  const memberships = user?.memberships ?? [];

  const choose = () => {
    if (!selection) return;

    setActiveOrganization(selection.organizationId, selection.organizationName, selection.role);
    setActiveStore(selection.storeId, selection.storeName);
    setActiveWarehouse(selection.warehouseId, selection.warehouseName);

    onSelected();
  };

  const isSelected = (
    organizationId: string,
    storeId: string,
    warehouseId: string
  ) =>
    selection?.organizationId === organizationId &&
    selection?.storeId === storeId &&
    (selection?.warehouseId === warehouseId || warehouseId === '');

  return (
    <div
      role="dialog"
      aria-modal="true"
      style={{
        position: 'fixed',
        inset: 0,
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
          width: '480px',
          maxWidth: '92%',
          boxShadow: theme.shadows.lg,
          border: `1px solid ${theme.colors.border}`,
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: theme.spacing.xl,
          }}
        >
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700, color: theme.colors.textPrimary }}>
            {isFa ? 'انتخاب کسب‌وکار' : 'Choose a business'}
          </h3>
          <button
            type="button"
            onClick={onClose}
            aria-label={isFa ? 'بستن' : 'Close'}
            style={{ background: 'none', border: 'none', fontSize: '18px', color: theme.colors.textMuted, cursor: 'pointer' }}
          >
            ✕
          </button>
        </div>

        {memberships.length === 0 ? (
          <p style={{ fontSize: '14px', color: theme.colors.textSecondary }}>
            {isFa ? 'شما هنوز عضو هیچ کسب‌وکاری نیستید.' : 'You do not belong to a business yet.'}
          </p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg, maxHeight: '360px', overflowY: 'auto' }}>
            {memberships.map((membership) => (
              <div
                key={membership.id}
                style={{
                  border: `1px solid ${theme.colors.border}`,
                  borderRadius: theme.borderRadius.lg,
                  padding: theme.spacing.lg,
                  backgroundColor: theme.colors.background,
                }}
              >
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'baseline',
                    marginBottom: theme.spacing.md,
                  }}
                >
                  <span style={{ fontWeight: 700, fontSize: '15px', color: theme.colors.primary }}>🏢 {membership.name}</span>
                  <span style={{ fontSize: '12px', color: theme.colors.textMuted }}>
                    {roleLabel(language, membership.role)}
                  </span>
                </div>

                {membership.stores.length === 0 ? (
                  <p style={{ fontSize: '13px', color: theme.colors.textSecondary, margin: 0 }}>
                    {isFa ? 'هنوز فروشگاهی برای این کسب‌وکار ثبت نشده است.' : 'No store has been set up for this business yet.'}
                  </p>
                ) : (
                  membership.stores.map((store) => (
                    <div key={store.id} style={{ marginBottom: theme.spacing.sm }}>
                      <button
                        type="button"
                        onClick={() =>
                          setSelection({
                            organizationId: membership.id,
                            organizationName: membership.name,
                            role: membership.role,
                            storeId: store.id,
                            storeName: store.name,
                            warehouseId: '',
                            warehouseName: '',
                          })
                        }
                        style={rowStyle(theme, isSelected(membership.id, store.id, ''))}
                      >
                        <span>🏪 {store.name}</span>
                      </button>

                      {store.warehouses.map((warehouse) => (
                        <button
                          key={warehouse.id}
                          type="button"
                          onClick={() =>
                            setSelection({
                              organizationId: membership.id,
                              organizationName: membership.name,
                              role: membership.role,
                              storeId: store.id,
                              storeName: store.name,
                              warehouseId: warehouse.id,
                              warehouseName: warehouse.name,
                            })
                          }
                          style={{
                            ...rowStyle(theme, isSelected(membership.id, store.id, warehouse.id)),
                            marginInlineStart: theme.spacing.lg,
                            fontSize: '12px',
                          }}
                        >
                          <span>📦 {warehouse.name}</span>
                        </button>
                      ))}
                    </div>
                  ))
                )}
              </div>
            ))}
          </div>
        )}

        <div style={{ display: 'flex', gap: '10px', marginTop: theme.spacing.xl }}>
          <button
            type="button"
            onClick={choose}
            disabled={!selection}
            style={{
              flex: 1,
              padding: theme.spacing.md,
              borderRadius: theme.borderRadius.md,
              border: 'none',
              backgroundColor: theme.colors.primary,
              color: theme.colors.primaryTextOnBrand,
              cursor: selection ? 'pointer' : 'default',
              opacity: selection ? 1 : 0.6,
              fontWeight: 700,
              fontSize: '14px',
            }}
          >
            {isFa ? 'تأیید انتخاب' : 'Confirm selection'}
          </button>
          <button
            type="button"
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
            {isFa ? 'انصراف' : 'Cancel'}
          </button>
        </div>
      </div>
    </div>
  );
};

const rowStyle = (theme: any, active: boolean) => ({
  width: '100%',
  textAlign: 'start' as const,
  backgroundColor: active ? theme.colors.primaryLight : theme.colors.surfaceHover,
  border: `1px solid ${active ? theme.colors.primary : theme.colors.border}`,
  borderRadius: theme.borderRadius.md,
  padding: `${theme.spacing.sm} ${theme.spacing.md}`,
  margin: `${theme.spacing.xs} 0`,
  cursor: 'pointer',
  fontWeight: 600,
  fontSize: '13px',
  color: active ? theme.colors.primaryDark : theme.colors.textPrimary,
});

export default TenantModal;
