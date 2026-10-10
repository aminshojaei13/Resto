import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { StatusBadge, StatusVariant } from '../components/StatusBadge';
import { FormError, ModalShell, PrimaryButton, SecondaryButton } from '../components/Field';
import { SalesOrder } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings, orderSourceLabel } from '../i18n/opsStrings';
import { formatMoney } from '../util/money';
import { formatPercent, formatPercentFa, quantityWithUnit } from '../util/units';

interface OrdersPageProps {
  language?: 'fa' | 'en';
}

/**
 * The order list.
 *
 * Every row is a real order from the server: the list, the search and the
 * status filters all run there, so what is shown cannot drift from what was
 * actually registered.
 */
export const OrdersPage: React.FC<OrdersPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [orders, setOrders] = useState<SalesOrder[]>([]);
  const [total, setTotal] = useState(0);
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  const [detailId, setDetailId] = useState<string | null>(null);
  const [detail, setDetail] = useState<SalesOrder | null>(null);
  const [detailError, setDetailError] = useState('');
  const [isActing, setIsActing] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const page = await apiClient.getOrders({ q: appliedSearch, status, perPage: 50 });
      setOrders(page.data);
      setTotal(page.total);
    } catch (err) {
      // A failed request stays a failure. The screen says so rather than
      // showing an empty list that looks like "no orders yet".
      setOrders([]);
      setTotal(0);
      setErrorMessage(err instanceof ApiError ? err.message : isFa ? 'دریافت سفارش‌ها ناموفق بود.' : 'Could not load orders.');
    } finally {
      setIsLoading(false);
    }
  }, [appliedSearch, status, isFa]);

  useEffect(() => {
    void load();
  }, [load]);

  const openDetail = async (id: string) => {
    setDetailId(id);
    setDetail(null);
    setDetailError('');
  };

  useEffect(() => {
    if (!detailId) return;

    apiClient
      .getOrder(detailId)
      .then(setDetail)
      .catch((err) =>
        setDetailError(err instanceof ApiError ? err.message : isFa ? 'دریافت جزئیات سفارش ناموفق بود.' : 'Could not load the order.')
      );
  }, [detailId, isFa]);

  const runAction = async (action: 'prepare' | 'pay' | 'cancel' | 'refund') => {
    if (!detailId) return;

    setIsActing(true);
    setDetailError('');

    try {
      if (action === 'prepare') await apiClient.prepareOrder(detailId);
      if (action === 'pay') await apiClient.payOrder(detailId);
      if (action === 'cancel') await apiClient.cancelOrder(detailId);
      if (action === 'refund') await apiClient.refundOrder(detailId);

      setDetail(await apiClient.getOrder(detailId));
      await load();
    } catch (err) {
      setDetailError(err instanceof ApiError ? err.message : isFa ? 'انجام عملیات ناموفق بود.' : 'The action failed.');
    } finally {
      setIsActing(false);
    }
  };

  const tabs = useMemo(
    () => [
      { value: '', label: s.filterAll },
      { value: 'NEW', label: s.filterNew },
      { value: 'PENDING_PAYMENT', label: s.filterPendingPayment },
      { value: 'PAID', label: s.filterPaid },
      { value: 'PREPARING', label: s.filterPreparing },
      { value: 'COMPLETED', label: s.filterCompleted },
      { value: 'CANCELLED', label: s.filterCancelled },
    ],
    [s]
  );

  const paymentBadge = (order: SalesOrder): { label: string; variant: StatusVariant } => {
    switch (order.paymentStatus) {
      case 'PAID':
        return { label: s.paid, variant: 'success' };
      case 'PENDING':
        return { label: s.statusPending, variant: 'warning' };
      case 'REFUNDED':
        return { label: s.statusRefunded, variant: 'neutral' };
      default:
        return { label: order.paymentStatus, variant: 'neutral' };
    }
  };

  const fulfilmentBadge = (order: SalesOrder): { label: string; variant: StatusVariant } => {
    switch (order.fulfillmentStatus) {
      case 'NEW':
        return { label: s.statusNew, variant: 'info' };
      case 'PREPARING':
        return { label: s.statusPreparing, variant: 'warning' };
      case 'COMPLETED':
        return { label: s.statusCompleted, variant: 'success' };
      case 'CANCELLED':
        return { label: s.statusCancelled, variant: 'error' };
      default:
        return { label: order.fulfillmentStatus, variant: 'neutral' };
    }
  };

  const can = (name: string) => detail?.availableActions?.includes(name) ?? false;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader title={s.ordersTitle} description={s.ordersSubtitle} />

      {/* Filters */}
      <div style={{ display: 'flex', gap: theme.spacing.sm, flexWrap: 'wrap' }}>
        {tabs.map((tab) => (
          <button
            key={tab.value}
            onClick={() => setStatus(tab.value)}
            style={{
              padding: '8px 14px',
              borderRadius: theme.borderRadius.full,
              border: `1px solid ${status === tab.value ? theme.colors.primary : theme.colors.border}`,
              backgroundColor: status === tab.value ? theme.colors.primaryLight : theme.colors.surface,
              color: status === tab.value ? theme.colors.primaryDark : theme.colors.textSecondary,
              fontSize: '13px',
              fontWeight: 600,
              cursor: 'pointer',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Search */}
      <form
        onSubmit={(e) => {
          e.preventDefault();
          setAppliedSearch(search.trim());
        }}
        style={{ display: 'flex', gap: theme.spacing.sm, flexWrap: 'wrap' }}
      >
        <input
          type="search"
          value={search}
          onChange={(e) => {
            setSearch(e.target.value);
            setAppliedSearch(e.target.value.trim());
          }}
          placeholder={s.searchOrders}
          style={{
            flex: 1,
            minWidth: 220,
            padding: '10px 14px',
            borderRadius: theme.borderRadius.md,
            border: `1px solid ${theme.colors.border}`,
            backgroundColor: theme.colors.surfaceElevated,
            color: theme.colors.textPrimary,
            fontSize: '14px',
          }}
        />
        <SecondaryButton type="submit">{s.searchOrders.split(' ')[0]}</SecondaryButton>
      </form>

      <FormError message={errorMessage} />

      {isLoading ? (
        <p style={{ color: theme.colors.textSecondary, fontSize: '14px' }}>
          {isFa ? 'در حال بارگذاری…' : 'Loading…'}
        </p>
      ) : orders.length === 0 ? (
        <EmptyState
          title={s.noOrders}
          description={s.noOrdersBody}
          icon="🧾"
        />
      ) : (
        <>
          <div
            style={{
              backgroundColor: theme.colors.surface,
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.xl,
              overflow: 'hidden',
              boxShadow: theme.shadows.card,
            }}
          >
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', minWidth: 820 }}>
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
                    <th style={{ padding: theme.spacing.lg }}>{s.orderNumber}</th>
                    <th style={{ padding: theme.spacing.lg }}>{s.customer}</th>
                    <th style={{ padding: theme.spacing.lg }}>{s.itemsCount}</th>
                    <th style={{ padding: theme.spacing.lg }}>{s.total}</th>
                    <th style={{ padding: theme.spacing.lg }}>{s.paymentStatus}</th>
                    <th style={{ padding: theme.spacing.lg }}>{s.source}</th>
                    <th style={{ padding: theme.spacing.lg }}>{s.view}</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                      <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.textPrimary }}>
                        <div>#{order.orderNumber}</div>
                        <div style={{ fontWeight: 400, fontSize: '12px', color: theme.colors.textMuted }}>
                          {new Date(order.createdAt).toLocaleString(isFa ? 'fa-IR' : 'en-GB')}
                        </div>
                      </td>
                      <td style={{ padding: theme.spacing.lg, color: theme.colors.textPrimary }}>{order.customerName}</td>
                      <td style={{ padding: theme.spacing.lg }}>
                        {isFa ? `${order.itemsCount} قلم` : order.itemsCount}
                      </td>
                      <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.primaryDark }}>
                        {formatMoney(order.totalAmount, isFa)}
                      </td>
                      <td style={{ padding: theme.spacing.lg }}>
                        <StatusBadge label={paymentBadge(order).label} variant={paymentBadge(order).variant} />
                      </td>
                      <td style={{ padding: theme.spacing.lg }}>
                        <div style={{ marginBottom: 4 }}>
                          <StatusBadge
                            label={fulfilmentBadge(order).label}
                            variant={fulfilmentBadge(order).variant}
                          />
                        </div>
                        <span style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                          {orderSourceLabel(order.source ?? 'POS', language)}
                        </span>
                      </td>
                      <td style={{ padding: theme.spacing.lg }}>
                        <button
                          onClick={() => openDetail(order.id)}
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
                          {s.view}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <p style={{ fontSize: '12px', color: theme.colors.textSecondary, margin: 0 }}>
            {isFa ? `${total} سفارش` : `${total} orders`}
          </p>
        </>
      )}

      {detailId && (
        <ModalShell title={s.orderDetail} onClose={() => setDetailId(null)} maxWidth={720}>
          <FormError message={detailError} />

          {!detail ? (
            <p style={{ color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg }}>
              <div style={{ display: 'flex', gap: theme.spacing.sm, flexWrap: 'wrap' }}>
                <StatusBadge label={paymentBadge(detail).label} variant={paymentBadge(detail).variant} />
                <StatusBadge label={fulfilmentBadge(detail).label} variant={fulfilmentBadge(detail).variant} />
                <StatusBadge label={orderSourceLabel(detail.source ?? 'POS', language)} variant="neutral" />
              </div>

              <section>
                <h4 style={{ margin: '0 0 6px', fontSize: '13px', color: theme.colors.textSecondary }}>{s.customer}</h4>
                <p style={{ margin: 0, fontSize: '15px', fontWeight: 700, color: theme.colors.textPrimary }}>
                  {detail.customerName}
                </p>
                <p style={{ margin: 0, fontSize: '12px', color: theme.colors.textMuted }}>
                  {orderSourceLabel(detail.source ?? 'POS', language)} ·{' '}
                  {new Date(detail.createdAt).toLocaleString(isFa ? 'fa-IR' : 'en-GB')}
                </p>
              </section>

              <section>
                <h4 style={{ margin: '0 0 6px', fontSize: '13px', color: theme.colors.textSecondary }}>{s.itemsCount}</h4>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
                  <tbody>
                    {detail.items.map((item, index) => (
                      <tr key={`${item.productId}-${index}`} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                        <td style={{ padding: '8px 0', fontWeight: 600, color: theme.colors.textPrimary }}>
                          {item.productName}
                        </td>
                        <td style={{ padding: '8px 0', color: theme.colors.textSecondary }}>
                          {quantityWithUnit(item.quantity, item.unit, language)}
                        </td>
                        <td style={{ padding: '8px 0', textAlign: isFa ? 'left' : 'right', fontWeight: 700 }}>
                          {formatMoney(item.totalPrice, isFa)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </section>

              <section
                style={{
                  backgroundColor: theme.colors.background,
                  borderRadius: theme.borderRadius.lg,
                  padding: theme.spacing.lg,
                  fontSize: '13px',
                }}
              >
                <Row label={s.subtotal} value={formatMoney(detail.subtotal, isFa)} />
                <Row label={s.discount} value={formatMoney(detail.discountAmount, isFa)} />
                <Row
                  label={`${s.tax} (${isFa ? formatPercentFa(detail.taxRate ?? 0) : `${formatPercent(detail.taxRate ?? 0)}%`})`}
                  value={formatMoney(detail.taxAmount, isFa)}
                />
                <Row label={s.total} value={formatMoney(detail.totalAmount, isFa)} strong />
              </section>

              {detail.timeline && detail.timeline.length > 0 && (
                <section>
                  <h4 style={{ margin: '0 0 6px', fontSize: '13px', color: theme.colors.textSecondary }}>{s.timeline}</h4>
                  <ul style={{ margin: 0, paddingInlineStart: 18, fontSize: '13px', color: theme.colors.textPrimary }}>
                    {detail.timeline.map((event, index) => (
                      <li key={`${event.status}-${index}`}>
                        {isFa ? event.label : event.status} —{' '}
                        {new Date(event.at).toLocaleString(isFa ? 'fa-IR' : 'en-GB')}
                      </li>
                    ))}
                  </ul>
                </section>
              )}

              <div style={{ display: 'flex', gap: theme.spacing.sm, flexWrap: 'wrap' }}>
                {can('prepare') && (
                  <SecondaryButton disabled={isActing} onClick={() => runAction('prepare')}>
                    {s.prepare}
                  </SecondaryButton>
                )}
                {can('pay') && (
                  <SecondaryButton disabled={isActing} onClick={() => runAction('pay')}>
                    {s.pay}
                  </SecondaryButton>
                )}
                {can('cancel') && (
                  <SecondaryButton disabled={isActing} onClick={() => runAction('cancel')}>
                    {s.cancel}
                  </SecondaryButton>
                )}
                {can('refund') && (
                  <SecondaryButton disabled={isActing} onClick={() => runAction('refund')}>
                    {s.refund}
                  </SecondaryButton>
                )}
              </div>
            </div>
          )}
        </ModalShell>
      )}
    </div>
  );
};

const Row: React.FC<{ label: string; value: string; strong?: boolean }> = ({ label, value, strong }) => (
  <div
    style={{
      display: 'flex',
      justifyContent: 'space-between',
      padding: '4px 0',
      fontWeight: strong ? 800 : 400,
      fontSize: strong ? '16px' : '13px',
    }}
  >
    <span>{label}</span>
    <span>{value}</span>
  </div>
);
