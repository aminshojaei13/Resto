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
  },
  ApiError: class extends Error {
    constructor(msg: string) {
      super(msg);
    }
  },
}));

describe('ImportCenterPage Foundation', () => {
  beforeEach(() => {
    jest.clearAllMocks();
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
});
