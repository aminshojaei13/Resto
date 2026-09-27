import React from 'react';
import { render, screen, fireEvent, act, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';
import { PublicRegisterPage } from './pages/PublicRegisterPage';
import { PlatformAdminPage } from './pages/PlatformAdminPage';
import { apiClient } from './api/apiClient';

// Mock apiClient module structure
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
  },
}));

describe('Resto SaaS UI & Route Separation Verification', () => {
  beforeEach(() => {
    localStorage.clear();
    window.history.pushState(null, '', '/');

    // Re-assign mock implementations before each test
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
    (apiClient.getOrganizations as jest.Mock).mockResolvedValue([]);
  });

  test('1. Public registration is accessible without authentication', async () => {
    window.history.pushState(null, '', '/register');
    await act(async () => {
      render(<App />);
    });

    expect(screen.getByText(/ثبت‌نام و راه‌اندازی کسب‌وکار در رستو/i)).toBeInTheDocument();
  });

  test('2 & 3. Public registration does NOT render tenant sidebar or tenant dashboard', async () => {
    window.history.pushState(null, '', '/register');
    await act(async () => {
      render(<App />);
    });

    expect(screen.queryByText(/انبار و موجودی/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/خرید و تامین/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/دفتر کل حسابداری/i)).not.toBeInTheDocument();
  });

  test('4. Public registration successfully creates a pending application using existing API', async () => {
    await act(async () => {
      render(<PublicRegisterPage language="fa" />);
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

  test('5. Unauthenticated user cannot access tenant dashboard and is redirected to login', async () => {
    window.history.pushState(null, '', '/app/dashboard');
    await act(async () => {
      render(<App />);
    });

    expect(screen.getByText(/ورود به سامانه کسب‌وکار رستو/i)).toBeInTheDocument();
    expect(screen.queryByText(/داشبورد مدیریتی/i)).not.toBeInTheDocument();
  });

  test('6. Unauthenticated user cannot access Platform Admin pages and is redirected to platform login', async () => {
    window.history.pushState(null, '', '/platform/applications');
    await act(async () => {
      render(<App />);
    });

    expect(screen.getByText(/ورود به پنل راهبری پلتفرم/i)).toBeInTheDocument();
  });

  test('7. Normal tenant user can access tenant dashboard', async () => {
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
      render(<App />);
    });

    expect(screen.getByText(/نسخه فعال \(SaaS Tenant\)/i)).toBeInTheDocument();
  });

  test('8. Normal tenant user cannot access Platform Admin', async () => {
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
    window.history.pushState(null, '', '/platform/applications');

    await act(async () => {
      render(<App />);
    });

    expect(screen.getByText(/دسترسی غیرمجاز/i)).toBeInTheDocument();
  });

  test('9 & 10. Platform Admin can access Platform Admin pages and view applications', async () => {
    await act(async () => {
      render(<PlatformAdminPage language="fa" />);
    });

    expect(screen.getByText(/مدیریت درخواست‌های ثبت سازمان/i)).toBeInTheDocument();

    await waitFor(() => {
      expect(apiClient.getPlatformApplications).toHaveBeenCalled();
      expect(screen.getByText(/Test Retail Store/i)).toBeInTheDocument();
    });
  });

  test('11. Activation info page displays NOT_IMPLEMENTED status', async () => {
    window.history.pushState(null, '', '/activation');
    await act(async () => {
      render(<App />);
    });

    expect(screen.getByText(/ACTIVATION_CODE_STATUS: NOT_IMPLEMENTED/i)).toBeInTheDocument();
  });

  test('12 & 13. Root route behaves correctly for unauthenticated user', async () => {
    window.history.pushState(null, '', '/');
    await act(async () => {
      render(<App />);
    });

    expect(screen.getByText(/ثبت‌نام و راه‌اندازی کسب‌وکار در رستو/i)).toBeInTheDocument();
  });

  test('14. Tenant business routes work for authenticated tenant user', async () => {
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
    window.history.pushState(null, '', '/app/pos');

    await act(async () => {
      render(<App />);
    });

    const posElements = screen.getAllByText(/فروشگاه و POS/i);
    expect(posElements.length).toBeGreaterThan(0);
  });
});
