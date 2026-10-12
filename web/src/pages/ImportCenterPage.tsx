import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { FormError, PrimaryButton, SecondaryButton } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { useTheme } from '../theme/ThemeContext';
import { ImportJobItem } from '../types';

interface ImportCenterPageProps {
  language?: 'fa' | 'en';
}

const IMPORT_TYPES = [
  { value: 'products', fa: 'کالاها و محصولات', en: 'Products & Catalog' },
  { value: 'customers', fa: 'مشتریان', en: 'Customers' },
  { value: 'inventory', fa: 'موجودی اولیه انبار', en: 'Initial Inventory Stock' },
  { value: 'expenses', fa: 'هزینه‌های جاری', en: 'Operating Expenses' },
  { value: 'orders', fa: 'سوابق سفارش‌ها', en: 'Historical Orders' },
  { value: 'suppliers', fa: 'تأمین‌کنندگان', en: 'Suppliers' },
  { value: 'purchases', fa: 'سفارش‌های خرید', en: 'Purchases' },
];

const STATUS_LABELS: Record<string, { fa: string; en: string; color: string; bg: string }> = {
  uploaded: { fa: 'بارگذاری شده', en: 'Uploaded', color: '#0369A1', bg: '#E0F2FE' },
  inspecting: { fa: 'در حال بررسی ساختار', en: 'Inspecting', color: '#D97706', bg: '#FEF3C7' },
  mapping_required: { fa: 'نیازمند تطبیق ستون‌ها', en: 'Mapping Required', color: '#B45309', bg: '#FDE68A' },
  ready_to_validate: { fa: 'آماده اعتبارسنجی', en: 'Ready to Validate', color: '#4F46E5', bg: '#EEF2FF' },
  validating: { fa: 'در حال اعتبارسنجی', en: 'Validating', color: '#7C3AED', bg: '#F5F3FF' },
  preview_ready: { fa: 'پیش‌نمایش آماده است', en: 'Preview Ready', color: '#059669', bg: '#D1FAE5' },
  ready_to_import: { fa: 'آماده درون‌ریزی', en: 'Ready to Import', color: '#0284C7', bg: '#E0F2FE' },
  importing: { fa: 'در حال انتقال داده‌ها', en: 'Importing', color: '#2563EB', bg: '#DBEAFE' },
  completed: { fa: 'تکمیل شده', en: 'Completed', color: '#16A34A', bg: '#DCFCE7' },
  completed_with_errors: { fa: 'تکمیل با خطا', en: 'Completed with Errors', color: '#EA580C', bg: '#FFEDD5' },
  failed: { fa: 'ناموفق', en: 'Failed', color: '#DC2626', bg: '#FEE2E2' },
  cancelled: { fa: 'لغو شده', en: 'Cancelled', color: '#6B7280', bg: '#F3F4F6' },
};

