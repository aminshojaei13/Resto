import React, { useCallback, useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { FormError, PrimaryButton, SecondaryButton } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { useTheme } from '../theme/ThemeContext';
import {
  ImportJobItem,
  ImportMappingResponse,
  ImportPreviewResponse,
  ImportValidationResultItem,
} from '../types';

interface ImportCenterPageProps {
  language?: 'fa' | 'en';
}

const IMPORT_TYPES = [
  { value: 'products', fa: 'کالاها و محصولات', en: 'Products & Catalog' },
  { value: 'customers', fa: 'مشتریان', en: 'Customers' },
  { value: 'inventory', fa: 'موجودی اولیه انبار', en: 'Opening Inventory Stock' },
  { value: 'expenses', fa: 'هزینه‌های جاری', en: 'Operating Expenses' },
  { value: 'orders', fa: 'سوابق سفارش‌ها (تاریخی)', en: 'Historical Orders' },
  { value: 'suppliers', fa: 'تأمین‌کنندگان (فازهای بعد)', en: 'Suppliers (Future Phase)' },
  { value: 'purchases', fa: 'سفارش‌های خرید (فازهای بعد)', en: 'Purchases (Future Phase)' },
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

  // Wizard Modal State
  const [wizardJobId, setWizardJobId] = useState<string | null>(null);
  const [wizardStep, setWizardStep] = useState<'mapping' | 'preview' | 'executing' | 'result'>('mapping');
  const [mappingData, setMappingData] = useState<ImportMappingResponse | null>(null);
  const [userFieldMappings, setUserFieldMappings] = useState<Record<string, string>>({});
  const [selectedSheetIndex, setSelectedSheetIndex] = useState(0);
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<string>('');
  const [selectedStoreId, setSelectedStoreId] = useState<string>('');
  const [previewData, setPreviewData] = useState<ImportPreviewResponse | null>(null);
  const [duplicateStrategy, setDuplicateStrategy] = useState<'skip' | 'update'>('skip');
  const [isWizardLoading, setIsWizardLoading] = useState(false);
  const [wizardError, setWizardError] = useState('');
  const [jobErrorsList, setJobErrorsList] = useState<ImportValidationResultItem[]>([]);
  const [executionResult, setExecutionResult] = useState<any>(null);

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
      // Automatically open wizard if in mapping_required or uploaded
      openWizard(res.job.id);
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
    if (!window.confirm(isFa ? 'آیا از لغو این عملیات مطمئن هستید؟' : 'Are you sure you want to cancel this job?')) {
      return;
    }

    try {
      await apiClient.cancelImportJob(jobId);
      setActionNotice(isFa ? 'عملیات درون‌ریزی لغو شد.' : 'Import job was cancelled.');
      await loadJobs();
    } catch (err) {
      if (err instanceof ApiError) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage(isFa ? 'خطا در لغو عملیات.' : 'Failed to cancel job.');
      }
    }
  };

  const openWizard = async (jobId: string) => {
    setWizardJobId(jobId);
    setWizardError('');
    setIsWizardLoading(true);

    try {
      const jobDetails = await apiClient.getImportJob(jobId);
      const currentStatus = jobDetails.job.status;

      if (['preview_ready', 'ready_to_import', 'completed', 'completed_with_errors'].includes(currentStatus)) {
        // Load preview directly
        const prev = await apiClient.getImportPreview(jobId);
        setPreviewData(prev);
        setWizardStep('preview');
      } else {
        // Load mapping step
        const mapRes = await apiClient.getImportMapping(jobId, 0);
        setMappingData(mapRes);
        setSelectedSheetIndex(mapRes.active_sheet_index);

        // Populate initial field mappings from suggested mappings or saved mapping
        const initialMap: Record<string, string> = {};
        if (mapRes.current_mapping && mapRes.current_mapping.fields) {
          Object.assign(initialMap, mapRes.current_mapping.fields);
        } else {
          mapRes.suggested_mappings.forEach((sug) => {
            if (sug.target_field) {
              initialMap[sug.target_field] = sug.source_column;
            }
          });
        }
        setUserFieldMappings(initialMap);

        if (mapRes.current_mapping && mapRes.current_mapping.warehouse_id) {
          setSelectedWarehouseId(mapRes.current_mapping.warehouse_id);
        } else if (mapRes.warehouses && mapRes.warehouses.length > 0) {
          setSelectedWarehouseId(mapRes.warehouses[0].id);
        } else {
          setSelectedWarehouseId('');
        }

        if (mapRes.current_mapping && mapRes.current_mapping.store_id) {
          setSelectedStoreId(mapRes.current_mapping.store_id);
        } else if (mapRes.stores && mapRes.stores.length > 0) {
          setSelectedStoreId(mapRes.stores[0].id);
        } else {
          setSelectedStoreId('');
        }

        setWizardStep('mapping');
      }
    } catch (err: any) {
      setWizardError(err.message || 'Error loading wizard');
    } finally {
      setIsWizardLoading(false);
    }
  };

  const handleSheetChange = async (newIndex: number) => {
    if (!wizardJobId) return;
    setIsWizardLoading(true);
    setSelectedSheetIndex(newIndex);
    try {
      const mapRes = await apiClient.getImportMapping(wizardJobId, newIndex);
      setMappingData(mapRes);
      const newMap: Record<string, string> = {};
      mapRes.suggested_mappings.forEach((sug) => {
        if (sug.target_field) {
          newMap[sug.target_field] = sug.source_column;
        }
      });
      setUserFieldMappings(newMap);
    } catch (err: any) {
      setWizardError(err.message);
    } finally {
      setIsWizardLoading(false);
    }
  };

  const handleSaveMappingAndValidate = async () => {
    if (!wizardJobId) return;
    setIsWizardLoading(true);
    setWizardError('');

    try {
      // 1. Save mapping
      await apiClient.saveImportMapping(wizardJobId, userFieldMappings, selectedSheetIndex, selectedWarehouseId, selectedStoreId);

      // 2. Run validation
      await apiClient.validateImportJob(wizardJobId);

      // 3. Load preview
      const prev = await apiClient.getImportPreview(wizardJobId);
      setPreviewData(prev);

      // 4. Load any validation errors
      const errs = await apiClient.getImportJobErrors(wizardJobId, 1);
      setJobErrorsList(errs.data ?? []);

      setWizardStep('preview');
      await loadJobs();
    } catch (err: any) {
      setWizardError(err.message || (isFa ? 'خطا در اعتبارسنجی فایل.' : 'Validation failed.'));
    } finally {
      setIsWizardLoading(false);
    }
  };

  const handleExecuteImport = async () => {
    if (!wizardJobId) return;
    setIsWizardLoading(true);
    setWizardError('');

    try {
      const res = await apiClient.executeImportJob(wizardJobId, duplicateStrategy);
      setExecutionResult(res.result);
      setWizardStep('result');
      await loadJobs();
    } catch (err: any) {
      setWizardError(err.message || (isFa ? 'خطا در اجرای درون‌ریزی.' : 'Import execution failed.'));
    } finally {
      setIsWizardLoading(false);
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
            ? 'مهاجرت مستقیم و تطبیق ستون‌های فایل‌های اکسل و CSV برای کالاها و مشتریان با اعتبارسنجی دقیق و حفظ یکپارچگی'
            : 'Migrate products and customers from Excel and CSV files with deterministic column mapping, row-by-row validation, and auditability'
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
                ? 'می‌توانید کاتالوگ محصولات یا لیست مشتریان خود را از طریق فایل اکسل (.xlsx) یا CSV به سامانه منتقل کنید.'
                : 'You can migrate your products or customers using an Excel (.xlsx) or CSV spreadsheet.'
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
                {isFa ? 'سوابق و وضعیت عملیات‌های درون‌ریزی' : 'Import Migration Records'}
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
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'معتبر / نامعتبر' : 'Valid / Invalid'}</th>
                    <th style={{ padding: '12px 16px' }}>{isFa ? 'درج شده' : 'Imported'}</th>
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

                    const canConfigure = ['mapping_required', 'ready_to_validate', 'validating', 'preview_ready', 'ready_to_import'].includes(job.status);

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
                          <div style={{ fontSize: '11px', color: theme.colors.textSecondary, display: 'flex', gap: '6px' }}>
                            <span>{job.detected_format?.toUpperCase()}</span>
                            <span>•</span>
                            <span>{formatFileSize(job.file_size)}</span>
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
                        <td style={{ padding: '12px 16px', fontSize: '13px' }}>
                          {job.valid_rows > 0 || job.invalid_rows > 0 ? (
                            <span>
                              <span style={{ color: '#16A34A', fontWeight: 600 }}>{job.valid_rows}</span>
                              {' / '}
                              <span style={{ color: job.invalid_rows > 0 ? '#DC2626' : theme.colors.textSecondary }}>{job.invalid_rows}</span>
                            </span>
                          ) : '—'}
                        </td>
                        <td style={{ padding: '12px 16px', fontWeight: 600, color: '#2563EB' }}>
                          {job.imported_rows > 0 ? job.imported_rows.toLocaleString() : '—'}
                        </td>
                        <td style={{ padding: '12px 16px', color: theme.colors.textSecondary }}>
                          {new Date(job.created_at).toLocaleDateString(isFa ? 'fa-IR' : 'en-US')}
                        </td>
                        <td style={{ padding: '12px 16px' }}>
                          <div style={{ display: 'flex', gap: '6px' }}>
                            {canConfigure ? (
                              <button
                                onClick={() => openWizard(job.id)}
                                style={{
                                  padding: '5px 10px',
                                  fontSize: '12px',
                                  borderRadius: '6px',
                                  border: '1px solid #3B82F6',
                                  backgroundColor: '#EFF6FF',
                                  color: '#1D4ED8',
                                  fontWeight: 600,
                                  cursor: 'pointer',
                                }}
                              >
                                {job.status === 'mapping_required' || job.status === 'ready_to_validate'
                                  ? (isFa ? 'تطبیق و بررسی' : 'Map & Validate')
                                  : (isFa ? 'پیش‌نمایش و اجرا' : 'Preview & Import')}
                              </button>
                            ) : (
                              <button
                                onClick={() => setSelectedJob(job)}
                                style={{
                                  padding: '5px 10px',
                                  fontSize: '12px',
                                  borderRadius: '6px',
                                  border: `1px solid ${theme.colors.border}`,
                                  backgroundColor: isDark ? theme.colors.surfaceElevated : '#FFFFFF',
                                  color: theme.colors.textPrimary,
                                  cursor: 'pointer',
                                }}
                              >
                                {isFa ? 'مشاهده' : 'View'}
                              </button>
                            )}

                            {isCancellable && (
                              <button
                                onClick={() => handleCancelJob(job.id)}
                                style={{
                                  padding: '5px 8px',
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

      {/* Multi-step Wizard Modal for Mapping, Validation, and Execution */}
      {wizardJobId && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0,0,0,0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: '16px',
          }}
        >
          <div
            style={{
              backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
              borderRadius: '16px',
              padding: '28px',
              maxWidth: '900px',
              width: '100%',
              maxHeight: '92vh',
              overflowY: 'auto',
              border: `1px solid ${theme.colors.border}`,
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
            }}
          >
            {/* Modal Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '20px', fontWeight: 700 }}>
                  {isFa ? 'مراحل آماده‌سازی و ورود داده‌ها' : 'Data Migration Wizard'}
                </h3>
                <div style={{ fontSize: '13px', color: theme.colors.textSecondary, marginTop: '4px' }}>
                  {wizardStep === 'mapping' && (isFa ? 'مرحله ۱: انتخاب کاربرگ و تطبیق ستون‌ها' : 'Step 1: Sheet & Column Mapping')}
                  {wizardStep === 'preview' && (isFa ? 'مرحله ۲: نتایج اعتبارسنجی و پیش‌نمایش رکوردها' : 'Step 2: Validation Summary & Row Preview')}
                  {wizardStep === 'result' && (isFa ? 'مرحله ۳: گزارش نهایی درون‌ریزی' : 'Step 3: Import Result Summary')}
                </div>
              </div>
              <button
                onClick={() => setWizardJobId(null)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '22px',
                  cursor: 'pointer',
                  color: theme.colors.textSecondary,
                }}
              >
                ✕
              </button>
            </div>

            {wizardError && <FormError message={wizardError} />}

            {/* STEP 1: MAPPING */}
            {wizardStep === 'mapping' && mappingData && (
              <div>
                {/* Worksheet Selector if multiple datasets */}
                {mappingData.datasets.length > 1 && (
                  <div style={{ marginBottom: '20px', padding: '12px', borderRadius: '8px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6' }}>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                      {isFa ? 'انتخاب کاربرگ (Sheet):' : 'Select Sheet:'}
                    </label>
                    <select
                      value={selectedSheetIndex}
                      onChange={(e) => handleSheetChange(Number(e.target.value))}
                      style={{
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: `1px solid ${theme.colors.border}`,
                        backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
                        color: theme.colors.textPrimary,
                      }}
                    >
                      {mappingData.datasets.map((ds) => (
                        <option key={ds.id} value={ds.sheet_index}>
                          {ds.name} ({ds.row_count} {isFa ? 'ردیف' : 'rows'})
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Default Warehouse Selector for inventory imports */}
                {mappingData.import_type === 'inventory' && mappingData.warehouses && mappingData.warehouses.length > 0 && (
                  <div style={{ marginBottom: '20px', padding: '12px', borderRadius: '8px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6' }}>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                      {isFa ? 'انبار پیش‌فرض (در صورت عدم تطبیق ستون انبار):' : 'Default Warehouse (if warehouse column is not mapped):'}
                    </label>
                    <select
                      value={selectedWarehouseId}
                      onChange={(e) => setSelectedWarehouseId(e.target.value)}
                      style={{
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: `1px solid ${theme.colors.border}`,
                        backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
                        color: theme.colors.textPrimary,
                        fontSize: '13px',
                        minWidth: '240px',
                      }}
                    >
                      <option value="">{isFa ? '-- انتخاب انبار پیش‌فرض --' : '-- Select Default Warehouse --'}</option>
                      {mappingData.warehouses.map((wh) => (
                        <option key={wh.id} value={wh.id}>
                          {wh.name} {wh.code ? `(${wh.code})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                {/* Default Store Selector for expenses and orders imports */}
                {(mappingData.import_type === 'expenses' || mappingData.import_type === 'orders') && mappingData.stores && mappingData.stores.length > 0 && (
                  <div style={{ marginBottom: '20px', padding: '12px', borderRadius: '8px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6' }}>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                      {isFa ? 'فروشگاه پیش‌فرض (در صورت عدم تطبیق ستون فروشگاه):' : 'Default Store (if store column is not mapped):'}
                    </label>
                    <select
                      value={selectedStoreId}
                      onChange={(e) => setSelectedStoreId(e.target.value)}
                      style={{
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: `1px solid ${theme.colors.border}`,
                        backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
                        color: theme.colors.textPrimary,
                        fontSize: '13px',
                        minWidth: '240px',
                      }}
                    >
                      <option value="">{isFa ? '-- انتخاب فروشگاه پیش‌فرض --' : '-- Select Default Store --'}</option>
                      {mappingData.stores.map((st) => (
                        <option key={st.id} value={st.id}>
                          {st.name} {st.code ? `(${st.code})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div style={{ marginBottom: '16px', fontSize: '14px', color: theme.colors.textSecondary }}>
                  {isFa
                    ? 'ستون‌های فایل خود را به فیلدهای متناظر در سیستم متصل نمایید. پیشنهادهای هوشمند بر اساس نام ستون‌ها اعمال شده‌اند.'
                    : 'Map your spreadsheet columns to Resto fields. Deterministic suggestions have been pre-selected.'}
                </div>

                <div style={{ border: `1px solid ${theme.colors.border}`, borderRadius: '8px', overflow: 'hidden' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left' }}>
                    <thead>
                      <tr style={{ backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB', borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                        <th style={{ padding: '10px 14px' }}>{isFa ? 'فیلد مقصد در سامانه' : 'Resto Field'}</th>
                        <th style={{ padding: '10px 14px' }}>{isFa ? 'ستون متناظر در فایل' : 'Spreadsheet Column'}</th>
                        <th style={{ padding: '10px 14px' }}>{isFa ? 'توضیحات فیلد' : 'Description'}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {mappingData.target_fields.map((field) => {
                        const currentSourceCol = userFieldMappings[field.name] || '';
                        return (
                          <tr key={field.name} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                            <td style={{ padding: '12px 14px', fontWeight: 600 }}>
                              {isFa ? field.label_fa : field.label_en}
                              {field.required && <span style={{ color: '#DC2626', marginRight: '4px' }}>*</span>}
                            </td>
                            <td style={{ padding: '12px 14px' }}>
                              <select
                                value={currentSourceCol}
                                onChange={(e) => {
                                  const val = e.target.value;
                                  setUserFieldMappings((prev) => ({
                                    ...prev,
                                    [field.name]: val,
                                  }));
                                }}
                                style={{
                                  width: '100%',
                                  padding: '8px 10px',
                                  borderRadius: '6px',
                                  border: `1px solid ${currentSourceCol ? '#3B82F6' : theme.colors.border}`,
                                  backgroundColor: isDark ? theme.colors.surfaceElevated : '#FFFFFF',
                                  color: theme.colors.textPrimary,
                                  fontSize: '13px',
                                }}
                              >
                                <option value="">{isFa ? '-- نادیده گرفتن این فیلد --' : '-- Ignore Field --'}</option>
                                {mappingData.headers.map((hdr) => (
                                  <option key={hdr} value={hdr}>
                                    {hdr}
                                  </option>
                                ))}
                              </select>
                            </td>
                            <td style={{ padding: '12px 14px', color: theme.colors.textSecondary, fontSize: '12px' }}>
                              {isFa ? field.description_fa : field.description_en}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>

                <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
                  <SecondaryButton onClick={() => setWizardJobId(null)}>
                    {isFa ? 'انصراف' : 'Cancel'}
                  </SecondaryButton>
                  <PrimaryButton onClick={handleSaveMappingAndValidate} disabled={isWizardLoading}>
                    {isWizardLoading
                      ? (isFa ? 'در حال بررسی و اعتبارسنجی...' : 'Validating...')
                      : (isFa ? 'تایید تطبیق و اعتبارسنجی داده‌ها' : 'Save & Run Validation')}
                  </PrimaryButton>
                </div>
              </div>
            )}

            {/* STEP 2: PREVIEW & DUPLICATE STRATEGY */}
            {wizardStep === 'preview' && previewData && (
              <div>
                {/* Stats cards */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '12px', marginBottom: '20px' }}>
                  <div style={{ padding: '14px', borderRadius: '10px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6' }}>
                    <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'کل ردیف‌ها' : 'Total Rows'}</div>
                    <div style={{ fontSize: '20px', fontWeight: 700 }}>{previewData.total_rows}</div>
                  </div>
                  <div style={{ padding: '14px', borderRadius: '10px', backgroundColor: isDark ? '#064E3B' : '#ECFDF5' }}>
                    <div style={{ fontSize: '12px', color: isDark ? '#A7F3D0' : '#065F46' }}>{isFa ? 'ردیف‌های معتبر' : 'Valid Rows'}</div>
                    <div style={{ fontSize: '20px', fontWeight: 700, color: '#10B981' }}>{previewData.valid_rows}</div>
                  </div>
                  <div style={{ padding: '14px', borderRadius: '10px', backgroundColor: isDark ? '#7F1D1D' : '#FEF2F2' }}>
                    <div style={{ fontSize: '12px', color: isDark ? '#FECACA' : '#991B1B' }}>{isFa ? 'دارای خطا' : 'Invalid Rows'}</div>
                    <div style={{ fontSize: '20px', fontWeight: 700, color: '#EF4444' }}>{previewData.invalid_rows}</div>
                  </div>
                  <div style={{ padding: '14px', borderRadius: '10px', backgroundColor: isDark ? '#78350F' : '#FFFBEB' }}>
                    <div style={{ fontSize: '12px', color: isDark ? '#FDE68A' : '#92400E' }}>{isFa ? 'مشابه در سامانه' : 'Existing Matches'}</div>
                    <div style={{ fontSize: '20px', fontWeight: 700, color: '#F59E0B' }}>{previewData.matched_existing_rows}</div>
                  </div>
                </div>

                {/* Duplicate Handling Strategy */}
                <div style={{ marginBottom: '20px', padding: '16px', borderRadius: '10px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                  <h4 style={{ margin: '0 0 10px 0', fontSize: '14px', fontWeight: 600 }}>
                    {isFa ? 'راهبرد برخورد با رکوردهای تکراری یا موجود در سیستم:' : 'Duplicate Records Handling Strategy:'}
                  </h4>
                  <div style={{ display: 'flex', gap: '20px' }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer' }}>
                      <input
                        type="radio"
                        name="dupStrategy"
                        value="skip"
                        checked={duplicateStrategy === 'skip'}
                        onChange={() => setDuplicateStrategy('skip')}
                      />
                      <span><strong>{isFa ? 'صرف‌نظر کردن (Skip):' : 'Skip:'}</strong> {isFa ? 'رکوردهای تکراری رد شده و تغییر نمی‌کنند' : 'Do not modify existing records'}</span>
                    </label>
                    <label style={{ display: 'flex', alignItems: 'center', gap: '8px', fontSize: '13px', cursor: 'pointer' }}>
                      <input
                        type="radio"
                        name="dupStrategy"
                        value="update"
                        checked={duplicateStrategy === 'update'}
                        onChange={() => setDuplicateStrategy('update')}
                      />
                      <span><strong>{isFa ? 'بروزرسانی (Update):' : 'Update:'}</strong> {isFa ? 'اطلاعات رکورد موجود با فایل جایگزین می‌شود' : 'Overwrite existing record with spreadsheet values'}</span>
                    </label>
                  </div>
                </div>

                {/* Sample Preview Rows */}
                <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: 600 }}>
                  {isFa ? 'نمونه ردیف‌های پردازش‌شده (پیش‌نمایش ۵۰ ردیف اول):' : 'Preview Sample Rows (First 50 Rows):'}
                </h4>
                <div style={{ border: `1px solid ${theme.colors.border}`, borderRadius: '8px', maxHeight: '300px', overflowY: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left' }}>
                    <thead>
                      <tr style={{ backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB', borderBottom: `1px solid ${theme.colors.border}`, fontSize: '12px' }}>
                        <th style={{ padding: '8px 10px' }}>{isFa ? 'ردیف' : 'Row'}</th>
                        <th style={{ padding: '8px 10px' }}>{isFa ? 'وضعیت' : 'Status'}</th>
                        <th style={{ padding: '8px 10px' }}>{isFa ? 'داده‌های استخراج‌شده' : 'Parsed Data'}</th>
                        <th style={{ padding: '8px 10px' }}>{isFa ? 'پیام / هشدار' : 'Messages'}</th>
                      </tr>
                    </thead>
                    <tbody>
                      {previewData.preview_rows.map((row) => (
                        <tr key={row.row_number} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '12px' }}>
                          <td style={{ padding: '8px 10px', fontWeight: 600 }}>{row.row_number}</td>
                          <td style={{ padding: '8px 10px' }}>
                            {row.is_valid ? (
                              row.has_match ? (
                                <span style={{ color: '#D97706', backgroundColor: '#FEF3C7', padding: '2px 6px', borderRadius: '4px' }}>
                                  {isFa ? 'موجود در سیستم' : 'Existing Match'}
                                </span>
                              ) : (
                                <span style={{ color: '#16A34A', backgroundColor: '#DCFCE7', padding: '2px 6px', borderRadius: '4px' }}>
                                  {isFa ? 'معتبر' : 'Valid'}
                                </span>
                              )
                            ) : (
                              <span style={{ color: '#DC2626', backgroundColor: '#FEE2E2', padding: '2px 6px', borderRadius: '4px' }}>
                                {isFa ? 'دارای خطا' : 'Invalid'}
                              </span>
                            )}
                          </td>
                          <td style={{ padding: '8px 10px' }}>
                            {Object.entries(row.mapped_data)
                              .filter(([_, v]) => v !== null && v !== '')
                              .map(([k, v]) => `${k}: ${v}`)
                              .join(' | ')}
                          </td>
                          <td style={{ padding: '8px 10px', color: row.is_valid ? '#D97706' : '#DC2626' }}>
                            {row.errors.length > 0 && row.errors.map((e) => e.message).join(', ')}
                            {row.warnings.length > 0 && row.warnings.map((w) => w.message).join(', ')}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <SecondaryButton
                    onClick={() => {
                      if (wizardJobId) openWizard(wizardJobId);
                    }}
                  >
                    {isFa ? '← بازگشت به تطبیق ستون‌ها' : '← Back to Mapping'}
                  </SecondaryButton>
                  <PrimaryButton
                    onClick={handleExecuteImport}
                    disabled={isWizardLoading || previewData.valid_rows === 0}
                  >
                    {isWizardLoading
                      ? (isFa ? 'در حال اجرای درون‌ریزی...' : 'Executing Import...')
                      : (isFa ? 'تایید و اجرای نهایی درون‌ریزی' : 'Confirm & Execute Import')}
                  </PrimaryButton>
                </div>
              </div>
            )}

            {/* STEP 3: RESULT */}
            {wizardStep === 'result' && executionResult && (
              <div style={{ textAlign: 'center', padding: '20px 0' }}>
                <div style={{ fontSize: '48px', marginBottom: '16px' }}>🎉</div>
                <h3 style={{ fontSize: '20px', fontWeight: 700, marginBottom: '8px' }}>
                  {isFa ? 'درون‌ریزی اطلاعات با موفقیت انجام شد' : 'Import Executed Successfully'}
                </h3>
                <p style={{ color: theme.colors.textSecondary, marginBottom: '24px' }}>
                  {isFa
                    ? 'داده‌های معتبر بدون دستکاری در انبار یا سوابق مالی ثبت شدند.'
                    : 'Valid records have been safely migrated into your catalog.'}
                </p>

                <div style={{ display: 'inline-flex', gap: '20px', padding: '16px 32px', borderRadius: '12px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6', marginBottom: '24px' }}>
                  <div>
                    <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{isFa ? 'درج / بروزرسانی شده' : 'Imported'}</div>
                    <div style={{ fontSize: '24px', fontWeight: 700, color: '#16A34A' }}>{executionResult.imported_rows}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{isFa ? 'رد شده (تکراری)' : 'Skipped'}</div>
                    <div style={{ fontSize: '24px', fontWeight: 700, color: '#F59E0B' }}>{executionResult.skipped_rows}</div>
                  </div>
                  <div>
                    <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>{isFa ? 'ناموفق (خطا)' : 'Failed'}</div>
                    <div style={{ fontSize: '24px', fontWeight: 700, color: '#DC2626' }}>{executionResult.failed_rows}</div>
                  </div>
                </div>

                <div>
                  <PrimaryButton
                    onClick={() => {
                      setWizardJobId(null);
                      void loadJobs();
                    }}
                  >
                    {isFa ? 'بستن و بازگشت به لیست' : 'Close and Return'}
                  </PrimaryButton>
                </div>
              </div>
            )}
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
                    ? 'حداکثر حجم مجاز: ۲۰ مگابایت. فرمت‌های اکسل (.xlsx) و CSV پشتیبانی می‌شوند.'
                    : 'Maximum size: 20MB. Excel (.xlsx) and CSV spreadsheets supported.'}
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
                    : (isFa ? 'بارگذاری و ادامه به مرحله تطبیق' : 'Upload & Proceed to Mapping')}
                </PrimaryButton>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Selected Job Details Modal */}
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
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'تعداد کل ردیف‌ها' : 'Total Rows'}</div>
                <div style={{ fontWeight: 600 }}>{selectedJob.total_rows.toLocaleString()}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary }}>{isFa ? 'رکوردهای درون‌ریزی شده' : 'Imported Rows'}</div>
                <div style={{ fontWeight: 600, color: '#16A34A' }}>{selectedJob.imported_rows.toLocaleString()}</div>
              </div>
            </div>

            <div style={{ marginTop: '24px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
              <SecondaryButton onClick={() => setSelectedJob(null)}>
                {isFa ? 'بستن' : 'Close'}
              </SecondaryButton>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ImportCenterPage;
