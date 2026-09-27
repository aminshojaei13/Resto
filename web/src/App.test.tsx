import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { BusinessApp } from './business/BusinessApp';
import { PlatformAdminApp } from './platform-admin/PlatformAdminApp';
import { apiClient } from './api/apiClient';

// Mock apiClient
jest.mock('./api/apiClient', () => ({
  __esModule: true,
  setTenantContext: jest.fn(),
  apiClient: {
    registerBusiness: jest.fn(),
    getPlatformApplications: jest.fn(),
    approvePlatformApplication: jest.fn(),
    rejectPlatformApplication: jest.fn(),
    getAccountingSummary: jest.fn(),
    getJournalEntries: jest.fn(),
    getProducts: jest.fn(),
    getCustomers: jest.fn(),
    getSuppliers: jest.fn(),
    getPurchases: jest.fn(),
    getExpenses: jest.fn(),
    getImportedMessages: jest.fn(),
    getOrganizations: jest.fn(),
    getPlatformAuditLogs: jest.fn(),
  },
}));

describe('P5.2 Resto SaaS App & Runtime Separation Tests', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState(null, '', '/');

    (apiClient.registerBusiness as jest.Mock).mockResolvedValue({
      application_id: 'app_test_123',
      status: 'PENDING',
    });
    (apiClient.getPlatformApplications as jest.Mock).mockResolvedValue([
      {
        id: 'app_test_123',
        business_name: 'Test Retail Store',
        owner_name: 'Test Owner',
        email: 'test@example.com',
        phone: '1234567890',
        business_type: 'RETAIL',
        status: 'PENDING',
      },
    ]);
    (apiClient.approvePlatformApplication as jest.Mock).mockResolvedValue({
      organization: { id: 'org_test', name: 'Test Retail Store' },
      status: 'APPROVED',
    });
    (apiClient.rejectPlatformApplication as jest.Mock).mockResolvedValue({
      status: 'REJECTED',
    });
    (apiClient.getAccountingSummary as jest.Mock).mockResolvedValue({
      totalRevenue: 5000,
      todayRevenue: 1000,
      totalSalesCount: 20,
      todaySalesCount: 4,
    });
    (apiClient.getJournalEntries as jest.Mock).mockResolvedValue([]);
    (apiClient.getProducts as jest.Mock).mockResolvedValue([]);
    (apiClient.getCustomers as jest.Mock).mockResolvedValue([]);
    (apiClient.getSuppliers as jest.Mock).mockResolvedValue([]);
    (apiClient.getPurchases as jest.Mock).mockResolvedValue([]);
    (apiClient.getExpenses as jest.Mock).mockResolvedValue([]);
    (apiClient.getImportedMessages as jest.Mock).mockResolvedValue([]);
    (apiClient.getOrganizations as jest.Mock).mockResolvedValue([
      { id: 'org_apex', name: 'Apex Retail Group', code: 'APEX', currencySymbol: '$', currencyCode: 'USD', subscriptionTier: 'ENTERPRISE', stores: [] },
    ]);
    (apiClient.getPlatformAuditLogs as jest.Mock).mockResolvedValue([
      { id: 'log_1', action: 'business.application.created', entity_type: 'BusinessApplication', entity_id: 'app_1', details: 'Submitted', created_at: new Date().toISOString() },
    ]);
  });

  test('1. Business app starts and renders business landing', async () => {
    window.history.pushState(null, '', '/register');
    await act(async () => {
      render(<BusinessApp />);
    });

    expect(screen.getByText(/ثبت‌نام و راه‌اندازی کسب‌وکار در رستو/i)).toBeInTheDocument();
  });

  test('2. Platform Admin app starts and renders operator portal', async () => {
    window.history.pushState(null, '', '/login');
    await act(async () => {
      render(<PlatformAdminApp />);
    });

    expect(screen.getByText(/ورود به پنل راهبری پلتفرم/i)).toBeInTheDocument();
  });

  test('3. Business registration works on Business App', async () => {
    window.history.pushState(null, '', '/register');
    await act(async () => {
      render(<BusinessApp />);
    });

    await act(async () => {
      fireEvent.change(screen.getByPlaceholderText(/مثال: کافه رستوران گرند/i), {
        target: { value: 'Test Retail Store' },
      });
      fireEvent.change(screen.getByPlaceholderText(/رضا علوی/i), {
        target: { value: 'Test Owner' },
      });
      fireEvent.change(screen.getByPlaceholderText(/reza@grandcoffee.com/i), {
        target: { value: 'test@example.com' },
      });
    });

    const submitBtn = screen.getByRole('button', { name: /ثبت درخواست ایجاد سازمان/i });
    const form = submitBtn.closest('form')!;

    await act(async () => {
      fireEvent.submit(form);
    });

    await waitFor(() => {
      expect(apiClient.registerBusiness).toHaveBeenCalled();
      expect(screen.getByText(/درخواست شما با موفقیت ثبت گردید/i)).toBeInTheDocument();
    });
  });

  test('4. Business login works on Business App', async () => {
    window.history.pushState(null, '', '/login');
    await act(async () => {
      render(<BusinessApp />);
    });

    expect(screen.getByText(/ورود به سامانه کسب‌وکار رستو/i)).toBeInTheDocument();

    const quickBtn = screen.getByRole('button', { name: /ورود سریع به عنوان مالک سازمان/i });
    await act(async () => {
      fireEvent.click(quickBtn);
    });

    expect(screen.getByText(/نسخه فعال \(SaaS Tenant\)/i)).toBeInTheDocument();
  });

  test('5. Tenant dashboard works on Business App for authenticated tenant user', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_owner_1',
        name: 'Reza Alavi',
        email: 'reza@grandcoffee.com',
        role: 'Owner',
        isPlatformAdmin: false,
      })
    );
    window.history.pushState(null, '', '/app/dashboard');

    await act(async () => {
      render(<BusinessApp />);
    });

    expect(screen.getByText(/نسخه فعال \(SaaS Tenant\)/i)).toBeInTheDocument();
  });

  test('6. Platform Admin login works on Platform Admin App (port 3001)', async () => {
    window.history.pushState(null, '', '/login');
    await act(async () => {
      render(<PlatformAdminApp />);
    });

    const quickBtn = screen.getByRole('button', { name: /ورود مستقیم تست/i });
    await act(async () => {
      fireEvent.click(quickBtn);
    });

    expect(screen.getByText(/مدیریت درخواست‌های ثبت سازمان/i)).toBeInTheDocument();
  });

  test('7. Platform applications review works on Platform Admin App', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_admin',
        name: 'Platform Admin',
        email: 'admin@resto.com',
        role: 'PlatformAdmin',
        isPlatformAdmin: true,
      })
    );
    window.history.pushState(null, '', '/applications');

    await act(async () => {
      render(<PlatformAdminApp />);
    });

    expect(screen.getByText(/مدیریت درخواست‌های ثبت سازمان/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(apiClient.getPlatformApplications).toHaveBeenCalled();
      expect(screen.getByText(/Test Retail Store/i)).toBeInTheDocument();
    });
  });

  test('8 & 9. Normal tenant user cannot gain Platform Admin UI access on port 3001', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_owner_1',
        name: 'Reza Alavi',
        email: 'reza@grandcoffee.com',
        role: 'Owner',
        isPlatformAdmin: false,
      })
    );
    window.history.pushState(null, '', '/applications');

    await act(async () => {
      render(<PlatformAdminApp />);
    });

    expect(screen.getByText(/خطای دسترسی غیرمجاز راهبر پلتفرم/i)).toBeInTheDocument();
  });

  test('10 & 11. Platform Admin can access Platform Admin UI and management APIs', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_admin',
        name: 'Platform Admin',
        email: 'admin@resto.com',
        role: 'PlatformAdmin',
        isPlatformAdmin: true,
      })
    );
    window.history.pushState(null, '', '/tenants');

    await act(async () => {
      render(<PlatformAdminApp />);
    });

    expect(screen.getByText(/مدیریت تننت‌ها و سازمان‌های فعال/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(apiClient.getOrganizations).toHaveBeenCalled();
      expect(screen.getByText(/Apex Retail Group/i)).toBeInTheDocument();
    });
  });

  test('12. Platform Admin audit trail works on Platform Admin App', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_admin',
        name: 'Platform Admin',
        email: 'admin@resto.com',
        role: 'PlatformAdmin',
        isPlatformAdmin: true,
      })
    );
    window.history.pushState(null, '', '/audit');

    await act(async () => {
      render(<PlatformAdminApp />);
    });

    expect(screen.getByText(/دفتر ثبت رویدادهای راهبری/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(apiClient.getPlatformAuditLogs).toHaveBeenCalled();
      expect(screen.getByText(/business.application.created/i)).toBeInTheDocument();
    });
  });

  test('13. No Platform Admin navigation appears in Business App', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_owner_1',
        name: 'Reza Alavi',
        email: 'reza@grandcoffee.com',
        role: 'Owner',
        isPlatformAdmin: false,
      })
    );
    window.history.pushState(null, '', '/app/dashboard');

    await act(async () => {
      render(<BusinessApp />);
    });

    expect(screen.queryByText(/مدیریت پلتفرم/i)).not.toBeInTheDocument();
  });

  test('14. No tenant business navigation appears in Platform Admin App', async () => {
    localStorage.setItem(
      'resto_auth_user',
      JSON.stringify({
        id: 'usr_admin',
        name: 'Platform Admin',
        email: 'admin@resto.com',
        role: 'PlatformAdmin',
        isPlatformAdmin: true,
      })
    );
    window.history.pushState(null, '', '/applications');

    await act(async () => {
      render(<PlatformAdminApp />);
    });

    expect(screen.queryByText(/فروشگاه و POS/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/انبار و موجودی/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/دفتر کل حسابداری/i)).not.toBeInTheDocument();
  });

  test('15. Legacy Platform Admin route on Business App redirects with notice', async () => {
    window.history.pushState(null, '', '/platform/applications');

    await act(async () => {
      render(<BusinessApp />);
    });

    expect(screen.getByText(/انتقال بخش مدیریت پلتفرم به سامانه اپراتور/i)).toBeInTheDocument();
  });
});
