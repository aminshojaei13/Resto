import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';
import { useTheme } from '../theme/ThemeContext';

interface PlatformAdminPageProps {
  language?: 'fa' | 'en';
  selectedApplicationId?: string;
  navigate?: (path: string) => void;
}

export const PlatformAdminPage: React.FC<PlatformAdminPageProps> = ({ language = 'fa', selectedApplicationId, navigate }) => {
  const [applications, setApplications] = useState<any[]>([]);
  const [selectedStatus, setSelectedStatus] = useState<string>('PENDING');
  const [selectedApp, setSelectedApp] = useState<any | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);

  const [isApproveModalOpen, setIsApproveModalOpen] = useState(false);


  const [isLoading, setIsLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState('');

  const isFa = language === 'fa';
  const { theme } = useTheme();

  useEffect(() => {
    loadApplications();
  }, [selectedStatus]);

  const loadApplications = async () => {
    setIsLoading(true);
    try {
      const list = await apiClient.getPlatformApplications(selectedStatus || undefined);
      setApplications(list || []);
      setIsLoading(false);
    } catch (err) {
      console.error('loadApplications error:', err);
      setApplications([]);
      setIsLoading(false);
    }
  };

  const openApproveModal = (app: any) => {
    setSelectedApp(app);
    setIsApproveModalOpen(true);
  };

  const handleApproveSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedApp) return;

    try {
      // No password is chosen here. The owner receives an invitation and sets
      // their own, so nobody else ever knows their credentials.
      const res = await apiClient.approvePlatformApplication(selectedApp.id);
      setIsApproveModalOpen(false);
      setActionMessage(isFa ? `سازمان "${res.organization?.name || 'جدید'}" با موفقیت ایجاد گردید!` : 'Tenant provisioned successfully!');
      loadApplications();
      setSelectedApp(null);
    } catch (err: any) {
      alert(err.message || 'Failed to approve application');
    }
  };

  const handleRejectSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedApp || !rejectionReason.trim()) return;

    try {
      await apiClient.rejectPlatformApplication(selectedApp.id, rejectionReason);
      setIsRejectModalOpen(false);
      setRejectionReason('');
      setActionMessage(isFa ? 'درخواست رد گردید.' : 'Application rejected.');
      loadApplications();
      setSelectedApp(null);
    } catch (err: any) {
      alert(err.message || 'Failed to reject application');
    }
  };

  return (
    <div style={{ padding: '24px', fontFamily: theme.typography.fontFamily, color: theme.colors.textPrimary }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold', color: theme.colors.textPrimary }}>
          {isFa ? 'مدیریت درخواست‌های ثبت سازمان (Platform Admin Dashboard)' : 'Platform Admin Business Applications'}
        </h2>
        <p style={{ margin: 0, color: theme.colors.textSecondary, fontSize: '14px' }}>
          {isFa
            ? 'بررسی درخواست‌های ثبت‌شده، تأیید یا رد آن‌ها، و راه‌اندازی خودکار کسب‌وکار'
            : 'Review registered business applications, approve or reject them, and set up the business automatically'}
        </p>
      </div>

      {actionMessage && (
        <div style={{ backgroundColor: theme.colors.successLight, color: theme.colors.success, padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontWeight: 'bold' }}>
          {actionMessage}
        </div>
      )}

      {/* Filter Tabs */}
      <div style={{ display: 'flex', gap: '12px', marginBottom: '20px' }}>
        {['PENDING', 'APPROVED', 'REJECTED'].map((status) => (
          <button
            key={status}
            onClick={() => setSelectedStatus(status)}
            style={{
              padding: '10px 18px',
              borderRadius: '20px',
              border: 'none',
              fontWeight: 'bold',
              fontSize: '13px',
              cursor: 'pointer',
              backgroundColor: selectedStatus === status ? theme.colors.info : theme.colors.surfaceHover,
              color: selectedStatus === status ? '#FFFFFF' : theme.colors.textSecondary,
            }}
          >
            {status === 'PENDING' ? (isFa ? 'در حال بررسی (PENDING)' : 'Pending') : status === 'APPROVED' ? (isFa ? 'تاییدشده (APPROVED)' : 'Approved') : (isFa ? 'ردشده (REJECTED)' : 'Rejected')}
          </button>
        ))}
      </div>

      {/* Applications Table */}
      <div style={{ backgroundColor: theme.colors.surface, borderRadius: '12px', border: `1px solid ${theme.colors.border}`, overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: theme.colors.textSecondary }}>{isFa ? 'در حال بارگذاری درخواست‌ها...' : 'Loading applications...'}</div>
        ) : applications.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: theme.colors.textMuted }}>{isFa ? 'هیچ درخواستی در این وضعیت یافت نشد.' : 'No applications found.'}</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: theme.colors.backgroundSecondary, textAlign: isFa ? 'right' : 'left', color: theme.colors.textSecondary, fontSize: '12px', fontWeight: 700 }}>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نام کسب‌وکار' : 'Business Name'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'مالک' : 'Owner Name'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'ایمیل ورود' : 'Email'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'تلفن' : 'Phone'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'وضعیت' : 'Status'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'عملیات' : 'Actions'}</th>
              </tr>
            </thead>
            <tbody>
              {applications.map((app) => {
                return (
                  <tr key={app.id} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                    <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{app.business_name}</td>
                    <td style={{ padding: '12px 16px' }}>{app.owner_name}</td>
                    <td style={{ padding: '12px 16px', fontFamily: 'monospace' }}>{app.email}</td>
                    <td style={{ padding: '12px 16px' }}>{app.phone || '-'}</td>
                    <td style={{ padding: '12px 16px' }}>
                      <span
                        style={{
                          padding: '4px 10px',
                          borderRadius: '12px',
                          fontSize: '12px',
                          fontWeight: 'bold',
                          backgroundColor: app.status === 'APPROVED' ? theme.colors.successLight : app.status === 'REJECTED' ? theme.colors.errorLight : theme.colors.warningLight,
                          color: app.status === 'APPROVED' ? theme.colors.success : app.status === 'REJECTED' ? theme.colors.error : theme.colors.warning,
                        }}
                      >
                        {app.status}
                      </span>
                    </td>
                    <td style={{ padding: '12px 16px', display: 'flex', gap: '8px' }}>
                      {app.status === 'PENDING' && (
                        <>
                          <button
                            onClick={() => openApproveModal(app)}
                            style={{ backgroundColor: theme.colors.success, color: '#0A2E1E', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
                          >
                            {isFa ? '✅ تایید و راه‌اندازی' : 'Approve'}
                          </button>
                          <button
                            onClick={() => {
                              setSelectedApp(app);
                              setIsRejectModalOpen(true);
                            }}
                            style={{ backgroundColor: theme.colors.error, color: '#FFFFFF', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
                          >
                            {isFa ? '❌ رد درخواست' : 'Reject'}
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      {/* Approve & Password Provisioning Modal */}
      {isApproveModalOpen && selectedApp && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, padding: '24px', borderRadius: '12px', width: '480px', maxWidth: '90%', color: theme.colors.textPrimary, border: `1px solid ${theme.colors.border}` }}>
            <h3 style={{ margin: '0 0 12px 0', fontSize: '18px', fontWeight: 'bold' }}>
              {isFa ? 'تایید درخواست و راه‌اندازی کسب‌وکار' : 'Approve and set up the business'}
            </h3>
            <p style={{ fontSize: '14px', color: theme.colors.textSecondary, marginBottom: '16px' }}>
              {isFa
                ? `کسب‌وکار «${selectedApp.business_name}» ساخته می‌شود و لینک فعال‌سازی برای ${selectedApp.email} ارسال می‌گردد. مالک خودش رمز عبورش را تعیین می‌کند.`
                : `The business “${selectedApp.business_name}” will be created and an activation link emailed to ${selectedApp.email}. The owner chooses their own password.`}
            </p>

            <form onSubmit={handleApproveSubmit}>
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button
                  type="button"
                  onClick={() => setIsApproveModalOpen(false)}
                  style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  style={{ backgroundColor: theme.colors.success, color: '#0A2E1E', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  🚀 {isFa ? 'تایید و ایجاد حساب' : 'Confirm & Provision'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reject Modal */}
      {isRejectModalOpen && selectedApp && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: theme.colors.overlay, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: theme.colors.surfaceElevated, padding: '24px', borderRadius: '12px', width: '480px', maxWidth: '90%', color: theme.colors.textPrimary, border: `1px solid ${theme.colors.border}` }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>{isFa ? 'رد درخواست ثبت‌نام سازمان' : 'Reject Application'}</h3>
            <p style={{ fontSize: '14px', color: theme.colors.textSecondary, marginBottom: '16px' }}>
              {isFa ? `درخواست کسب‌وکار "${selectedApp.business_name}" رد خواهد شد.` : `Application for "${selectedApp.business_name}" will be marked as REJECTED.`}
            </p>

            <form onSubmit={handleRejectSubmit}>
              <label style={{ display: 'block', fontWeight: 'bold', fontSize: '13px', marginBottom: '6px' }}>{isFa ? 'دلیل رد درخواست:' : 'Rejection Reason:'} *</label>
              <textarea
                required
                rows={4}
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder={isFa ? 'دلیل عدم تایید درخواست را وارد کنید...' : 'Enter rejection reason...'}
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: `1px solid ${theme.colors.borderStrong}`, backgroundColor: theme.colors.surface, color: theme.colors.textPrimary, fontSize: '14px', boxSizing: 'border-box', marginBottom: '16px' }}
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button
                  type="button"
                  onClick={() => setIsRejectModalOpen(false)}
                  style={{ backgroundColor: theme.colors.surfaceHover, color: theme.colors.textPrimary, border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  style={{ backgroundColor: theme.colors.error, color: '#FFFFFF', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'ثبت رد درخواست' : 'Confirm Rejection'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
