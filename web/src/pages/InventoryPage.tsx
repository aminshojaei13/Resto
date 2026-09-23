import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { Product } from '../types';

interface InventoryPageProps {
  language?: 'fa' | 'en';
}

export const InventoryPage: React.FC<InventoryPageProps> = ({ language = 'fa' }) => {
  const [products, setProducts] = useState<Product[]>([]);
  const isFa = language === 'fa';

  useEffect(() => {
    apiClient.getProducts().then(setProducts);
  }, []);

  const handleAdjust = async (productId: string) => {
    await apiClient.adjustStock(productId, 'wh_apex_1a', 5, 'Web Admin Restock');
    alert(isFa ? 'موجودی انبار با موفقیت اصلاح شد (+۵ عدد)' : 'Stock updated successfully (+5 units)');
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
              <td style={{ padding: '12px 16px' }}>
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
    </div>
  );
};
