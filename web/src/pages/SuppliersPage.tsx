import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Supplier } from '../types';
import { useTheme } from '../theme/ThemeContext';

interface SuppliersPageProps {
  language?: 'fa' | 'en';
}

export const SuppliersPage: React.FC<SuppliersPageProps> = ({ language = 'fa' }) => {
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  // Modal States
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [editingSupplier, setEditingSupplier] = useState<Supplier | null>(null);
  const [detailSupplier, setDetailSupplier] = useState<Supplier | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [address, setAddress] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  useEffect(() => {
    loadSuppliers();
  }, []);

  const loadSuppliers = async (query = searchQuery) => {
    setIsLoading(true);
    try {
      const list = await apiClient.getSuppliers(query);
      setSuppliers(list);
    } catch {
      // Handled in apiClient
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setSearchQuery(val);
    loadSuppliers(val);
  };

  const openCreateModal = () => {
    setName('');
    setEmail('');
    setPhone('');
    setAddress('');
    setErrorMessage('');
    setIsCreateModalOpen(true);
  };

  const openEditModal = (supplier: Supplier) => {
    setEditingSupplier(supplier);
    setName(supplier.name);
    setEmail(supplier.email || '');
    setPhone(supplier.phone || '');
    setAddress(supplier.address || '');
    setErrorMessage('');
  };

  const closeModals = () => {
    setIsCreateModalOpen(false);
    setEditingSupplier(null);
    setDetailSupplier(null);
    setErrorMessage('');
  };

  const handleCreateSupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setErrorMessage(isFa ? 'نام تامین‌کننده الزامی است.' : 'Supplier name is required.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await apiClient.createSupplier({
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
        address: address.trim(),
      });
      setIsSubmitting(false);
      closeModals();
      alert(isFa ? 'تامین‌کننده جدید با موفقیت ثبت شد.' : 'Supplier created successfully.');
      loadSuppliers();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ثبت تامین‌کننده' : 'Failed to create supplier'));
    }
  };

  const handleUpdateSupplier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingSupplier) return;

    if (!name.trim()) {
      setErrorMessage(isFa ? 'نام تامین‌کننده الزامی است.' : 'Supplier name is required.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await apiClient.updateSupplier(editingSupplier.id, {
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
        address: address.trim(),
      });
      setIsSubmitting(false);
      closeModals();
      alert(isFa ? 'اطلاعات تامین‌کننده بروزرسانی شد.' : 'Supplier updated successfully.');
      loadSuppliers();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در بروزرسانی تامین‌کننده' : 'Failed to update supplier'));
    }
  };

  const handleDeleteSupplier = async (supplier: Supplier) => {
    const confirmMsg = isFa
      ? `آیا از حذف تامین‌کننده (${supplier.name}) اطمینان دارید؟`
      : `Are you sure you want to delete supplier (${supplier.name})?`;

    if (!window.confirm(confirmMsg)) return;

    try {
      await apiClient.deleteSupplier(supplier.id);
      alert(isFa ? 'تامین‌کننده با موفقیت حذف شد.' : 'Supplier deleted successfully.');
      loadSuppliers();
    } catch (err: any) {
      alert(err.message || (isFa ? 'خطا در حذف تامین‌کننده' : 'Failed to delete supplier'));
    }
  };

  const handleViewDetail = async (supplier: Supplier) => {
    try {
      const detailed = await apiClient.getSupplierById(supplier.id);
      setDetailSupplier(detailed);
    } catch {
      setDetailSupplier(supplier);
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ margin: 0, color: theme.colors.textPrimary }}>{isFa ? 'مدیریت تامین‌کنندگان' : 'Supplier Management'}</h2>
        <button
          onClick={openCreateModal}
          style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '8px', padding: '10px 18px', fontSize: '14px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          {isFa ? '+ ثبت تامین‌کننده جدید' : '+ Add New Supplier'}
        </button>
      </div>

      {/* Search Input */}
      <input
        type="text"
        placeholder={isFa ? 'جستجوی تامین‌کننده بر اساس نام، تلفن، ایمیل...' : 'Search suppliers by name, phone, email...'}
        value={searchQuery}
        onChange={handleSearchChange}
        style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '15px', marginBottom: '20px', boxSizing: 'border-box' }}
      />

      {/* Supplier Grid */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری اطلاعات...' : 'Loading suppliers...'}</div>
      ) : suppliers.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: theme.colors.textMuted, backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}` }}>
          {isFa ? 'هیچ تامین‌کننده‌ای یافت نشد.' : 'No suppliers found.'}
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '16px' }}>
          {suppliers.map((s) => (
            <div key={s.id} style={{ backgroundColor: theme.colors.surface, border: `1px solid ${theme.colors.border}`, borderRadius: '12px', padding: '20px', boxShadow: theme.shadows.card, display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
              <div>
                <h3 style={{ margin: '0 0 6px 0', fontSize: '18px', fontWeight: 'bold', color: theme.colors.textPrimary }}>{s.name}</h3>
                <p style={{ margin: '0 0 10px 0', color: theme.colors.textSecondary, fontSize: '13px' }}>
                  📞 {s.phone || (isFa ? 'بدون شماره' : 'No phone')} • ✉️ {s.email || (isFa ? 'بدون ایمیل' : 'No email')}
                </p>
                {s.address && (
                  <p style={{ margin: '0 0 12px 0', color: theme.colors.textMuted, fontSize: '13px' }}>
                    📍 {s.address}
                  </p>
                )}
              </div>

              <div style={{ display: 'flex', gap: '8px', borderTop: `1px solid ${theme.colors.border}`, paddingTop: '12px', marginTop: '12px' }}>
                <button
                  onClick={() => handleViewDetail(s)}
                  style={{ flex: 1, backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? '👁️ جزئیات' : '👁️ View'}
                </button>
                <button
                  onClick={() => openEditModal(s)}
                  style={{ flex: 1, backgroundColor: theme.colors.primaryLight, color: theme.colors.primaryDark, border: 'none', borderRadius: '6px', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? '✏️ ویرایش' : '✏️ Edit'}
                </button>
                <button
                  onClick={() => handleDeleteSupplier(s)}
                  style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, border: 'none', borderRadius: '6px', padding: '6px 10px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? '🗑️ حذف' : '🗑️ Delete'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Supplier Modal */}
      {isCreateModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? 'ثبت تامین‌کننده جدید' : 'Create New Supplier'}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleCreateSupplier} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'نام تامین‌کننده / شرکت *' : 'Supplier Name *'}</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'شماره تماس' : 'Phone Number'}</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'ایمیل' : 'Email Address'}</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'آدرس' : 'Address'}</label>
                <textarea
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
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
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ثبت تامین‌کننده' : 'Save Supplier')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Edit Supplier Modal (Pre-Populated) */}
      {editingSupplier && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '480px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? `ویرایش تامین‌کننده (${editingSupplier.name})` : `Edit Supplier (${editingSupplier.name})`}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: theme.colors.errorLight, color: theme.colors.error, padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleUpdateSupplier} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'نام تامین‌کننده / شرکت *' : 'Supplier Name *'}</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'شماره تماس' : 'Phone Number'}</label>
                <input
                  type="text"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'ایمیل' : 'Email Address'}</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'آدرس' : 'Address'}</label>
                <textarea
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
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

      {/* Supplier Detail Modal */}
      {detailSupplier && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, borderRadius: '12px', width: '100%', maxWidth: '560px', padding: '24px', boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', color: theme.colors.textPrimary }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>{detailSupplier.name}</h3>
              <button onClick={closeModals} style={{ backgroundColor: 'transparent', border: 'none', fontSize: '18px', cursor: 'pointer', color: theme.colors.textSecondary }}>✕</button>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '20px', fontSize: '14px', color: theme.colors.textPrimary }}>
              <div><strong>{isFa ? 'شماره تماس:' : 'Phone:'}</strong> {detailSupplier.phone || '-'}</div>
              <div><strong>{isFa ? 'ایمیل:' : 'Email:'}</strong> {detailSupplier.email || '-'}</div>
              <div><strong>{isFa ? 'آدرس:' : 'Address:'}</strong> {detailSupplier.address || '-'}</div>
            </div>

            <h4 style={{ margin: '0 0 12px 0', fontSize: '16px', fontWeight: 'bold', borderTop: `1px solid ${theme.colors.border}`, paddingTop: '12px', color: theme.colors.textPrimary }}>
              {isFa ? 'سابقه سفارشات خرید (Purchase Orders)' : 'Purchase Order History'}
            </h4>

            {(!detailSupplier.purchases || detailSupplier.purchases.length === 0) ? (
              <p style={{ color: theme.colors.textMuted, fontSize: '13px' }}>{isFa ? 'هیچ سفارش خریدی ثبت نشده است.' : 'No purchase orders recorded.'}</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', maxHeight: '200px', overflowY: 'auto' }}>
                {detailSupplier.purchases.map((p: any) => (
                  <div key={p.id} style={{ padding: '10px', backgroundColor: theme.colors.backgroundSecondary, borderRadius: '6px', border: `1px solid ${theme.colors.border}`, display: 'flex', justifyContent: 'space-between' }}>
                    <div>
                      <div style={{ fontWeight: 'bold', fontSize: '13px', color: theme.colors.textPrimary }}>{p.purchase_number || p.id}</div>
                      <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{p.status} • {p.payment_status}</div>
                    </div>
                    <div style={{ fontWeight: 'bold', color: theme.colors.primary }}>
                      {isFa ? `${Number(p.total_amount || 0).toLocaleString('fa-IR')} تومان` : `$${Number(p.total_amount || 0).toFixed(2)}`}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
              <button
                onClick={closeModals}
                style={{ backgroundColor: theme.colors.primary, color: theme.colors.primaryTextOnBrand, border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
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
