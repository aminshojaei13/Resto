import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { FormError, ModalShell, PrimaryButton, SecondaryButton, SelectInput } from '../components/Field';
import { BusinessWarehouse, Purchase } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { quantityWithUnit } from '../util/units';

interface ReceivingPageProps {
  language?: 'fa' | 'en';
}

/**
 * Receiving: the physical act of goods arriving into a warehouse.
 *
 * This is deliberately not the purchase screen. Ordering says what was agreed;
 * receiving says what actually turned up — possibly less, possibly in more
 * than one delivery — and it is the only thing here that moves stock.
 */
export const ReceivingPage: React.FC<ReceivingPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [queue, setQueue] = useState<Purchase[]>([]);
  const [warehouses, setWarehouses] = useState<BusinessWarehouse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [active, setActive] = useState<Purchase | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const [purchases, warehouseList] = await Promise.all([
        apiClient.getReceivingQueue(),
        apiClient.getWarehouses(),
      ]);

      setQueue(purchases);
      setWarehouses(warehouseList);
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : isFa ? 'دریافت فهرست خریدها ناموفق بود.' : 'Could not load the receiving queue.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [isFa]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader title={s.receivingTitle} description={s.receivingSubtitle} />

      <FormError message={errorMessage} />
      <FormError message={notice} />

      {isLoading ? (
        <p style={{ color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
      ) : queue.length === 0 ? (
        <EmptyState title={s.receivingQueueEmpty} description={s.receivingQueueEmptyBody} icon="📥" />
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
          {queue.map((purchase) => (
            <div
              key={purchase.id}
              style={{
                backgroundColor: theme.colors.surface,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.xl,
                padding: theme.spacing.lg,
                boxShadow: theme.shadows.card,
              }}
            >
              <div
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: theme.spacing.md,
                  flexWrap: 'wrap',
                  marginBottom: theme.spacing.md,
                }}
              >
                <div>
                  <strong style={{ color: theme.colors.textPrimary, fontSize: '15px' }}>{purchase.purchaseNumber}</strong>
                  <span style={{ marginInlineStart: 8, color: theme.colors.textSecondary, fontSize: '13px' }}>
                    {purchase.supplierName}
                  </span>
                </div>
                <PrimaryButton onClick={() => setActive(purchase)}>{isFa ? 'ثبت دریافت' : 'Receive'}</PrimaryButton>
              </div>

              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                <tbody>
                  {purchase.items.map((item) => {
                    const outstanding = item.quantity - item.receivedQuantity;

                    return (
                      <tr key={item.id} style={{ borderTop: `1px solid ${theme.colors.border}` }}>
                        <td style={{ padding: '8px 0', color: theme.colors.textPrimary, fontWeight: 600 }}>
                          {item.productName}
                        </td>
                        <td style={{ padding: '8px 0', color: theme.colors.textSecondary }}>
                          {s.orderedQty}: {quantityWithUnit(item.quantity, item.unit, language)}
                        </td>
                        <td style={{ padding: '8px 0', color: theme.colors.textSecondary }}>
                          {s.receivedQty}: {quantityWithUnit(item.receivedQuantity, item.unit, language)}
                        </td>
                        <td
                          style={{
                            padding: '8px 0',
                            textAlign: isFa ? 'left' : 'right',
                            fontWeight: 700,
                            color: outstanding > 0 ? theme.colors.warning : theme.colors.success,
                          }}
                        >
                          {outstanding > 0 ? quantityWithUnit(outstanding, item.unit, language) : s.received}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ))}
        </div>
      )}

      {active && (
        <ReceiveModal
          language={language}
          purchase={active}
          warehouses={warehouses}
          onClose={() => setActive(null)}
          onReceived={(message) => {
            setActive(null);
            setNotice(message);
            void load();
          }}
        />
      )}
    </div>
  );
};

const ReceiveModal: React.FC<{
  language: 'fa' | 'en';
  purchase: Purchase;
  warehouses: BusinessWarehouse[];
  onClose: () => void;
  onReceived: (message: string) => void;
}> = ({ language, purchase, warehouses, onClose, onReceived }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [warehouseId, setWarehouseId] = useState(
    () => purchase.warehouseId || (warehouses.length === 1 ? warehouses[0].id : '')
  );
  const [amounts, setAmounts] = useState<Record<string, number>>(() =>
    Object.fromEntries(
      purchase.items
        .filter((item) => item.quantity - item.receivedQuantity > 0)
        .map((item) => [item.id as string, item.quantity - item.receivedQuantity])
    )
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!warehouseId) {
      setErrors({ warehouse_id: isFa ? 'انبار مقصد را انتخاب کنید.' : 'Choose a destination warehouse.' });
      return;
    }

    const lines = Object.entries(amounts)
      .filter(([, amount]) => Number(amount) > 0)
      .map(([purchaseItemId, amount]) => ({
        purchaseItemId,
        receivedQuantity: Math.min(Number(amount), outstandingFor(purchaseItemId)),
      }));

    if (lines.length === 0) {
      setErrors({ items: isFa ? 'حداقل برای یک قلم مقدار دریافتی وارد کنید.' : 'Enter a received amount for at least one item.' });
      return;
    }

    setIsSubmitting(true);
    setFormError('');

    try {
      await apiClient.receivePurchase(purchase.id, { warehouseId, lines });
      onReceived(s.receivingDone);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ثبت دریافت ناموفق بود.' : 'Receiving failed.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const outstandingFor = (purchaseItemId: string): number => {
    const item = purchase.items.find((i) => i.id === purchaseItemId);
    return item ? item.quantity - item.receivedQuantity : 0;
  };

  return (
    <ModalShell title={`${s.submitReceiving} — ${purchase.purchaseNumber}`} onClose={onClose} maxWidth={640}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <FormError message={formError} />

        <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
          {isFa ? 'تأمین‌کننده' : 'Supplier'}: <strong style={{ color: theme.colors.textPrimary }}>{purchase.supplierName}</strong>
        </div>

        <SelectInput
          label={s.destinationWarehouse}
          required
          value={warehouseId}
          error={errors.warehouse_id}
          onChange={(e) => setWarehouseId(e.target.value)}
          hint={warehouses.length === 1 ? warehouses[0].name : undefined}
        >
          <option value="">{s.selectWarehouse}</option>
          {warehouses.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
              {w.storeName ? ` — ${w.storeName}` : ''}
            </option>
          ))}
        </SelectInput>

        {purchase.items.map((item) => {
          const outstanding = item.quantity - item.receivedQuantity;

          return (
            <div
              key={item.id}
              style={{
                backgroundColor: theme.colors.background,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.md,
              }}
            >
              <div style={{ fontSize: '13px', fontWeight: 700, color: theme.colors.textPrimary }}>{item.productName}</div>
              <div style={{ fontSize: '12px', color: theme.colors.textSecondary, marginBottom: 8 }}>
                {s.orderedQty}: {quantityWithUnit(item.quantity, item.unit, language)} · {s.receivedQty}:{' '}
                {quantityWithUnit(item.receivedQuantity, item.unit, language)}
              </div>

              {outstanding > 0 ? (
                <input
                  type="number"
                  min={0}
                  max={outstanding}
                  value={amounts[item.id as string] ?? ''}
                  onChange={(e) =>
                    setAmounts((prev) => ({ ...prev, [item.id as string]: e.target.value === '' ? 0 : Number(e.target.value) }))
                  }
                  style={{
                    width: '100%',
                    padding: '8px 10px',
                    borderRadius: theme.borderRadius.md,
                    border: `1px solid ${theme.colors.border}`,
                    backgroundColor: theme.colors.surfaceElevated,
                    color: theme.colors.textPrimary,
                    fontSize: '14px',
                    boxSizing: 'border-box',
                  }}
                />
              ) : (
                <span style={{ fontSize: '12px', color: theme.colors.success }}>{s.received}</span>
              )}
            </div>
          );
        })}

        {errors.items && (
          <p style={{ margin: 0, fontSize: '12px', color: theme.colors.error }}>{errors.items}</p>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : s.submitReceiving}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};
const toFieldErrors = (fieldErrors: Record<string, string[]>): Record<string, string> =>
  Object.fromEntries(Object.entries(fieldErrors).map(([key, value]) => [key, value[0] ?? '']));
