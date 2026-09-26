import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';

interface MessagesPageProps {
  language?: 'fa' | 'en';
}

export const MessagesPage: React.FC<MessagesPageProps> = ({ language = 'fa' }) => {
  const [activeSubTab, setActiveTab] = useState<'import' | 'history'>('import');
  const [rawText, setRawText] = useState('');
  const [parsedPayload, setParsedPayload] = useState<any | null>(null);
  const [importedMessages, setImportedMessages] = useState<any[]>([]);
  const [isParsing, setIsSubmitting] = useState(false);
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const isFa = language === 'fa';

  const sampleTemplate = `RESTO_ORDER
Customer: Ali Rezaei
Phone: +1 (555) 888-9999
SKU: APX-LAP-001
Quantity: 2
Address: Tehran, Freedom Square
Payment: Cash`;

  useEffect(() => {
    if (activeSubTab === 'history') {
      loadMessageHistory();
    }
  }, [activeSubTab]);

  const loadMessageHistory = async () => {
    try {
      const list = await apiClient.getImportedMessages();
      setImportedMessages(list);
    } catch {
      // Fallback
    }
  };

  const handleInsertSample = () => {
    setRawText(sampleTemplate);
    setParsedPayload(null);
    setErrorMessage('');
  };

  const handleParseMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rawText.trim()) {
      setErrorMessage(isFa ? 'لطفاً متن پیام سفارش را وارد کنید.' : 'Please enter message text.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const result = await apiClient.parseMessage(rawText, 'manual_paste');
      setParsedPayload(result);
      setIsSubmitting(false);
    } catch (err: any) {
      setIsSubmitting(false);
      setErrorMessage(err.message || (isFa ? 'خطا در پردازش پیام سفارش' : 'Failed to parse order message'));
    }
  };

  const handleConfirmOrder = async () => {
    if (!parsedPayload) return;

    setIsCheckingOut(true);
    try {
      const order = await apiClient.checkout({
        customer_id: parsedPayload.customer?.id,
        customer_name: parsedPayload.customer?.name || 'Social Customer',
        payment_method: parsedPayload.payment_method || 'CASH',
        items: parsedPayload.items.map((i: any) => ({
          product_id: i.product_id,
          product_name: i.product_name,
          sku: i.sku,
          price: i.price,
          quantity: i.quantity,
        })),
      });

      setIsCheckingOut(false);
      alert(isFa ? `سفارش شماره ${order.orderNumber} با موفقیت ثبت شد و موجودی انبار کسر گردید!` : `Order ${order.orderNumber} created successfully! Stock deducted.`);
      setParsedPayload(null);
      setRawText('');
    } catch (err: any) {
      setIsCheckingOut(false);
      alert(err.message || (isFa ? 'خطا در ثبت نهایی سفارش' : 'Failed to checkout order'));
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ margin: '0 0 4px 0' }}>{isFa ? 'ورود و پردازش پیام‌های سفارش (Social Message Import)' : 'Social Order Message Import'}</h2>
        <p style={{ margin: 0, color: '#666', fontSize: '13px' }}>
          {isFa ? 'استخراج قطعی و دقیق مشخصات مشتری و کالا از پیام‌های اینستاگرام، تلگرام و واتس‌اپ بدون واسطه AI' : 'Deterministic parsing of social order messages into sales orders and stock deductions'}
        </p>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '12px', borderBottom: '2px solid #E0E0E0', marginBottom: '20px' }}>
        <button
          onClick={() => setActiveTab('import')}
          style={{
            padding: '10px 16px',
            border: 'none',
            background: 'none',
            fontWeight: 'bold',
            fontSize: '14px',
            cursor: 'pointer',
            borderBottom: activeSubTab === 'import' ? '3px solid #005AC1' : 'none',
            color: activeSubTab === 'import' ? '#005AC1' : '#666',
          }}
        >
          {isFa ? '📋 پردازش و ثبت پیام جدید' : '📋 Import New Message'}
        </button>
        <button
          onClick={() => setActiveTab('history')}
          style={{
            padding: '10px 16px',
            border: 'none',
            background: 'none',
            fontWeight: 'bold',
            fontSize: '14px',
            cursor: 'pointer',
            borderBottom: activeSubTab === 'history' ? '3px solid #005AC1' : 'none',
            color: activeSubTab === 'history' ? '#005AC1' : '#666',
          }}
        >
          {isFa ? '📂 سابقه پیام‌های دریافتی' : '📂 Message History'}
        </button>
      </div>

      {activeSubTab === 'import' ? (
        <div style={{ display: 'grid', gridTemplateColumns: parsedPayload ? '1fr 1fr' : '1fr', gap: '24px' }}>
          {/* Form Input Area */}
          <div style={{ backgroundColor: '#FFF', padding: '20px', borderRadius: '12px', border: '1px solid #E0E0E0', boxShadow: '0 2px 4px rgba(0,0,0,0.05)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold' }}>{isFa ? 'متن پیام دریافتی را وارد کنید:' : 'Enter Received Message Text:'}</h3>
              <button
                type="button"
                onClick={handleInsertSample}
                style={{ backgroundColor: '#E3F2FD', color: '#0D47A1', border: 'none', borderRadius: '6px', padding: '6px 12px', fontSize: '12px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                {isFa ? '📋 درج نمونه پیام سفارش' : '📋 Insert Sample Message'}
              </button>
            </div>

            {errorMessage && (
              <div style={{ backgroundColor: '#F8D7DA', color: '#842029', padding: '10px 14px', borderRadius: '6px', marginBottom: '16px', fontSize: '14px' }}>
                {errorMessage}
              </div>
            )}

            <form onSubmit={handleParseMessage} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <textarea
                value={rawText}
                onChange={(e) => setRawText(e.target.value)}
                placeholder={isFa ? "متن پیام را کپی و اینجا پیست کنید...\nمثال:\nCALCUAPP_ORDER\nCustomer: علی رضایی\nPhone: 09121234567\nSKU: APX-LAP-001\nQuantity: 2" : "Paste message text here..."}
                rows={10}
                style={{ width: '100%', padding: '12px', borderRadius: '8px', border: '1px solid #CCC', fontSize: '14px', fontFamily: 'monospace', boxSizing: 'border-box' }}
                required
              />

              <button
                type="submit"
                disabled={isParsing || !rawText.trim()}
                style={{ backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '8px', padding: '12px', fontSize: '15px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                {isParsing ? (isFa ? 'در حال پردازش قطعی...' : 'Parsing...') : (isFa ? '🔍 پردازش و استخراج اطلاعات سفارش' : '🔍 Parse Order Message')}
              </button>
            </form>
          </div>

          {/* Parsed Preview Card */}
          {parsedPayload && (
            <div style={{ backgroundColor: '#FFF', padding: '20px', borderRadius: '12px', border: '1px solid #005AC1', boxShadow: '0 4px 12px rgba(0,90,193,0.1)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', borderBottom: '1px solid #EEE', paddingBottom: '10px' }}>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 'bold', color: '#005AC1' }}>
                  {isFa ? 'پیش‌نمایش سفارش استخراج‌شده' : 'Parsed Order Preview'}
                </h3>
                <span style={{ backgroundColor: '#E3F2FD', color: '#0D47A1', padding: '4px 10px', borderRadius: '12px', fontSize: '12px', fontWeight: 'bold' }}>
                  {isFa ? 'تاییدشده' : 'Parsed Valid'}
                </span>
              </div>

              {/* Customer Info */}
              <div style={{ backgroundColor: '#F9F9F9', padding: '12px', borderRadius: '8px', marginBottom: '16px', fontSize: '13px' }}>
                <div><strong>{isFa ? 'مشتری:' : 'Customer:'}</strong> {parsedPayload.customer.name} {parsedPayload.customer.is_new ? (isFa ? '(مشتری جدید)' : '(New Customer)') : ''}</div>
                <div><strong>{isFa ? 'تلفن:' : 'Phone:'}</strong> {parsedPayload.customer.phone || '-'}</div>
                <div><strong>{isFa ? 'آدرس تحویل:' : 'Address:'}</strong> {parsedPayload.customer.address || '-'}</div>
              </div>

              {/* Matched Products */}
              <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: 'bold' }}>{isFa ? 'اقلام تطبیق‌یافته کاتالوگ:' : 'Matched Catalog Items:'}</h4>
              <table style={{ width: '100%', fontSize: '13px', borderCollapse: 'collapse', marginBottom: '16px' }}>
                <thead>
                  <tr style={{ backgroundColor: '#F0F0F0', textAlign: isFa ? 'right' : 'left' }}>
                    <th style={{ padding: '6px' }}>{isFa ? 'نام کالا' : 'Product'}</th>
                    <th style={{ padding: '6px' }}>{isFa ? 'SKU' : 'SKU'}</th>
                    <th style={{ padding: '6px' }}>{isFa ? 'تعداد' : 'Qty'}</th>
                    <th style={{ padding: '6px' }}>{isFa ? 'مبلغ' : 'Subtotal'}</th>
                  </tr>
                </thead>
                <tbody>
                  {parsedPayload.items.map((item: any, idx: number) => (
                    <tr key={idx} style={{ borderBottom: '1px solid #EEE' }}>
                      <td style={{ padding: '6px', fontWeight: 'bold' }}>{item.product_name}</td>
                      <td style={{ padding: '6px', fontFamily: 'monospace' }}>{item.sku}</td>
                      <td style={{ padding: '6px' }}>{item.quantity}</td>
                      <td style={{ padding: '6px', fontWeight: 'bold' }}>
                        {isFa ? `${item.subtotal.toLocaleString('fa-IR')} تومان` : `$${item.subtotal.toFixed(2)}`}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>

              {/* Order Totals */}
              <div style={{ backgroundColor: '#F5F5F5', padding: '12px', borderRadius: '8px', marginBottom: '16px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '4px' }}>
                  <span>{isFa ? 'جمع کل اقلام:' : 'Subtotal:'}</span>
                  <span>{isFa ? `${parsedPayload.subtotal.toLocaleString('fa-IR')} تومان` : `$${parsedPayload.subtotal.toFixed(2)}`}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', marginBottom: '4px' }}>
                  <span>{isFa ? 'مالیات (۸٪):' : 'Tax (8%):'}</span>
                  <span>{isFa ? `${parsedPayload.tax_amount.toLocaleString('fa-IR')} تومان` : `$${parsedPayload.tax_amount.toFixed(2)}`}</span>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '18px', fontWeight: 'bold', color: '#005AC1', borderTop: '1px solid #DDD', paddingTop: '8px', marginTop: '4px' }}>
                  <span>{isFa ? 'مبلغ قابل پرداخت:' : 'Grand Total:'}</span>
                  <span>{isFa ? `${parsedPayload.grand_total.toLocaleString('fa-IR')} تومان` : `$${parsedPayload.grand_total.toFixed(2)}`}</span>
                </div>
              </div>

              <button
                onClick={handleConfirmOrder}
                disabled={isCheckingOut || parsedPayload.items.length === 0}
                style={{ width: '100%', backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '8px', padding: '14px', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer' }}
              >
                {isCheckingOut ? (isFa ? 'در حال ثبت سفارش...' : 'Processing Order...') : (isFa ? '🛒 تایید نهایی و ثبت فاکتور فروش' : '🛒 Confirm & Checkout Sales Order')}
              </button>
            </div>
          )}
        </div>
      ) : (
        /* History Sub-tab */
        <div style={{ backgroundColor: '#FFF', borderRadius: '12px', padding: '20px', border: '1px solid #E0E0E0' }}>
          <h3 style={{ margin: '0 0 16px 0', fontSize: '16px', fontWeight: 'bold' }}>{isFa ? 'سابقه پیام‌های ورودی دریافتی' : 'Imported Messages Trail'}</h3>

          {importedMessages.length === 0 ? (
            <p style={{ color: '#888', textAlign: 'center', padding: '40px 0' }}>{isFa ? 'هیچ پیامی قبلاً ثبت نشده است.' : 'No imported messages logged yet.'}</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
              <thead>
                <tr style={{ backgroundColor: '#F5F5F5', textAlign: isFa ? 'right' : 'left' }}>
                  <th style={{ padding: '8px 12px' }}>{isFa ? 'منبع' : 'Source'}</th>
                  <th style={{ padding: '8px 12px' }}>{isFa ? 'متن پیام' : 'Raw Text'}</th>
                  <th style={{ padding: '8px 12px' }}>{isFa ? 'وضعیت' : 'Status'}</th>
                  <th style={{ padding: '8px 12px' }}>{isFa ? 'تاریخ' : 'Date'}</th>
                </tr>
              </thead>
              <tbody>
                {importedMessages.map((msg: any) => (
                  <tr key={msg.id} style={{ borderBottom: '1px solid #EEE' }}>
                    <td style={{ padding: '8px 12px', fontWeight: 'bold' }}>{msg.source}</td>
                    <td style={{ padding: '8px 12px', fontFamily: 'monospace', maxWidth: '300px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{msg.raw_text}</td>
                    <td style={{ padding: '8px 12px' }}>
                      <span style={{ padding: '2px 8px', borderRadius: '10px', backgroundColor: '#D1E7DD', color: '#0F5132', fontWeight: 'bold', fontSize: '11px' }}>
                        {msg.status}
                      </span>
                    </td>
                    <td style={{ padding: '8px 12px' }}>{msg.created_at || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
};
