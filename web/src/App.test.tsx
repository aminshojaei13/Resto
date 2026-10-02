import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ThemeProvider } from './theme/ThemeContext';
import { BusinessApp } from './business/BusinessApp';
import { PlatformAdminApp } from './platform-admin/PlatformAdminApp';
import { AuthProvider } from './auth/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/ResetPasswordPage';
import { AcceptInvitationPage } from './pages/AcceptInvitationPage';
import { setAccessToken, clearSession, setBusinessContext } from './auth/session';

const mockLogin = jest.fn();
const mockGetProfile = jest.fn();
const mockLogout = jest.fn();
const mockForgotPassword = jest.fn();
const mockResetPassword = jest.fn();
const mockGetInvitation = jest.fn();

jest.mock('./api/apiClient', () => ({
  ApiError: class ApiError extends Error {
    status: number;
    fieldErrors: Record<string, string[]>;
    constructor(message: string, status = 500, fieldErrors: Record<string, string[]> = {}) {
      super(message);
      this.status = status;
      this.fieldErrors = fieldErrors;
    }
  },
  apiClient: {
    login: (...args: unknown[]) => mockLogin(...args),
    getProfile: (...args: unknown[]) => mockGetProfile(...args),
    logout: (...args: unknown[]) => mockLogout(...args),
    logoutEverywhere: jest.fn().mockResolvedValue({}),
    forgotPassword: (...args: unknown[]) => mockForgotPassword(...args),
    resetPassword: (...args: unknown[]) => mockResetPassword(...args),
    getInvitation: (...args: unknown[]) => mockGetInvitation(...args),
    acceptInvitation: jest.fn().mockResolvedValue({ message: 'ok' }),
    getOrganizations: jest.fn().mockResolvedValue([]),
    getProducts: jest.fn().mockResolvedValue([]),
    getCustomers: jest.fn().mockResolvedValue([]),
    getSuppliers: jest.fn().mockResolvedValue([]),
    getPurchases: jest.fn().mockResolvedValue([]),
    getExpenses: jest.fn().mockResolvedValue([]),
    getImportedMessages: jest.fn().mockResolvedValue([]),
    getJournalEntries: jest.fn().mockResolvedValue([]),
    getAccountingSummary: jest
      .fn()
      .mockResolvedValue({ totalRevenue: 0, todayRevenue: 0, totalSalesCount: 0, todaySalesCount: 0 }),
    getStaff: jest.fn().mockResolvedValue({ members: [], pending_invitations: [], can_manage_staff: false }),
    inviteStaff: jest.fn().mockResolvedValue({ message: 'ok', invitation: {} }),
  },
  setUnauthorizedHandler: jest.fn(),
  getCurrentContext: () => ({ organizationId: 'org_1', storeId: 'store_1', warehouseId: 'wh_1' }),
  setTenantContext: jest.fn(),
}));

const OWNER = {
  id: 'usr_owner',
  firstName: 'محمد',
  lastName: 'رضایی',
  name: 'محمد رضایی',
  displayName: 'محمد رضایی',
  email: 'm.rezaei@example.com',
  phone: null,
  status: 'ACTIVE',
  role: 'OWNER',
  isPlatformAdmin: false,
  permissions: ['staff.manage', 'catalog.manage', 'inventory.adjust'],
  memberships: [
    {
      id: 'org_1',
      name: 'کافه رستو',
      role: 'OWNER',
      status: 'ACTIVE',
      stores: [{ id: 'store_1', name: 'فروشگاه مرکزی', warehouses: [{ id: 'wh_1', name: 'انبار مرکزی' }] }],
    },
  ],
  token: 'token-abc',
};

const CASHIER = {
  ...OWNER,
  id: 'usr_cashier',
  firstName: 'سارا',
  lastName: 'کریمی',
  name: 'سارا کریمی',
  displayName: 'سارا کریمی',
  email: 's.karimi@example.com',
  role: 'STAFF',
  isPlatformAdmin: false,
  permissions: ['sales.create', 'inventory.view'],
  token: 'token-cashier',
};

const renderWithTheme = (ui: React.ReactElement) =>
  render(
    <ThemeProvider>
      <AuthProvider>{ui}</AuthProvider>
    </ThemeProvider>
  );

const renderBusinessApp = () =>
  render(
    <ThemeProvider>
      <BusinessApp />
    </ThemeProvider>
  );

const renderPlatformApp = () =>
  render(
    <ThemeProvider>
      <PlatformAdminApp />
    </ThemeProvider>
  );

