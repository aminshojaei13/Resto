import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Product } from '../types';

interface InventoryPageProps {
  language?: 'fa' | 'en';
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ language = 'fa' }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);

  // Edit form pre-populated state
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

  useEffect(() => {
    loadProducts();
  }, []);

  const loadProducts = () => {
    apiClient.getProducts().then(setProducts);
  };

  const openEditModal = (p: Product) => {
    setEditingProduct(p);
    setName(p.name);
    setSku(p.sku);
    setBarcode(p.barcode);
    setDescription(p.description || '');
    setPrice(p.price);
    setCostPrice(p.costPrice);
    setCategory(p.category);
    setUnit(p.unit || 'pcs');
    setErrorMessage('');
  };

  const closeEditModal = () => {
    setEditingProduct(null);
    setErrorMessage('');
  };

  const handleUpdateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProduct) return;

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

      setIsSubmitting(false);
      closeEditModal();
      alert(isFa ? 'اطلاعات کالا با موفقیت بروزرسانی شد.' : 'Product updated successfully.');
      loadProducts();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ثبت تغییرات' : 'Failed to update product'));
    }
  };

  const handleAdjust = async (productId: string) => {
    await apiClient.adjustStock(productId, 'wh_apex_1a', 5, 'Web Admin Restock');
    alert(isFa ? 'موجودی انبار با موفقیت اصلاح شد (+۵ عدد)' : 'Stock updated successfully (+5 units)');
    loadProducts();
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <h2 style={{ margin: '0 0 16px 0' }}>{isFa ? 'مدیریت کالاها و موجودی انبارها' : 'Inventory Catalog & Warehouse Stock'}</h2>

      <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#FFF', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
        <thead>
          <tr style={{ backgroundColor: '#F5F5F5', textAlign: isFa ? 'right' : 'left', borderBottom: '2px solid #DDD' }}>
            <th style={{ padding: '12px 16px' }}>{isFa ? 'نام کالا' : 'Product Name'}</th>
            <th style={{ padding: '12px 16px' }}>{isFa ? 'کد SKU' : 'SKU'}</th>
            <th style={{ padding: '12px 16px' }}>{isFa ? 'دسته‌بندی' : 'Category'}</th>
            <th style={{ padding: '12px 16px' }}>{isFa ? 'قیمت فروش' : 'Price'}</th>
            <th style={{ padding: '12px 16px' }}>{isFa ? 'موجودی انبار اصلی' : 'Main WH Stock'}</th>
            <th style={{ padding: '12px 16px' }}>{isFa ? 'عملیات' : 'Actions'}</th>
          </tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id} style={{ borderBottom: '1px solid #EEE' }}>
              <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{p.name}</td>
              <td style={{ padding: '12px 16px' }}>{p.sku}</td>
              <td style={{ padding: '12px 16px' }}>{p.category}</td>
              <td style={{ padding: '12px 16px', color: '#005AC1', fontWeight: 'bold' }}>
                {isFa ? `${p.price.toLocaleString('fa-IR')} تومان` : `$${p.price.toFixed(2)}`}
              </td>
              <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>
                {isFa ? `${(p.stockQuantityByWarehouse?.wh_apex_1a || 20).toLocaleString('fa-IR')} عدد` : `${p.stockQuantityByWarehouse?.wh_apex_1a || 20} pcs`}
              </td>
              <td style={{ padding: '12px 16px', display: 'flex', gap: '8px' }}>
                <button
                  onClick={() => openEditModal(p)}
                  style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? '✏️ ویرایش' : '✏️ Edit'}
                </button>
                <button
                  onClick={() => handleAdjust(p.id)}
                  style={{ backgroundColor: '#D8E2FF', color: '#001A41', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? '+ اصلاح موجودی' : '+ Adjust Stock'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Edit Product Pre-Populated Modal */}
      {editingProduct && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: '#FFF', borderRadius: '12px', width: '100%', maxWidth: '520px', padding: '24px', boxShadow: '0 4px 12px rgba(0,0,0,0.15)', direction: isFa ? 'rtl' : 'ltr' }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? `ویرایش مشخصات کالا (${editingProduct.name})` : `Edit Product Details (${editingProduct.name})`}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: '#F8D7DA', color: '#842029', padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleUpdateProduct} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'نام کالا *' : 'Product Name *'}</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                  required
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'کد SKU *' : 'SKU *'}</label>
                  <input
                    type="text"
                    value={sku}
                    onChange={(e) => setSku(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'بارکد' : 'Barcode'}</label>
                  <input
                    type="text"
                    value={barcode}
                    onChange={(e) => setBarcode(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'قیمت فروش *' : 'Selling Price *'}</label>
                  <input
                    type="number"
                    step="0.01"
                    value={price}
                    onChange={(e) => setPrice(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'قیمت خرید (تمام‌شده)' : 'Cost Price'}</label>
                  <input
                    type="number"
                    step="0.01"
                    value={costPrice}
                    onChange={(e) => setCostPrice(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'دسته‌بندی' : 'Category'}</label>
                  <input
                    type="text"
                    value={category}
                    onChange={(e) => setCategory(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'واحد سنجش' : 'Unit'}</label>
                  <input
                    type="text"
                    value={unit}
                    onChange={(e) => setUnit(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                  />
                </div>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'توضیحات' : 'Description'}</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={2}
                  style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={closeEditModal}
                  disabled={isSubmitting}
                  style={{ backgroundColor: '#E0E0E0', color: '#333', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Saving...') : (isFa ? 'ثبت تغییرات' : 'Save Changes')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
