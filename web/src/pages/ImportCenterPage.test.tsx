import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ImportCenterPage } from './ImportCenterPage';
import { apiClient } from '../api/apiClient';

jest.mock('../api/apiClient', () => ({
  apiClient: {
    getImportJobs: jest.fn(),
    getImportJob: jest.fn(),
    createImportJob: jest.fn(),
    cancelImportJob: jest.fn(),
    getImportJobProgress: jest.fn(),
    getImportJobErrors: jest.fn(),
    getImportTemplates: jest.fn(),
    downloadImportTemplate: jest.fn(),
    getImportMapping: jest.fn(),
    saveImportMapping: jest.fn(),
    validateImportJob: jest.fn(),
    getImportPreview: jest.fn(),
    executeImportJob: jest.fn(),
  },
  ApiError: class extends Error {
    constructor(msg: string) {
      super(msg);
    }
  },
}));

const mockTemplates = [
  {
    type: 'products',
    filename: 'resto_template_products.csv',
    title_fa: 'قالب کالاها و محصولات',
    title_en: 'Products Template',
    description_fa: 'کاتالوگ کالاها، بارکد، قیمت و دسته‌بندی.',
    description_en: 'Product master catalog.',
    is_multiline: false,
    headers: ['نام کالا', 'کد کالا', 'قیمت فروش'],
    field_keys: ['name', 'sku', 'selling_price'],
    required: [true, false, true],
    sample_rows: [['قهوه اسپرسو', 'SKU-001', '85000']],
    domain_notes_fa: ['نام کالا الزامی است.', 'قیمت باید عددی مثبت باشد.'],
    domain_notes_en: ['Product Name is required.'],
  },
  {
    type: 'orders',
    filename: 'resto_template_historical_orders.csv',
    title_fa: 'قالب سوابق سفارش‌ها (تاریخی)',
    title_en: 'Historical Orders Template',
    description_fa: 'ورود سوابق فاکتورهای فروش گذشته.',
    description_en: 'Past customer order history.',
    is_multiline: true,
    headers: ['شناسه سفارش', 'کد کالا', 'تعداد', 'قیمت واحد', 'تاریخ'],
    field_keys: ['order_id', 'sku', 'quantity', 'unit_price', 'date'],
    required: [true, true, true, true, true],
    sample_rows: [
      ['ORD-101', 'SKU-001', '2', '85000', '1403/06/10'],
      ['ORD-101', 'SKU-002', '1', '40000', '1403/06/10'],
    ],
    domain_notes_fa: ['سفارش‌های تاریخی بر موجودی فعلی اثری ندارند.'],
    domain_notes_en: ['Historical orders do not alter live inventory.'],
  },
];

