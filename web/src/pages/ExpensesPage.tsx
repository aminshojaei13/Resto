import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Expense } from '../types';
import { useTheme } from '../theme/ThemeContext';

interface ExpensesPageProps {
  language?: 'fa' | 'en';
}

export const ExpensesPage: React.FC<ExpensesPageProps> = ({ language = 'fa' }) => {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategoryFilter, setSelectedCategoryFilter] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingExpense, setEditingExpense] = useState<Expense | null>(null);
  const [detailExpense, setDetailExpense] = useState<Expense | null>(null);

  // Form State
  const [category, setCategory] = useState('Store Utilities');
  const [amount, setAmount] = useState<number | ''>(100);
  const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [notes, setNotes] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  const categoryOptions = [
    'Store Utilities',
    'Marketing & Ads',
    'Packaging & Shipping',
    'Gateway & Platform Fees',
    'Rent & Lease',
    'Office Supplies',
    'Miscellaneous'
  ];

  useEffect(() => {
    loadExpenses();
  }, []);

  const loadExpenses = async () => {
    setIsLoading(true);
    try {
      const list = await apiClient.getExpenses();
      setExpenses(list);
    } catch {
      // Handled in apiClient
    } finally {
      setIsLoading(false);
    }
  };

  const openCreateModal = () => {
    setCategory('Store Utilities');
    setAmount(100);
    setDate(new Date().toISOString().split('T')[0]);
    setPaymentMethod('CASH');
    setNotes('');
    setErrorMessage('');
    setIsCreateModalOpen(true);
  };

  const openEditModal = (exp: Expense) => {
    setEditingExpense(exp);
    setCategory(exp.category);
    setAmount(exp.amount);
    setDate(exp.date || new Date().toISOString().split('T')[0]);
    setPaymentMethod(exp.paymentMethod || 'CASH');
    setNotes(exp.notes || '');
    setErrorMessage('');
  };

  const closeModals = () => {
    setIsCreateModalOpen(false);
    setEditingExpense(null);
    setDetailExpense(null);
    setErrorMessage('');
  };

  const handleCreateExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) {
      setErrorMessage(isFa ? 'لطفاً مبلغ معتبری وارد کنید.' : 'Please enter a valid amount.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await apiClient.createExpense({
        category: category.trim(),
        amount: numAmount,
        date: date,
        payment_method: paymentMethod,
        notes: notes.trim(),
      });

      setIsSubmitting(false);
      closeModals();
      alert(isFa ? 'هزینه با موفقیت ثبت شد و سند حسابداری صادر گردید.' : 'Expense recorded successfully and accounting posted.');
      loadExpenses();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ثبت هزینه' : 'Failed to record expense'));
    }
  };

  const handleUpdateExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingExpense) return;

    const numAmount = Number(amount);
    if (!numAmount || numAmount <= 0) {
      setErrorMessage(isFa ? 'لطفاً مبلغ معتبری وارد کنید.' : 'Please enter a valid amount.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await apiClient.updateExpense(editingExpense.id, {
        category: category.trim(),
        amount: numAmount,
        date: date,
        payment_method: paymentMethod,
        notes: notes.trim(),
      });

      setIsSubmitting(false);
      closeModals();
      alert(isFa ? 'اطلاعات هزینه با موفقیت بروزرسانی شد.' : 'Expense details updated successfully.');
      loadExpenses();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ویرایش هزینه' : 'Failed to update expense'));
    }
  };

  const handleDeleteExpense = async (exp: Expense) => {
    const confirmMsg = isFa
      ? `آیا از حذف هزینه (${exp.category} - ${exp.amount.toLocaleString('fa-IR')} تومان) اطمینان دارید؟`
      : `Are you sure you want to delete expense (${exp.category} - $${exp.amount.toFixed(2)})?`;

    if (!window.confirm(confirmMsg)) return;

    try {
      await apiClient.deleteExpense(exp.id);
      alert(isFa ? 'هزینه با موفقیت حذف شد.' : 'Expense deleted successfully.');
      loadExpenses();
    } catch (err: any) {
      alert(err.message || (isFa ? 'خطا در حذف هزینه' : 'Failed to delete expense'));
    }
  };

  const filteredExpenses = expenses.filter((e) => {
    const matchesCategory = !selectedCategoryFilter || e.category === selectedCategoryFilter;
    const matchesQuery = !searchQuery || e.category.toLowerCase().includes(searchQuery.toLowerCase()) || (e.notes && e.notes.toLowerCase().includes(searchQuery.toLowerCase()));
    return matchesCategory && matchesQuery;
  });

  const totalExpenseAmount = filteredExpenses.reduce((sum, e) => sum + e.amount, 0);

  return (
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <div>
          <h2 style={{ margin: '0 0 4px 0', color: theme.colors.textPrimary }}>{isFa ? 'مدیریت هزینه‌های جاری و عملیاتی' : 'Operating Expenses Management'}</h2>
          <p style={{ margin: 0, color: theme.colors.textSecondary, fontSize: '13px' }}>
            {isFa ? 'ثبت هزینه‌های فروشگاه با صدور خودکار سند حسابداری بدهکار/بستانکار' : 'Track operating expenses with automatic double-entry journal posting'}
          </p>
        </div>
        <button
          onClick={openCreateModal}
          style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '10px 18px', fontSize: '14px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          {isFa ? '+ ثبت هزینه جدید' : '+ Record New Expense'}
        </button>
      </div>

      {/* Filter Controls */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
        <input
          type="text"
          placeholder={isFa ? 'جستجو در شرح یا بابت هزینه...' : 'Search category or notes...'}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          style={{ flex: 1, padding: '10px 14px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px' }}
        />

        <select
          value={selectedCategoryFilter}
          onChange={(e) => setSelectedCategoryFilter(e.target.value)}
          style={{ padding: '10px 14px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, fontSize: '14px', backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary }}
        >
          <option value="">{isFa ? 'همه دسته‌بندی‌ها' : 'All Categories'}</option>
          {categoryOptions.map((cat, idx) => (
            <option key={idx} value={cat}>{cat}</option>
          ))}
        </select>
      </div>

      {/* Expense Summary KPI Banner */}
      <div style={{ backgroundColor: theme.colors.surface, borderRadius: '12px', padding: '16px 20px', border: `1px solid ${theme.colors.border}`, marginBottom: '20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: theme.shadows.card }}>
        <span style={{ fontSize: '15px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
          {isFa ? `مجموع هزینه‌های ثبت‌شده (${filteredExpenses.length} فقره):` : `Total Expenses (${filteredExpenses.length} items):`}
        </span>
        <span style={{ fontSize: '22px', fontWeight: 'bold', color: theme.colors.error }}>
          {isFa ? `${totalExpenseAmount.toLocaleString('fa-IR')} تومان` : `$${totalExpenseAmount.toFixed(2)}`}
        </span>
      </div>

      {/* Expenses Table */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری هزینه‌ها...' : 'Loading expenses...'}</div>
      ) : filteredExpenses.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: theme.colors.textMuted, backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}` }}>
          {isFa ? 'هیچ هزینه‌ای ثبت نشده است.' : 'No expenses recorded.'}
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: theme.colors.surface, borderRadius: '8px', overflow: 'hidden', boxShadow: theme.shadows.card, color: theme.colors.textPrimary }}>
          <thead>
            <tr style={{ backgroundColor: theme.colors.backgroundSecondary, textAlign: isFa ? 'right' : 'left', borderBottom: `2px solid ${theme.colors.border}`, fontSize: '12px', fontWeight: 700, color: theme.colors.textSecondary }}>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'تاریخ' : 'Date'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'دسته‌بندی' : 'Category'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'توضیحات / بابت' : 'Notes / Description'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'روش پرداخت' : 'Payment Method'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'مبلغ' : 'Amount'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'عملیات' : 'Actions'}</th>
            </tr>
          </thead>
          <tbody>
            {filteredExpenses.map((exp) => (
              <tr key={exp.id} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontWeight: 'bold' }}>{exp.date}</td>
                <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{exp.category}</td>
                <td style={{ padding: '12px 16px', color: theme.colors.textSecondary }}>{exp.notes || '-'}</td>
                <td style={{ padding: '12px 16px' }}>
                  <span style={{ padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold', backgroundColor: theme.colors.surfaceHover, color: theme.colors.textSecondary }}>
                    {exp.paymentMethod}
                  </span>
                </td>
                <td style={{ padding: '12px 16px', color: theme.colors.error, fontWeight: 'bold', fontSize: '15px' }}>
                  {isFa ? `${exp.amount.toLocaleString('fa-IR')} تومان` : `$${exp.amount.toFixed(2)}`}
                </td>
                <td style={{ padding: '12px 16px', display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => setDetailExpense(exp)}
                    style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '👁️ جزئیات' : '👁️ View'}
                  </button>
                  <button
                    onClick={() => openEditModal(exp)}
                    style={{ backgroundColor: theme.colors.primaryLight, color: theme.colors.primaryDark, border: 'none', borderRadius: '6px', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '✏️ ویرایش' : '✏️ Edit'}
                  </button>
                  <button
                    onClick={() => handleDeleteExpense(exp)}
                    style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, border: 'none', borderRadius: '6px', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '🗑️ حذف' : '🗑️ Delete'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Create Expense Modal */}
      {isCreateModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? 'ثبت هزینه جدید' : 'Record New Operating Expense'}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleCreateExpense} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'دسته‌بندی هزینه *' : 'Category *'}</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  required
                >
                  {categoryOptions.map((cat, idx) => (
                    <option key={idx} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'مبلغ هزینه *' : 'Amount *'}</label>
                  <input
                    type="number"
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'تاریخ *' : 'Date *'}</label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'روش پرداخت' : 'Payment Method'}</label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                >
                  <option value="CASH">{isFa ? 'نقدی از صندق' : 'Cash'}</option>
                  <option value="BANK_TRANSFER">{isFa ? 'حواله / کارت‌به‌کارت' : 'Bank Transfer'}</option>
                  <option value="CARD">{isFa ? 'کارتخوان' : 'POS Card'}</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'بابت / توضیحات' : 'Notes / Description'}</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={2}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={closeModals}
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ثبت هزینه' : 'Save Expense')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Expense Modal (Pre-Populated) */}
      {editingExpense && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? 'ویرایش سند هزینه' : 'Edit Expense Details'}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleUpdateExpense} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'دسته‌بندی هزینه *' : 'Category *'}</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  required
                >
                  {categoryOptions.map((cat, idx) => (
                    <option key={idx} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'مبلغ هزینه *' : 'Amount *'}</label>
                  <input
                    type="number"
                    step="0.01"
                    value={amount}
                    onChange={(e) => setAmount(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'تاریخ *' : 'Date *'}</label>
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'روش پرداخت' : 'Payment Method'}</label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                >
                  <option value="CASH">{isFa ? 'نقدی از صندوق' : 'Cash'}</option>
                  <option value="BANK_TRANSFER">{isFa ? 'حواله / کارت‌به‌کارت' : 'Bank Transfer'}</option>
                  <option value="CARD">{isFa ? 'کارتخوان' : 'POS Card'}</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'بابت / توضیحات' : 'Notes / Description'}</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={2}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={closeModals}
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ثبت تغییرات' : 'Save Changes')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Detail Modal */}
      {detailExpense && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '440px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold' }}>{detailExpense.category}</h3>
              <button onClick={closeModals} style={{ backgroundColor: 'transparent', border: 'none', fontSize: '18px', cursor: 'pointer', color: theme.colors.textSecondary }}>✕</button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', fontSize: '14px', backgroundColor: theme.colors.backgroundSecondary, padding: '12px', borderRadius: '8px', marginBottom: '20px', color: theme.colors.textPrimary }}>
              <div><strong>{isFa ? 'تاریخ:' : 'Date:'}</strong> {detailExpense.date}</div>
              <div><strong>{isFa ? 'مبلغ:' : 'Amount:'}</strong> {isFa ? `${detailExpense.amount.toLocaleString('fa-IR')} تومان` : `$${detailExpense.amount.toFixed(2)}`}</div>
              <div><strong>{isFa ? 'روش پرداخت:' : 'Payment Method:'}</strong> {detailExpense.paymentMethod}</div>
              <div><strong>{isFa ? 'توضیحات:' : 'Notes:'}</strong> {detailExpense.notes || '-'}</div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
              <button
                onClick={closeModals}
                style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                {isFa ? 'بستن' : 'Close'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
