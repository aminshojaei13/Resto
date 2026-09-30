import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/apiClient';
import { useTheme } from '../../theme/ThemeContext';

interface PlatformAuditPageProps {
  language?: 'fa' | 'en';
}

export const PlatformAuditPage: React.FC<PlatformAuditPageProps> = ({ language = 'fa' }) => {
  const [logs, setLogs] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  const isFa = language === 'fa';
  const { theme } = useTheme();

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
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
          {isFa ? 'دفتر ثبت رویدادهای راهبری (Platform Admin Audit Trail)' : 'Platform Operational Audit Trail'}
        </h2>
        <p style={{ margin: 0, color: theme.colors.textSecondary, fontSize: '14px' }}>
          {isFa ? 'گزارش غیرقابل تغییر از تمامی اکشن‌های راهبران، ثبت سازمان‌ها و تغییرات سستمی' : 'Immutable audit stream tracking platform administrative actions and security events'}
        </p>
      </div>

      <div style={{ backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}`, overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری رویدادها...' : 'Loading audit trail...'}</div>
        ) : logs.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: theme.colors.textMuted }}>{isFa ? 'هیچ رویدادی ثبت نشده است.' : 'No audit records found.'}</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
            <thead>
              <tr style={{ backgroundColor: theme.colors.backgroundSecondary, textAlign: isFa ? 'right' : 'left', color: theme.colors.textSecondary, fontSize: '12px', fontWeight: 700 }}>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'اکشن' : 'Action'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نوع موجودیت' : 'Entity Type'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'شناسه موجودیت' : 'Entity ID'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'جزئیات' : 'Details'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'تاریخ' : 'Timestamp'}</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold', color: theme.colors.info }}>{log.action}</td>
                  <td style={{ padding: '12px 16px' }}>{log.entity_type || log.entityType}</td>
                  <td style={{ padding: '12px 16px', fontFamily: 'monospace' }}>{log.entity_id || log.entityId}</td>
                  <td style={{ padding: '12px 16px' }}>{log.details}</td>
                  <td style={{ padding: '12px 16px', color: theme.colors.textSecondary }}>{log.created_at || log.createdAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};
