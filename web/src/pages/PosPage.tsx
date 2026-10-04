import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, apiClient, getCurrentContext } from '../api/apiClient';
import { BusinessWarehouse, BusinessSettings, Customer, Product } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { formatMoney } from '../util/money';
import { formatPercent, formatPercentFa, quantityWithUnit } from '../util/units';
import { FormError, PrimaryButton, SelectInput, TextInput } from '../components/Field';

interface PosPageProps {
  language?: 'fa' | 'en';
}

interface CartLine {
  productId: string;
  productName: string;
  sku: string;
  unit: string;
  price: number;
  quantity: number;
  discountPercent: number;
}

/**
 * Registering a sale.
 *
 * The tax rate shown here is the business rate read from the server, and the
 * server recomputes the order from that same rate. The preview is a courtesy:
 * the stored order is priced by the backend.
 */
export const PosPage: React.FC<PosPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [warehouses, setWarehouses] = useState<BusinessWarehouse[]>([]);
  const [settings, setSettings] = useState<BusinessSettings | null>(null);
  const [search, setSearch] = useState('');
  const [cart, setCart] = useState<CartLine[]>([]);
  const [customerId, setCustomerId] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [warehouseId, setWarehouseId] = useState('');
  const [taxRate, setTaxRate] = useState<number | '' | null>(null);
  const [errorMessage, setErrorMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const [productList, customerList, warehouseList, businessSettings] = await Promise.all([
        apiClient.getProducts(search || undefined),
        apiClient.getCustomers(),
        apiClient.getWarehouses(),
        apiClient.getBusinessSettings(),
      ]);

      setProducts(productList);
      setCustomers(customerList);
      setWarehouses(warehouseList);
      setSettings(businessSettings);

      // A single warehouse is preselected but still shown by name; the server
      // decides, never a hardcoded id.
      setWarehouseId((current) => {
        if (current && warehouseList.some((w) => w.id === current)) return current;
        if (warehouseList.length === 1) return warehouseList[0].id;
        return getCurrentContext().warehouseId || '';
      });

      setTaxRate((current) =>
        current === null || businessSettings.canOverrideTaxPerOrder ? current : businessSettings.defaultTaxRate
      );
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : isFa ? 'دریافت اطلاعات ناموفق بود.' : 'Could not load the till.');
    } finally {
      setIsLoading(false);
    }
  }, [search]);

  useEffect(() => {
    void load();
  }, []);

  const effectiveRate = useMemo(() => {
    if (taxRate !== null && settings?.canOverrideTaxPerOrder) return Number(taxRate);
    return settings?.defaultTaxRate ?? 0;
  }, [taxRate, settings]);

  const addToCart = (product: Product) => {
    setCart((prev) => {
      const existing = prev.find((line) => line.productId === product.id);

      if (existing) {
        return prev.map((line) => (line.productId === product.id ? { ...line, quantity: line.quantity + 1 } : line));
      }

      return [
        ...prev,
        {
          productId: product.id,
          productName: product.name,
          sku: product.sku,
          unit: product.unit,
          price: product.price,
          quantity: 1,
          discountPercent: 0,
        },
      ];
    });
  };

  const subtotal = Math.round(cart.reduce((sum, line) => sum + line.price * line.quantity, 0) * 100) / 100;
  const discount = Math.round(
    cart.reduce((sum, line) => sum + line.price * line.quantity * (line.discountPercent / 100), 0) * 100
  ) / 100;
  const tax = Math.round((subtotal - discount) * (effectiveRate / 100) * 100) / 100;
  const total = Math.round((subtotal - discount + tax) * 100) / 100;

  const checkout = async () => {
    if (cart.length === 0) return;

    if (!warehouseId) {
      setErrorMessage(isFa ? 'برای ثبت فروش باید انبار انتخاب شود.' : 'Choose a warehouse before registering a sale.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');
    setNotice('');

    try {
      const order = await apiClient.checkout({
        warehouseId,
        customerId: customerId || undefined,
        customerName: customers.find((c) => c.id === customerId)?.name,
        paymentMethod,
        source: 'POS',
        taxRate: taxRate === null ? undefined : Number(taxRate),
        items: cart.map((line) => ({
          productId: line.productId,
          quantity: line.quantity,
          discountPercent: line.discountPercent,
        })),
      });

      setNotice(
        isFa
          ? `سفارش ${order.orderNumber} ثبت شد. مبلغ نهایی ${formatMoney(order.totalAmount, true)} با نرخ مالیات ${formatPercentFa(order.taxRate ?? effectiveRate)}.`
          : `Order ${order.orderNumber} registered. Total ${formatMoney(order.totalAmount, false)} at ${formatPercent(order.taxRate ?? effectiveRate)}% tax.`
      );

      setCart([]);
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : isFa ? 'ثبت سفارش ناموفق بود.' : 'Could not register the order.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ display: 'flex', gap: theme.spacing.xl, flexWrap: 'wrap', alignItems: 'flex-start' }}>
      <div style={{ flex: 2, minWidth: 280 }}>
        <TextInput
          label={isFa ? 'جستجوی کالا' : 'Search products'}
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={isFa ? 'نام، کد یا بارکد کالا' : 'Name, SKU or barcode'}
        />

        <FormError message={errorMessage} />
        <FormError message={notice} />

        {isLoading ? (
          <p style={{ color: theme.colors.textSecondary, fontSize: '13px' }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(190px, 1fr))', gap: theme.spacing.md, marginTop:16 }}>
            {products.map((product) => (
              <button
                key={product.id}
                onClick={() => addToCart(product)}
                style={{
                  textAlign: isFa ? 'right' : 'left',
                  backgroundColor: theme.colors.surface,
                  border: `1px solid ${theme.colors.border}`,
                  borderRadius: theme.borderRadius.lg,
                  padding: theme.spacing.md,
                  cursor: 'pointer',
                  boxShadow: theme.shadows.card,
                }}
              >
                <div style={{ fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>{product.name}</div>
                <div style={{ fontSize: '11px', color: theme.colors.textMuted, marginBottom: 8 }}>{product.sku}</div>
                <div style={{ fontSize: '16px', fontWeight: 800, color: theme.colors.primary }}>
                  {formatMoney(product.price, isFa)}
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      <div
        style={{
          flex: 1,
          minWidth: 280,
          backgroundColor: theme.colors.surfaceElevated,
          border: `1px solid ${theme.colors.border}`,
          borderRadius: theme.borderRadius.lg,
          padding: theme.spacing.lg,
        }}
      >
        <h3 style={{ margin: '0 0 12px', fontSize: '16px', fontWeight: 800, color: theme.colors.textPrimary }}>
          {isFa ? `سبد (${cart.reduce((sum, l) => sum + l.quantity, 0)})` : `Cart (${cart.reduce((sum, l) => sum + l.quantity, 0)})`}
        </h3>

        {cart.length === 0 ? (
          <p style={{ color: theme.colors.textMuted, fontSize: '13px', textAlign: 'center', padding: '24px 0' }}>
            {isFa ? 'سبد خالی است' : 'The cart is empty'}
          </p>
        ) : (
          cart.map((line, index) => (
            <div
              key={line.productId}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                gap: theme.spacing.sm,
                borderBottom: `1px solid ${theme.colors.border}`,
                paddingBottom: 8,
                marginBottom: 8,
                fontSize: '13px',
              }}
            >
              <div>
                <div style={{ fontWeight: 700, color: theme.colors.textPrimary }}>{line.productName}</div>
                <div style={{ color: theme.colors.textSecondary }}>
                  {quantityWithUnit(line.quantity, line.unit, language)} × {formatMoney(line.price, isFa)}
                </div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
                <span style={{ fontWeight: 700, color: theme.colors.textPrimary }}>
                  {formatMoney(line.price * line.quantity, isFa)}
                </span>
                <button
                  onClick={() => setCart((prev) => prev.filter((_, i) => i !== index))}
                  style={{ background: 'none', border: 'none', color: theme.colors.error, cursor: 'pointer' }}
                >
                  ✕
                </button>
              </div>
            </div>
          ))
        )}

        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.sm, marginTop: theme.spacing.lg }}>
          <SelectInput label={s.warehouse} required value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)}>
            <option value="">{s.selectWarehouse}</option>
            {warehouses.map((w) => (
              <option key={w.id} value={w.id}>
                {w.name}
              </option>
            ))}
          </SelectInput>

          <SelectInput label={s.customer} value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
            <option value="">{isFa ? 'مشتری حضوری' : 'Walk-in customer'}</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </SelectInput>

          <SelectInput label={s.paymentMethod} value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
            <option value="CASH">{isFa ? 'نقدی' : 'Cash'}</option>
            <option value="CARD">{isFa ? 'کارت' : 'Card'}</option>
            <option value="BANK_TRANSFER">{isFa ? 'حواله' : 'Bank transfer'}</option>
          </SelectInput>

          {settings?.canOverrideTaxPerOrder && (
            <TextInput
              label={`${s.orderTaxRate} (%)`}
              type="number"
              min={0}
              max={100}
              step="0.01"
              value={taxRate ?? settings.defaultTaxRate}
              onChange={(e) => setTaxRate(e.target.value === '' ? '' : Number(e.target.value))}
              hint={s.taxFromBusiness}
            />
          )}
        </div>

        <div
          style={{
            marginTop: theme.spacing.lg,
            borderTop: `2px solid ${theme.colors.border}`,
            paddingTop: theme.spacing.md,
            fontSize: '13px',
          }}
        >
          <Line label={s.subtotal} value={formatMoney(subtotal, isFa)} />
          <Line label={s.discount} value={formatMoney(discount, isFa)} />
          <Line
            label={`${s.tax} (${isFa ? formatPercentFa(effectiveRate) : `${formatPercent(effectiveRate)}%`})`}
            value={formatMoney(tax, isFa)}
          />
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              fontSize: '18px',
              fontWeight: 800,
              color: theme.colors.primary,
              marginTop: 8,
            }}
          >
            <span>{s.total}</span>
            <span>{formatMoney(total, isFa)}</span>
          </div>
        </div>

        <div style={{ marginTop: theme.spacing.md }}>
          <PrimaryButton
            onClick={checkout}
            disabled={isSubmitting || cart.length === 0 || !warehouseId}
            style={{ width: '100%' }}
          >
            {isSubmitting ? (isFa ? 'در حال ثبت…' : 'Registering…') : s.registerOrder}
          </PrimaryButton>
        </div>
      </div>
    </div>
  );
};

const Line: React.FC<{ label: string; value: string }> = ({ label, value }) => (
  <div style={{ display: 'flex', justifyContent: 'space-between', padding: '3px 0' }}>
    <span>{label}</span>
    <span>{value}</span>
  </div>
);