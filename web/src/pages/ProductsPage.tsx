import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { FormError, ModalShell, PrimaryButton, SecondaryButton, SelectInput, TextInput } from '../components/Field';
import { Product, UnitOption } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { formatMoney } from '../util/money';
import { quantityWithUnit, unitLabel } from '../util/units';

interface ProductsPageProps {
  language?: 'fa' | 'en';
}

/**
 * The product catalog.
 *
 * A product declares the unit it is counted in once, here, and that unit then
 * reads the same everywhere: stock, purchases, receiving, orders and reports.
 */
export const ProductsPage: React.FC<ProductsPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [products, setProducts] = useState<Product[]>([]);
  const [units, setUnits] = useState<UnitOption[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [editing, setEditing] = useState<Product | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const [productList, unitList] = await Promise.all([apiClient.getProducts(), apiClient.getUnits()]);
      setProducts(productList ?? []);
      setUnits(unitList ?? []);
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : isFa ? 'دریافت کالاها ناموفق بود.' : 'Could not load products.'
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
      <PageHeader
        title={isFa ? 'کالاها' : 'Products'}
        description={
          isFa
            ? 'هر کالا واحد خودش را دارد؛ همان واحد در انبار، خرید و سفارش هم استفاده می‌شود.'
            : 'Each product declares the unit it is counted in; the same unit is used in stock, purchases and orders.'
        }
        actions={
          <PrimaryButton onClick={() => { setEditing(null); setIsOpen(true); }}>
            ➕ {isFa ? 'افزودن کالای جدید' : 'Add product'}
          </PrimaryButton>
        }
      />

      {units.length > 0 && (
        <div style={{ display: 'flex', gap: theme.spacing.sm, alignItems: 'center', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
            {isFa ? 'واحدهای سنجش تعریف‌شده:' : 'Measurement units:'}
          </span>
          {units.map((u) => (
            <span
              key={u.code}
              style={{
                fontSize: '12px',
                padding: '4px 10px',
                borderRadius: theme.borderRadius.full,
                backgroundColor: theme.colors.surfaceElevated,
                border: `1px solid ${theme.colors.border}`,
                color: theme.colors.textPrimary,
              }}
            >
              {u.label}
            </span>
          ))}
        </div>
      )}

      <FormError message={errorMessage} />

      {isLoading ? (
        <p style={{ color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
      ) : (products ?? []).length === 0 ? (
        <EmptyState
          title={isFa ? 'هنوز کالایی ثبت نشده است' : 'No products yet'}
          description={
            isFa
              ? 'کالا اضافه کنید تا موجودی انبار، خرید و سفارش‌ها قابل پیگیری باشد.'
              : 'Add products so stock, purchases and orders can be tracked.'
          }
          actionText={isFa ? 'افزودن کالا' : 'Add product'}
          onAction={() => { setEditing(null); setIsOpen(true); }}
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
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', minWidth: 720 }}>
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
                  <th style={{ padding: theme.spacing.lg }}>{isFa ? 'نام کالا' : 'Product'}</th>
                  <th style={{ padding: theme.spacing.lg }}>SKU</th>
                  <th style={{ padding: theme.spacing.lg }}>{s.unitOfProduct}</th>
                  <th style={{ padding: theme.spacing.lg }}>{isFa ? 'قیمت فروش' : 'Selling price'}</th>
                  <th style={{ padding: theme.spacing.lg }}>{isFa ? 'موجودی کل' : 'Total stock'}</th>
                  <th style={{ padding: theme.spacing.lg }}>{isFa ? 'عملیات' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody>
                {products.map((product) => {
                  const total = (product.stockByWarehouse ?? []).reduce((sum, w) => sum + w.quantity, 0);

                  return (
                    <tr key={product.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                      <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.textPrimary }}>
                        {product.name}
                        {product.category ? (
                          <div style={{ fontWeight: 400, fontSize: '11px', color: theme.colors.textMuted }}>
                            {product.category}
                          </div>
                        ) : null}
                      </td>
                      <td style={{ padding: theme.spacing.lg, fontFamily: 'monospace', color: theme.colors.textSecondary }}>
                        {product.sku}
                      </td>
                      <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>
                        {product.unitLabel ?? unitLabel(product.unit, language)}
                      </td>
                      <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.primaryDark }}>
                        {formatMoney(product.price, isFa)}
                      </td>
                      <td style={{ padding: theme.spacing.lg, color: theme.colors.textPrimary }}>
                        {quantityWithUnit(total, product.unit, language)}
                      </td>
                      <td style={{ padding: theme.spacing.lg }}>
                        <button
                          onClick={() => { setEditing(product); setIsOpen(true); }}
                          style={{
                            backgroundColor: theme.colors.primaryLight,
                            color: theme.colors.primaryDark,
                            border: 'none',
                            borderRadius: theme.borderRadius.md,
                            padding: '6px 12px',
                            fontWeight: 700,
                            fontSize: '12px',
                            cursor: 'pointer',
                          }}
                        >
                          {isFa ? 'ویرایش' : 'Edit'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {isOpen && (
        <ProductModal
          language={language}
          product={editing}
          units={units}
          onClose={() => setIsOpen(false)}
          onSaved={() => {
            setIsOpen(false);
            void load();
          }}
        />
      )}
    </div>
  );
};

const ProductModal: React.FC<{
  language: 'fa' | 'en';
  product: Product | null;
  units: UnitOption[];
  onClose: () => void;
  onSaved: () => void;
}> = ({ language, product, units, onClose, onSaved }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [name, setName] = useState(product?.name ?? '');
  const [sku, setSku] = useState(product?.sku ?? '');
  const [barcode, setBarcode] = useState(product?.barcode ?? '');
  const [category, setCategory] = useState(product?.category ?? '');
  const [price, setPrice] = useState<number | ''>(product?.price ?? 0);
  const [costPrice, setCostPrice] = useState<number | ''>(product?.costPrice ?? 0);
  const [unit, setUnit] = useState(product?.unit ?? 'piece');
  const [description, setDescription] = useState(product?.description ?? '');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();

    const next: Record<string, string> = {};
    if (!name.trim()) next.name = isFa ? 'نام کالا الزامی است.' : 'Product name is required.';
    if (!sku.trim()) next.sku = isFa ? 'کد کالا الزامی است.' : 'SKU is required.';
    if (!unit) next.unit = s.unitRequired;
    if (price === '' || Number(price) < 0) next.price = isFa ? 'قیمت نامعتبر است.' : 'Invalid price.';

    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setIsSubmitting(true);
    setFormError('');

    const payload = {
      name: name.trim(),
      sku: sku.trim(),
      barcode: barcode.trim(),
      category: category.trim(),
      price: Number(price),
      cost_price: Number(costPrice) || 0,
      unit,
      description: description.trim(),
    };

    try {
      if (product) {
        await apiClient.updateProduct(product.id, payload);
      } else {
        await apiClient.createProduct(payload);
      }

      onSaved();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ذخیره کالا ناموفق بود.' : 'Could not save the product.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalShell title={product ? (isFa ? `ویرایش ${product.name}` : `Edit ${product.name}`) : isFa ? 'کالای جدید' : 'New product'} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <FormError message={formError} />

        <TextInput label={isFa ? 'نام کالا' : 'Product name'} required value={name} error={errors.name} onChange={(e) => setName(e.target.value)} />

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md }}>
          <TextInput label="SKU" required value={sku} error={errors.sku} onChange={(e) => setSku(e.target.value)} />
          <TextInput label={isFa ? 'بارکد' : 'Barcode'} value={barcode} onChange={(e) => setBarcode(e.target.value)} />
        </div>

        {/* One unit vocabulary, offered as a choice — never typed free-hand. */}
        <SelectInput label={s.unitOfProduct} required value={unit} error={errors.unit} onChange={(e) => setUnit(e.target.value)}>
          <option value="">{s.chooseUnit}</option>
          {units.map((option) => (
            <option key={option.code} value={option.code}>
              {option.label}
            </option>
          ))}
        </SelectInput>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md }}>
          <TextInput
            label={isFa ? 'قیمت فروش' : 'Selling price'}
            required
            type="number"
            min={0}
            step="0.01"
            value={price}
            error={errors.price}
            onChange={(e) => setPrice(e.target.value === '' ? '' : Number(e.target.value))}
          />
          <TextInput
            label={isFa ? 'قیمت خرید' : 'Cost price'}
            type="number"
            min={0}
            step="0.01"
            value={costPrice}
            onChange={(e) => setCostPrice(e.target.value === '' ? '' : Number(e.target.value))}
          />
        </div>

        <TextInput label={isFa ? 'دسته‌بندی' : 'Category'} value={category} onChange={(e) => setCategory(e.target.value)} />

        <TextInput label={isFa ? 'توضیحات' : 'Description'} value={description} onChange={(e) => setDescription(e.target.value)} />

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md, marginTop: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : isFa ? 'ذخیره کالا' : 'Save product'}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};
const toFieldErrors = (fieldErrors: Record<string, string[]>): Record<string, string> =>
  Object.fromEntries(Object.entries(fieldErrors).map(([key, value]) => [key, value[0] ?? '']));