export const ImportCenterPage: React.FC<ImportCenterPageProps> = ({ language = 'fa' }) => {
  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  const [jobs, setJobs] = useState<ImportJobItem[]>([]);
  const [selectedJob, setSelectedJob] = useState<ImportJobItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [actionNotice, setActionNotice] = useState('');

  // Upload modal state
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [selectedType, setSelectedType] = useState('products');
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState('');

  const loadJobs = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage('');
    try {
      const res = await apiClient.getImportJobs(1);
      setJobs(res.data ?? []);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage(isFa ? 'خطا در بارگیری سوابق درون‌ریزی.' : 'Failed to load import jobs.');
      }
    } finally {
      setIsLoading(false);
    }
  }, [isFa]);

  useEffect(() => {
    void loadJobs();
  }, [loadJobs]);

  const handleUploadSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile) {
      setUploadError(isFa ? 'لطفاً یک فایل اکسل یا CSV انتخاب کنید.' : 'Please select an Excel or CSV file.');
      return;
    }

    setIsUploading(true);
    setUploadError('');

    try {
      const formData = new FormData();
      formData.append('file', selectedFile);
      formData.append('import_type', selectedType);

      const res = await apiClient.createImportJob(formData);
      setIsUploadModalOpen(false);
      setSelectedFile(null);
      setActionNotice(
        isFa
          ? `عملیات درون‌ریزی برای فایل "${res.job.original_filename}" با موفقیت ایجاد شد.`
          : `Import job for "${res.job.original_filename}" created successfully.`
      );
      await loadJobs();
    } catch (err) {
      if (err instanceof ApiError) {
        setUploadError(err.message);
      } else {
        setUploadError(isFa ? 'خطا در بارگذاری فایل.' : 'Failed to upload file.');
      }
    } finally {
      setIsUploading(false);
    }
  };

  const handleCancelJob = async (jobId: string) => {
    if (!window.confirm(isFa ? 'آیا از لغو این عملیات اطمینان دارید؟' : 'Are you sure you want to cancel this job?')) {
      return;
    }

    try {
      const res = await apiClient.cancelImportJob(jobId, isFa ? 'لغو توسط کاربر' : 'Cancelled by user');
      setActionNotice(res.message || (isFa ? 'عملیات لغو شد.' : 'Job cancelled.'));
      await loadJobs();
      if (selectedJob && selectedJob.id === jobId) {
        setSelectedJob(res.job);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage(isFa ? 'خطا در لغو عملیات.' : 'Failed to cancel job.');
      }
    }
  };

  const handleViewJob = async (jobId: string) => {
    try {
      const res = await apiClient.getImportJob(jobId);
      setSelectedJob(res.job);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      }
    }
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  return (
    <div
      style={{
        padding: '24px',
        maxWidth: '1200px',
        margin: '0 auto',
        fontFamily: theme.typography.fontFamily,
        direction: isFa ? 'rtl' : 'ltr',
        color: theme.colors.textPrimary,
      }}
    >
      <PageHeader
        title={isFa ? 'مرکز درون‌ریزی داده‌ها' : 'Import Center'}
        description={
          isFa
            ? 'مهاجرت و انتقال اطلاعات قبلی (کالاها، مشتریان، موجودی و سوابق مالی) از فایل‌های اکسل و CSV'
            : 'Migrate business data (products, customers, inventory, and finances) from Excel and CSV files'
        }
        actions={
          <PrimaryButton onClick={() => setIsUploadModalOpen(true)}>
            {isFa ? '+ درون‌ریزی فایل جدید' : '+ New Import'}
          </PrimaryButton>
        }
      />

      {errorMessage && <FormError message={errorMessage} />}

      {actionNotice && (
        <div
          style={{
            margin: '16px 0',
            padding: '12px 16px',
            borderRadius: '8px',
            backgroundColor: isDark ? '#064E3B' : '#ECFDF5',
            color: isDark ? '#A7F3D0' : '#065F46',
            border: `1px solid ${isDark ? '#047857' : '#A7F3D0'}`,
            fontSize: '14px',
          }}
        >
          {actionNotice}
        </div>
      )}

      {/* Main Content Area */}
      <div style={{ marginTop: '24px' }}>
        {isLoading ? (
          <div style={{ textAlign: 'center', padding: '48px', color: theme.colors.textSecondary }}>
            {isFa ? 'در حال بارگذاری اطلاعات...' : 'Loading import history...'}
          </div>
        ) : jobs.length === 0 ? (
          <EmptyState
            title={isFa ? 'هیچ فایل درون‌ریزی ثبت نشده است' : 'No import jobs recorded'}
            description={
              isFa
                ? 'می‌توانید اطلاعات کسب‌وکار خود را از طریق فایل اکسل (.xlsx) یا CSV به سامانه منتقل کنید.'
                : 'You can migrate your existing business records using an Excel (.xlsx) or CSV spreadsheet.'
            }
            actionText={isFa ? 'شروع اولین درون‌ریزی' : 'Start First Import'}
            onAction={() => setIsUploadModalOpen(true)}
          />
        ) : (
          <div
            style={{
              backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
              borderRadius: '12px',
              border: `1px solid ${theme.colors.border}`,
              overflow: 'hidden',
              boxShadow: isDark ? 'none' : '0 1px 3px rgba(0,0,0,0.05)',
            }}
          >
            <div
              style={{
                padding: '16px 20px',
                borderBottom: `1px solid ${theme.colors.border}`,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 600 }}>
                {isFa ? 'تاریخچه عملیات‌های مهاجرت داده' : 'Migration Job History'}
              </h3>
              <SecondaryButton onClick={loadJobs}>
                {isFa ? 'بروزرسانی لیست' : 'Refresh List'}
              </SecondaryButton>
            </div>

            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left' }}>
                <thead>
                  <tr
                    style={{
                      backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB',
                      borderBottom: `1px solid ${theme.colors.border}`,
                      fontSize: '13px',
                      color: theme.colors.textSecondary,
                    }}
                  >
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'نام فایل' : 'File Name'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'نوع داده' : 'Data Type'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'وضعیت' : 'Status'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'تعداد ردیف' : 'Rows'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'حجم فایل' : 'Size'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'تاریخ ثبت' : 'Created At'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'عملیات' : 'Actions'}</th>
                  </tr>
                </thead>
                <tbody>
                  {jobs.map((job) => {
                    const statusInfo = STATUS_LABELS[job.status] || {
                      fa: job.status,
                      en: job.status,
                      color: '#4B5563',
                      bg: '#E5E7EB',
                    };
                    const typeInfo = IMPORT_TYPES.find((t) => t.value === job.import_type);
                    const isCancellable = [
                      'uploaded',
                      'inspecting',
                      'mapping_required',
                      'ready_to_validate',
                      'validating',
                      'preview_ready',
                      'ready_to_import',
                    ].includes(job.status);

                    return (
                      <tr
                        key={job.id}
                        style={{
                          borderBottom: `1px solid ${theme.colors.border}`,
                          fontSize: '14px',
                        }}
                      >
                        <td style={{ padding: '12px 16px', fontWeight: 500 }}>
                          {job.original_filename}
                          <div style={{ fontSize: '11px', color: theme.colors.textSecondary }}>
                            {job.detected_format?.toUpperCase()}
                          </div>
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          {typeInfo ? (isFa ? typeInfo.fa : typeInfo.en) : job.import_type}
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <span
                            style={{
                              display: 'inline-block',
                              padding: '4px 10px',
                              borderRadius: '9999px',
                              fontSize: '12px',
                              fontWeight: 500,
                              color: statusInfo.color,
                              backgroundColor: statusInfo.bg,
                            }}
                          >
                            {isFa ? statusInfo.fa : statusInfo.en}
                          </span>
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          {job.total_rows > 0 ? job.total_rows.toLocaleString() : '—'}
                        </td>
                        <td style={{ padding: '12px 16px', color: theme.colors.textSecondary }}>
                          {formatFileSize(job.file_size)}
                        </td>
                        <td style={{ padding: '12px 16px', color: theme.colors.textSecondary }}>
                          {new Date(job.created_at).toLocaleDateString(isFa ? 'fa-IR' : 'en-US')}
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <div style={{ display: 'flex', gap: '8px' }}>
                            <button
                              onClick={() => handleViewJob(job.id)}
                              style={{
                                padding: '4px 8px',
                                fontSize: '12px',
                                borderRadius: '6px',
                                border: `1px solid ${theme.colors.border}`,
                                backgroundColor: isDark ? theme.colors.surfaceElevated : '#FFFFFF',
                                color: theme.colors.textPrimary,
                                cursor: 'pointer',
                              }}
                            >
                              {isFa ? 'جزئیات' : 'Details'}
                            </button>
                            {isCancellable && (
                              <button
                                onClick={() => handleCancelJob(job.id)}
                                style={{
                                  padding: '4px 8px',
                                  fontSize: '12px',
                                  borderRadius: '6px',
                                  border: '1px solid #FCA5A5',
                                  backgroundColor: isDark ? '#7F1D1D' : '#FEF2F2',
                                  color: '#DC2626',
                                  cursor: 'pointer',
                                }}
                              >
                                {isFa ? 'لغو' : 'Cancel'}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </div>

      {/* Job Details Modal / Drawer */}
      {selectedJob && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '16px',
          }}
          onClick={() => setSelectedJob(null)}
        >
          <div
            style={{
              backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
              borderRadius: '16px',
              padding: '24px',
              maxWidth: '650px',
              width: '100%',
              maxHeight: '90vh',
              overflowY: 'auto',
              border: `1px solid ${theme.colors.border}`,
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 600 }}>
                {isFa ? 'جزئیات عملیات درون‌ریزی' : 'Import Job Details'}
              </h3>
              <button
                onClick={() => setSelectedJob(null)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '20px',
                  cursor: 'pointer',
                  color: theme.colors.textSecondary,
                }}
              >
                ✕
              </button>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '20px' }}>
              <div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'نام فایل' : 'Filename'}</div>
                <div style={{ fontWeight: 500 }}>{selectedJob.original_filename}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'وضعیت جاری' : 'Current Status'}</div>
                <div>{STATUS_LABELS[selectedJob.status]?.[isFa ? 'fa' : 'en'] || selectedJob.status}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'نوع محتوا' : 'Content Type'}</div>
                <div>{IMPORT_TYPES.find((t) => t.value === selectedJob.import_type)?.[isFa ? 'fa' : 'en'] || selectedJob.import_type}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'تعداد کل ردیف‌ها' : 'Total Rows'}</div>
                <div style={{ fontWeight: 600 }}>{selectedJob.total_rows.toLocaleString()}</div>
              </div>
            </div>

            {selectedJob.error_summary && (
              <div
                style={{
                  padding: '12px',
                  backgroundColor: isDark ? '#7F1D1D' : '#FEF2F2',
                  color: '#DC2626',
                  borderRadius: '8px',
                  fontSize: '13px',
                  marginBottom: '16px',
                }}
              >
                <strong>{isFa ? 'گزارش خطا:' : 'Error Summary:'}</strong> {selectedJob.error_summary}
              </div>
            )}

            {/* Datasets / Worksheets if discovered */}
            {selectedJob.datasets && selectedJob.datasets.length > 0 && (
              <div style={{ marginTop: '16px' }}>
                <h4 style={{ fontSize: '14px', fontWeight: 600, marginBottom: '8px' }}>
                  {isFa ? 'کاربرگ‌ها و جداول شناسایی‌شده' : 'Discovered Worksheets'}
                </h4>
                {selectedJob.datasets.map((ds) => (
                  <div
                    key={ds.id}
                    style={{
                      padding: '12px',
                      borderRadius: '8px',
                      backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB',
                      marginBottom: '8px',
                      border: `1px solid ${theme.colors.border}`,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <strong>{ds.name}</strong>
                      <span style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                        {ds.row_count} {isFa ? 'ردیف' : 'rows'}
                      </span>
                    </div>
                    {ds.headers && ds.headers.length > 0 && (
                      <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                        <strong>{isFa ? 'سرستون‌ها: ' : 'Headers: '}</strong>
                        {ds.headers.join(' | ')}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <SecondaryButton onClick={() => setSelectedJob(null)}>
                {isFa ? 'بستن' : 'Close'}
              </SecondaryButton>
            </div>
          </div>
        </div>
      )}

      {/* Upload File Modal */}
      {isUploadModalOpen && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '16px',
          }}
          onClick={() => !isUploading && setIsUploadModalOpen(false)}
        >
          <div
            style={{
              backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
              borderRadius: '16px',
              padding: '24px',
              maxWidth: '520px',
              width: '100%',
              border: `1px solid ${theme.colors.border}`,
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ margin: '0 0 16px 0', fontSize: '18px', fontWeight: 600 }}>
              {isFa ? 'درون‌ریزی فایل جدید' : 'Import New Data File'}
            </h3>

            {uploadError && <FormError message={uploadError} />}

            <form onSubmit={handleUploadSubmit}>
              <div style={{ marginBottom: '16px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, marginBottom: '6px' }}>
                  {isFa ? 'نوع داده‌ها برای مهاجرت' : 'Select Target Domain'}
                </label>
                <select
                  value={selectedType}
                  onChange={(e) => setSelectedType(e.target.value)}
                  disabled={isUploading}
                  style={{
                    width: '100%',
                    padding: '10px 12px',
                    borderRadius: '8px',
                    border: `1px solid ${theme.colors.border}`,
                    backgroundColor: isDark ? theme.colors.surfaceElevated : '#FFFFFF',
                    color: theme.colors.textPrimary,
                    fontSize: '14px',
                  }}
                >
                  {IMPORT_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {isFa ? t.fa : t.en}
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ marginBottom: '20px' }}>
                <label style={{ display: 'block', fontSize: '13px', fontWeight: 500, marginBottom: '6px' }}>
                  {isFa ? 'انتخاب فایل (.xlsx یا .csv)' : 'Select File (.xlsx or .csv)'}
                </label>
                <input
                  type="file"
                  accept=".xlsx,.csv"
                  disabled={isUploading}
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) setSelectedFile(f);
                  }}
                  style={{
                    width: '100%',
                    padding: '10px',
                    borderRadius: '8px',
                    border: `1px dashed ${theme.colors.border}`,
                    backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB',
                    cursor: 'pointer',
                  }}
                />
                <div style={{ fontSize: '11px', color: theme.colors.textSecondary, marginTop: '4px' }}>
                  {isFa
                    ? 'حداکثر حجم مجاز: ۲۰ مگابایت. فایل‌های ماکرو (.xlsm) پذیرفته نمی‌شوند.'
                    : 'Maximum size: 20MB. Macro-enabled workbooks (.xlsm) are rejected.'}
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                <SecondaryButton
                  onClick={() => setIsUploadModalOpen(false)}
                  disabled={isUploading}
                >
                  {isFa ? 'انصراف' : 'Cancel'}
                </SecondaryButton>
                <PrimaryButton type="submit" disabled={isUploading || !selectedFile}>
                  {isUploading
                    ? (isFa ? 'در حال ارسال و ثبت...' : 'Uploading...')
                    : (isFa ? 'بارگذاری و شروع بررسی' : 'Upload & Start Inspection')}
                </PrimaryButton>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ImportCenterPage;