describe('ImportCenterPage Foundation & UX', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    (apiClient.getImportTemplates as jest.Mock).mockResolvedValue(mockTemplates);
  });

  test('renders page title and empty state when no jobs exist', async () => {
    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [],
      current_page: 1,
      total: 0,
    });

    render(<ImportCenterPage language="fa" />);

    expect(screen.getByText('مرکز درون‌ریزی داده‌ها')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('هیچ فایل درون‌ریزی ثبت نشده است')).toBeInTheDocument();
    });
  });

  test('renders jobs list with filename, format, status and counters', async () => {
    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [
        {
          id: 'job-123',
          original_filename: 'products_v1.xlsx',
          detected_format: 'xlsx',
          import_type: 'products',
          status: 'mapping_required',
          total_rows: 250,
          file_size: 102400,
          created_at: new Date().toISOString(),
        },
      ],
      current_page: 1,
      total: 1,
    });

    render(<ImportCenterPage language="fa" />);

    await waitFor(() => {
      expect(screen.getByText('products_v1.xlsx')).toBeInTheDocument();
      expect(screen.getByText('XLSX')).toBeInTheDocument();
      expect(screen.getByText('نیازمند تطبیق ستون‌ها')).toBeInTheDocument();
      expect(screen.getByText('250')).toBeInTheDocument();
    });
  });

  test('opens upload modal and allows selecting file and type', async () => {
    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [],
      current_page: 1,
      total: 0,
    });

    render(<ImportCenterPage language="fa" />);

    const openBtn = screen.getByText('+ درون‌ریزی فایل جدید');
    fireEvent.click(openBtn);

    expect(screen.getByText('درون‌ریزی فایل جدید')).toBeInTheDocument();
    expect(screen.getByText('انتخاب فایل (.xlsx یا .csv)')).toBeInTheDocument();
  });

  test('toggles 6-step user guide and shows domain integrity rules', async () => {
    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [],
      current_page: 1,
      total: 0,
    });

    render(<ImportCenterPage language="fa" />);

    const toggleBtn = screen.getByText('▼ مشاهده راهنما');
    expect(toggleBtn).toBeInTheDocument();

    // Expand guide
    fireEvent.click(toggleBtn);

    expect(screen.getByText('مراحل ۶گانه آماده‌سازی و درون‌ریزی اطلاعات:')).toBeInTheDocument();
    expect(screen.getByText('۱. دانلود قالب آماده CSV')).toBeInTheDocument();
    expect(screen.getByText('۲. پر کردن داده‌ها و اقلام چندسطری')).toBeInTheDocument();
    expect(screen.getByText('۶. اجرای قطعی و نهایی')).toBeInTheDocument();
    expect(screen.getByText('سفارش‌ها و خریدهای تاریخی (HISTORICAL_RECORD_ONLY):')).toBeInTheDocument();

    // Collapse guide
    const collapseBtn = screen.getByText('▲ بستن راهنما');
    fireEvent.click(collapseBtn);
    expect(screen.queryByText('مراحل ۶گانه آماده‌سازی و درون‌ریزی اطلاعات:')).not.toBeInTheDocument();
  });

  test('opens templates modal, displays column specs and triggers download', async () => {
    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [],
      current_page: 1,
      total: 0,
    });
    (apiClient.downloadImportTemplate as jest.Mock).mockResolvedValueOnce(undefined);

    render(<ImportCenterPage language="fa" />);

    const openTemplatesBtn = screen.getByText('📥 قالب‌های آماده اکسل / CSV');
    fireEvent.click(openTemplatesBtn);

    await waitFor(() => {
      expect(screen.getByText('قالب‌های آماده و استاندارد اکسل و CSV')).toBeInTheDocument();
      expect(screen.getByText('قالب کالاها و محصولات')).toBeInTheDocument();
      expect(screen.getByText('resto_template_products.csv')).toBeInTheDocument();
      expect(screen.getByText('نام کالا الزامی است.')).toBeInTheDocument();
    });

    // Click download button
    const downloadBtn = screen.getByText('📥 دریافت فایل قالب CSV (UTF-8 BOM)');
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(apiClient.downloadImportTemplate).toHaveBeenCalledWith('products', 'resto_template_products.csv');
    });
  });

  test('displays smart mapping suggestions with badges, explanations, and bulk actions', async () => {
    const mockJob = {
      id: 'job-sm-1',
      original_filename: 'catalog.csv',
      detected_format: 'csv',
      import_type: 'products',
      status: 'mapping_required',
      total_rows: 10,
      file_size: 2048,
      created_at: new Date().toISOString(),
    };

    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [mockJob],
      current_page: 1,
      total: 1,
    });
    (apiClient.getImportJob as jest.Mock).mockResolvedValueOnce({ job: mockJob });
    (apiClient.getImportMapping as jest.Mock).mockResolvedValueOnce({
      job_id: 'job-sm-1',
      import_type: 'products',
      status: 'mapping_required',
      target_fields: [
        { name: 'name', label_fa: 'نام کالا', label_en: 'Product Name', type: 'string', required: true, description_fa: 'عنوان کالا', description_en: 'Item Title', aliases: ['کالا'] },
        { name: 'sku', label_fa: 'کد کالا / SKU', label_en: 'SKU', type: 'string', required: true, description_fa: 'شناسه کالا', description_en: 'Item SKU', aliases: ['کد فنی'] },
        { name: 'price', label_fa: 'قیمت فروش', label_en: 'Price', type: 'numeric', required: true, description_fa: 'قیمت کالا', description_en: 'Item Price', aliases: ['فی فروش'] },
      ],
      datasets: [],
      active_sheet_index: 0,
      headers: ['نام کالا', 'کد کالا', 'فی فروش'],
      sample_rows: [['پیتزا', 'PIZ-01', '120000']],
      suggested_mappings: [
        { source_column: 'نام کالا', source_index: 0, target_field: 'name', confidence: 1.0, match_category: 'exact_canonical', reason_fa: 'تطبیق قطعی با نام رسمی فیلد' },
        { source_column: 'کد کالا', source_index: 1, target_field: 'sku', confidence: 0.95, match_category: 'exact_alias', reason_fa: 'تطبیق دقیق با عنوان مترادف' },
        { source_column: 'فی فروش', source_index: 2, target_field: 'price', confidence: 0.90, match_category: 'normalized_alias', reason_fa: 'تطبیق نرمال‌شده با عنوان مترادف' },
      ],
      conflicts: [],
      ambiguous_headers: [],
      unmapped_required_fields: [],
      current_mapping: null,
    });

    render(<ImportCenterPage language="fa" />);

    await waitFor(() => {
      expect(screen.getByText('catalog.csv')).toBeInTheDocument();
    });

    // Open mapping wizard
    const openWizardBtn = screen.getByText('تطبیق و بررسی');
    fireEvent.click(openWizardBtn);

    await waitFor(() => {
      // Badges
      expect(screen.getByText('تطبیق قطعی (۱۰۰٪)')).toBeInTheDocument();
      expect(screen.getByText('مترادف دقیق (۹۵٪)')).toBeInTheDocument();
      expect(screen.getByText('مترادف استاندارد (۹۰٪)')).toBeInTheDocument();
      // Explanations
      expect(screen.getByText('تطبیق قطعی با نام رسمی فیلد')).toBeInTheDocument();
      expect(screen.getByText('تطبیق دقیق با عنوان مترادف')).toBeInTheDocument();
      expect(screen.getByText('تطبیق نرمال‌شده با عنوان مترادف')).toBeInTheDocument();
    });

    // Test Clear All
    const clearAllBtn = screen.getByText('✕ پاک کردن همه');
    fireEvent.click(clearAllBtn);

    // After clearing, required fields missing banner should appear
    await waitFor(() => {
      expect(screen.getByText('فیلدهای ضروری متصل نشده:')).toBeInTheDocument();
    });

    // Test Accept All Suggestions
    const acceptAllBtn = screen.getByText('✓ تایید همه پیشنهادها');
    fireEvent.click(acceptAllBtn);

    await waitFor(() => {
      // All mapped rows now have User Confirmed pill
      expect(screen.getAllByText('✓ تایید کاربر')).toHaveLength(3);
    });
  });

  test('detects duplicate source column assignment error and prevents submission', async () => {
    const mockJob = {
      id: 'job-sm-2',
      original_filename: 'catalog2.csv',
      detected_format: 'csv',
      import_type: 'products',
      status: 'mapping_required',
      total_rows: 5,
      file_size: 1024,
      created_at: new Date().toISOString(),
    };

    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [mockJob],
      current_page: 1,
      total: 1,
    });
    (apiClient.getImportJob as jest.Mock).mockResolvedValueOnce({ job: mockJob });
    (apiClient.getImportMapping as jest.Mock).mockResolvedValueOnce({
      job_id: 'job-sm-2',
      import_type: 'products',
      status: 'mapping_required',
      target_fields: [
        { name: 'name', label_fa: 'نام کالا', label_en: 'Product Name', type: 'string', required: true, description_fa: 'عنوان', description_en: 'Title', aliases: [] },
        { name: 'sku', label_fa: 'کد کالا / SKU', label_en: 'SKU', type: 'string', required: true, description_fa: 'کد', description_en: 'SKU', aliases: [] },
      ],
      datasets: [],
      active_sheet_index: 0,
      headers: ['ستون الف', 'ستون ب'],
      sample_rows: [['ردیف ۱', 'ردیف ۲']],
      suggested_mappings: [
        { source_column: 'ستون الف', source_index: 0, target_field: 'name', confidence: 1.0, match_category: 'exact_canonical' },
        { source_column: 'ستون ب', source_index: 1, target_field: 'sku', confidence: 1.0, match_category: 'exact_canonical' },
      ],
      conflicts: [],
      ambiguous_headers: [],
      unmapped_required_fields: [],
      current_mapping: null,
    });

    render(<ImportCenterPage language="fa" />);

    await waitFor(() => {
      expect(screen.getByText('catalog2.csv')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('تطبیق و بررسی'));

    await waitFor(() => {
      expect(screen.getByText('نام کالا')).toBeInTheDocument();
    });

    // Change the second select to also select "ستون الف", creating a duplicate
    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[1], { target: { value: 'ستون الف' } });

    await waitFor(() => {
      // Duplicate error banner must appear
      expect(screen.getByText('خطای انتساب تکراری ستون منبع:')).toBeInTheDocument();
      // Inline duplicate error messages must appear
      expect(screen.getAllByText('⚠️ این ستون به فیلد دیگری نیز اختصاص یافته است.')).toHaveLength(2);
      // Submit button must be disabled
      const submitBtn = screen.getByText('تایید تطبیق و اعتبارسنجی داده‌ها');
      expect(submitBtn).toBeDisabled();
    });
  });

  test('displays conflict and ambiguous header alert banners', async () => {
    const mockJob = {
      id: 'job-sm-3',
      original_filename: 'catalog3.csv',
      detected_format: 'csv',
      import_type: 'products',
      status: 'mapping_required',
      total_rows: 5,
      file_size: 1024,
      created_at: new Date().toISOString(),
    };

    (apiClient.getImportJobs as jest.Mock).mockResolvedValueOnce({
      data: [mockJob],
      current_page: 1,
      total: 1,
    });
    (apiClient.getImportJob as jest.Mock).mockResolvedValueOnce({ job: mockJob });
    (apiClient.getImportMapping as jest.Mock).mockResolvedValueOnce({
      job_id: 'job-sm-3',
      import_type: 'products',
      status: 'mapping_required',
      target_fields: [
        { name: 'name', label_fa: 'نام کالا', label_en: 'Product Name', type: 'string', required: true, description_fa: 'عنوان', description_en: 'Title', aliases: [] },
      ],
      datasets: [],
      active_sheet_index: 0,
      headers: ['کد_نامشخص', 'کد ۱', 'کد ۲'],
      sample_rows: [],
      suggested_mappings: [],
      conflicts: [
        {
          target_field: 'sku',
          target_label_fa: 'کد کالا',
          target_label_en: 'SKU',
          competing_columns: ['کد ۱', 'کد ۲'],
          dominant_column: null,
          message_fa: 'ستون‌های کد ۱ و کد ۲ برای فیلد مقصد رقابت دارند.',
          message_en: 'Columns compete.',
        },
      ],
      ambiguous_headers: [
        {
          source_column: 'کد_نامشخص',
          source_index: 0,
          candidate_fields: ['sku', 'name'],
          candidate_labels_fa: ['کد کالا', 'نام کالا'],
          confidence: 0.8,
          message_fa: 'ستون کد_نامشخص با چندین فیلد مقصد مطابقت یکسان دارد.',
          message_en: 'Ambiguous column.',
        },
      ],
      unmapped_required_fields: [
        { name: 'name', label_fa: 'نام کالا', label_en: 'Product Name', type: 'string', description_fa: '', description_en: '' },
      ],
      current_mapping: null,
    });

    render(<ImportCenterPage language="fa" />);

    await waitFor(() => {
      expect(screen.getByText('catalog3.csv')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText('تطبیق و بررسی'));

    await waitFor(() => {
      expect(screen.getByText('بررسی رقابت ستون‌ها (تداخل):')).toBeInTheDocument();
      expect(screen.getByText('ستون‌های کد ۱ و کد ۲ برای فیلد مقصد رقابت دارند.')).toBeInTheDocument();
      expect(screen.getByText('ستون‌های دارای ابهام (انتخاب خودکار انجام نشد):')).toBeInTheDocument();
      expect(screen.getByText('ستون کد_نامشخص با چندین فیلد مقصد مطابقت یکسان دارد.')).toBeInTheDocument();
    });
  });
});
