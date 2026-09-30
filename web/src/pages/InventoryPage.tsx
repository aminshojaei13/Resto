import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Product } from '../types';
import { EmptyState } from '../components/EmptyState';
import { PageHeader } from '../components/PageHeader';
import { useTheme } from '../theme/ThemeContext';

interface InventoryPageProps {
  language?: 'fa' | 'en';
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ language = 'fa' }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  // Form state
  const [name, setName] = useState('');
  const [sku, setSku] = useState('');
  const [barcode, setBarcode] = useState('');
  const [description, setDescription] = useState('');
  const [price, setPrice] = useState<number | ''>(0);
  const [costPrice, setCostPrice] = useState<number | ''>(0);
  const [category, setCategory] = useState('');
  const [unit, setUnit] = useState('pcs');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  useEffect(() => {
    loadProducts();
  }, []);

  const loadProducts = () => {
    apiClient.getProducts().then(setProducts);
  };

  const openCreateModal = () => {
    setEditingProduct(null);
    setName('');
    setSku(`SKU-${Math.floor(1000 + Math.random() * 9000)}`);
    setBarcode('');
    setDescription('');
    setPrice(0);
    setCostPrice(0);
    setCategory(isFa ? 'عمومی' : 'General');
    setUnit('pcs');
    setErrorMessage('');
    setIsCreateModalOpen(true);
  };

  const openEditModal = (p: Product) => {
    setEditingProduct(p);
    setName(p.name);
    setSku(p.sku);
    setBarcode(p.barcode || '');
    setDescription(p.description || '');
    setPrice(p.price);
    setCostPrice(p.costPrice);
    setCategory(p.category || '');
    setUnit(p.unit || 'pcs');
    setErrorMessage('');
    setIsCreateModalOpen(true);
  };

  const closeModal = () => {
    setEditingProduct(null);
    setIsCreateModalOpen(false);
    setErrorMessage('');
  };

  const handleSaveProduct = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!name.trim()) {
      setErrorMessage(isFa ? 'نام کالا الزامی است.' : 'Product name is required.');
      return;
    }
    if (!sku.trim()) {
      setErrorMessage(isFa ? 'کد SKU الزامی است.' : 'SKU is required.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      if (editingProduct) {
        await apiClient.updateProduct(editingProduct.id, {
          name: name.trim(),
          sku: sku.trim(),
          barcode: barcode.trim(),
          description: description.trim(),
          price: Number(price) || 0,
          cost_price: Number(costPrice) || 0,
          category: category.trim(),
          unit: unit.trim(),
        });
      } else {
        await apiClient.createProduct({
          name: name.trim(),
          sku: sku.trim(),
          barcode: barcode.trim(),
          description: description.trim(),
          price: Number(price) || 0,
          cost_price: Number(costPrice) || 0,
          category: category.trim(),
          unit: unit.trim(),
        });
      }

      setIsSubmitting(false);
      closeModal();
      loadProducts();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ثبت اطلاعات کالا' : 'Failed to save product'));
    }
  };

  const handleAdjust = async (productId: string) => {
    try {
      await apiClient.adjustStock(productId, '', 5, 'اصلاح موجودی دستی انبار');
      alert(isFa ? 'موجودی انبار با موفقیت ۵ عدد افزایش یافت.' : 'Stock increased by 5 units.');
      loadProducts();
    } catch (err: any) {
      alert(err.message || (isFa ? 'خطا در افزایش موجودی' : 'Stock adjustment failed'));
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl }}>
      <PageHeader
        title={isFa ? 'کنترل موجودی انبار و کالاها' : 'Inventory & Product Catalog'}
        description={isFa ? 'مدیریت متمرکز کالاها، قیمت‌ها، دسته‌بندی و موجودی انبارها' : 'Manage products, stock levels, warehouse stock, and pricing'}
        actions={
          <button
            onClick={openCreateModal}
            style={{
              backgroundColor: theme.colors.primary,
              color: '#FFF',
              border: 'none',
              borderRadius: theme.borderRadius.md,
              padding: '10px 20px',
              fontSize: '13px',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: theme.spacing.sm,
            }}
          >
            <span>➕</span>
            <span>{isFa ? 'افزودن کالای جدید' : 'Add New Product'}</span>
          </button>
        }
      />

      {products.length === 0 ? (
        <EmptyState
          title={isFa ? 'هنوز کالایی ثبت نشده است' : 'No Products Created Yet'}
          description={
            isFa
              ? 'کالاهای خود را اضافه کنید تا موجودی انبار، ثبت سفارش‌ها و خریدهای شما در یکجا مدیریت شوند.'
              : 'Add your products to track warehouse stock levels, register online orders, and manage purchases in one place.'
          }
          actionText={isFa ? 'افزودن اولین کالا' : 'Add First Product'}
          onAction={openCreateModal}
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
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left' }}>
              <thead>
                <tr style={{ backgroundColor: theme.colors.background, borderBottom: `2px solid ${theme.colors.border}`, fontSize: '12px', fontWeight: 700, color: theme.colors.textSecondary }}>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'نام کالا' : 'Product Name'}</th>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'کد SKU' : 'SKU'}</th>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'دسته‌بندی' : 'Category'}</th>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'قیمت فروش' : 'Selling Price'}</th>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'قیمت خرید' : 'Cost Price'}</th>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'موجودی کل' : 'Total Stock'}</th>
                  <th style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>{isFa ? 'عملیات' : 'Actions'}</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => {
                  const totalStock = Object.values(p.stockQuantityByWarehouse || {}).reduce((acc, v) => acc + Number(v), 0);
                  return (
                    <tr key={p.id} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                      <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}`, fontWeight: 700, color: theme.colors.textPrimary }}>
                        {p.name}
                      </td>
                      <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}`, fontFamily: 'monospace', color: theme.colors.textSecondary }}>
                        {p.sku}
                      </td>
                      <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>
                        <span style={{ backgroundColor: theme.colors.background, border: `1px solid ${theme.colors.border}`, padding: '4px 10px', borderRadius: theme.borderRadius.full, fontSize: '12px' }}>
                          {p.category || (isFa ? 'عمومی' : 'General')}
                        </span>
                      </td>
                      <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}`, fontWeight: 700, color: theme.colors.primaryDark }}>
                        {isFa ? `${p.price.toLocaleString('fa-IR')} تومان` : `$${p.price.toFixed(2)}`}
                      </td>
                      <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}`, color: theme.colors.textSecondary }}>
                        {isFa ? `${p.costPrice.toLocaleString('fa-IR')} تومان` : `$${p.costPrice.toFixed(2)}`}
                      </td>                       <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}`, fontWeight: 700, color: totalStock <= 5 ? theme.colors.error : theme.colors.success }}>
                        {isFa ? `${totalStock.toLocaleString('fa-IR')} ${p.unit || 'عدد'}` : `${totalStock} ${p.unit || 'pcs'}`}
                      </td>
                      <td style={{ padding: `${theme.spacing.lg} ${theme.spacing.xl}` }}>
                        <div style={{ display: 'flex', gap: theme.spacing.sm }}>
                          <button
                            onClick={() => openEditModal(p)}
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
                            ✏️ {isFa ? 'ویرایش' : 'Edit'}
                          </button>
                          <button
                            onClick={() => handleAdjust(p.id)}
                            style={{
                              backgroundColor: theme.colors.surfaceHover,
                              color: theme.colors.textPrimary,
                              border: `1px solid ${theme.colors.borderStrong}`,
                              borderRadius: theme.borderRadius.md,
                              padding: '6px 12px',
                              fontWeight: 600,
                              fontSize: '12px',
                              cursor: 'pointer',
                            }}
                          >
                            ➕ {isFa ? 'افزایش موجودی' : '+5 Stock'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal for Product Creation & Editing */}
      {isCreateModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surface, borderRadius: theme.borderRadius.xl, width: '100%', maxWidth: '540px', padding: theme.spacing['2xl'], boxShadow: theme.shadows.lg, direction: isFa ? 'rtl' : 'ltr', fontFamily: theme.typography.fontFamily, border: `1px solid ${theme.colors.border}` }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: theme.spacing.xl }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: theme.colors.textPrimary }}>
                {editingProduct
                  ? (isFa ? `ویرایش مشخصات کالا (${editingProduct.name})` : `Edit Product (${editingProduct.name})`)
                  : (isFa ? 'افزودن کالای جدید به انبار' : 'Add New Product to Inventory')}
              </h3>
              <button onClick={closeModal} style={{ background: 'none', border: 'none', fontSize: '18px', cursor: 'pointer', color: theme.colors.textMuted }}>✕</button>
            </div>

            {errorMessage && (               <div style={{ backgroundColor: isDark ? theme.colors.errorLight : '#FEE2E2', color: isDark ? theme.colors.error : '#991B1B', padding: theme.spacing.md, borderRadius: theme.borderRadius.md, marginBottom: theme.spacing.lg, fontSize: '13px', fontWeight: 600 }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleSaveProduct} style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                  {isFa ? 'نام کالا *' : 'Product Name *'}
                </label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                    {isFa ? 'کد SKU *' : 'SKU *'}
                  </label>
                  <input
                    type="text"
                    value={sku}
                    onChange={(e) => setSku(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                    {isFa ? 'بارکد کالا' : 'Barcode'}
                  </label>
                  <input
                    type="text"
                    value={barcode}
                    onChange={(e) => setBarcode(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                    {isFa ? 'قیمت فروش *' : 'Selling Price *'}
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={price}
                    onChange={(e) => setPrice(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                    {isFa ? 'قیمت خرید (خالص)' : 'Cost Price'}
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    value={costPrice}
                    onChange={(e) => setCostPrice(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: theme.spacing.md }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                    {isFa ? 'دسته‌بندی' : 'Category'}
                  </label>
                  <input
                    type="text"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                    {isFa ? 'واحد سنجش' : 'Unit'}
                  </label>
                  <input
                    type="text"
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                    style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 700, marginBottom: '4px', color: theme.colors.textPrimary }}>
                  {isFa ? 'توضیحات تکمیلی' : 'Description'}
                </label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={2}
                  style={{ width: '100%', padding: '10px 12px', borderRadius: theme.borderRadius.md, border: `1px solid ${theme.colors.border}`, fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md, marginTop: theme.spacing.lg }}>
                <button
                  type="button"
                  onClick={closeModal}
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.background, color: theme.colors.textPrimary, border: `1px solid ${theme.colors.border}`, borderRadius: theme.borderRadius.md, padding: '10px 20px', fontWeight: 600, fontSize: '14px', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ backgroundColor: theme.colors.primary, color: '#FFF', border: 'none', borderRadius: theme.borderRadius.md, padding: '10px 24px', fontWeight: 700, fontSize: '14px', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ذخیره کالا' : 'Save Product')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