beforeEach(() => {
  jest.clearAllMocks();
  clearSession();
  window.history.pushState(null, '', '/login');
  mockLogout.mockResolvedValue({});
  mockGetProfile.mockRejectedValue(new Error('no session'));
});

describe('P9 — business sign in', () => {
  test('offers only an email and password form', () => {
    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    expect(screen.getByLabelText('ایمیل')).toBeInTheDocument();
    expect(screen.getByLabelText('رمز عبور')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'ورود' })).toBeInTheDocument();
  });

  test('exposes no demo, quick or test login control anywhere on the page', () => {
    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    const html = document.body.innerHTML;
    expect(html).not.toMatch(/demo/i);
    expect(html).not.toMatch(/ورود سریع/i);
    expect(html).not.toMatch(/test login/i);
    expect(html).not.toMatch(/usr_owner_1|usr_admin/);
  });

  test('supports password managers and does not obstruct them', () => {
    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    expect(screen.getByLabelText('ایمیل')).toHaveAttribute('autocomplete', 'username');
    expect(screen.getByLabelText('رمز عبور')).toHaveAttribute('autocomplete', 'current-password');
  });

  test('can reveal and hide the password', () => {
    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    const input = screen.getByLabelText('رمز عبور');
    expect(input).toHaveAttribute('type', 'password');

    fireEvent.click(screen.getByRole('button', { name: 'نمایش رمز عبور' }));
    expect(screen.getByLabelText('رمز عبور')).toHaveAttribute('type', 'text');
  });

  test('signs in with the credentials the person typed', async () => {
    mockLogin.mockResolvedValue(OWNER);
    const navigate = jest.fn();

    renderWithTheme(<LoginPage language="fa" navigate={navigate} />);

    fireEvent.change(screen.getByLabelText('ایمیل'), { target: { value: 'm.rezaei@example.com' } });
    fireEvent.change(screen.getByLabelText('رمز عبور'), { target: { value: 'Correct-Horse-9' } });
    fireEvent.click(screen.getByRole('button', { name: 'ورود' }));

    await waitFor(() =>
      expect(mockLogin).toHaveBeenCalledWith(
        expect.objectContaining({ email: 'm.rezaei@example.com', password: 'Correct-Horse-9' })
      )
    );
    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/app/dashboard'));
  });

  test('shows a generic error and never reveals whether the account exists', async () => {
    const { ApiError } = jest.requireMock('./api/apiClient');
    mockLogin.mockRejectedValue(
      new ApiError('ایمیل یا رمز عبور صحیح نیست.', 422, { email: ['ایمیل یا رمز عبور صحیح نیست.'] })
    );

    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    fireEvent.change(screen.getByLabelText('ایمیل'), { target: { value: 'nobody@example.com' } });
    fireEvent.change(screen.getByLabelText('رمز عبور'), { target: { value: 'Wrong-Horse-9' } });
    fireEvent.click(screen.getByRole('button', { name: 'ورود' }));

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    expect(screen.getByRole('alert').textContent).toContain('ایمیل یا رمز عبور صحیح نیست.');
    expect(screen.getByRole('alert').textContent).not.toMatch(/not found|does not exist|wrong password/i);
  });

  test('says so plainly when too many attempts were made', async () => {
    const { ApiError } = jest.requireMock('./api/apiClient');
    mockLogin.mockRejectedValue(new ApiError('too many', 429));

    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    fireEvent.change(screen.getByLabelText('ایمیل'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByLabelText('رمز عبور'), { target: { value: 'x' } });
    fireEvent.click(screen.getByRole('button', { name: 'ورود' }));

    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain('تلاش‌های ناموفق زیاد بوده است')
    );
  });
});

