import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge, StatusVariant } from '../components/StatusBadge';
import { FormError, ModalShell, PrimaryButton, SecondaryButton, SelectInput, TextInput } from '../components/Field';
import { Product, Purchase, Supplier } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { formatMoney } from '../util/money';
import { quantityWithUnit } from '../util/units';

interface PurchasesPageProps {
  language?: 'fa' | 'en';
  navigate?: (path: string) => void;
}

interface DraftLine {
  productId: string;
  quantity: number;
  unitCost: number;
}

/**
 * Purchase orders: what the business agreed to buy from a supplier.
 *
 * Recording a purchase does not move stock — goods only appear in a warehouse
 * through Receiving. Keeping that separate is the whole point of this screen.
 */
export const PurchasesPage: React.FC<PurchasesPageProps> = ({ language = 'fa', navigate }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [notice, setNotice] = useState('');

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [payFor, setPayFor] = useState<Purchase | null>(null);

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      setPurchases(await apiClient.getPurchases());
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : isFa ? 'دریافت سفارش‌های خرید ناموفق بود.' : 'Could not load purchases.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [isFa]);

  useEffect(() => {
    void load();
    apiClient.getSuppliers().then(setSuppliers).catch(() => setSuppliers([]));
    apiClient.getProducts().then(setProducts).catch(() => setProducts([]));
  }, []);

  const receivingBadge = (status: Purchase['status']): { label: string; variant: StatusVariant } => {
    switch (status) {
      case 'RECEIVED':
        return { label: s.received, variant: 'success' };
      case 'PARTIALLY_RECEIVED':
        return { label: s.partiallyReceived, variant: 'warning' };
      case 'CANCELLED':
        return { label: s.statusCancelled, variant: 'error' };
      default:
        return { label: s.ordered, variant: 'neutral' };
    }
  };

  const paymentBadge = (status: Purchase['paymentStatus']): { label: string; variant: StatusVariant } => {
    switch (status) {
      case 'PAID':
        return { label: s.paid, variant: 'success' };
      case 'PARTIAL':
        return { label: s.partialPaid, variant: 'warning' };
      default:
        return { label: s.unpaid, variant: 'neutral' };
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader
        title={s.purchasesTitle}
        description={s.purchasesSubtitle}
        actions={<PrimaryButton onClick={() => setIsCreateOpen(true)}>➕ {s.newPurchase}</PrimaryButton>}
      />

      <FormError message={errorMessage} />
      <FormError message={notice} />

      {isLoading ? (
        <p style={{ color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
      ) : purchases.length === 0 ? (
        <EmptyState
          title={isFa ? 'سفارش خریدی ثبت نشده است' : 'No purchase orders yet'}
          description={s.purchasesSubtitle}
          actionText={s.newPurchase}
          onAction={() => setIsCreateOpen(true)}
          icon="🛍️"
        />
      ) : (
        <div
          style={{
            backgroundColor: theme.colors.surface,
            borderRadius: theme.borderRadius.xl,
            border: `1px solid ${theme.colors.border}`,
            overflow: 'hidden',
            boxShadow: theme.shadows.card,
          }}
        >
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', minWidth: 860 }}>
              <thead>
                <tr
                  style={{
                    backgroundColor: theme.colors.background,
                    borderBottom: `2px solid ${theme.colors.border}`,
                    fontSize: '12px',
                    fontWeight: 700,
                    color: theme.colors.textSecondary,
                  }}
                >
                  <th style={{ padding: theme.spacing.lg }}>{s.purchaseNumber}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.supplier}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.purchaseDate}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.purchaseTotal}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.paymentStatus}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.receivingStatus}</th>
                  <th style={{ padding: theme.spacing.lg }}>{isFa ? 'عملیات' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody>
                {purchases.map((purchase) => (
                  <tr key={purchase.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                    <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.textPrimary }}>
                      {purchase.purchaseNumber}
                    </td>
                    <td style={{ padding: theme.spacing.lg, color: theme.colors.textPrimary }}>{purchase.supplierName || '—'}</td>
                    <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>
                      {purchase.purchaseDate || purchase.createdAt?.slice(0, 10) || '—'}
                    </td>
                    <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.primaryDark }}>
                      {formatMoney(purchase.totalAmount, isFa)}
                    </td>
                    <td style={{ padding: theme.spacing.lg }}>
                      <StatusBadge label={paymentBadge(purchase.paymentStatus).label} variant={paymentBadge(purchase.paymentStatus).variant} />
                    </td>
                    <td style={{ padding: theme.spacing.lg }}>
                      <StatusBadge label={receivingBadge(purchase.status).label} variant={receivingBadge(purchase.status).variant} />
                    </td>
                    <td style={{ padding: theme.spacing.lg }}>
                      <div style={{ display: 'flex', gap: theme.spacing.sm, flexWrap: 'wrap' }}>
                        {purchase.status !== 'RECEIVED' && purchase.status !== 'CANCELLED' && navigate && (
                          <button
                            onClick={() => navigate('/app/receiving')}
                            style={{
                              backgroundColor: theme.colors.primaryLight,
                              color: theme.colors.primaryDark,
                              border: 'none',
                              borderRadius: theme.borderRadius.md,
                              padding: '6px 10px',
                              fontWeight: 700,
                              fontSize: '12px',
                              cursor: 'pointer',
                            }}
                          >
                            {isFa ? 'دریافت کالا' : 'Receive'}
                          </button>
                        )}
                        {purchase.paymentStatus !== 'PAID' && (
                          <button
                            onClick={() => setPayFor(purchase)}
                            style={{
                              backgroundColor: theme.colors.surfaceHover,
                              color: theme.colors.textPrimary,
                              border: `1px solid ${theme.colors.borderStrong}`,
                              borderRadius: theme.borderRadius.md,
                              padding: '6px 10px',
                              fontWeight: 600,
                              fontSize: '12px',
                              cursor: 'pointer',
                            }}
                          >
                            {s.markPaid}
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {isCreateOpen && (
        <CreatePurchaseModal
          language={language}
          suppliers={suppliers}
          products={products}
          onClose={() => setIsCreateOpen(false)}
          onCreated={(message) => {
            setIsCreateOpen(false);
            setNotice(message);
            void load();
          }}
        />
      )}

      {payFor && (
        <PaymentModal
          language={language}
          purchase={payFor}
          onClose={() => setPayFor(null)}
          onPaid={(message) => {
            setPayFor(null);
            setNotice(message);
            void load();
          }}
        />
      )}
    </div>
  );
};

const CreatePurchaseModal: React.FC<{
  language: 'fa' | 'en';
  suppliers: Supplier[];
  products: Product[];
  onClose: () => void;
  onCreated: (message: string) => void;
}> = ({ language, suppliers, products, onClose, onCreated }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [supplierId, setSupplierId] = useState('');
  const [purchaseDate, setPurchaseDate] = useState(new Date().toISOString().slice(0, 10));
  const [lines, setLines] = useState<DraftLine[]>([]);
  const [productId, setProductId] = useState('');
  const [quantity, setQuantity] = useState<number | ''>(1);
  const [unitCost, setUnitCost] = useState<number | ''>('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const total = lines.reduce((sum, line) => sum + line.quantity * line.unitCost, 0);

  const addLine = () => {
    if (!productId) {
      setErrors({ product: isFa ? 'کالا را انتخاب کنید.' : 'Choose a product.' });
      return;
    }

    const product = products.find((p) => p.id === productId);

    setLines((prev) => [
      ...prev,
      {
        productId,
        quantity: Number(quantity) || 1,
        unitCost: unitCost === '' ? product?.costPrice ?? 0 : Number(unitCost),
      },
    ]);

    setProductId('');
    setQuantity(1);
    setUnitCost('');
    setErrors((prev) => ({ ...prev, product: '' }));
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();

    const next: Record<string, string> = {};
    if (!supplierId) next.supplier_id = isFa ? 'تأمین‌کننده را انتخاب کنید.' : 'Choose a supplier.';
    if (lines.length === 0) next.items = isFa ? 'حداقل یک کالا اضافه کنید.' : 'Add at least one item.';

    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setIsSubmitting(true);
    setFormError('');

    try {
      await apiClient.createPurchase({
        supplier_id: supplierId,
        purchase_date: purchaseDate,
        items: lines.map((line) => ({
          product_id: line.productId,
          quantity: line.quantity,
          unit_cost: line.unitCost,
        })),
      });

      onCreated(isFa ? 'سفارش خرید ثبت شد. برای افزودن کالا به انبار، بخش دریافت کالا را باز کنید.' : 'Purchase order created. Open Receiving to add the goods to a warehouse.');
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ثبت سفارش خرید ناموفق بود.' : 'Could not create the purchase order.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalShell title={s.newPurchase} onClose={onClose} maxWidth={640}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <FormError message={formError} />

        <SelectInput label={s.supplier} required value={supplierId} error={errors.supplier_id} onChange={(e) => setSupplierId(e.target.value)}>
          <option value="">{isFa ? 'انتخاب کنید' : 'Select a supplier'}</option>
          {suppliers.map((supplier) => (
            <option key={supplier.id} value={supplier.id}>
              {supplier.name}
            </option>
          ))}
        </SelectInput>

        <TextInput label={s.purchaseDate} type="date" value={purchaseDate} onChange={(e) => setPurchaseDate(e.target.value)} />

        <div style={{ borderTop: `1px solid ${theme.colors.border}`, paddingTop: theme.spacing.md }}>
          <SelectInput label={isFa ? 'کالا' : 'Product'} value={productId} error={errors.product} onChange={(e) => setProductId(e.target.value)}>
            <option value="">{isFa ? 'انتخاب کنید' : 'Select a product'}</option>
            {products.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </SelectInput>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr auto', gap: theme.spacing.md, marginTop: theme.spacing.md, alignItems: 'end' }}>
            <TextInput
              label={s.quantity}
              type="number"
              min={1}
              value={quantity}
              onChange={(e) => setQuantity(e.target.value === '' ? '' : Number(e.target.value))}
            />
            <TextInput
              label={isFa ? 'قیمت واحد' : 'Unit cost'}
              type="number"
              min={0}
              step="0.01"
              value={unitCost}
              onChange={(e) => setUnitCost(e.target.value === '' ? '' : Number(e.target.value))}
            />
            <SecondaryButton type="button" onClick={addLine}>
              {isFa ? 'افزودن' : 'Add'}
            </SecondaryButton>
          </div>
        </div>

        {errors.items && <p style={{ margin: 0, fontSize: '12px', color: theme.colors.error }}>{errors.items}</p>}

        {lines.length > 0 && (
          <div style={{ backgroundColor: theme.colors.background, borderRadius: theme.borderRadius.md, padding: theme.spacing.md }}>
            {lines.map((line, index) => {
              const product = products.find((p) => p.id === line.productId);

              return (
                <div
                  key={`${line.productId}-${index}`}
                  style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '6px 0', fontSize: '13px' }}
                >
                  <span style={{ color: theme.colors.textPrimary }}>
                    {product?.name}{' '}
                    <span style={{ color: theme.colors.textMuted }}>
                      × {quantityWithUnit(line.quantity, product?.unit, language)}
                    </span>
                  </span>
                  <span style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
                    <span style={{ color: theme.colors.textPrimary, fontWeight: 700 }}>
                      {formatMoney(line.quantity * line.unitCost, isFa)}
                    </span>
                    <button
                      type="button"
                      onClick={() => setLines((prev) => prev.filter((_, i) => i !== index))}
                      style={{ background: 'none', border: 'none', cursor: 'pointer', color: theme.colors.error }}
                    >
                      ✕
                    </button>
                  </span>
                </div>
              );
            })}
          </div>
        )}

        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '15px', fontWeight: 800 }}>
          <span>{s.purchaseTotal}</span>
          <span>{formatMoney(total, isFa)}</span>
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md, marginTop: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : isFa ? 'ثبت سفارش خرید' : 'Create purchase order'}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};

const PaymentModal: React.FC<{
  language: 'fa' | 'en';
  purchase: Purchase;
  onClose: () => void;
  onPaid: (message: string) => void;
}> = ({ language, purchase, onClose, onPaid }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [amount, setAmount] = useState<number | ''>(purchase.totalAmount);
  const [method, setMethod] = useState('BANK_TRANSFER');
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    setFormError('');

    try {
      await apiClient.payPurchase(purchase.id, Number(amount), method);
      onPaid(isFa ? 'پرداخت به تأمین‌کننده ثبت شد.' : 'Supplier payment recorded.');
    } catch (err) {
      setFormError(err instanceof ApiError ? err.message : isFa ? 'ثبت پرداخت ناموفق بود.' : 'Payment failed.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalShell title={s.markPaid} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <FormError message={formError} />
        <p style={{ margin: 0, fontSize: '13px', color: theme.colors.textSecondary }}>
          {purchase.purchaseNumber} · {formatMoney(purchase.totalAmount, isFa)}
        </p>

        <TextInput
          label={s.amount}
          required
          type="number"
          min={0.01}
          step="0.01"
          value={amount}
          onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
        />

        <SelectInput label={s.paymentMethod} value={method} onChange={(e) => setMethod(e.target.value)}>
          <option value="CASH">{isFa ? 'نقدی' : 'Cash'}</option>
          <option value="BANK_TRANSFER">{isFa ? 'حواله بانکی' : 'Bank transfer'}</option>
          <option value="CARD">{isFa ? 'کارت' : 'Card'}</option>
        </SelectInput>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : s.markPaid}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};
const toFieldErrors = (fieldErrors: Record<string, string[]>): Record<string, string> =>
  Object.fromEntries(Object.entries(fieldErrors).map(([key, value]) => [key, value[0] ?? '']));
