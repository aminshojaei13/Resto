import React, { useCallback, useEffect, useState, useMemo } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { FormError, PrimaryButton, SecondaryButton } from '../components/Field';
import { EmptyState } from '../components/EmptyState';
import { SectionHelp } from '../components/HelpSystem';
import { useTheme } from '../theme/ThemeContext';
import {
  ImportJobItem,
  ImportMappingResponse,
  ImportPreviewResponse,
  ImportValidationResultItem,
  ImportTemplateInfo,
  ImportMatchCategory,
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
  { value: 'suppliers', fa: 'تأمین‌کنندگان', en: 'Suppliers' },
  { value: 'purchases', fa: 'سوابق خرید (تاریخی)', en: 'Historical Purchases' },
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
  const [confirmedFields, setConfirmedFields] = useState<Record<string, boolean>>({});
  const [selectedSheetIndex, setSelectedSheetIndex] = useState(0);
  const [selectedWarehouseId, setSelectedWarehouseId] = useState<string>('');
  const [selectedStoreId, setSelectedStoreId] = useState<string>('');
  const [selectedSupplierId, setSelectedSupplierId] = useState<string>('');
  const [previewData, setPreviewData] = useState<ImportPreviewResponse | null>(null);
  const [duplicateStrategy, setDuplicateStrategy] = useState<'skip' | 'update'>('skip');
  const [isWizardLoading, setIsWizardLoading] = useState(false);
  const [wizardError, setWizardError] = useState('');
  const [jobErrorsList, setJobErrorsList] = useState<ImportValidationResultItem[]>([]);
  const [executionResult, setExecutionResult] = useState<any>(null);

  // Template catalog and UX Guide state
  const [templates, setTemplates] = useState<ImportTemplateInfo[]>([]);
  const [isTemplatesModalOpen, setIsTemplatesModalOpen] = useState(false);
  const [activeTemplateType, setActiveTemplateType] = useState('products');
  const [isGuideExpanded, setIsGuideExpanded] = useState(false);
  const [downloadingTemplateType, setDownloadingTemplateType] = useState<string | null>(null);

  const loadTemplates = useCallback(async () => {
    try {
      const list = await apiClient.getImportTemplates();
      setTemplates(list ?? []);
    } catch {
      // Non-blocking for templates catalog
    }
  }, []);

  useEffect(() => {
    void loadTemplates();
  }, [loadTemplates]);

  const handleDownloadTemplate = async (type: string, filename?: string) => {
    setDownloadingTemplateType(type);
    try {
      await apiClient.downloadImportTemplate(type, filename);
      setActionNotice(
        isFa
          ? `قالب استاندارد CSV با انکودینگ UTF-8 BOM برای "${type}" با موفقیت دانلود شد.`
          : `Standard CSV template with UTF-8 BOM for "${type}" was downloaded.`
      );
    } catch (err: any) {
      setErrorMessage(
        isFa
          ? `خطا در دریافت فایل قالب CSV: ${err.message || 'خطای شبکه'}`
          : `Failed to download CSV template: ${err.message || 'Network error'}`
      );
    } finally {
      setDownloadingTemplateType(null);
    }
  };

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
        try {
          const errs = await apiClient.getImportJobErrors(jobId, 1);
          setJobErrorsList(errs.data ?? []);
        } catch {
          setJobErrorsList([]);
        }
        setWizardStep('preview');
      } else {
        // Load mapping step
        const mapRes = await apiClient.getImportMapping(jobId, 0);
        setMappingData(mapRes);
        setSelectedSheetIndex(mapRes.active_sheet_index);

        // Populate initial field mappings from suggested mappings or saved mapping
        const initialMap: Record<string, string> = {};
        const initialConfirmed: Record<string, boolean> = {};
        if (mapRes.current_mapping && mapRes.current_mapping.fields) {
          Object.assign(initialMap, mapRes.current_mapping.fields);
          Object.keys(mapRes.current_mapping.fields).forEach((f) => {
            initialConfirmed[f] = true;
          });
        } else {
          mapRes.suggested_mappings.forEach((sug) => {
            if (sug.target_field) {
              initialMap[sug.target_field] = sug.source_column;
            }
          });
        }
        setUserFieldMappings(initialMap);
        setConfirmedFields(initialConfirmed);

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

        if (mapRes.current_mapping && mapRes.current_mapping.supplier_id) {
          setSelectedSupplierId(mapRes.current_mapping.supplier_id);
        } else if (mapRes.suppliers && mapRes.suppliers.length > 0) {
          setSelectedSupplierId(mapRes.suppliers[0].id);
        } else {
          setSelectedSupplierId('');
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
      setConfirmedFields({});
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
      await apiClient.saveImportMapping(wizardJobId, userFieldMappings, selectedSheetIndex, selectedWarehouseId, selectedStoreId, selectedSupplierId);

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

  const duplicateSourceColumns = useMemo(() => {
    const counts: Record<string, number> = {};
    Object.values(userFieldMappings).forEach((col) => {
      if (col && col.trim() !== '') {
        counts[col] = (counts[col] || 0) + 1;
      }
    });
    return Object.keys(counts).filter((col) => counts[col] > 1);
  }, [userFieldMappings]);

  const missingRequiredFields = useMemo(() => {
    if (!mappingData) return [];
    return mappingData.target_fields.filter(
      (f) => f.required && (!userFieldMappings[f.name] || userFieldMappings[f.name].trim() === '')
    );
  }, [mappingData, userFieldMappings]);

  const getMatchCategoryBadge = (category?: ImportMatchCategory) => {
    switch (category) {
      case 'exact_canonical':
        return {
          label: isFa ? 'تطبیق قطعی (۱۰۰٪)' : 'Exact Canonical (100%)',
          bg: isDark ? '#064E3B' : '#ECFDF5',
          color: '#10B981',
          border: isDark ? '#047857' : '#A7F3D0',
        };
      case 'exact_alias':
        return {
          label: isFa ? 'مترادف دقیق (۹۵٪)' : 'Exact Alias (95%)',
          bg: isDark ? '#164E63' : '#ECFEFF',
          color: '#06B6D4',
          border: isDark ? '#0891B2' : '#A5F3FC',
        };
      case 'normalized_alias':
        return {
          label: isFa ? 'مترادف استاندارد (۹۰٪)' : 'Normalized (90%)',
          bg: isDark ? '#1E3A8A' : '#EFF6FF',
          color: '#3B82F6',
          border: isDark ? '#1D4ED8' : '#BFDBFE',
        };
      case 'fuzzy_high':
        return {
          label: isFa ? 'شباهت بالا (۸۰٪)' : 'High Similarity (80%)',
          bg: isDark ? '#78350F' : '#FFFBEB',
          color: '#F59E0B',
          border: isDark ? '#B45309' : '#FDE68A',
        };
      case 'ambiguous':
        return {
          label: isFa ? 'دارای ابهام' : 'Ambiguous',
          bg: isDark ? '#7F1D1D' : '#FEF2F2',
          color: '#EF4444',
          border: isDark ? '#B91C1C' : '#FECACA',
        };
      case 'conflict':
        return {
          label: isFa ? 'تداخل ستون‌ها' : 'Conflict',
          bg: isDark ? '#7C2D12' : '#FFF7ED',
          color: '#EA580C',
          border: isDark ? '#C2410C' : '#FFEDD5',
        };
      case 'unmapped':
      default:
        return {
          label: isFa ? 'بدون انطباق' : 'Unmapped',
          bg: isDark ? '#374151' : '#F3F4F6',
          color: theme.colors.textSecondary,
          border: isDark ? '#4B5563' : '#E5E7EB',
        };
    }
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
            ? 'مهاجرت امن داده‌ها (کالاها، مشتریان، موجودی اولیه، هزینه‌ها، سفارش‌های گذشته، تأمین‌کنندگان و خریدها) با تطبیق دقیق، اعتبارسنجی ردیفی و قالب‌های استاندارد'
            : 'Secure data migration (products, customers, opening stock, expenses, historical orders, suppliers, and purchases) with deterministic mapping and standard templates'
        }
        actions={
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            <SecondaryButton onClick={() => setIsTemplatesModalOpen(true)}>
              {isFa ? '📥 قالب‌های آماده اکسل / CSV' : '📥 Download Templates'}
            </SecondaryButton>
            <PrimaryButton onClick={() => setIsUploadModalOpen(true)}>
              {isFa ? '+ درون‌ریزی فایل جدید' : '+ New Import'}
            </PrimaryButton>
          </div>
        }
        helpSection="imports"
        language={language}
      />

      <SectionHelp language={language} sectionKey="imports" />

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

      {/* Collapsible 6-Step Guide and Domain Integrity Notice */}
      <div
        style={{
          marginTop: '16px',
          borderRadius: '12px',
          border: `1px solid ${theme.colors.border}`,
          backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
          overflow: 'hidden',
          boxShadow: isDark ? 'none' : '0 1px 3px rgba(0,0,0,0.05)',
        }}
      >
        <div
          onClick={() => setIsGuideExpanded(!isGuideExpanded)}
          style={{
            padding: '14px 18px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            cursor: 'pointer',
            backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB',
            borderBottom: isGuideExpanded ? `1px solid ${theme.colors.border}` : 'none',
            userSelect: 'none',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '18px' }}>📖</span>
            <div>
              <span style={{ fontWeight: 600, fontSize: '14px' }}>
                {isFa ? 'راهنمای گام‌به‌گام و قواعد سلامت درون‌ریزی داده‌ها' : 'Step-by-Step Guide & Data Integrity Rules'}
              </span>
              <span
                style={{
                  fontSize: '12px',
                  color: theme.colors.textSecondary,
                  marginRight: isFa ? '8px' : 0,
                  marginLeft: !isFa ? '8px' : 0,
                }}
              >
                {isFa ? '(۶ گام استاندارد + هشدارهای یکپارچگی)' : '(6 Standard Steps + Invariants)'}
              </span>
            </div>
          </div>
          <button
            type="button"
            style={{
              background: 'none',
              border: 'none',
              color: '#2563EB',
              fontSize: '13px',
              cursor: 'pointer',
              fontWeight: 600,
            }}
          >
            {isGuideExpanded ? (isFa ? '▲ بستن راهنما' : '▲ Collapse') : (isFa ? '▼ مشاهده راهنما' : '▼ Expand Guide')}
          </button>
        </div>

        {isGuideExpanded && (
          <div style={{ padding: '20px' }}>
            <h4 style={{ margin: '0 0 12px 0', fontSize: '14px', fontWeight: 600 }}>
              {isFa ? 'مراحل ۶گانه آماده‌سازی و درون‌ریزی اطلاعات:' : '6-Step Import Migration Process:'}
            </h4>

            {/* 6 Step Cards */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
                gap: '12px',
                marginBottom: '20px',
              }}
            >
              <div style={{ padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#2563EB', marginBottom: '4px' }}>
                  {isFa ? '۱. دانلود قالب آماده CSV' : '1. Download Ready Template'}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.6' }}>
                  {isFa
                    ? 'قالب نمونه با انکودینگ UTF-8 BOM دانلود کنید تا حروف فارسی در اکسل بدون نقص نمایش داده شوند.'
                    : 'Download blank CSV templates with UTF-8 BOM so Excel opens Persian text without encoding issues.'}
                </div>
              </div>

              <div style={{ padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#2563EB', marginBottom: '4px' }}>
                  {isFa ? '۲. پر کردن داده‌ها و اقلام چندسطری' : '2. Fill Data & Group Items'}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.6' }}>
                  {isFa
                    ? 'برای سفارش‌ها و خریدهای گذشته، ردیف‌های یک فاکتور باید شناسه، تاریخ، مشتری و فروشگاه یکسان داشته باشند.'
                    : 'For multi-item orders or purchases, items in the same invoice must share identical ID, date, customer, and store.'}
                </div>
              </div>

              <div style={{ padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#2563EB', marginBottom: '4px' }}>
                  {isFa ? '۳. بارگذاری فایل (اکسل یا CSV)' : '3. Upload Spreadsheet'}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.6' }}>
                  {isFa
                    ? 'فایل تکمیل‌شده با پسوند .xlsx یا .csv تا سقف ۲۰ مگابایت را بارگذاری کنید. ساختار فایل خودکار بررسی می‌شود.'
                    : 'Upload your completed .xlsx or .csv spreadsheet (up to 20MB). File structure is inspected automatically.'}
                </div>
              </div>

              <div style={{ padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#2563EB', marginBottom: '4px' }}>
                  {isFa ? '۴. تطبیق ستون‌ها و مقادیر پیش‌فرض' : '4. Map Columns & Fallbacks'}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.6' }}>
                  {isFa
                    ? 'ستون‌های فایل با فیلدهای مقصد تطبیق می‌یابند. در صورت نبود ستون انبار یا فروشگاه، انبار/فروشگاه پیش‌فرض تعیین می‌شود.'
                    : 'Spreadsheet columns map to destination fields. Default warehouse or store handles rows without explicit targets.'}
                </div>
              </div>

              <div style={{ padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#2563EB', marginBottom: '4px' }}>
                  {isFa ? '۵. اعتبارسنجی و استراتژی تکراری‌ها' : '5. Validate & Choose Strategy'}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.6' }}>
                  {isFa
                    ? 'خطاها ردیف‌به‌ردیف نمایش داده می‌شوند. برای رکوردهای موجود، استراتژی صرف‌نظر (Skip) یا بروزرسانی (Update) را انتخاب کنید.'
                    : 'Preview validation flags invalid rows. Choose Skip or Update strategy for existing catalog records.'}
                </div>
              </div>

              <div style={{ padding: '12px', borderRadius: '8px', border: `1px solid ${theme.colors.border}`, backgroundColor: isDark ? theme.colors.surfaceElevated : '#FAFAFA' }}>
                <div style={{ fontWeight: 600, fontSize: '13px', color: '#2563EB', marginBottom: '4px' }}>
                  {isFa ? '۶. اجرای قطعی و نهایی' : '6. Transactional Execution'}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.6' }}>
                  {isFa
                    ? 'درون‌ریزی به صورت تراکنشی انجام شده و عملیات قفل می‌شود تا هیچ داده تکراری با اجرای مجدد ایجاد نشود.'
                    : 'Execution runs within atomic transactions and locks the job to prevent duplicate records upon retry.'}
                </div>
              </div>
            </div>

            {/* Domain Invariants Alerts */}
            <h4 style={{ margin: '0 0 10px 0', fontSize: '13px', fontWeight: 600, color: theme.colors.textSecondary }}>
              {isFa ? '🔒 هشدارهای سلامت و قواعد دامنه‌ای سیستم:' : '🔒 Domain Invariants & Audit Safeguards:'}
            </h4>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '10px' }}>
              <div
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  backgroundColor: isDark ? '#1E1B4B' : '#EEF2FF',
                  border: `1px solid ${isDark ? '#3730A3' : '#C7D2FE'}`,
                  fontSize: '12px',
                  lineHeight: '1.5',
                  color: isDark ? '#E0E7FF' : '#312E81',
                }}
              >
                <strong>{isFa ? 'سفارش‌ها و خریدهای تاریخی (HISTORICAL_RECORD_ONLY):' : 'Historical Orders & Purchases:'}</strong>{' '}
                {isFa
                  ? 'این سوابق فقط برای تاریخچه مشتریان و تحلیل سود/زیان وارد می‌شوند. هیچ اثری بر موجودی انبار فعلی یا صندوق زنده ندارند.'
                  : 'Imported purely for customer history and CRM analytics; zero live stock deductions and zero GL cash pollution.'}
              </div>

              <div
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  backgroundColor: isDark ? '#14532D' : '#F0FDF4',
                  border: `1px solid ${isDark ? '#166534' : '#BBF7D0'}`,
                  fontSize: '12px',
                  lineHeight: '1.5',
                  color: isDark ? '#DCFCE7' : '#14532D',
                }}
              >
                <strong>{isFa ? 'موجودی اولیه انبار (Opening Inventory):' : 'Opening Inventory:'}</strong>{' '}
                {isFa
                  ? 'صرفاً برای ثبت سند شمارش اولیه اول دوره استفاده می‌شود. کالاها باید از قبل در کاتالوگ سازمان تعریف شده باشند.'
                  : 'Creates opening stock ledger movements. Products must already exist in tenant catalog.'}
              </div>

              <div
                style={{
                  padding: '10px 14px',
                  borderRadius: '8px',
                  backgroundColor: isDark ? '#431407' : '#FFF7ED',
                  border: `1px solid ${isDark ? '#7C2D12' : '#FFEDD5'}`,
                  fontSize: '12px',
                  lineHeight: '1.5',
                  color: isDark ? '#FFEDD5' : '#7C2D12',
                }}
              >
                <strong>{isFa ? 'هزینه‌های جاری (Operating Expenses):' : 'Operating Expenses:'}</strong>{' '}
                {isFa
                  ? 'هزینه‌ها پس از ورود مستقیماً سند مالی دوبل بدهکار هزینه و بستانکار وجه نقد/بانک را در دفتر کل ثبت می‌کنند.'
                  : 'Operating expenses immediately generate balanced double-entry ledger transactions.'}
              </div>
            </div>
          </div>
        )}
      </div>

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

                {/* Default Warehouse Selector for inventory and purchases imports */}
                {(mappingData.import_type === 'inventory' || mappingData.import_type === 'purchases') && mappingData.warehouses && mappingData.warehouses.length > 0 && (
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

                {/* Default Store Selector for expenses, orders, and purchases imports */}
                {(mappingData.import_type === 'expenses' || mappingData.import_type === 'orders' || mappingData.import_type === 'purchases') && mappingData.stores && mappingData.stores.length > 0 && (
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

                {/* Default Supplier Selector for purchases imports */}
                {mappingData.import_type === 'purchases' && mappingData.suppliers && mappingData.suppliers.length > 0 && (
                  <div style={{ marginBottom: '20px', padding: '12px', borderRadius: '8px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6' }}>
                    <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, marginBottom: '6px' }}>
                      {isFa ? 'تأمین‌کننده پیش‌فرض (در صورت عدم تطبیق ستون تأمین‌کننده):' : 'Default Supplier (if supplier column is not mapped):'}
                    </label>
                    <select
                      value={selectedSupplierId}
                      onChange={(e) => setSelectedSupplierId(e.target.value)}
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
                      <option value="">{isFa ? '-- انتخاب تأمین‌کننده پیش‌فرض --' : '-- Select Default Supplier --'}</option>
                      {mappingData.suppliers.map((sup) => (
                        <option key={sup.id} value={sup.id}>
                          {sup.name} {sup.code ? `(${sup.code})` : ''}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div style={{ marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
                  <div style={{ fontSize: '14px', color: theme.colors.textSecondary }}>
                    {isFa
                      ? 'ستون‌های فایل خود را به فیلدهای متناظر در سیستم متصل نمایید. پیشنهادهای هوشمند بر اساس نام ستون‌ها اعمال شده‌اند.'
                      : 'Map your spreadsheet columns to Resto fields. Deterministic suggestions have been pre-selected.'}
                  </div>
                  <div style={{ display: 'flex', gap: '8px' }}>
                    <button
                      type="button"
                      onClick={() => {
                        const allSuggested: Record<string, string> = {};
                        const allConfirmed: Record<string, boolean> = {};
                        mappingData.suggested_mappings.forEach((sug) => {
                          if (sug.target_field) {
                            allSuggested[sug.target_field] = sug.source_column;
                            allConfirmed[sug.target_field] = true;
                          }
                        });
                        setUserFieldMappings(allSuggested);
                        setConfirmedFields(allConfirmed);
                      }}
                      style={{
                        padding: '6px 12px',
                        borderRadius: '6px',
                        border: `1px solid ${isDark ? '#047857' : '#A7F3D0'}`,
                        backgroundColor: isDark ? '#064E3B' : '#ECFDF5',
                        color: '#10B981',
                        fontSize: '12px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      {isFa ? '✓ تایید همه پیشنهادها' : '✓ Accept All Suggestions'}
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setUserFieldMappings({});
                        setConfirmedFields({});
                      }}
                      style={{
                        padding: '6px 12px',
                        borderRadius: '6px',
                        border: `1px solid ${theme.colors.border}`,
                        backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6',
                        color: theme.colors.textSecondary,
                        fontSize: '12px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      {isFa ? '✕ پاک کردن همه' : '✕ Clear All'}
                    </button>
                  </div>
                </div>

                {/* Duplicate Source Column Assignment Error Banner */}
                {duplicateSourceColumns.length > 0 && (
                  <div
                    style={{
                      marginBottom: '16px',
                      padding: '12px 16px',
                      borderRadius: '8px',
                      backgroundColor: isDark ? '#450A0A' : '#FEF2F2',
                      border: `1px solid ${isDark ? '#7F1D1D' : '#FCA5A5'}`,
                      color: isDark ? '#FECACA' : '#991B1B',
                      fontSize: '13px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}
                  >
                    <span style={{ fontSize: '18px' }}>🚫</span>
                    <div>
                      <strong>{isFa ? 'خطای انتساب تکراری ستون منبع:' : 'Duplicate Source Column Error:'}</strong>{' '}
                      {isFa
                        ? `ستون‌های (${duplicateSourceColumns.join('، ')}) به بیش از یک فیلد مقصد اختصاص یافته‌اند. هر ستون منبع تنها می‌تواند به یک فیلد متصل شود.`
                        : `Columns (${duplicateSourceColumns.join(', ')}) are assigned to multiple target fields. Each column can only map to one field.`}
                    </div>
                  </div>
                )}

                {/* Missing Required Fields Alert Banner */}
                {missingRequiredFields.length > 0 && (
                  <div
                    style={{
                      marginBottom: '16px',
                      padding: '12px 16px',
                      borderRadius: '8px',
                      backgroundColor: isDark ? '#78350F' : '#FFFBEB',
                      border: `1px solid ${isDark ? '#B45309' : '#FDE68A'}`,
                      color: isDark ? '#FDE68A' : '#92400E',
                      fontSize: '13px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}
                  >
                    <span style={{ fontSize: '18px' }}>⚠️</span>
                    <div>
                      <strong>{isFa ? 'فیلدهای ضروری متصل نشده:' : 'Unmapped Required Fields:'}</strong>{' '}
                      {missingRequiredFields.map((f) => (isFa ? f.label_fa : f.label_en)).join('، ')}
                    </div>
                  </div>
                )}

                {/* Conflicts Alert Banner */}
                {mappingData.conflicts && mappingData.conflicts.length > 0 && (
                  <div
                    style={{
                      marginBottom: '16px',
                      padding: '12px 16px',
                      borderRadius: '8px',
                      backgroundColor: isDark ? '#431407' : '#FFF7ED',
                      border: `1px solid ${isDark ? '#7C2D12' : '#FFEDD5'}`,
                      color: isDark ? '#FFEDD5' : '#9A3412',
                      fontSize: '13px',
                    }}
                  >
                    <div style={{ fontWeight: 600, marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span>⚡</span>
                      {isFa ? 'بررسی رقابت ستون‌ها (تداخل):' : 'Column Competition (Conflicts):'}
                    </div>
                    <ul style={{ margin: 0, paddingRight: isFa ? '20px' : 0, paddingLeft: isFa ? 0 : '20px', fontSize: '12px' }}>
                      {mappingData.conflicts.map((c, i) => (
                        <li key={i}>{isFa ? c.message_fa : c.message_en}</li>
                      ))}
                    </ul>
                  </div>
                )}

                {/* Ambiguous Headers Alert Banner */}
                {mappingData.ambiguous_headers && mappingData.ambiguous_headers.length > 0 && (
                  <div
                    style={{
                      marginBottom: '16px',
                      padding: '12px 16px',
                      borderRadius: '8px',
                      backgroundColor: isDark ? '#312E81' : '#EEF2FF',
                      border: `1px solid ${isDark ? '#4338CA' : '#C7D2FE'}`,
                      color: isDark ? '#E0E7FF' : '#3730A3',
                      fontSize: '13px',
                    }}
                  >
                    <div style={{ fontWeight: 600, marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span>ℹ️</span>
                      {isFa ? 'ستون‌های دارای ابهام (انتخاب خودکار انجام نشد):' : 'Ambiguous Headers (Left Unmapped):'}
                    </div>
                    <ul style={{ margin: 0, paddingRight: isFa ? '20px' : 0, paddingLeft: isFa ? 0 : '20px', fontSize: '12px' }}>
                      {mappingData.ambiguous_headers.map((a, i) => (
                        <li key={i}>{isFa ? a.message_fa : a.message_en}</li>
                      ))}
                    </ul>
                  </div>
                )}

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
                        const isDuplicate = currentSourceCol !== '' && duplicateSourceColumns.includes(currentSourceCol);
                        const isConfirmed = Boolean(confirmedFields[field.name]);

                        const suggestion = mappingData.suggested_mappings.find(
                          (s) => s.target_field === field.name || (currentSourceCol && s.source_column === currentSourceCol)
                        );
                        const categoryBadge = getMatchCategoryBadge(suggestion?.match_category);

                        return (
                          <tr key={field.name} style={{ borderBottom: `1px solid ${theme.colors.border}`, fontSize: '13px' }}>
                            <td style={{ padding: '12px 14px', fontWeight: 600 }}>
                              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                                <span>{isFa ? field.label_fa : field.label_en}</span>
                                {field.required && <span style={{ color: '#DC2626' }}>*</span>}
                              </div>
                              <div style={{ fontSize: '11px', color: theme.colors.textSecondary, fontFamily: 'monospace', marginTop: '2px' }}>
                                {field.name} ({field.type})
                              </div>
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
                                  setConfirmedFields((prev) => ({
                                    ...prev,
                                    [field.name]: true,
                                  }));
                                }}
                                style={{
                                  width: '100%',
                                  padding: '8px 10px',
                                  borderRadius: '6px',
                                  border: `1px solid ${
                                    isDuplicate
                                      ? '#EF4444'
                                      : currentSourceCol
                                      ? '#3B82F6'
                                      : theme.colors.border
                                  }`,
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

                              {/* Inline duplicate error */}
                              {isDuplicate && (
                                <div style={{ color: '#EF4444', fontSize: '11px', marginTop: '4px', fontWeight: 600 }}>
                                  {isFa ? '⚠️ این ستون به فیلد دیگری نیز اختصاص یافته است.' : '⚠️ This column is assigned to another field.'}
                                </div>
                              )}

                              {/* Smart Match category badge & user confirmation pill */}
                              {currentSourceCol && (
                                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '6px', flexWrap: 'wrap' }}>
                                  {isConfirmed ? (
                                    <span
                                      style={{
                                        fontSize: '10px',
                                        padding: '2px 8px',
                                        borderRadius: '12px',
                                        fontWeight: 600,
                                        backgroundColor: isDark ? '#064E3B' : '#ECFDF5',
                                        color: '#10B981',
                                        border: `1px solid ${isDark ? '#047857' : '#A7F3D0'}`,
                                      }}
                                    >
                                      {isFa ? '✓ تایید کاربر' : '✓ User Confirmed'}
                                    </span>
                                  ) : (
                                    <span
                                      style={{
                                        fontSize: '10px',
                                        padding: '2px 8px',
                                        borderRadius: '12px',
                                        fontWeight: 600,
                                        backgroundColor: categoryBadge.bg,
                                        color: categoryBadge.color,
                                        border: `1px solid ${categoryBadge.border}`,
                                      }}
                                    >
                                      {categoryBadge.label}
                                    </span>
                                  )}

                                  {/* Explanation reason */}
                                  {suggestion && (
                                    <span style={{ fontSize: '11px', color: theme.colors.textSecondary }}>
                                      {isFa ? suggestion.reason_fa : suggestion.reason_en}
                                    </span>
                                  )}
                                </div>
                              )}
                            </td>
                            <td style={{ padding: '12px 14px', color: theme.colors.textSecondary, fontSize: '12px' }}>
                              <div>{isFa ? field.description_fa : field.description_en}</div>
                              {field.aliases && field.aliases.length > 0 && (
                                <div style={{ fontSize: '11px', color: theme.colors.textSecondary, marginTop: '4px', opacity: 0.8 }}>
                                  <em>{isFa ? 'مترادف‌های شناخته‌شده: ' : 'Known aliases: '}</em>
                                  {field.aliases.slice(0, 4).join('، ')}
                                </div>
                              )}
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
                  <PrimaryButton
                    onClick={handleSaveMappingAndValidate}
                    disabled={isWizardLoading || duplicateSourceColumns.length > 0 || missingRequiredFields.length > 0}
                  >
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

                {/* Invalid Rows Warning Banner */}
                {previewData.invalid_rows > 0 && (
                  <div
                    style={{
                      marginBottom: '16px',
                      padding: '12px 16px',
                      borderRadius: '8px',
                      backgroundColor: isDark ? '#450A0A' : '#FEF2F2',
                      border: `1px solid ${isDark ? '#7F1D1D' : '#FCA5A5'}`,
                      color: isDark ? '#FECACA' : '#991B1B',
                      fontSize: '13px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}
                  >
                    <span style={{ fontSize: '18px' }}>⚠️</span>
                    <div>
                      <strong>{isFa ? 'توجه در اعتبارسنجی:' : 'Validation Alert:'}</strong>{' '}
                      {isFa
                        ? `تعداد ${previewData.invalid_rows} ردیف از این فایل دارای خطای ساختاری یا اعتبارسنجی هستند و در هنگام اجرای درون‌ریزی رد خواهند شد. جزئیات کامل خطاها و اقدام پیشنهادی در زیر درج شده است.`
                        : `${previewData.invalid_rows} rows have errors and will be skipped upon execution. See the detailed error list and suggested actions below.`}
                    </div>
                  </div>
                )}

                {/* Detailed Row-Level Validation Errors Table if any */}
                {jobErrorsList.length > 0 && (
                  <div style={{ marginBottom: '20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                      <h4 style={{ margin: 0, fontSize: '14px', fontWeight: 600, color: '#DC2626' }}>
                        {isFa ? `لیست خطاهای اعتبارسنجی ردیف‌ها (${jobErrorsList.length} مورد):` : `Row Validation Errors (${jobErrorsList.length} items):`}
                      </h4>
                      <span style={{ fontSize: '12px', color: theme.colors.textSecondary }}>
                        {isFa ? 'ردیف‌های نامعتبر وارد سامانه نخواهند شد' : 'Invalid rows will be skipped'}
                      </span>
                    </div>

                    <div style={{ border: `1px solid ${isDark ? '#7F1D1D' : '#FCA5A5'}`, borderRadius: '8px', maxHeight: '220px', overflowY: 'auto' }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', fontSize: '12px' }}>
                        <thead>
                          <tr style={{ backgroundColor: isDark ? '#450A0A' : '#FEF2F2', borderBottom: `1px solid ${isDark ? '#7F1D1D' : '#FCA5A5'}` }}>
                            <th style={{ padding: '8px 10px', width: '65px' }}>{isFa ? 'ردیف' : 'Row'}</th>
                            <th style={{ padding: '8px 10px', width: '120px' }}>{isFa ? 'فیلد مربوطه' : 'Field'}</th>
                            <th style={{ padding: '8px 10px', width: '130px' }}>{isFa ? 'کد خطا' : 'Error Code'}</th>
                            <th style={{ padding: '8px 10px' }}>{isFa ? 'شرح خطا' : 'Error Message'}</th>
                            <th style={{ padding: '8px 10px' }}>{isFa ? 'راهکار رفع خطا' : 'Suggested Action'}</th>
                          </tr>
                        </thead>
                        <tbody>
                          {jobErrorsList.map((err, idx) => (
                            <tr key={err.id || idx} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                              <td style={{ padding: '8px 10px', fontWeight: 600 }}>{err.row_number}</td>
                              <td style={{ padding: '8px 10px' }}>
                                <span style={{ padding: '2px 6px', borderRadius: '4px', backgroundColor: isDark ? theme.colors.surfaceElevated : '#F3F4F6', fontFamily: 'monospace' }}>
                                  {err.field_name || (isFa ? 'عمومی' : 'general')}
                                </span>
                              </td>
                              <td style={{ padding: '8px 10px', color: '#DC2626', fontFamily: 'monospace', fontSize: '11px' }}>
                                {err.error_code}
                              </td>
                              <td style={{ padding: '8px 10px', color: theme.colors.textPrimary }}>
                                {err.message}
                                {err.original_value && (
                                  <div style={{ fontSize: '11px', color: theme.colors.textSecondary, marginTop: '2px' }}>
                                    {isFa ? 'مقدار: ' : 'Value: '}
                                    <code>{err.original_value}</code>
                                  </div>
                                )}
                              </td>
                              <td style={{ padding: '8px 10px', color: '#0369A1', fontSize: '11px' }}>
                                {err.suggested_action || (isFa ? 'اصلاح مقدار در فایل اصلی اکسل/CSV' : 'Fix value in original spreadsheet')}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )}

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
                <div style={{ border: `1px solid ${theme.colors.border}`, borderRadius: '8px', maxHeight: '260px', overflowY: 'auto' }}>
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

                {/* Pre-Execution Confirmation & Finality Notice */}
                <div
                  style={{
                    marginTop: '20px',
                    padding: '12px 16px',
                    borderRadius: '8px',
                    backgroundColor: isDark ? '#422006' : '#FFFBEB',
                    border: `1px solid ${isDark ? '#B45309' : '#FCD34D'}`,
                    color: isDark ? '#FDE68A' : '#92400E',
                    fontSize: '13px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '10px',
                  }}
                >
                  <span style={{ fontSize: '20px' }}>🔒</span>
                  <div>
                    <strong>{isFa ? 'هشدار قطعی بودن و عدم تکرار (Finality):' : 'Finality Notice:'}</strong>{' '}
                    {isFa
                      ? `با تایید شما، تعداد ${previewData.valid_rows} رکورد معتبر درون‌ریزی خواهند شد و ردیف‌های دارای خطا رد می‌شوند. این عملیات در پایگاه داده قطعی بوده و امکان بازگشت خودکار یا اجرای مجدد آن وجود ندارد.`
                      : `Upon confirmation, ${previewData.valid_rows} valid records will be imported and invalid rows will be skipped. This action is atomic, final, and cannot be re-executed.`}
                  </div>
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

                <div style={{ marginTop: '8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                  <button
                    type="button"
                    onClick={() => void handleDownloadTemplate(selectedType)}
                    disabled={downloadingTemplateType === selectedType}
                    style={{
                      background: 'none',
                      border: 'none',
                      padding: 0,
                      color: '#2563EB',
                      fontSize: '12px',
                      fontWeight: 600,
                      cursor: 'pointer',
                      textDecoration: 'underline',
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: '4px',
                    }}
                  >
                    {downloadingTemplateType === selectedType
                      ? (isFa ? 'در حال دریافت فایل...' : 'Downloading...')
                      : (isFa ? '📥 دریافت فایل نمونه خام (CSV سازگار با اکسل)' : '📥 Download Blank CSV Template')}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setActiveTemplateType(selectedType);
                      setIsTemplatesModalOpen(true);
                    }}
                    style={{
                      background: 'none',
                      border: 'none',
                      padding: 0,
                      color: theme.colors.textSecondary,
                      fontSize: '12px',
                      cursor: 'pointer',
                      textDecoration: 'underline',
                    }}
                  >
                    {isFa ? 'راهنمای ستون‌ها' : 'Column specs'}
                  </button>
                </div>
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

      {/* Templates Catalog Modal */}
      {isTemplatesModalOpen && (
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
          onClick={() => setIsTemplatesModalOpen(false)}
        >
          <div
            style={{
              backgroundColor: isDark ? theme.colors.surface : '#FFFFFF',
              borderRadius: '16px',
              padding: '24px',
              maxWidth: '920px',
              width: '100%',
              maxHeight: '92vh',
              overflowY: 'auto',
              border: `1px solid ${theme.colors.border}`,
              boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
            }}
            onClick={(e) => e.stopPropagation()}
          >
            {/* Header */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
              <div>
                <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 700 }}>
                  {isFa ? 'قالب‌های آماده و استاندارد اکسل و CSV' : 'Standard Data Migration Templates'}
                </h3>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, marginTop: '4px' }}>
                  {isFa
                    ? 'دانلود قالب‌های خام همراه با انکودینگ UTF-8 BOM جهت نمایش بدون نقص حروف فارسی در اکسل ویندوز و مک'
                    : 'Download blank CSV templates with UTF-8 BOM for perfect Persian encoding in Microsoft Excel'}
                </div>
              </div>
              <button
                onClick={() => setIsTemplatesModalOpen(false)}
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

            {/* Type Selector Tabs */}
            <div
              style={{
                display: 'flex',
                gap: '8px',
                overflowX: 'auto',
                paddingBottom: '8px',
                marginBottom: '16px',
                borderBottom: `1px solid ${theme.colors.border}`,
              }}
            >
              {IMPORT_TYPES.map((t) => {
                const isSelected = activeTemplateType === t.value;
                return (
                  <button
                    key={t.value}
                    type="button"
                    onClick={() => setActiveTemplateType(t.value)}
                    style={{
                      padding: '8px 14px',
                      borderRadius: '8px',
                      border: `1px solid ${isSelected ? '#3B82F6' : theme.colors.border}`,
                      backgroundColor: isSelected ? (isDark ? '#1E3A8A' : '#EFF6FF') : (isDark ? theme.colors.surfaceElevated : '#F9FAFB'),
                      color: isSelected ? (isDark ? '#93C5FD' : '#1D4ED8') : theme.colors.textPrimary,
                      fontWeight: isSelected ? 600 : 500,
                      fontSize: '13px',
                      cursor: 'pointer',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {isFa ? t.fa : t.en}
                  </button>
                );
              })}
            </div>

            {/* Active Template Details */}
            {(() => {
              const activeTpl = templates.find((t) => t.type === activeTemplateType);
              if (!activeTpl) {
                return (
                  <div style={{ textAlign: 'center', padding: '32px', color: theme.colors.textSecondary }}>
                    {isFa ? 'در حال بارگذاری مشخصات قالب...' : 'Loading template specifications...'}
                  </div>
                );
              }

              return (
                <div>
                  {/* Top Bar with Title, Badges, and Download Button */}
                  <div
                    style={{
                      display: 'flex',
                      flexWrap: 'wrap',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '16px',
                      borderRadius: '10px',
                      backgroundColor: isDark ? theme.colors.surfaceElevated : '#F8FAFC',
                      border: `1px solid ${theme.colors.border}`,
                      marginBottom: '16px',
                    }}
                  >
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                        <span style={{ fontSize: '16px', fontWeight: 700 }}>
                          {isFa ? activeTpl.title_fa : activeTpl.title_en}
                        </span>
                        <code
                          style={{
                            fontSize: '11px',
                            padding: '2px 8px',
                            borderRadius: '4px',
                            backgroundColor: isDark ? '#374151' : '#E2E8F0',
                            color: theme.colors.textPrimary,
                          }}
                        >
                          {activeTpl.filename}
                        </code>
                        {activeTpl.is_multiline && (
                          <span
                            style={{
                              fontSize: '11px',
                              padding: '2px 8px',
                              borderRadius: '4px',
                              backgroundColor: '#FEF3C7',
                              color: '#92400E',
                              fontWeight: 600,
                            }}
                          >
                            {isFa ? 'چندسطری (چند قلم در فاکتور)' : 'Multi-Line Grouping'}
                          </span>
                        )}
                      </div>
                      <div style={{ fontSize: '13px', color: theme.colors.textSecondary }}>
                        {isFa ? activeTpl.description_fa : activeTpl.description_en}
                      </div>
                    </div>

                    <PrimaryButton
                      onClick={() => void handleDownloadTemplate(activeTpl.type, activeTpl.filename)}
                      disabled={downloadingTemplateType === activeTpl.type}
                    >
                      {downloadingTemplateType === activeTpl.type
                        ? (isFa ? 'در حال دانلود...' : 'Downloading...')
                        : (isFa ? '📥 دریافت فایل قالب CSV (UTF-8 BOM)' : '📥 Download CSV Template')}
                    </PrimaryButton>
                  </div>

                  {/* Domain Notes / Important Guidelines */}
                  {((isFa ? activeTpl.domain_notes_fa : activeTpl.domain_notes_en)?.length ?? 0) > 0 && (
                    <div
                      style={{
                        marginBottom: '16px',
                        padding: '12px 16px',
                        borderRadius: '8px',
                        backgroundColor: isDark ? '#1E293B' : '#F1F5F9',
                        border: `1px solid ${isDark ? '#334155' : '#CBD5E1'}`,
                      }}
                    >
                      <div style={{ fontSize: '13px', fontWeight: 600, marginBottom: '6px', color: isDark ? '#93C5FD' : '#1E40AF' }}>
                        💡 {isFa ? 'نکات کلیدی برای تکمیل این قالب:' : 'Key Guidelines for this Template:'}
                      </div>
                      <ul style={{ margin: 0, paddingRight: isFa ? '20px' : 0, paddingLeft: !isFa ? '20px' : 0, fontSize: '12px', lineHeight: '1.7', color: theme.colors.textPrimary }}>
                        {(isFa ? activeTpl.domain_notes_fa : activeTpl.domain_notes_en).map((note, idx) => (
                          <li key={idx}>{note}</li>
                        ))}
                      </ul>
                    </div>
                  )}

                  {/* Columns Definition Table */}
                  <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: 600 }}>
                    {isFa ? 'ساختار ستون‌های فایل و مقادیر مجاز:' : 'Column Specifications & Allowed Values:'}
                  </h4>
                  <div style={{ border: `1px solid ${theme.colors.border}`, borderRadius: '8px', overflowX: 'auto', marginBottom: '16px' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', fontSize: '12px' }}>
                      <thead>
                        <tr style={{ backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB', borderBottom: `1px solid ${theme.colors.border}` }}>
                          <th style={{ padding: '8px 12px' }}>{isFa ? 'نام ستون در فایل (Header)' : 'Spreadsheet Column'}</th>
                          <th style={{ padding: '8px 12px' }}>{isFa ? 'فیلد متناظر در سامانه' : 'Resto Field'}</th>
                          <th style={{ padding: '8px 12px' }}>{isFa ? 'وضعیت الزامی' : 'Requirement'}</th>
                          <th style={{ padding: '8px 12px' }}>{isFa ? 'نمونه مقدار معتبر' : 'Sample Value'}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {activeTpl.headers.map((header, idx) => {
                          const isReq = activeTpl.required?.[idx] ?? false;
                          const fieldKey = activeTpl.field_keys?.[idx] ?? '';
                          const sampleVal = activeTpl.sample_rows?.[0]?.[idx] ?? '—';
                          return (
                            <tr key={idx} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                              <td style={{ padding: '8px 12px', fontWeight: 600 }}>{header}</td>
                              <td style={{ padding: '8px 12px', fontFamily: 'monospace', color: '#2563EB' }}>{fieldKey}</td>
                              <td style={{ padding: '8px 12px' }}>
                                {isReq ? (
                                  <span style={{ color: '#DC2626', backgroundColor: '#FEE2E2', padding: '2px 6px', borderRadius: '4px', fontWeight: 600, fontSize: '11px' }}>
                                    {isFa ? 'الزامی' : 'Required'}
                                  </span>
                                ) : (
                                  <span style={{ color: '#4B5563', backgroundColor: '#F3F4F6', padding: '2px 6px', borderRadius: '4px', fontSize: '11px' }}>
                                    {isFa ? 'اختیاری' : 'Optional'}
                                  </span>
                                )}
                              </td>
                              <td style={{ padding: '8px 12px', color: theme.colors.textSecondary }}>
                                <code>{sampleVal}</code>
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>

                  {/* Sample Rows Preview */}
                  {activeTpl.sample_rows.length > 0 && (
                    <div>
                      <h4 style={{ margin: '0 0 8px 0', fontSize: '14px', fontWeight: 600 }}>
                        {isFa ? 'پیش‌نمایش داده‌های نمونه موجود در فایل:' : 'Sample Data Preview:'}
                      </h4>
                      <div style={{ border: `1px solid ${theme.colors.border}`, borderRadius: '8px', overflowX: 'auto', maxHeight: '180px' }}>
                        <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: isFa ? 'right' : 'left', fontSize: '12px' }}>
                          <thead>
                            <tr style={{ backgroundColor: isDark ? theme.colors.surfaceElevated : '#F9FAFB', borderBottom: `1px solid ${theme.colors.border}` }}>
                              {activeTpl.headers.map((h, i) => (
                                <th key={i} style={{ padding: '6px 10px', whiteSpace: 'nowrap' }}>{h}</th>
                              ))}
                            </tr>
                          </thead>
                          <tbody>
                            {activeTpl.sample_rows.map((row, rIdx) => (
                              <tr key={rIdx} style={{ borderBottom: `1px solid ${theme.colors.border}` }}>
                                {row.map((cell, cIdx) => (
                                  <td key={cIdx} style={{ padding: '6px 10px', whiteSpace: 'nowrap', color: theme.colors.textPrimary }}>
                                    {cell || '—'}
                                  </td>
                                ))}
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  )}

                  <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                    <SecondaryButton onClick={() => setIsTemplatesModalOpen(false)}>
                      {isFa ? 'بستن' : 'Close'}
                    </SecondaryButton>
                  </div>
                </div>
              );
            })()}
          </div>
        </div>
      )}
    </div>
  );
};

export default ImportCenterPage;
