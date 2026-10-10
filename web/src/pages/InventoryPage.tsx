import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient, getCurrentContext } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge } from '../components/StatusBadge';
import { FormError, ModalShell, PrimaryButton, SecondaryButton, SelectInput, TextInput } from '../components/Field';
import { BusinessWarehouse, Product, StockMovement, StockRow } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { quantityWithUnit } from '../util/units';

interface InventoryPageProps {
  language?: 'fa' | 'en';
}

/**
 * "What do I have right now?"
 *
 * Stock is read per warehouse from the server. Recording new stock is a
 * separate, explicit operation that requires the warehouse to be chosen — a
 * business with one warehouse has it preselected and still sees its name; a
 * business with none is told to create one rather than being handed a
 * warehouse it does not have.
 */
export const InventoryPage: React.FC<InventoryPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [stock, setStock] = useState<StockRow[]>([]);
  const [warehouses, setWarehouses] = useState<BusinessWarehouse[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [warehouseFilter, setWarehouseFilter] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  // "This business genuinely has no warehouse" and "the request failed" are
  // different facts. Only the first one may lead to the create-a-warehouse
  // screen; conflating them would tell the user to fix something that is not
  // broken and would hide a real outage.
  const [warehousesLoaded, setWarehousesLoaded] = useState(false);

  const [isStockInOpen, setIsStockInOpen] = useState(false);
  const [isWarehouseOpen, setIsWarehouseOpen] = useState(false);
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      // Everything this screen shows comes from the server. A failed request
      // is reported; it is never replaced with an empty, healthy-looking list.
      const [warehouseList, stockRows, productList] = await Promise.all([
        apiClient.getWarehouses(),
        apiClient.getStock(warehouseFilter || undefined),
        apiClient.getProducts(),
      ]);

      setWarehouses(warehouseList ?? []);
      setStock(stockRows ?? []);
      setProducts(productList ?? []);
      const movRes = await apiClient.getStockMovements(warehouseFilter ? { warehouseId: warehouseFilter } : {});
      setMovements(Array.isArray(movRes) ? movRes : Array.isArray((movRes as any)?.data) ? (movRes as any).data : []);
      setWarehousesLoaded(true);
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : isFa ? 'دریافت موجودی ناموفق بود.' : 'Could not load stock.'
      );
    } finally {
      setIsLoading(false);
    }
  }, [warehouseFilter, isFa]);

  useEffect(() => {
    void load();
  }, [load]);

  if (!isLoading && warehousesLoaded && warehouses.length === 0) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
        <PageHeader title={s.currentStockTitle} description={s.currentStockSubtitle} />

        <div
          style={{
            backgroundColor: theme.colors.surface,
            border: `1px solid ${theme.colors.border}`,
            borderRadius: theme.borderRadius.xl,
            padding: theme.spacing['3xl'],
            textAlign: 'center',
            boxShadow: theme.shadows.card,
          }}
        >
          <div style={{ fontSize: '40px', marginBottom: theme.spacing.md }}>🏭</div>
          <h3 style={{ margin: '0 0 8px', fontSize: '18px', color: theme.colors.textPrimary }}>{s.noWarehouseTitle}</h3>
          <p style={{ margin: '0 auto 20px', maxWidth: 460, fontSize: '14px', color: theme.colors.textSecondary, lineHeight: 1.8 }}>
            {s.noWarehouseBody}
          </p>
          <PrimaryButton onClick={() => setIsWarehouseOpen(true)}>{s.createWarehouse}</PrimaryButton>
        </div>

        {isWarehouseOpen && (
          <WarehouseModal
            language={language}
            onClose={() => setIsWarehouseOpen(false)}
            onCreated={() => {
              setIsWarehouseOpen(false);
              void load();
            }}
          />
        )}
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader
        title={s.currentStockTitle}
        description={s.currentStockSubtitle}
        actions={
          <PrimaryButton onClick={() => setIsStockInOpen(true)}>➕ {isFa ? 'افزایش موجودی' : 'Increase stock'}</PrimaryButton>
        }
      />

      <div style={{ display: 'flex', gap: theme.spacing.sm, alignItems: 'center', flexWrap: 'wrap' }}>
        <span style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{s.warehouse}:</span>
        <select
          value={warehouseFilter}
          onChange={(e) => setWarehouseFilter(e.target.value)}
          style={{
            padding: '8px 12px',
            borderRadius: theme.borderRadius.md,
            border: `1px solid ${theme.colors.border}`,
            backgroundColor: theme.colors.surfaceElevated,
            color: theme.colors.textPrimary,
            fontSize: '13px',
          }}
        >
          <option value="">{isFa ? 'همه انبارها' : 'All warehouses'}</option>
          {warehouses.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
            </option>
          ))}
        </select>
      </div>

      <FormError message={errorMessage} />
      <FormError message={notice} />

      {isLoading ? (
        <p style={{ color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
      ) : (stock ?? []).length === 0 ? (
        <EmptyState
          title={isFa ? 'موجودی ثبت نشده است' : 'No stock recorded yet'}
          description={isFa ? 'برای ثبت موجودی از دکمه بالا استفاده کنید.' : 'Use the button above to record stock coming in.'}
          icon="📦"
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
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', minWidth: 680 }}>
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
                  <th style={{ padding: theme.spacing.lg }}>{isFa ? 'کالا' : 'Product'}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.warehouse}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.quantityWithUnit}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.availableStock}</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.reservedStock}</th>
                </tr>
              </thead>
              <tbody>
                {stock.map((row) => (
                  <tr key={row.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                    <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.textPrimary }}>
                      {row.productName}
                      <div style={{ fontWeight: 400, fontSize: '11px', color: theme.colors.textMuted }}>{row.sku}</div>
                    </td>
                    <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>{row.warehouseName}</td>
                    <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.textPrimary }}>
                      {quantityWithUnit(row.quantity, row.unit, language)}
                    </td>
                    <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>
                      {quantityWithUnit(row.availableQuantity, row.unit, language)}
                    </td>
                    <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>
                      {quantityWithUnit(row.reservedQuantity, row.unit, language)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {(movements ?? []).length > 0 && (
        <section>
          <h3 style={{ margin: 0, fontSize: '15px', fontWeight: 700, color: theme.colors.textPrimary }}>
            {s.stockMovementsTitle}
          </h3>
          <p style={{ margin: '2px 0 12px', fontSize: '13px', color: theme.colors.textSecondary }}>
            {s.stockMovementsSubtitle}
          </p>

          <div
            style={{
              backgroundColor: theme.colors.surface,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.xl,
              overflow: 'hidden',
            }}
          >
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', minWidth: 560 }}>
                <tbody>
                  {movements.slice(0, 20).map((movement) => (
                    <tr key={movement.id} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                      <td style={{ padding: '10px 16px', width: 90 }}>
                        <StatusBadge
                          label={movement.type === 'IN' ? s.inStock : s.outOfStock}
                          variant={Number(movement.quantity) >= 0 ? 'success' : 'warning'}
                        />
                      </td>
                      <td style={{ padding: '10px 16px', color: theme.colors.textPrimary }}>{movement.reason}</td>
                      <td style={{ padding: '10px 16px', color: theme.colors.textSecondary }}>
                        {new Date(movement.created_at).toLocaleString(isFa ? 'fa-IR' : 'en-GB')}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </section>
      )}

      {isStockInOpen && (
        <StockInModal
          language={language}
          warehouses={warehouses}
          products={products}
          onClose={() => setIsStockInOpen(false)}
          onDone={(message) => {
            setNotice(message);
            setIsStockInOpen(false);
            void load();
          }}
        />
      )}

      {isWarehouseOpen && (
        <WarehouseModal
          language={language}
          onClose={() => setIsWarehouseOpen(false)}
          onCreated={() => {
            setIsWarehouseOpen(false);
            void load();
          }}
        />
      )}
    </div>
  );
};

/**
 * The explicit stock-in form: product, warehouse, quantity, the product's own
 * unit, and a reason. Every field is validated where it is typed.
 */
const StockInModal: React.FC<{
  language: 'fa' | 'en';
  warehouses: BusinessWarehouse[];
  products: Product[];
  onClose: () => void;
  onDone: (message: string) => void;
}> = ({ language, warehouses, products, onClose, onDone }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [productId, setProductId] = useState('');
  const [warehouseId, setWarehouseId] = useState(() =>
    warehouses.length === 1 ? warehouses[0].id : getCurrentContext().warehouseId
  );
  const [quantity, setQuantity] = useState<number | ''>('');
  const [reason, setReason] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const product = products.find((p) => p.id === productId);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();

    const next: Record<string, string> = {};

    if (!productId) next.product = isFa ? 'کالا را انتخاب کنید.' : 'Choose a product.';
    if (!warehouseId) next.warehouse = isFa ? 'انبار را انتخاب کنید.' : 'Choose a warehouse.';

    const amount = Number(quantity);
    if (quantity === '' || Number.isNaN(amount) || amount <= 0) {
      next.quantity = isFa ? 'مقدار باید بزرگ‌تر از صفر باشد.' : 'Quantity must be greater than zero.';
    }

    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setIsSubmitting(true);
    setFormError('');

    try {
      await apiClient.stockIn({
        warehouseId,
        productId,
        quantity: amount,
        reason: reason.trim() || undefined,
      });

      onDone(s.stockInDone);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ثبت موجودی ناموفق بود.' : 'Could not record stock.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalShell title={s.increaseStockTitle} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <p style={{ margin: 0, fontSize: '13px', color: theme.colors.textSecondary, marginBottom: 4 }}>
          {s.increaseStockSubtitle}
        </p>

        <FormError message={formError} />

        <SelectInput
          label={isFa ? 'کالا' : 'Product'}
          required
          value={productId}
          error={errors.product}
          onChange={(e) => setProductId(e.target.value)}
        >
          <option value="">{isFa ? 'انتخاب کنید' : 'Select a product'}</option>
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </SelectInput>

        <SelectInput
          label={s.warehouse}
          required
          value={warehouseId}
          error={errors.warehouse_id ?? errors.warehouse}
          onChange={(e) => setWarehouseId(e.target.value)}
          hint={warehouses.length === 1 ? s.warehouse : undefined}
        >
          <option value="">{s.selectWarehouse}</option>
          {warehouses.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name}
              {w.storeName ? ` — ${w.storeName}` : ''}
            </option>
          ))}
        </SelectInput>

        <TextInput
          label={s.quantity}
          required
          type="number"
          min={1}
          step={1}
          value={quantity}
          error={errors.quantity}
          onChange={(e) => setQuantity(e.target.value === '' ? '' : Number(e.target.value))}
          placeholder="0"
        />

        {/* The unit belongs to the product; it is shown, never chosen. */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 12px',
            borderRadius: theme.borderRadius.md,
            backgroundColor: theme.colors.background,
            border: `1px solid ${theme.colors.border}`,
            fontSize: '13px',
          }}
        >
          <span style={{ color: theme.colors.textSecondary }}>{s.unitOfProduct}</span>
          <strong style={{ color: theme.colors.textPrimary }}>
            {product ? product.unitLabel ?? product.unit : isFa ? '—' : '—'}
          </strong>
        </div>

        <TextInput
          label={s.reasons}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          placeholder={isFa ? 'مثال: شمارش دستی صبح' : 'e.g. Morning stock count'}
        />

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md, marginTop: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : s.submitStockIn}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};

