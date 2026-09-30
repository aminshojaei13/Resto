import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { CartItem, Customer, Product, SalesOrder } from '../types';
import { formatMoney, moneyAdd, moneyMultiply, moneyRound } from '../util/money';
import { useTheme } from '../theme/ThemeContext';

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
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  useEffect(() => {
    loadProducts();
  }, []);

  const loadProducts = async () => {
    try {
      const list = await apiClient.getProducts(searchQuery);
      setProducts(list || []);
    } catch {
      setProducts([]);
    }
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

  const subtotal = moneyRound(cart.reduce((sum, item) => sum + moneyMultiply(item.price, item.quantity), 0));
  const tax = moneyMultiply(subtotal, 0.08);
  const grandTotal = moneyAdd(subtotal, tax);

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
    <div style={{ display: 'flex', gap: '24px', padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      {/* Catalog Grid */}
      <div style={{ flex: 2, minWidth: 0 }}>
        <input
          type="text"
          placeholder={isFa ? 'جستجوی کالا بر اساس نام، بارکد، کد...' : 'Search products by name, SKU, barcode...'}
          value={searchQuery}
          onChange={(e) => {
            setSearchQuery(e.target.value);
            loadProducts();
          }}
          style={{ width: '100%', padding: '12px 16px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, fontSize: '15px', marginBottom: '20px', backgroundColor: theme.colors.surfaceElevated, color: theme.colors.textPrimary, boxSizing: 'border-box' }}
        />

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '16px' }}>
          {products.map((p) => (
            <div
              key={p.id}
              onClick={() => addToCart(p)}
              style={{ backgroundColor: theme.colors.surface, border: `1px solid ${theme.colors.border}`, borderRadius: '12px', padding: '16px', cursor: 'pointer', boxShadow: theme.shadows.card }}
            >
              <h4 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: 'bold', color: theme.colors.textPrimary }}>{p.name}</h4>
              <p style={{ margin: '0 0 8px 0', fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'کد SKU:' : 'SKU:'} {p.sku}</p>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '18px', fontWeight: 'bold', color: theme.colors.primary }}>
                  {formatMoney(p.price, isFa)}
                </span>
                <button style={{ backgroundColor: theme.colors.primaryLight, color: theme.colors.primaryDark, border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer' }}>
                  {isFa ? '+ افزودن' : '+ Add'}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Live Cart Sidebar */}
      <div style={{ flex: 1, backgroundColor: theme.colors.surfaceElevated, border: `1px solid ${theme.colors.border}`, borderRadius: '12px', padding: '20px', display: 'flex', flexDirection: 'column', height: 'fit-content' }}>
        <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
          {isFa ? `سبد خرید (${cart.reduce((s, i) => s + i.quantity, 0)})` : `Current Cart (${cart.reduce((s, i) => s + i.quantity, 0)})`}
        </h3>

        {cart.length === 0 ? (
          <p style={{ color: theme.colors.textMuted, textAlign: 'center', padding: '40px 0' }}>{isFa ? 'سبد خرید خالی است' : 'Cart is empty'}</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', maxHeight: '300px', overflowY: 'auto' }}>
            {cart.map((item) => (
              <div key={item.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: `1px solid ${theme.colors.border}`, paddingBottom: '8px' }}>
                <div>
                  <div style={{ fontWeight: 'bold', fontSize: '14px', color: theme.colors.textPrimary }}>{item.productName}</div>
                  <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                    {formatMoney(item.price, isFa)} ea
                  </div>
                </div>
                <div style={{ fontWeight: 'bold', color: theme.colors.primary }}>
                  x{item.quantity} = {formatMoney(moneyMultiply(item.price, item.quantity), isFa)}
                </div>
              </div>
            ))}
          </div>
        )}

        <div style={{ marginTop: '20px', borderTop: `2px solid ${theme.colors.border}`, paddingTop: '16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
            <span>{isFa ? 'جمع کل' : 'Subtotal'}</span>
            <span>{formatMoney(subtotal, isFa)}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
            <span>{isFa ? 'مالیات (۸٪)' : 'Tax (8%)'}</span>
            <span>{formatMoney(tax, isFa)}</span>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '20px', fontWeight: 'bold', color: theme.colors.primary, margin: '12px 0' }}>
            <span>{isFa ? 'مبلغ قابل پرداخت' : 'Total'}</span>
            <span>{formatMoney(grandTotal, isFa)}</span>
          </div>

          <button
            onClick={handleCheckout}
            disabled={cart.length === 0}
            style={{ width: '100%', backgroundColor: cart.length > 0 ? theme.colors.primary : theme.colors.surfaceHover, color: cart.length > 0 ? theme.colors.primaryTextOnBrand : theme.colors.textMuted, border: 'none', borderRadius: '8px', padding: '14px', fontSize: '16px', fontWeight: 'bold', cursor: cart.length > 0 ? 'pointer' : 'not-allowed' }}
          >
            {isFa ? `تسویه و ثبت فاکتور (${formatMoney(grandTotal, true)})` : `Checkout (${formatMoney(grandTotal, false)})`}
          </button>
        </div>
      </div>
    </div>
  );
};
