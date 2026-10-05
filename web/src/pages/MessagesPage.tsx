import React, { useEffect, useState } from 'react';
import { ApiError, apiClient, getCurrentContext } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { FormError, InfoNote, PrimaryButton, SecondaryButton } from '../components/Field';
import { useTheme } from '../theme/ThemeContext';
import { messageExamples, opsStrings } from '../i18n/opsStrings';
import { formatMoney } from '../util/money';
import { formatPercent, formatPercentFa, quantityWithUnit } from '../util/units';

interface MessagesPageProps {
  language?: 'fa' | 'en';
}

interface DraftLine {
  productId: string;
  productName: string;
  sku: string;
  unit?: string;
  unitLabel?: string;
  price: number;
  quantity: number;
  discountPercent: number;
}

interface Draft {
  id: string;
  customer: { id?: string; name?: string; phone?: string; address?: string; is_new: boolean };
  items: DraftLine[];
  unmatched_items: { raw_text: string; product_name: string; quantity: number; reason: string }[];
  tax_rate: number;
  payment_method: string;
  subtotal: number;
  discount_amount: number;
  tax_amount: number;
  grand_total: number;
}

const STEPS = ['messageStep1', 'messageStep2', 'messageStep3', 'messageStep4', 'messageStep5', 'messageStep6'] as const;

/**
 * Order from a customer message.
 *
 * Paste what the customer wrote, read the draft, correct anything the parser
 * could not read, then register the order through the normal order flow. The
 * parsing is deterministic rule matching — it is not described as AI, because
 * it is not AI.
 */
