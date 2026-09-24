import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { CartItem, Customer, Product, SalesOrder } from '../types';

interface PosPageProps {
  language?: 'fa' | 'en';
}

export const PosPage: React.FC<PosPageProps> = ({ language = 'fa' }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const [cart, setCart] = useState<CartItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [lastOrder, setLastOrder] = useState<SalesOrder | null>(null);

  const isFa = language === 'fa';

  useEffect(() => {
    loadProducts();
  }, []);

  const loadProducts = async () => {
    const list = await apiClient.getProducts(searchQuery);
    setProducts(list);
  };

  const addToCart = (product: Product) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.productId === product.id);
      if (existing) {
        return prev.map((item) =>
          item.productId === product.id ? { ...item, quantity: item.quantity + 1 } : item
        );
      }
      return [
        ...prev,
        {
          id: product.id,
          productId: product.id,
          productName: product.name,
          sku: product.sku,
          barcode: product.barcode,
          price: product.price,
          quantity: 1,
          discountPercent: 0,
          taxRate: 0.08,
        },
      ];
    });
  };

  const subtotal = cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
  const tax = subtotal * 0.08;
  const grandTotal = subtotal + tax;

  const handleCheckout = async () => {
    if (cart.length === 0) return;
    const order = await apiClient.checkout({
      customer_name: selectedCustomer?.name || (isFa ? 'مشتری حضوری' : 'Walk-in Customer'),
      payment_method: paymentMethod,
      items: cart.map((i) => ({
        product_id: i.productId,
        product_name: i.productName,
        sku: i.sku,
        price: i.price,
        quantity: i.quantity,
      })),
    });
    setLastOrder(order);
    setCart([]);
  };

  return (
    <div style={{ display: 'flex', gap: '24px', padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      {/* Catalog Grid */}
      <div style={{ flex: 2 }}>
        <input
          type="text"
          placeholder={isFa ? 'جستجوی کالا بر اساس نام، بارکد، کد...' : 'Search products by name, SKU, barcode...'}
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            loadProducts();
          }}
          style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: '1px solid #CCC', fontSize: '15px', marginBottom: '20px' }}
        />

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '16px' }}>
          {products.map((p) => (
            <div
              key={p.id}
              onClick={() => addToCart(p)}
              style={{ backgroundColor: '#FFFFFF', border: '1px solid #E0E0E0', borderRadius: '12px', padding: '16px', cursor: 'pointer', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}
            >
              <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: 'bold' }}>{p.name}</h4>
              <p style={{ margin: '0 0 8px 0', fontSize: '12px', color: '#666' }}>{isFa ? 'کد SKU:' : 'SKU:'} {p.sku}</p>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '18px', fontWeight: 'bold', color: '#005AC1' }}>
                  {isFa ? `${p.price.toLocaleString('fa-IR')} تومان` : `$${p.price.toFixed(2)}`}
                </span>
                <button style={{ backgroundColor: '#D8E2FF', color: '#001A41', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer' }}>
                  {isFa ? '+ افزودن' : '+ Add'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Live Cart Sidebar */}
      <div style={{ flex: 1, backgroundColor: '#FFFFFF', border: '1px solid #E0E0E0', borderRadius: '12px', padding: '20px', display: 'flex', flexDirection: 'column', height: 'fit-content' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>
          {isFa ? `سبد خرید (${cart.reduce((s, i) => s + i.quantity, 0)})` : `Current Cart (${cart.reduce((s, i) => s + i.quantity, 0)})`}
        </h3>

        {cart.length === 0 ? (
          <p style={{ color: '#888', textAlign: 'center', padding: '40px 0' }}>{isFa ? 'سبد خرید خالی است' : 'Cart is empty'}</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '300px', overflowY: 'auto' }}>
            {cart.map((item) => (
              <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid #F0F0F0', paddingBottom: '8px' }}>
                <div>
                  <div style={{ fontWeight: 'bold', fontSize: '14px' }}>{item.productName}</div>
                  <div style={{ fontSize: '12px', color: '#666' }}>
                    {isFa ? `${item.price.toLocaleString('fa-IR')} تومان` : `$${item.price.toFixed(2)} ea`}
                  </div>
                </div>
                <div style={{ fontWeight: 'bold', color: '#005AC1' }}>
                  x{item.quantity} = {isFa ? `${(item.price * item.quantity).toLocaleString('fa-IR')} تومان` : `$${(item.price * item.quantity).toFixed(2)}`}
                </div>
              </div>
            ))}
          </div>
        )}

        <div style={{ marginTop: '20px', borderTop: '2px solid #EEE', paddingTop: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
            <span>{isFa ? 'جمع کل' : 'Subtotal'}</span>
            <span>{isFa ? `${subtotal.toLocaleString('fa-IR')} تومان` : `$${subtotal.toFixed(2)}`}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
            <span>{isFa ? 'مالیات (۸٪)' : 'Tax (8%)'}</span>
            <span>{isFa ? `${tax.toLocaleString('fa-IR')} تومان` : `$${tax.toFixed(2)}`}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '20px', fontWeight: 'bold', color: '#005AC1', margin: '12px 0' }}>
            <span>{isFa ? 'مبلغ قابل پرداخت' : 'Total'}</span>
            <span>{isFa ? `${grandTotal.toLocaleString('fa-IR')} تومان` : `$${grandTotal.toFixed(2)}`}</span>
          </div>

          <button
            onClick={handleCheckout}
            disabled={cart.length === 0}
            style={{ width: '100%', backgroundColor: cart.length > 0 ? '#005AC1' : '#CCC', color: '#FFF', border: 'none', borderRadius: '8px', padding: '14px', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            {isFa ? `تسویه و ثبت فاکتور (${grandTotal.toLocaleString('fa-IR')} تومان)` : `Checkout ($${grandTotal.toFixed(2)})`}
          </button>
        </div>
      </div>
    </div>
  );
};

