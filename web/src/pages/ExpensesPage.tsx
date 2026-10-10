import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { EmptyState } from '../components/EmptyState';
import { FormError, InfoNote, ModalShell, PrimaryButton, SecondaryButton, SelectInput, TextInput } from '../components/Field';
import { Expense, ExpenseCategory, ExpenseSummary } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { formatMoney } from '../util/money';

interface ExpensesPageProps {
  language?: 'fa' | 'en';
}

/**
 * Operating expenses: money the business spends to keep running.
 *
 * Deliberately separate from purchasing. Buying goods is a purchase order that
 * creates stock; this is a cost of the period that creates neither stock nor a
 * payable. The screen says so, because the two being confused is the reported
 * problem.
 */
export const ExpensesPage: React.FC<ExpensesPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [categories, setCategories] = useState<ExpenseCategory[]>([]);
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [categoryFilter, setCategoryFilter] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [notice, setNotice] = useState('');

  const [editing, setEditing] = useState<Expense | null>(null);
  const [isOpen, setIsOpen] = useState(false);

  const load = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const [expenseList, categoryList, dashboard] = await Promise.all([
        apiClient.getExpenses(),
        apiClient.getExpenseCategories(),
        apiClient.getExpenseSummary(),
      ]);

      setExpenses(expenseList);
      setCategories(categoryList);
      setSummary(dashboard);
    } catch (err) {
      setErrorMessage(
        err instanceof ApiError ? err.message : isFa ? 'دریافت هزینه‌ها ناموفق بود.' : 'Could not load expenses.'
      );
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const visible = categoryFilter ? (expenses || []).filter((e) => e.category === categoryFilter) : (expenses || []);

  const remove = async (expense: Expense) => {
    try {
      await apiClient.deleteExpense(expense.id);
      setNotice(isFa ? 'هزینه حذف شد و سند حسابداری آن برگشت خورد.' : 'Expense removed and its journal entry reversed.');
      await load();
    } catch (err) {
      setErrorMessage(err instanceof ApiError ? err.message : isFa ? 'حذف هزینه ناموفق بود.' : 'Could not remove the expense.');
    }
  };

  const maxTrend = summary ? Math.max(1, ...summary.trend.map((t) => t.total)) : 1;
  const maxCategory = summary && summary.byCategory.length > 0 ? Math.max(1, ...summary.byCategory.map((c) => c.total)) : 1;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader
        title={s.expensesTitle}
        description={s.expensesSubtitle}
        actions={<PrimaryButton onClick={() => { setEditing(null); setIsOpen(true); }}>➕ {s.newExpense}</PrimaryButton>}
      />

      <InfoNote>{s.expensesPurpose}</InfoNote>
      <InfoNote>{s.expensesVsPurchase}</InfoNote>

      <FormError message={errorMessage} />
      <FormError message={notice} />

      {isLoading ? (
        <p style={{ color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری…' : 'Loading…'}</p>
      ) : (
        <>
          {summary && (
            <>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: theme.spacing.md }}>
                <Tile label={s.monthTotal} value={formatMoney(summary.monthTotal, isFa)} />
                <Tile label={s.todayTotal} value={formatMoney(summary.todayTotal, isFa)} />
                <Tile
                  label={s.vsLastMonth}
                  value={formatMoney(summary.previousMonthTotal, isFa)}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: theme.spacing.lg }}>
                <section
                  style={{
                    backgroundColor: theme.colors.surface,
                    border: `1px solid ${theme.colors.border}`,
                    borderRadius: theme.borderRadius.xl,
                    padding: theme.spacing.lg,
                  }}
                >
                  <h3 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                    {s.byCategory}
                  </h3>
                  {summary.byCategory.length === 0 ? (
                    <p style={{ margin: 0, fontSize: '13px', color: theme.colors.textSecondary }}>
                      {isFa ? 'این ماه هنوز هزینه‌ای ثبت نشده است.' : 'No expenses recorded this month yet.'}
                    </p>
                  ) : (
                    summary.byCategory.map((row) => (
                      <div key={row.category} style={{ marginBottom: 10 }}>
                        <div
                          style={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            fontSize: '12px',
                            color: theme.colors.textSecondary,
                            marginBottom: 4,
                          }}
                        >
                          <span>{row.label}</span>
                          <span>{formatMoney(row.total, isFa)}</span>
                        </div>
                        <div style={{ height: 8, backgroundColor: theme.colors.background, borderRadius: 999 }}>
                          <div
                            style={{
                              width: `${Math.round((row.total / maxCategory) * 100)}%`,
                              height: '100%',
                              borderRadius: 999,
                              backgroundColor: theme.colors.primary,
                            }}
                          />
                        </div>
                      </div>
                    ))
                  )}
                </section>

                <section
                  style={{
                    backgroundColor: theme.colors.surface,
                    border: `1px solid ${theme.colors.border}`,
                    borderRadius: theme.borderRadius.xl,
                    padding: theme.spacing.lg,
                  }}
                >
                  <h3 style={{ margin: '0 0 12px', fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                    {s.trend}
                  </h3>
                  <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8, height: 140 }}>
                    {summary.trend.map((point) => (
                      <div key={point.month} style={{ flex: 1, textAlign: 'center' }}>
                        <div
                          title={formatMoney(point.total, isFa)}
                          style={{
                            height: `${Math.max(4, Math.round((point.total / maxTrend) * 110))}px`,
                            backgroundColor: theme.colors.primary,
                            borderRadius: `${theme.borderRadius.sm} ${theme.borderRadius.sm} 0 0`,
                          }}
                        />
                        <span style={{ fontSize: '11px', color: theme.colors.textSecondary }}>{point.label}</span>
                      </div>
                    ))}
                  </div>
                </section>
              </div>
            </>
          )}

          <div style={{ display: 'flex', gap: theme.spacing.sm, alignItems: 'center', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{s.category}:</span>
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              style={{
                padding: '8px 12px',
                borderRadius: theme.borderRadius.md,
                border: `1px solid ${theme.colors.border}`,
                backgroundColor: theme.colors.surfaceElevated,
                color: theme.colors.textPrimary,
                fontSize: '13px',
              }}
            >
              <option value="">{isFa ? 'همه' : 'All'}</option>
              {(categories || []).map((c, idx) => {
                const val = c.code || (c as any).key || String(idx);
                return (
                  <option key={val} value={val}>
                    {c.label}
                  </option>
                );
              })}
            </select>
          </div>

          {visible.length === 0 ? (
            <EmptyState
              title={isFa ? 'هنوز هزینه‌ای ثبت نشده است' : 'No expenses yet'}
              description={isFa ? 'خرج‌های جاری کسب‌وکار را اینجا ثبت کنید.' : 'Record the business running costs here.'}
              icon="💸"
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
                <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', minWidth: 760 }}>
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
                      <th style={{ padding: theme.spacing.lg }}>{s.expenseTitle}</th>
                      <th style={{ padding: theme.spacing.lg }}>{s.category}</th>
                      <th style={{ padding: theme.spacing.lg }}>{s.date}</th>
                      <th style={{ padding: theme.spacing.lg }}>{s.amount}</th>
                      <th style={{ padding: theme.spacing.lg }}>{isFa ? 'عملیات' : 'Actions'}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {visible.map((expense) => (
                      <tr key={expense.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                        <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.textPrimary }}>
                          {expense.title}
                          {expense.notes ? (
                            <div style={{ fontWeight: 400, fontSize: '11px', color: theme.colors.textMuted }}>{expense.notes}</div>
                          ) : null}
                        </td>
                        <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>
                          {expense.categoryLabel ?? expense.category}
                        </td>
                        <td style={{ padding: theme.spacing.lg, color: theme.colors.textSecondary }}>{expense.date}</td>
                        <td style={{ padding: theme.spacing.lg, fontWeight: 700, color: theme.colors.primaryDark }}>
                          {formatMoney(expense.amount, isFa)}
                        </td>
                        <td style={{ padding: theme.spacing.lg }}>
                          <div style={{ display: 'flex', gap: theme.spacing.sm }}>
                            <button
                              onClick={() => { setEditing(expense); setIsOpen(true); }}
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
                              {isFa ? 'ویرایش' : 'Edit'}
                            </button>
                            <button
                              onClick={() => remove(expense)}
                              style={{
                                backgroundColor: theme.colors.surfaceHover,
                                color: theme.colors.error,
                                border: `1px solid ${theme.colors.borderStrong}`,
                                borderRadius: theme.borderRadius.md,
                                padding: '6px 10px',
                                fontWeight: 600,
                                fontSize: '12px',
                                cursor: 'pointer',
                              }}
                            >
                              {isFa ? 'حذف' : 'Delete'}
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <InfoNote title={s.automationNoteTitle}>{s.automationNoteBody}</InfoNote>
        </>
      )}

      {isOpen && (
        <ExpenseModal
          language={language}
          expense={editing}
          categories={categories}
          onClose={() => setIsOpen(false)}
          onSaved={(message) => {
            setIsOpen(false);
            setNotice(message);
            void load();
          }}
        />
      )}
    </div>
  );
};

const Tile: React.FC<{ label: string; value: string }> = ({ label, value }) => {
  const { theme } = useTheme();

  return (
    <div
      style={{
        backgroundColor: theme.colors.surface,
        border: `1px solid ${theme.colors.border}`,
        borderRadius: theme.borderRadius.xl,
        padding: theme.spacing.lg,
      }}
    >
      <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{label}</div>
      <div style={{ fontSize: '20px', fontWeight: 800, color: theme.colors.textPrimary, marginTop: 4 }}>{value}</div>
    </div>
  );
};

const ExpenseModal: React.FC<{
  language: 'fa' | 'en';
  expense: Expense | null;
  categories: ExpenseCategory[];
  onClose: () => void;
  onSaved: (message: string) => void;
}> = ({ language, expense, categories, onClose, onSaved }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [title, setTitle] = useState(expense?.title ?? '');
  const [category, setCategory] = useState(expense?.category ?? '');
  const [amount, setAmount] = useState<number | ''>(expense?.amount ?? '');
  const [date, setDate] = useState(expense?.date ?? new Date().toISOString().slice(0, 10));
  const [method, setMethod] = useState(expense?.paymentMethod ?? 'CASH');
  const [notes, setNotes] = useState(expense?.notes ?? '');
  const [attachmentUrl, setAttachmentUrl] = useState(expense?.attachmentUrl ?? '');

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();

    const next: Record<string, string> = {};
    if (!title.trim()) next.title = isFa ? 'عنوان هزینه را وارد کنید.' : 'Enter a title.';
    if (!category) next.category = isFa ? 'دسته‌بندی را انتخاب کنید.' : 'Choose a category.';
    if (amount === '' || Number(amount) <= 0) next.amount = isFa ? 'مبلغ باید بزرگ‌تر از صفر باشد.' : 'Amount must be greater than zero.';

    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setIsSubmitting(true);
    setFormError('');

    try {
      const payload = {
        title: title.trim(),
        category,
        amount: Number(amount),
        date,
        notes: notes.trim(),
      };

      if (expense) {
        await apiClient.updateExpense(expense.id, payload);
      } else {
        await apiClient.createExpense({ ...payload, payment_method: method, attachment_url: attachmentUrl.trim() });
      }

      onSaved(isFa ? 'هزینه ثبت شد و سند حسابداری صادر گردید.' : 'Expense recorded and posted to accounting.');
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ثبت هزینه ناموفق بود.' : 'Could not save the expense.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <ModalShell title={expense ? (isFa ? 'ویرایش هزینه' : 'Edit expense') : s.newExpense} onClose={onClose}>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
        <FormError message={formError} />

        <TextInput
          label={s.expenseTitle}
          required
          value={title}
          error={errors.title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder={s.expenseTitlePlaceholder}
        />

        <SelectInput label={s.category} required value={category} error={errors.category} onChange={(e) => setCategory(e.target.value)}>
          <option value="">{isFa ? 'انتخاب کنید' : 'Select a category'}</option>
          {(categories || []).map((c, idx) => {
            const val = c.code || (c as any).key || String(idx);
            return (
              <option key={val} value={val}>
                {c.label}
              </option>
            );
          })}
        </SelectInput>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md }}>
          <TextInput
            label={s.amount}
            required
            type="number"
            min={0.01}
            step="0.01"
            value={amount}
            error={errors.amount}
            onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
          />
          <TextInput label={s.date} required type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        </div>

        {!expense && (
          <SelectInput label={s.paymentMethod} value={method} onChange={(e) => setMethod(e.target.value)}>
            <option value="CASH">{isFa ? 'نقدی' : 'Cash'}</option>
            <option value="CARD">{isFa ? 'کارت' : 'Card'}</option>
            <option value="BANK_TRANSFER">{isFa ? 'حواله بانکی' : 'Bank transfer'}</option>
          </SelectInput>
        )}

        <TextInput label={s.notes} value={notes} onChange={(e) => setNotes(e.target.value)} />

        {!expense && (
          <TextInput
            label={s.attachment}
            value={attachmentUrl}
            onChange={(e) => setAttachmentUrl(e.target.value)}
            placeholder={isFa ? 'نشانی فایل رسید (اختیاری)' : 'Receipt file URL (optional)'}
          />
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md, marginTop: theme.spacing.md }}>
          <SecondaryButton type="button" onClick={onClose} disabled={isSubmitting}>
            {isFa ? 'انصراف' : 'Cancel'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? s.submitting : isFa ? 'ذخیره هزینه' : 'Save expense'}
          </PrimaryButton>
        </div>
      </form>
    </ModalShell>
  );
};
const toFieldErrors = (fieldErrors: Record<string, string[]>): Record<string, string> =>
  Object.fromEntries(Object.entries(fieldErrors).map(([key, value]) => [key, value[0] ?? '']));
