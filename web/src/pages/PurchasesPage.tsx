import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Product, Purchase, Supplier } from '../types';

interface PurchasesPageProps {
  language?: 'fa' | 'en';
}

interface NewPurchaseItem {
  productId: string;
  quantity: number;
  unitCost: number;
}

export const PurchasesPage: React.FC<PurchasesPageProps> = ({ language = 'fa' }) => {
  const [purchases, setPurchases] = useState<Purchase[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  // Modals
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [selectedPurchaseDetail, setSelectedPurchaseDetail] = useState<Purchase | null>(null);

  // Create Form State
  const [selectedSupplierId, setSelectedSupplierId] = useState('');
  const [selectedWarehouseId, setSelectedWarehouseId] = useState('wh_apex_1a');
  const [orderItems, setOrderItems] = useState<NewPurchaseItem[]>([]);
  const [selectedProductId, setSelectedProductId] = useState('');
  const [inputQuantity, setInputQuantity] = useState<number | ''>(1);
  const [inputUnitCost, setInputUnitCost] = useState<number | ''>(100);

  // Payment Form State
  const [paymentAmount, setPaymentAmount] = useState<number | ''>(0);
  const [paymentMethod, setPaymentMethod] = useState('BANK_TRANSFER');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';

  useEffect(() => {
    loadPurchases();
    apiClient.getSuppliers().then(setSuppliers);
    apiClient.getProducts().then(setProducts);
  }, []);

  const loadPurchases = async () => {
    setIsLoading(true);
    try {
      const list = await apiClient.getPurchases();
      setPurchases(list);
    } catch {
      // Handled in apiClient
    } finally {
      setIsLoading(false);
    }
  };

  const openCreateModal = () => {
    setSelectedSupplierId(suppliers[0]?.id || '');
    setSelectedWarehouseId('wh_apex_1a');
    setOrderItems([]);
    setErrorMessage('');
    setIsCreateModalOpen(true);
  };

  const closeModals = () => {
    setIsCreateModalOpen(false);
    setSelectedPurchaseDetail(null);
    setErrorMessage('');
  };

  const addItemToOrder = () => {
    if (!selectedProductId) return;
    const prod = products.find((p) => p.id === selectedProductId);
    const qty = Number(inputQuantity) || 1;
    const cost = Number(inputUnitCost) || (prod ? prod.costPrice : 100);

    setOrderItems((prev) => [
      ...prev,
      {
        productId: selectedProductId,
        quantity: qty,
        unitCost: cost,
      },
    ]);

    setSelectedProductId('');
    setInputQuantity(1);
    setInputUnitCost(100);
  };

  const removeItemFromOrder = (index: number) => {
    setOrderItems((prev) => prev.filter((_, i) => i !== index));
  };

  const orderTotal = orderItems.reduce((sum, item) => sum + item.quantity * item.unitCost, 0);

  const handleCreatePurchase = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSupplierId) {
      setErrorMessage(isFa ? 'لطفاً تامین‌کننده را انتخاب کنید.' : 'Please select a supplier.');
      return;
    }
    if (orderItems.length === 0) {
      setErrorMessage(isFa ? 'حداقل یک کالا باید به سفارش اضافه شود.' : 'Please add at least one product item.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      await apiClient.createPurchase({
        store_id: 'store_apex_1',
        warehouse_id: selectedWarehouseId,
        supplier_id: selectedSupplierId,
        items: orderItems.map((item) => ({
          product_id: item.productId,
          quantity: item.quantity,
          unit_cost: item.unitCost,
        })),
      });

      setIsSubmitting(false);
      closeModals();
      alert(isFa ? 'سفارش خرید با موفقیت ثبت شد.' : 'Purchase Order created successfully.');
      loadPurchases();
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در ثبت سفارش خرید' : 'Failed to create purchase order'));
    }
  };

  const handleReceiveGoods = async (purchase: Purchase) => {
    const confirmMsg = isFa
      ? `آیا از تحویل قطعی کالاهای فاکتور (${purchase.purchaseNumber}) و افزایش موجودی انبار اطمینان دارید؟`
      : `Are you sure you want to receive goods for (${purchase.purchaseNumber}) into inventory?`;

    if (!window.confirm(confirmMsg)) return;

    try {
      await apiClient.receivePurchase(purchase.id);
      alert(isFa ? 'کالاها با موفقیت تحویل انبار شدند و سند حسابداری صادر گردید.' : 'Goods received into inventory and accounting posted.');
      closeModals();
      loadPurchases();
    } catch (err: any) {
      alert(err.message || (isFa ? 'خطا در تحویل کالا به انبار' : 'Failed to receive goods into inventory'));
    }
  };

  const handlePaySupplier = async (purchase: Purchase) => {
    const amount = Number(paymentAmount);
    if (!amount || amount <= 0) {
      alert(isFa ? 'لطفاً مبلغ معتبری وارد کنید.' : 'Please enter a valid payment amount.');
      return;
    }

    try {
      await apiClient.payPurchase(purchase.id, amount, paymentMethod);
      alert(isFa ? 'پرداخت به تامین‌کننده با موفقیت ثبت گردید.' : 'Supplier payment recorded successfully.');
      setPaymentAmount(0);
      closeModals();
      loadPurchases();
    } catch (err: any) {
      alert(err.message || (isFa ? 'خطا در ثبت پرداخت' : 'Failed to record payment'));
    }
  };

  const handleViewDetail = async (id: string) => {
    try {
      const detailed = await apiClient.getPurchaseById(id);
      setSelectedPurchaseDetail(detailed);
      setPaymentAmount(detailed.totalAmount);
    } catch {
      // Fallback
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <h2 style={{ margin: 0 }}>{isFa ? 'مدیریت خریدهای تامین و تحویل کالا' : 'Purchasing & Goods Receiving'}</h2>
        <button
          onClick={openCreateModal}
          style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '8px', padding: '10px 18px', fontSize: '14px', fontWeight: 'bold', cursor: 'pointer' }}
        >
          {isFa ? '+ ثبت سفارش خرید جدید' : '+ New Purchase Order'}
        </button>
      </div>

      {/* Purchases List Table */}
      {isLoading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#666' }}>{isFa ? 'در حال بارگذاری فاکتورهای خرید...' : 'Loading purchase orders...'}</div>
      ) : purchases.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#888', backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E0E0E0' }}>
          {isFa ? 'هیچ فاکتور خریدی ثبت نشده است.' : 'No purchase orders found.'}
        </div>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#FFF', borderRadius: '8px', overflow: 'hidden', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
          <thead>
            <tr style={{ backgroundColor: '#F5F5F5', textAlign: isFa ? 'right' : 'left', borderBottom: '2px solid #DDD' }}>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'شماره فاکتور' : 'PO Number'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'تامین‌کننده' : 'Supplier'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'مبلغ کل' : 'Total Amount'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'وضعیت تحویل' : 'Receiving Status'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'وضعیت پرداخت' : 'Payment Status'}</th>
              <th style={{ padding: '12px 16px' }}>{isFa ? 'عملیات' : 'Actions'}</th>
            </tr>
          </thead>
          <tbody>
            {purchases.map((p) => (
              <tr key={p.id} style={{ borderBottom: '1px solid #EEE' }}>
                <td style={{ padding: '12px 16px', fontFamily: 'monospace', fontWeight: 'bold' }}>{p.purchaseNumber}</td>
                <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{p.supplierName || 'TechImport Global Co.'}</td>
                <td style={{ padding: '12px 16px', color: '#005AC1', fontWeight: 'bold' }}>
                  {isFa ? `${p.totalAmount.toLocaleString('fa-IR')} تومان` : `$${p.totalAmount.toFixed(2)}`}
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <span style={{
                    padding: '4px 10px',
                    borderRadius: '12px',
                    fontSize: '12px',
                    fontWeight: 'bold',
                    backgroundColor: p.status === 'RECEIVED' ? '#D1E7DD' : '#FFF3CD',
                    color: p.status === 'RECEIVED' ? '#0F5132' : '#664D03'
                  }}>
                    {p.status === 'RECEIVED' ? (isFa ? '📦 تحویل‌شده به انبار' : 'RECEIVED') : (isFa ? '⏳ در انتظار تحویل' : 'ORDERED')}
                  </span>
                </td>
                <td style={{ padding: '12px 16px' }}>
                  <span style={{
                    padding: '4px 10px',
                    borderRadius: '12px',
                    fontSize: '12px',
                    fontWeight: 'bold',
                    backgroundColor: p.paymentStatus === 'PAID' ? '#D1E7DD' : '#F8D7DA',
                    color: p.paymentStatus === 'PAID' ? '#0F5132' : '#842029'
                  }}>
                    {p.paymentStatus === 'PAID' ? (isFa ? '💳 تسویه‌شده' : 'PAID') : (isFa ? '⚠️ تسویه‌نشده' : 'UNPAID')}
                  </span>
                </td>
                <td style={{ padding: '12px 16px', display: 'flex', gap: '8px' }}>
                  <button
                    onClick={() => handleViewDetail(p.id)}
                    style={{ backgroundColor: '#F0F0F0', color: '#333', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '👁️ جزئیات' : '👁️ Details'}
                  </button>
                  {p.status === 'ORDERED' && (
                    <button
                      onClick={() => handleReceiveGoods(p)}
                      style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer' }}
                    >
                      {isFa ? '📦 تحویل کالا' : '📦 Receive Goods'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Create Purchase Order Modal */}
      {isCreateModalOpen && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: '#FFF', borderRadius: '12px', width: '100%', maxWidth: '640px', padding: '24px', boxShadow: '0 4px 12px rgba(0,0,0,0.15)', direction: isFa ? 'rtl' : 'ltr' }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? 'ثبت فاکتور سفارش خرید جدید' : 'New Purchase Order'}
            </h3>

            {errorMessage && (
              <div style={{ backgroundColor: '#F8D7DA', color: '#842029', padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleCreatePurchase} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '4px' }}>{isFa ? 'انتخاب تامین‌کننده *' : 'Select Supplier *'}</label>
                  <select
                    value={selectedSupplierId}
                    onChange={(e) => setSelectedSupplierId(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  >
                    {suppliers.map((s) => (
                      <option key={s.id} value={s.id}>{s.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '13px', fontWeight 600, marginBottom: '4px' }}>{isFa ? 'انبار مقصد تحویل *' : 'Target Warehouse *'}</label>
                  <select
                    value={selectedWarehouseId}
                    onChange={(e) => setSelectedWarehouseId(e.target.value)}
                    style={{ width: '100%', padding: '8px 12px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box' }}
                    required
                  >
                    <option value="wh_apex_1a">{isFa ? 'انبار اصلی (WH-MAIN)' : 'Main Warehouse (WH-MAIN)'}</option>
                    <option value="wh_apex_1b">{isFa ? 'انبار اکسپرس (WH-EXP)' : 'Express Hub (WH-EXP)'}</option>
                  </select>
                </div>
              </div>

              {/* Add Item Builder */}
              <div style={{ backgroundColor: '#F9F9F9', padding: '12px', borderRadius: '8px', border: '1px solid #EEE', marginTop: '8px' }}>
                <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: 'bold' }}>{isFa ? 'افزودن کالا به فاکتور خرید' : 'Add Items to Order'}</h4>
                <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr auto', gap: '8px', alignItems: 'center' }}>
                  <select
                    value={selectedProductId}
                    onChange={(e) => {
                      setSelectedProductId(e.target.value);
                      const prod = products.find((p) => p.id === e.target.value);
                      if (prod) setInputUnitCost(prod.costPrice);
                    }}
                    style={{ padding: '8px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '13px' }}
                  >
                    <option value="">{isFa ? '-- انتخاب کالا --' : '-- Select Product --'}</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>
                    ))}
                  </select>

                  <input
                    type="number"
                    min="1"
                    placeholder={isFa ? 'تعداد' : 'Qty'}
                    value={inputQuantity}
                    onChange={(e) => setInputQuantity(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ padding: '8px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '13px' }}
                  />

                  <input
                    type="number"
                    step="0.01"
                    placeholder={isFa ? 'قیمت خرید' : 'Unit Cost'}
                    value={inputUnitCost}
                    onChange={(e) => setInputUnitCost(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ padding: '8px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '13px' }}
                  />

                  <button
                    type="button"
                    onClick={addItemToOrder}
                    disabled={!selectedProductId}
                    style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 12px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '+ افزودن' : '+ Add'}
                  </button>
                </div>
              </div>

              {/* Items List */}
              <div style={{ maxHeight: '180px', overflowY: 'auto' }}>
                <table style={{ width: '100%', fontSize: '13px', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ backgroundColor: '#F0F0F0' }}>
                      <th style={{ padding: '6px', textAlign: isFa ? 'right' : 'left' }}>{isFa ? 'کالا' : 'Product'}</th>
                      <th style={{ padding: '6px' }}>{isFa ? 'تعداد' : 'Qty'}</th>
                      <th style={{ padding: '6px' }}>{isFa ? 'قیمت واحد' : 'Unit Cost'}</th>
                      <th style={{ padding: '6px' }}>{isFa ? 'جمع' : 'Total'}</th>
                      <th style={{ padding: '6px' }}></th>
                    </tr>
                  </thead>
                  <tbody>
                    {orderItems.map((item, idx) => {
                      const prod = products.find((p) => p.id === item.productId);
                      return (
                        <tr key={idx} style={{ borderBottom: '1px solid #EEE' }}>
                          <td style={{ padding: '6px' }}>{prod?.name || item.productId}</td>
                          <td style={{ padding: '6px', textAlign: 'center' }}>{item.quantity}</td>
                          <td style={{ padding: '6px', textAlign: 'center' }}>{isFa ? `${item.unitCost.toLocaleString('fa-IR')} تومان` : `$${item.unitCost}`}</td>
                          <td style={{ padding: '6px', textAlign: 'center', fontWeight: 'bold' }}>{isFa ? `${(item.quantity * item.unitCost).toLocaleString('fa-IR')} تومان` : `$${item.quantity * item.unitCost}`}</td>
                          <td style={{ padding: '6px', textAlign: 'center' }}>
                            <button type="button" onClick={() => removeItemFromOrder(idx)} style={{ color: 'red', border: 'none', background: 'none', cursor: 'pointer' }}>✕</button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '2px solid #EEE', paddingTop: '12px', marginTop: '8px' }}>
                <span style={{ fontSize: '16px', fontWeight: 'bold' }}>{isFa ? 'مبلغ کل فاکتور:' : 'Total Cost:'}</span>
                <span style={{ fontSize: '20px', fontWeight: 'bold', color: '#005AC1' }}>
                  {isFa ? `${orderTotal.toLocaleString('fa-IR')} تومان` : `$${orderTotal.toFixed(2)}`}
                </span>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '16px' }}>
                <button
                  type="button"
                  onClick={closeModals}
                  disabled={isSubmitting}
                  style={{ backgroundColor: '#E0E0E0', color: '#333', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting || orderItems.length === 0}
                  style={{ backgroundColor: orderItems.length > 0 ? '#005AC1' : '#CCC', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 18px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isSubmitting ? (isFa ? 'در حال ثبت...' : 'Submitting...') : (isFa ? 'ثبت نهائی سفارش خرید' : 'Submit Purchase Order')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Purchase Order Detail Modal */}
      {selectedPurchaseDetail && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: '#FFF', borderRadius: '12px', width: '100%', maxWidth: '600px', padding: '24px', boxShadow: '0 4px 12px rgba(0,0,0,0.15)', direction: isFa ? 'rtl' : 'ltr' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>
                {isFa ? `جزئیات فاکتور خرید (${selectedPurchaseDetail.purchaseNumber})` : `PO Details (${selectedPurchaseDetail.purchaseNumber})`}
              </h3>
              <button onClick={closeModals} style={{ backgroundColor: 'transparent', border: 'none', fontSize: '18px', cursor: 'pointer' }}>✕</button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '13px', marginBottom: '16px', backgroundColor: '#F9F9F9', padding: '12px', borderRadius: '8px' }}>
              <div><strong>{isFa ? 'تامین‌کننده:' : 'Supplier:'}</strong> {selectedPurchaseDetail.supplierName || 'TechImport Global Co.'}</div>
              <div><strong>{isFa ? 'وضعیت تحویل:' : 'Receiving Status:'}</strong> {selectedPurchaseDetail.status}</div>
              <div><strong>{isFa ? 'انبار مقصد:' : 'Warehouse:'}</strong> {selectedPurchaseDetail.warehouseId}</div>
              <div><strong>{isFa ? 'وضعیت پرداخت:' : 'Payment Status:'}</strong> {selectedPurchaseDetail.paymentStatus}</div>
            </div>

            <h4 style={{ margin: '0 0 8px 0', fontSize: '15px', fontWeight: 'bold' }}>{isFa ? 'اقلام سفارش خرید:' : 'Order Items:'}</h4>
            <table style={{ width: '100%', fontSize: '13px', borderCollapse: 'collapse', marginBottom: '16px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F0F0F0', textAlign: isFa ? 'right' : 'left' }}>
                  <th style={{ padding: '8px' }}>{isFa ? 'کالا' : 'Product'}</th>
                  <th style={{ padding: '8px' }}>{isFa ? 'تعداد' : 'Qty'}</th>
                  <th style={{ padding: '8px' }}>{isFa ? 'قیمت واحد' : 'Unit Cost'}</th>
                  <th style={{ padding: '8px' }}>{isFa ? 'مبلغ کل' : 'Total'}</th>
                </tr>
              </thead>
              <tbody>
                {selectedPurchaseDetail.items.map((item, idx) => (
                  <tr key={idx} style={{ borderBottom: '1px solid #EEE' }}>
                    <td style={{ padding: '8px', fontWeight: 'bold' }}>{item.productName || item.productId}</td>
                    <td style={{ padding: '8px' }}>{item.quantity}</td>
                    <td style={{ padding: '8px' }}>{isFa ? `${item.unitCost.toLocaleString('fa-IR')} تومان` : `$${item.unitCost.toFixed(2)}`}</td>
                    <td style={{ padding: '8px', fontWeight: 'bold' }}>{isFa ? `${item.totalCost.toLocaleString('fa-IR')} تومان` : `$${item.totalCost.toFixed(2)}`}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* Goods Receiving Action */}
            {selectedPurchaseDetail.status === 'ORDERED' && (
              <div style={{ backgroundColor: '#EBF3FF', border: '1px solid #B8D5FF', padding: '12px', borderRadius: '8px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div>
                  <div style={{ fontWeight: 'bold', fontSize: '14px', color: '#003A8C' }}>{isFa ? 'تحویل قطعی کالا به انبار' : 'Receive Goods into Warehouse'}</div>
                  <div style={{ fontSize: '12px', color: '#002166' }}>{isFa ? 'تحویل کالا باعث افزایش موجودی انبار و ثبت سند حسابداری می‌شود.' : 'Receiving increments warehouse stock and posts AP journal.'}</div>
                </div>
                <button
                  onClick={() => handleReceiveGoods(selectedPurchaseDetail)}
                  style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? '📦 تحویل کالا' : '📦 Confirm Receive'}
                </button>
              </div>
            )}

            {/* Supplier Payment Section */}
            {selectedPurchaseDetail.paymentStatus !== 'PAID' && (
              <div style={{ backgroundColor: '#FFF8E6', border: '1px solid #FFE5B4', padding: '12px', borderRadius: '8px', marginBottom: '16px' }}>
                <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: 'bold', color: '#8A5300' }}>{isFa ? 'پرداخت به تامین‌کننده (بدهی حساب)' : 'Pay Supplier (Payable)'}</h4>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                  <input
                    type="number"
                    step="0.01"
                    placeholder={isFa ? 'مبلغ پرداخت' : 'Payment Amount'}
                    value={paymentAmount}
                    onChange={(e) => setPaymentAmount(e.target.value === '' ? '' : Number(e.target.value))}
                    style={{ flex: 1, padding: '8px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '13px' }}
                  />
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                    style={{ padding: '8px', borderRadius: '6px', border: '1px solid #CCC', fontSize: '13px' }}
                  >
                    <option value="BANK_TRANSFER">{isFa ? 'حواله بانکی' : 'Bank Transfer'}</option>
                    <option value="CASH">{isFa ? 'نقدی' : 'Cash'}</option>
                  </select>
                  <button
                    onClick={() => handlePaySupplier(selectedPurchaseDetail)}
                    style={{ backgroundColor: '#006C4C', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 14px', fontWeight: 'bold', cursor: 'pointer' }}
                  >
                    {isFa ? '💳 ثبت پرداخت' : '💳 Record Payment'}
                  </button>
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
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
