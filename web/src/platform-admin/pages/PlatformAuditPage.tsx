import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/apiClient';

interface PlatformAuditPageProps {
  language?: 'fa' | 'en';
}

export const PlatformAuditPage: React.FC<PlatformAuditPageProps> = ({ language = 'fa' }) => {
  const [logs, setLogs] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const isFa = language === 'fa';

  useEffect(() => {
    loadAuditLogs();
  }, []);

  const loadAuditLogs = async () => {
    setIsLoading(true);
    try {
      const list = await apiClient.getPlatformAuditLogs();
      setLogs(list || []);
      setIsLoading(false);
    } catch {
      setLogs([]);
      setIsLoading(false);
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold' }}>
          {isFa ? 'دفتر ثبت رویدادهای راهبری (Platform Audit Trail)' : 'Platform Operational Audit Trail'}
        </h2>
        <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>
          {isFa ? 'گزارش غیرقابل تغییر از تمامی اکشن‌های راهبران، ثبت سازمان‌ها و تغییرات سستمی' : 'Immutable audit stream tracking platform administrative actions and security events'}
        </p>
      </div>

      <div style={{ backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E0E0E0', overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>{isFa ? 'در حال بارگذاری رویدادها...' : 'Loading audit trail...'}</div>
        ) : logs.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>{isFa ? 'هیچ رویدادی ثبت نشده است.' : 'No audit records found.'}</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: '#F5F5F5', textAlign: isFa ? 'right' : 'left' }}>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'اکشن' : 'Action'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نوع موجودیت' : 'Entity Type'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'شناسه موجودیت' : 'Entity ID'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'جزئیات' : 'Details'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'تاریخ' : 'Timestamp'}</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} style={{ borderBottom: '1px solid #EEE' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold', color: '#005AC1' }}>{log.action}</td>
                  <td style={{ padding: '12px 16px' }}>{log.entity_type || log.entityType}</td>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace' }}>{log.entity_id || log.entityId}</td>
                  <td style={{ padding: '12px 16px' }}>{log.details}</td>
                  <td style={{ padding: '12px 16px', color: '#666' }}>{log.created_at || log.createdAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