const WarehouseModal: React.FC<{
  language: 'fa' | 'en';
  onClose: () => void;
  onCreated: () => void;
}> = ({ language, onClose, onCreated }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [name, setName] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const storeId = getCurrentContext().storeId;

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      setErrors({ name: isFa ? 'نام انبار را وارد کنید.' : 'Enter a warehouse name.' });
      return;
    }

    setIsSubmitting(true);
    setFormError('');

    try {
      await apiClient.createWarehouse({
        storeId,
        name: name.trim(),
        code: `WH-${name.trim().slice(0, 6).toUpperCase().replace(/\s+/g, '')}`,
      });

      onCreated();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ایجاد انبار ناموفق بود.' : 'Could not create the warehouse.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalShell title={s.createWarehouse} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <FormError message={formError} />

        <TextInput
          label={isFa ? 'نام انبار' : 'Warehouse name'}
          required
          value={name}
          error={errors.name}
          onChange={(e) => setName(e.target.value)}
          placeholder={isFa ? 'انبار مرکزی' : 'Main warehouse'}
        />

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md, marginTop: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : s.createWarehouse}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};
const toFieldErrors = (fieldErrors: Record<string, string[]>): Record<string, string> =>
  Object.fromEntries(Object.entries(fieldErrors).map(([key, value]) => [key, value[0] ?? '']));
