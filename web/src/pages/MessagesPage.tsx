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
  const [warehouses, setWarehouses] = useState<{ id: string; name: string }[]>([]);
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
  }, []);

  const review = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!rawText.trim()) return;

    setIsParsing(true);
    setErrorMessage('');

    try {
      const result = await apiClient.parseMessage(rawText.trim(), 'manual_paste');
      setDraft(result);
      setCustomerName(result.customer?.name ?? '');
      setCustomerPhone(result.customer?.phone ?? '');
      setPaymentMethod(result.payment_method ?? 'CASH');
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
      const order = await apiClient.checkout({
        warehouseId,
        customerId: draft.customer.id,
        customerName: customerName.trim() || undefined,
        paymentMethod,
        source: 'MESSAGE',
        items: draft.items.map((line) => ({
          productId: line.productId,
          quantity: Math.max(1, line.quantity),
          discountPercent: line.discountPercent,
        })),
      });

      setNotice(
        isFa
          ? `سفارش ${order.orderNumber} ثبت شد و موجودی انبار کسر گردید.`
          : `Order ${order.orderNumber} registered and stock deducted.`
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
            <h3 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
              {isFa ? 'کالاها' : 'Items'}
            </h3>

            {draft.items.length === 0 ? (
              <p style={{ margin: 0, fontSize: '13px', color: theme.colors.textSecondary }}>
                {isFa ? 'هیچ کالایی از این پیام خوانده نشد.' : 'No items could be read from this message.'}
              </p>
            ) : (
              draft.items.map((line, index) => (
                <div
                  key={`${line.productId}-${index}`}
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
                    <div style={{ fontWeight: 700, fontSize: '13px', color: theme.colors.textPrimary }}>{line.productName}</div>
                    <div style={{ fontSize: '11px', color: theme.colors.textMuted }}>
                      {line.sku} · {formatMoney(line.price, isFa)} / {line.unitLabel ?? line.unit}
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
              ))
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
              <p style={{ margin: '0 0 8px', fontSize: '13px', color: theme.colors.textSecondary }}>{s.unmatchedExplain}</p>
              {draft.unmatched_items.map((line, index) => (
                <div key={index} style={{ fontSize: '13px', color: theme.colors.textPrimary }}>
                  • {line.raw_text}
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