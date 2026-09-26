import React, { useState, useEffect } from 'react';
import { apiClient } from '../api/apiClient';

interface PlatformAdminPageProps {
  language?: 'fa' | 'en';
}

export const PlatformAdminPage: React.FC<PlatformAdminPageProps> = ({ language = 'fa' }) => {
  const [applications, setApplications] = useState<any[]>([]);
  const [selectedStatus, setSelectedStatus] = useState<string>('PENDING');
  const [selectedApp, setSelectedApp] = useState<any | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');
  const [isRejectModalOpen, setIsRejectModalOpen] = useState(false);

  const [isLoading, setIsLoading] = useState(false);
  const [actionMessage, setActionMessage] = useState('');

  const isFa = language === 'fa';

  useEffect(() => {
    loadApplications();
  }, [selectedStatus]);

  const loadApplications = async () => {
    setIsLoading(true);
    try {
      const list = await apiClient.getPlatformApplications(selectedStatus || undefined);
      setApplications(list);
      setIsLoading(false);
    } catch {
      setIsLoading(false);
    }
  };

  const handleApprove = async (id: string) => {
    if (!window.confirm(isFa ? 'آیا از تایید این درخواست و راه‌اندازی اتوماتیک سازمان/تننت مطمئن هستید؟' : 'Approve application and provision tenant?')) return;

    try {
      const res = await apiClient.approvePlatformApplication(id);
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
    <div style={{ padding: '24px', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ margin: '0 0 6px 0', fontSize: '22px', fontWeight: 'bold' }}>
          {isFa ? 'مدیریت درخواست‌های ثبت سازمان (Platform Admin Dashboard)' : 'Platform Admin Business Applications'}
        </h2>
        <p style={{ margin: 0, color: '#666', fontSize: '14px' }}>
          {isFa ? 'بررسی درخواست‌های آنلاین کسب‌وکارها، تایید و ایجاد اتوماتیک تننت، کاربر مالک، انبار و فروشگاه اولیه' : 'Review incoming business applications, approve transactional tenant provisioning, or reject'}
        </p>
      </div>

      {actionMessage && (
        <div style={{ backgroundColor: '#D1E7DD', color: '#0F5132', padding: '12px 16px', borderRadius: '8px', marginBottom: '20px', fontSize: '14px', fontWeight: 'bold' }}>
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
              backgroundColor: selectedStatus === status ? '#005AC1' : '#E0E0E0',
              color: selectedStatus === status ? '#FFF' : '#333',
            }}
          >
            {status === 'PENDING' ? (isFa ? 'در حال بررسی (PENDING)' : 'Pending') : status === 'APPROVED' ? (isFa ? 'تاییدشده (APPROVED)' : 'Approved') : (isFa ? 'ردشده (REJECTED)' : 'Rejected')}
          </button>
        ))}
      </div>

      {/* Applications Table */}
      <div style={{ backgroundColor: '#FFF', borderRadius: '12px', border: '1px solid #E0E0E0', overflow: 'hidden' }}>
        {isLoading ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>{isFa ? 'در حال بارگذاری درخواست‌ها...' : 'Loading applications...'}</div>
        ) : applications.length === 0 ? (
          <div style={{ padding: '40px', textAlign: 'center', color: '#888' }}>{isFa ? 'هیچ درخواستی در این وضعیت یافت نشد.' : 'No applications found.'}</div>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '14px' }}>
            <thead>
              <tr style={{ backgroundColor: '#F5F5F5', textAlign: isFa ? 'right' : 'left' }}>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نام کسب‌وکار' : 'Business Name'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'مالک' : 'Owner Name'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'ایمیل' : 'Email'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'تلفن' : 'Phone'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'نوع کسب‌وکار' : 'Type'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'وضعیت' : 'Status'}</th>
                <th style={{ padding: '12px 16px' }}>{isFa ? 'عملیات' : 'Actions'}</th>
              </tr>
            </thead>
            <tbody>
              {applications.map((app) => (
                <tr key={app.id} style={{ borderBottom: '1px solid #EEE' }}>
                  <td style={{ padding: '12px 16px', fontWeight: 'bold' }}>{app.business_name}</td>
                  <td style={{ padding: '12px 16px' }}>{app.owner_name}</td>
                  <td style={{ padding: '12px 16px' }}>{app.email}</td>
                  <td style={{ padding: '12px 16px' }}>{app.phone || '-'}</td>
                  <td style={{ padding: '12px 16px' }}>{app.business_type}</td>
                  <td style={{ padding: '12px 16px' }}>
                    <span
                      style={{
                        padding: '4px 10px',
                        borderRadius: '12px',
                        fontSize: '12px',
                        fontWeight: 'bold',
                        backgroundColor: app.status === 'APPROVED' ? '#D1E7DD' : app.status === 'REJECTED' ? '#F8D7DA' : '#FFF3CD',
                        color: app.status === 'APPROVED' ? '#0F5132' : app.status === 'REJECTED' ? '#842029' : '#664D03',
                      }}
                    >
                      {app.status}
                    </span>
                  </td>
                  <td style={{ padding: '12px 16px', display: 'flex', gap: '8px' }}>
                    {app.status === 'PENDING' && (
                      <>
                        <button
                          onClick={() => handleApprove(app.id)}
                          style={{ backgroundColor: '#198754', color: '#FFF', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
                        >
                          {isFa ? '✅ تایید و راه‌اندازی' : 'Approve'}
                        </button>
                        <button
                          onClick={() => {
                            setSelectedApp(app);
                            setIsRejectModalOpen(true);
                          }}
                          style={{ backgroundColor: '#DC3545', color: '#FFF', border: 'none', borderRadius: '6px', padding: '6px 12px', fontWeight: 'bold', cursor: 'pointer', fontSize: '12px' }}
                        >
                          {isFa ? '❌ رد درخواست' : 'Reject'}
                        </button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Reject Modal */}
      {isRejectModalOpen && selectedApp && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ backgroundColor: '#FFF', padding: '24px', borderRadius: '12px', width: '480px', maxWidth: '90%' }}>
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 'bold' }}>{isFa ? 'رد درخواست ثبت‌نام سازمان' : 'Reject Application'}</h3>
            <p style={{ fontSize: '14px', color: '#666', marginBottom: '16px' }}>
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
                style={{ width: '100%', padding: '10px', borderRadius: '8px', border: '1px solid #CCC', fontSize: '14px', boxSizing: 'border-box', marginBottom: '16px' }}
              />

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button
                  type="button"
                  onClick={() => setIsRejectModalOpen(false)}
                  style={{ backgroundColor: '#E0E0E0', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </button>
                <button
                  type="submit"
                  style={{ backgroundColor: '#DC3545', color: '#FFF', border: 'none', borderRadius: '6px', padding: '8px 16px', fontWeight: 'bold', cursor: 'pointer' }}
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
