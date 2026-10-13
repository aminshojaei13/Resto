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
});