export const MessagesPage: React.FC<MessagesPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [rawText, setRawText] = useState('');
  const [draft, setDraft] = useState<Draft | null>(null);
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [orderPaymentStatus, setOrderPaymentStatus] = useState('PENDING');
  const [warehouses, setWarehouses] = useState<{ id: string; name: string }[]>([]);
  const [catalogProducts, setCatalogProducts] = useState<{ id: string; name: string; sku: string; price: number; unit?: string }[]>([]);
  const [warehouseId, setWarehouseId] = useState('');
  const [showExamples, setShowExamples] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [notice, setNotice] = useState('');
  const [isParsing, setIsParsing] = useState(false);
  const [isCheckingOut, setIsCheckingOut] = useState(false);

  useEffect(() => {
    apiClient
      .getWarehouses()
      .then((list) => {
        setWarehouses(list);
        setWarehouseId(list.length === 1 ? list[0].id : getCurrentContext().warehouseId);
      })
      .catch(() => setWarehouses([]));

    apiClient
      .getProducts()
      .then(setCatalogProducts)
      .catch(() => setCatalogProducts([]));
  }, []);

  const addCatalogProductToDraft = (product: { id: string; name: string; sku: string; price: number; unit?: string }, quantity: number = 1) => {
    setDraft((prev) => {
      if (!prev) return prev;

      const existingIndex = prev.items.findIndex((line) => line.productId === product.id);

      let updatedItems: DraftLine[];
      if (existingIndex >= 0) {
        updatedItems = prev.items.map((line, i) =>
          i === existingIndex ? { ...line, quantity: line.quantity + quantity } : line
        );
      } else {
        updatedItems = [
          ...prev.items,
          {
            productId: product.id,
            productName: product.name,
            sku: product.sku,
            unit: product.unit,
            price: product.price,
            quantity: quantity,
            discountPercent: 0,
          },
        ];
      }

      return {
        ...prev,
        items: updatedItems,
      };
    });
  };

  const review = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!rawText.trim()) return;

    setIsParsing(true);
    setErrorMessage('');

    try {
      const result = await apiClient.parseMessage(rawText.trim(), 'manual_paste');

      const mappedDraft: Draft = {
        id: result.id,
        customer: result.customer || { is_new: true },
        items: (result.items || []).map((item: any) => ({
          productId: item.product_id || item.productId || '',
          productName: item.product_name || item.productName || '',
          sku: item.sku || '',
          unit: item.unit,
          unitLabel: item.unit_label || item.unitLabel,
          price: Number(item.price) || 0,
          quantity: Number(item.quantity) || 1,
          discountPercent: Number(item.discount_percent ?? item.discountPercent) || 0,
        })),
        unmatched_items: result.unmatched_items || [],
        tax_rate: Number(result.tax_rate) || 0,
        payment_method: result.payment_method || 'CASH',
        subtotal: Number(result.subtotal) || 0,
        discount_amount: Number(result.discount_amount) || 0,
        tax_amount: Number(result.tax_amount) || 0,
        grand_total: Number(result.grand_total) || 0,
      };

      setDraft(mappedDraft);
      setCustomerName(mappedDraft.customer?.name ?? '');
      setCustomerPhone(mappedDraft.customer?.phone ?? '');
      setPaymentMethod(mappedDraft.payment_method ?? 'CASH');
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : isFa ? 'بررسی پیام ناموفق بود.' : 'Could not read the message.');
    } finally {
      setIsParsing(false);
    }
  };

  const updateLine = (index: number, patch: Partial<DraftLine>) => {
    setDraft((prev) =>
      prev
        ? {
            ...prev,
            items: prev.items.map((line, i) => (i === index ? { ...line, ...patch } : line)),
          }
        : prev
    );
  };

  const removeLine = (index: number) => {
    setDraft((prev) => (prev ? { ...prev, items: prev.items.filter((_, i) => i !== index) } : prev));
  };

  /**
   * The preview is recalculated from the corrected lines using the same
   * arithmetic as the server; the server recomputes it again on checkout and
   * that value is what is stored.
   */
  const draftLines = draft?.items ?? [];
  const draftTaxRate = draft?.tax_rate ?? 0;

  const totals = draftLines.reduce(
    (acc, line) => {
      const subtotal = Math.round(line.price * line.quantity * 100) / 100;
      const discount = Math.round(subtotal * (line.discountPercent / 100) * 100) / 100;
      const taxable = Math.round((subtotal - discount) * 100) / 100;
      const tax = Math.round(taxable * (draftTaxRate / 100) * 100) / 100;

      acc.subtotal += subtotal;
      acc.discount += discount;
      acc.tax += tax;
      return acc;
    },
    { subtotal: 0, discount: 0, tax: 0 }
  );

  const grandTotal = Math.round((totals.subtotal - totals.discount + totals.tax) * 100) / 100;

  const checkout = async () => {
    if (!draft || draft.items.length === 0) return;

    setIsCheckingOut(true);
    setErrorMessage('');

    try {
      const validItems = draft.items
        .map((line: any) => ({
          productId: line.productId || line.product_id || '',
          quantity: Math.max(1, Number(line.quantity) || 1),
          discountPercent: Number(line.discountPercent ?? line.discount_percent) || 0,
        }))
        .filter((item) => Boolean(item.productId && item.productId.trim()));

      if (validItems.length === 0) {
        setErrorMessage(isFa ? 'هیچ کالای معتبری در پیش‌نویس برای ثبت وجود ندارد.' : 'No valid items in draft to checkout.');
        return;
      }

      const order = await apiClient.checkout({
        warehouseId,
        customerId: draft.customer.id,
        customerName: customerName.trim() || undefined,
        paymentMethod,
        paymentStatus: orderPaymentStatus,
        source: 'MESSAGE',
        items: validItems,
      });

      setNotice(
        isFa
          ? `سفارش ${order.orderNumber} با موفقیت ثبت شد (${order.paymentStatus === 'PAID' ? 'پرداخت‌شده' : 'در انتظار پرداخت'}).`
          : `Order ${order.orderNumber} registered (${order.paymentStatus === 'PAID' ? 'Paid' : 'Pending payment'}).`
      );

      setDraft(null);
      setRawText('');
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : isFa ? 'ثبت سفارش ناموفق بود.' : 'Could not register the order.');
    } finally {
      setIsCheckingOut(false);
    }
  };

  const examples = messageExamples(language);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader title={s.messageTitle} description={s.messageSubtitle} />

      {/* The six steps, always visible so the flow is never a mystery. */}
      <ol
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
          gap: theme.spacing.sm,
          listStyle: 'none',
          margin: 0,
          padding: 0,
        }}
      >
        {STEPS.map((step, index) => (
          <li
            key={step}
            style={{
              backgroundColor: theme.colors.surface,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.lg,
              padding: theme.spacing.md,
              fontSize: '12px',
              fontWeight: 600,
              color: theme.colors.textSecondary,
            }}
          >
            <span style={{ color: theme.colors.primary, fontWeight: 800, marginInlineEnd: 6 }}>{index + 1}.</span>
            {s[step]}
          </li>
        ))}
      </ol>

      <FormError message={errorMessage} />
      <FormError message={notice} />

      {/* 1. paste */}
      <form onSubmit={review} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: theme.spacing.sm }}>
          <label style={{ fontSize: '13px', fontWeight: 700, color: theme.colors.textPrimary }}>{s.messageInputLabel}</label>
          <SecondaryButton type="button" onClick={() => setShowExamples((v) => !v)}>
            {showExamples ? s.hideExamples : s.showExamples}
          </SecondaryButton>
        </div>

        <textarea
          value={rawText}
          onChange={(e) => setRawText(e.target.value)}
          placeholder={s.messageInputPlaceholder}
          rows={8}
          style={{
            width: '100%',
            padding: theme.spacing.md,
            borderRadius: theme.borderRadius.lg,
            border: `1px solid ${theme.colors.border}`,
            backgroundColor: theme.colors.surfaceElevated,
            color: theme.colors.textPrimary,
            fontSize: '14px',
            lineHeight: 1.9,
            boxSizing: 'border-box',
            fontFamily: isFa ? 'inherit' : 'ui-monospace, monospace',
          }}
        />

        <PrimaryButton type="submit" disabled={isParsing || !rawText.trim()}>
          {isParsing ? (isFa ? 'در حال بررسی…' : 'Reading…') : `🔍 ${s.reviewMessage}`}
        </PrimaryButton>
      </form>

      {showExamples && (
        <InfoNote title={s.examplesTitle}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 8 }}>
            {examples.map((example, index) => (
              <button
                key={index}
                onClick={() => setRawText(example)}
                style={{
                  textAlign: isFa ? 'right' : 'left',
                  backgroundColor: theme.colors.background,
                  border: `1px solid ${theme.colors.border}`,
                  borderRadius: theme.borderRadius.md,
                  padding: theme.spacing.md,
                  color: theme.colors.textPrimary,
                  fontSize: '13px',
                  cursor: 'pointer',
                  whiteSpace: 'pre-wrap',
                  lineHeight: 1.9,
                }}
              >
                {example}
              </button>
            ))}
          </div>
        </InfoNote>
      )}

      {/* 2-5. review and correct */}
      {!draft ? (
        <p style={{ color: theme.colors.textSecondary, fontSize: '13px' }}>{s.nothingToReview}</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg }}>
          {/* customer */}
          <section
            style={{
              backgroundColor: theme.colors.surface,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.xl,
              padding: theme.spacing.lg,
            }}
          >
            <h3 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {s.customer}
            </h3>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: theme.spacing.md }}>
              <input
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder={isFa ? 'نام مشتری' : 'Customer name'}
                style={inputStyle(theme)}
              />
              <input
                value={customerPhone}
                onChange={(e) => setCustomerPhone(e.target.value)}
                placeholder={isFa ? 'شماره تماس' : 'Phone'}
                style={inputStyle(theme)}
              />
              <select value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)} style={inputStyle(theme)}>
                <option value="CASH">{isFa ? 'نقدی' : 'Cash'}</option>
                <option value="CARD">{isFa ? 'کارت' : 'Card'}</option>
                <option value="BANK_TRANSFER">{isFa ? 'حواله' : 'Bank transfer'}</option>
              </select>
              <select value={orderPaymentStatus} onChange={(e) => setOrderPaymentStatus(e.target.value)} style={inputStyle(theme)}>
                <option value="PENDING">{isFa ? 'در انتظار پرداخت' : 'Pending payment'}</option>
                <option value="PAID">{isFa ? 'پرداخت شده' : 'Paid'}</option>
              </select>
              <select value={warehouseId} onChange={(e) => setWarehouseId(e.target.value)} style={inputStyle(theme)}>
                <option value="">{s.selectWarehouse}</option>
                {warehouses.map((w) => (
                  <option key={w.id} value={w.id}>
                    {w.name}
                  </option>
                ))}
              </select>
            </div>
          </section>

          {/* items */}
          <section
            style={{
              backgroundColor: theme.colors.surface,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.xl,
              padding: theme.spacing.lg,
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                {isFa ? 'کالاهای سفارش' : 'Order Items'}
              </h3>

              {catalogProducts.length > 0 && (
                <select
                  defaultValue=""
                  onChange={(e) => {
                    const selected = catalogProducts.find((p) => p.id === e.target.value);
                    if (selected) {
                      addCatalogProductToDraft(selected, 1);
                      e.target.value = '';
                    }
                  }}
                  style={{ ...inputStyle(theme), width: 'auto', minWidth: 200, fontSize: '12px' }}
                >
                  <option value="">{isFa ? '＋ افزودن دستی کالا از انبار…' : '＋ Add product manually…'}</option>
                  {catalogProducts.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name} ({formatMoney(p.price, isFa)})
                    </option>
                  ))}
                </select>
              )}
            </div>

            {draft.items.length === 0 ? (
              <p style={{ margin: 0, fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'هیچ کالایی متصل نشده است. از لیست زیر یا انبار کالا را انتخاب کنید.' : 'No items attached yet. Match or select a product below.'}
              </p>
            ) : (
              draft.items.map((line: any, index) => {
                const pId = line.productId || line.product_id || '';
                const pName = line.productName || line.product_name || (isFa ? 'کالای انتخابی' : 'Selected Product');
                const pSku = line.sku || '';

                return (
                  <div
                    key={`${pId}-${index}`}
                    style={{
                      display: 'flex',
                      gap: theme.spacing.sm,
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      padding: '10px 0',
                      borderBottom: `1px solid ${theme.colors.border}`,
                    }}
                  >
                    <div style={{ flex: 1, minWidth: 180 }}>
                      <div style={{ fontWeight: 700, fontSize: '13px', color: theme.colors.textPrimary }}>{pName}</div>
                      <div style={{ fontSize: '11px', color: theme.colors.textMuted }}>
                        {pSku ? `${pSku} · ` : ''}{formatMoney(line.price, isFa)} / {line.unitLabel ?? line.unit}
                      </div>
                    </div>

                  <input
                    type="number"
                    min={1}
                    value={line.quantity}
                    onChange={(e) => updateLine(index, { quantity: Math.max(1, Number(e.target.value) || 1) })}
                    style={{ ...inputStyle(theme), width: 90 }}
                  />
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    value={line.discountPercent}
                    onChange={(e) => updateLine(index, { discountPercent: Number(e.target.value) || 0 })}
                    style={{ ...inputStyle(theme), width: 90 }}
                    title={s.discount}
                  />

                  <button
                    onClick={() => removeLine(index)}
                    style={{ background: 'none', border: 'none', color: theme.colors.error, cursor: 'pointer', fontSize: '15px' }}
                  >
                    ✕
                  </button>
                </div>
                );
              })
            )}
          </section>

          {/* lines the parser could not read */}
          {draft.unmatched_items.length > 0 && (
            <section
              style={{
                backgroundColor: theme.colors.warningLight,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.xl,
                padding: theme.spacing.lg,
              }}
            >
              <h3 style={{ margin: '0 0 8px', fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                {s.unmatchedLines}
              </h3>
              <p style={{ margin: '0 0 12px', fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'متن‌های زیر مستقیماً به کالا متصل نشدند. می‌توانید کالای مربوطه را از انبار انتخاب کنید:' : s.unmatchedExplain}
              </p>
              {draft.unmatched_items.map((line, index) => (
                <div
                  key={index}
                  style={{
                    display: 'flex',
                    gap: theme.spacing.md,
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                    padding: '8px 0',
                    borderBottom: index < draft.unmatched_items.length - 1 ? `1px dashed ${theme.colors.border}` : 'none',
                  }}
                >
                  <div style={{ fontSize: '13px', fontWeight: 600, color: theme.colors.textPrimary }}>
                    • {line.raw_text} ({quantityWithUnit(line.quantity, 'عدد', language)})
                  </div>

                  {catalogProducts.length > 0 && (
                    <select
                      defaultValue=""
                      onChange={(e) => {
                        const selected = catalogProducts.find((p) => p.id === e.target.value);
                        if (selected) {
                          addCatalogProductToDraft(selected, line.quantity || 1);
                          setDraft((prev) =>
                            prev
                              ? {
                                  ...prev,
                                  unmatched_items: prev.unmatched_items.filter((_, i) => i !== index),
                                }
                              : prev
                          );
                        }
                      }}
                      style={{ ...inputStyle(theme), maxWidth: 260, fontSize: '12px' }}
                    >
                      <option value="">{isFa ? '🔍 اتصال به کالا در انبار…' : 'Match to catalog product…'}</option>
                      {catalogProducts.map((p) => (
                        <option key={p.id} value={p.id}>
                          {p.name} ({formatMoney(p.price, isFa)})
                        </option>
                      ))}
                    </select>
                  )}
                </div>
              ))}
            </section>
          )}

          {/* totals */}
          <section
            style={{
              backgroundColor: theme.colors.background,
              borderRadius: theme.borderRadius.xl,
              padding: theme.spacing.lg,
              fontSize: '13px',
            }}
          >
            <TotalRow label={s.subtotal} value={formatMoney(totals.subtotal, isFa)} />
            <TotalRow label={s.discount} value={formatMoney(totals.discount, isFa)} />
            <TotalRow
              label={`${s.tax} (${isFa ? formatPercentFa(draft.tax_rate) : `${formatPercent(draft.tax_rate)}%`}) — ${s.taxFromBusiness}`}
              value={formatMoney(totals.tax, isFa)}
            />
            <TotalRow label={s.total} value={formatMoney(grandTotal, isFa)} strong />
          </section>

          <PrimaryButton
            onClick={checkout}
            disabled={isCheckingOut || draft.items.length === 0 || !warehouseId}
          >
            {isCheckingOut ? (isFa ? 'در حال ثبت…' : 'Registering…') : `🛒 ${s.fixAndCheckout}`}
          </PrimaryButton>
        </div>
      )}
    </div>
  );
};

const TotalRow: React.FC<{ label: string; value: string; strong?: boolean }> = ({ label, value, strong }) => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'space-between',
      padding: '5px 0',
      fontWeight: strong ? 800 : 400,
      fontSize: strong ? '16px' : '13px',
    }}
  >
    <span>{label}</span>
    <span>{value}</span>
  </div>
);

const inputStyle = (theme: any): React.CSSProperties => ({
  padding: '10px 12px',
  borderRadius: theme.borderRadius.md,
  border: `1px solid ${theme.colors.border}`,
  backgroundColor: theme.colors.surfaceElevated,
  color: theme.colors.textPrimary,
  fontSize: '13px',
  boxSizing: 'border-box',
  width: '100%',
});