describe('P9 — identity', () => {
  test('the shell shows the exact name the server returned', async () => {
    setAccessToken('token-abc');
    mockGetProfile.mockResolvedValue(OWNER);

    renderBusinessApp();

    await waitFor(() => expect(screen.getByText(/محمد رضایی/)).toBeInTheDocument());
    expect(screen.queryByText(/مالک کسب‌وکار/)).not.toBeInTheDocument();
    expect(screen.queryByText('Business Owner')).not.toBeInTheDocument();
  });

  test('an employee is never shown as the business owner', async () => {
    setAccessToken('token-cashier');
    mockGetProfile.mockResolvedValue(CASHIER);

    renderBusinessApp();

    await waitFor(() => expect(screen.getByText(/سارا کریمی/)).toBeInTheDocument());
    expect(screen.queryByText(/محمد رضایی/)).not.toBeInTheDocument();
  });

  test('the name is re-read from the server rather than trusted from storage', async () => {
    setAccessToken('token-abc');
    // A cached name from an earlier session must never win over the server.
    window.localStorage.setItem('resto_auth_user', JSON.stringify({ name: 'کاربر قبلی' }));
    mockGetProfile.mockResolvedValue({ ...OWNER, displayName: 'نام تازه از سرور' });

    renderBusinessApp();

    await waitFor(() => expect(mockGetProfile).toHaveBeenCalled());
    await waitFor(() => expect(screen.getByText(/نام تازه از سرور/)).toBeInTheDocument());
    expect(screen.queryByText(/کاربر قبلی/)).not.toBeInTheDocument();
  });

  test('signing out clears the session and the name', async () => {
    setAccessToken('token-abc');
    mockGetProfile.mockResolvedValue(OWNER);

    renderBusinessApp();
    await waitFor(() => expect(screen.getByText(/محمد رضایی/)).toBeInTheDocument());

    fireEvent.click(screen.getByRole('button', { name: /خروج/ }));

    await waitFor(() => expect(mockLogout).toHaveBeenCalled());
    expect(window.localStorage.getItem('resto_access_token')).toBeNull();
    expect(window.localStorage.getItem('resto_auth_user')).toBeNull();
  });
});

describe('P9 — password recovery', () => {
  test('asks for an email and shows the reset code nowhere', () => {
    renderWithTheme(<ForgotPasswordPage language="fa" navigate={jest.fn()} />);

    expect(screen.getByLabelText('ایمیل')).toBeInTheDocument();
    expect(document.body.innerHTML).not.toMatch(/reset_code/);
    expect(document.body.innerHTML).not.toMatch(/123456/);
  });

  test('confirms the request without revealing whether the address exists', async () => {
    mockForgotPassword.mockResolvedValue({ message: 'ok' });

    renderWithTheme(<ForgotPasswordPage language="fa" navigate={jest.fn()} />);

    fireEvent.change(screen.getByLabelText('ایمیل'), { target: { value: 'ghost@example.com' } });
    fireEvent.click(screen.getByRole('button', { name: 'ارسال لینک بازیابی' }));

    await waitFor(() => expect(screen.getByRole('status')).toBeInTheDocument());
    expect(screen.getByRole('status').textContent).toContain('درخواست شما ثبت شد');
  });

  test('resets from the emailed link and never asks for a numeric code by default', async () => {
    mockResetPassword.mockResolvedValue({ message: 'changed' });

    renderWithTheme(
      <ResetPasswordPage
        language="fa"
        navigate={jest.fn()}
        search="?token=long-random-token&email=m.rezaei%40example.com"
      />
    );

    fireEvent.change(screen.getByLabelText('رمز عبور جدید'), { target: { value: 'Brand-New-Pass-77' } });
    fireEvent.change(screen.getByLabelText('تکرار رمز عبور جدید'), { target: { value: 'Brand-New-Pass-77' } });
    fireEvent.click(screen.getByRole('button', { name: 'ثبت رمز عبور جدید' }));

    await waitFor(() =>
      expect(mockResetPassword).toHaveBeenCalledWith(
        expect.objectContaining({
          email: 'm.rezaei@example.com',
          token: 'long-random-token',
          password: 'Brand-New-Pass-77',
        })
      )
    );
  });

  test('refuses to submit when the two passwords differ', async () => {
    renderWithTheme(<ResetPasswordPage language="fa" navigate={jest.fn()} search="?token=t&email=a@b.com" />);

    fireEvent.change(screen.getByLabelText('رمز عبور جدید'), { target: { value: 'One-Pass-1111' } });
    fireEvent.change(screen.getByLabelText('تکرار رمز عبور جدید'), { target: { value: 'Other-Pass-222' } });
    fireEvent.click(screen.getByRole('button', { name: 'ثبت رمز عبور جدید' }));

    await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('یکسان نیستند'));
    expect(mockResetPassword).not.toHaveBeenCalled();
  });
});

describe('P9 — staff invitation', () => {
  test('greets the invited person by their own name and business', async () => {
    mockGetInvitation.mockResolvedValue({
      invitation: {
        email: 'new.staff@example.com',
        first_name: 'رضا',
        last_name: 'جعفری',
        role: 'STAFF',
        organization_name: 'کافه رستو',
        status: 'PENDING',
        expires_at: '2099-01-01T00:00:00+00:00',
      },
    });

    renderWithTheme(<AcceptInvitationPage language="fa" navigate={jest.fn()} search="?token=invite-token" />);

    await waitFor(() => expect(screen.getByText(/رضا جعفری/)).toBeInTheDocument());
    expect(screen.getByText(/کافه رستو/)).toBeInTheDocument();
    expect(document.body.innerHTML).not.toMatch(/invite-token/);
  });

  test('reports an invalid or expired invitation honestly', async () => {
    mockGetInvitation.mockRejectedValue(new Error('gone'));

    renderWithTheme(<AcceptInvitationPage language="fa" navigate={jest.fn()} search="?token=stale" />);

    await waitFor(() =>
      expect(screen.getByRole('alert').textContent).toContain('نامعتبر یا منقضی شده است')
    );
  });
});

describe('P9 — language separation', () => {
  test('the English sign-in screen contains no Persian copy', () => {
    renderWithTheme(<LoginPage language="en" navigate={jest.fn()} />);

    expect(screen.getByRole('button', { name: 'Sign in' })).toBeInTheDocument();
    expect(screen.getByLabelText('Email')).toBeInTheDocument();

    const text = document.body.textContent ?? '';
    expect(text).not.toMatch(/[؀-ۿ]/);
  });

  test('the Persian sign-in screen contains no English labels', () => {
    renderWithTheme(<LoginPage language="fa" navigate={jest.fn()} />);

    const text = document.body.textContent ?? '';
    expect(text).not.toMatch(/Sign in|Email|Password|Login/);
  });
});

describe('P9 — no technical jargon in the business interface', () => {
  test('the signed-in business shell avoids platform terminology', async () => {
    setAccessToken('token-abc');
    mockGetProfile.mockResolvedValue(OWNER);

    renderBusinessApp();
    await waitFor(() => expect(screen.getByText(/محمد رضایی/)).toBeInTheDocument());

    const text = document.body.textContent ?? '';
    expect(text).not.toMatch(/SaaS|Tenant|OrganizationMembership|API|Sanctum|CRUD|endpoint|token|backend|middleware/i);
  });
});

describe('P9 — system administration console', () => {
  test('asks the server rather than inventing a session', async () => {
    mockLogin.mockResolvedValue({ ...OWNER, isPlatformAdmin: true, role: 'OWNER' });

    renderPlatformApp();

    fireEvent.change(screen.getByLabelText('ایمیل'), { target: { value: 'operator@resto.com' } });
    fireEvent.change(screen.getByLabelText('رمز عبور'), { target: { value: 'Str0ng-Pass-1' } });
    fireEvent.click(screen.getByRole('button', { name: 'ورود' }));

    await waitFor(() => expect(mockLogin).toHaveBeenCalled());
  });

  test('a normal business user is refused, with no quick-access button', async () => {
    setAccessToken('token-cashier');
    mockGetProfile.mockResolvedValue(CASHIER);

    renderPlatformApp();

    await waitFor(() => expect(screen.getByText(/دسترسی غیرمجاز/)).toBeInTheDocument());
    expect(document.body.innerHTML).not.toMatch(/Platform Admin Demo|ورود سریع/i);
  });

  test('a signed-in platform administrator reaches the console', async () => {
    setAccessToken('token-admin');
    mockGetProfile.mockResolvedValue({
      ...OWNER,
      isPlatformAdmin: true,
      displayName: 'مدیر سامانه',
    });

    renderPlatformApp();

    await waitFor(() => expect(screen.getByText(/مدیر سامانه/)).toBeInTheDocument());
  });
});

describe('P9 — business context', () => {
  test('the selected business, store and warehouse survive a reload as context only', () => {
    setBusinessContext({
      organizationId: 'org_1',
      organizationName: 'کافه رستو',
      role: 'OWNER',
      storeId: 'store_1',
      storeName: 'فروشگاه مرکزی',
      warehouseId: 'wh_1',
      warehouseName: 'انبار مرکزی',
    });

    const raw = window.localStorage.getItem('resto_business_context') ?? '';
    expect(raw).toContain('wh_1');
    expect(raw).not.toContain('محمد رضایی');
  });
});

describe('P9 — unauthenticated access', () => {
  test('an unauthenticated visitor cannot reach a business module', async () => {
    window.history.pushState(null, '', '/app/dashboard');

    renderBusinessApp();

    await waitFor(() => expect(screen.getByLabelText('ایمیل')).toBeInTheDocument());
    expect(mockGetProfile).not.toHaveBeenCalled();
  });

  test('a token that the server rejects does not leave a name on screen', async () => {
    setAccessToken('stale-token');
    mockGetProfile.mockRejectedValue(new Error('401'));

    renderBusinessApp();

    await waitFor(() => expect(screen.getByLabelText('ایمیل')).toBeInTheDocument());
    expect(screen.queryByText(/محمد رضایی/)).not.toBeInTheDocument();
  });
});

afterEach(() => {
  act(() => {
    window.localStorage.clear();
  });
});
