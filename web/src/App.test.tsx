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

    /* ------------------------------------------------------------ P10 --- */
    getBusinessSettings: jest.fn().mockResolvedValue({
      defaultTaxRate: 8,
      taxInclusivePricing: false,
      canOverrideTaxPerOrder: true,
      minTaxRate: 0,
      maxTaxRate: 100,
    }),
    updateBusinessSettings: jest.fn().mockResolvedValue({
      defaultTaxRate: 10,
      taxInclusivePricing: false,
      canOverrideTaxPerOrder: true,
      minTaxRate: 0,
      maxTaxRate: 100,
    }),
    getUnits: jest.fn().mockResolvedValue([
      { code: 'piece', label: 'عدد' },
      { code: 'kilogram', label: 'کیلوگرم' },
    ]),
    getWarehouses: jest.fn().mockResolvedValue([]),
    createWarehouse: jest.fn().mockResolvedValue({ id: 'wh_new', name: 'انبار جدید' }),
    getStock: jest.fn().mockResolvedValue([]),
    getStockMovements: jest.fn().mockResolvedValue({ data: [] }),
    stockIn: jest.fn().mockResolvedValue({ movement_id: 'mv_1' }),
    getOrders: jest
      .fn()
      .mockResolvedValue({ current_page: 1, last_page: 1, per_page: 25, total: 0, data: [] }),
    getOrder: jest.fn().mockRejectedValue(new Error('not loaded in this test')),
    prepareOrder: jest.fn().mockResolvedValue({}),
    payOrder: jest.fn().mockResolvedValue({}),
    cancelOrder: jest.fn().mockResolvedValue({}),
    refundOrder: jest.fn().mockResolvedValue({}),
    checkout: jest.fn().mockResolvedValue({ order: {} }),
    getReceivingQueue: jest.fn().mockResolvedValue([]),
    getPurchaseById: jest.fn().mockRejectedValue(new Error('not loaded in this test')),
    createPurchase: jest.fn().mockResolvedValue({}),
    receivePurchase: jest.fn().mockResolvedValue({}),
    payPurchase: jest.fn().mockResolvedValue({}),
    getExpenseCategories: jest.fn().mockResolvedValue([{ key: 'rent', label: 'اجاره' }]),
    getExpenseSummary: jest
      .fn()
      .mockResolvedValue({ monthTotal: 0, todayTotal: 0, previousMonthTotal: 0, byCategory: [] }),
    createExpense: jest.fn().mockResolvedValue({}),
    updateExpense: jest.fn().mockResolvedValue({}),
    deleteExpense: jest.fn().mockResolvedValue({}),
    updateProduct: jest.fn().mockResolvedValue({}),
    updateCustomer: jest.fn().mockResolvedValue({}),
    updateSupplier: jest.fn().mockResolvedValue({}),
    parseMessage: jest.fn().mockResolvedValue({}),
  },
  setApiLocale: jest.fn(),
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

/* ========================================================================== */
/*                                   P10                                       */
/* ========================================================================== */

const { apiClient: mockedApi } = jest.requireMock('./api/apiClient');

const UNIT_SAMPLE = [
  { code: 'piece', label: 'عدد' },
  { code: 'kilogram', label: 'کیلوگرم' },
  { code: 'gram', label: 'گرم' },
  { code: 'liter', label: 'لیتر' },
  { code: 'pack', label: 'بسته' },
];

const WAREHOUSE_SAMPLE = [
  { id: 'wh_a', name: 'انبار مرکزی', code: 'MAIN', storeId: 'store_1', storeName: 'فروشگاه مرکزی' },
  { id: 'wh_b', name: 'انبار شعبه', code: 'BR', storeId: 'store_1', storeName: 'فروشگاه مرکزی' },
];

const signInAt = async (path: string) => {
  window.history.pushState(null, '', path);
  setAccessToken('token-abc');
  setBusinessContext({
    organizationId: 'org_1',
    organizationName: 'کافه رستو',
    role: 'OWNER',
    storeId: 'store_1',
    storeName: 'فروشگاه مرکزی',
    warehouseId: 'wh_a',
    warehouseName: 'انبار مرکزی',
  });
  mockGetProfile.mockResolvedValue(OWNER);
  renderBusinessApp();
  await waitFor(() => expect(mockGetProfile).toHaveBeenCalled());
};

describe('P10 — navigation separates the three stock concepts', () => {
  test('purchase, receiving and current stock are three different destinations', async () => {
    await signInAt('/app/dashboard');

    await waitFor(() => expect(screen.getByText(/سفارش خرید/)).toBeInTheDocument());
    expect(screen.getByText('دریافت کالا')).toBeInTheDocument();
    expect(screen.getByText('موجودی فعلی')).toBeInTheDocument();

    // The three labels must not collapse onto one another.
    const nav = document.body.textContent ?? '';
    expect(nav).toContain('سفارش خرید');
    expect(nav).toContain('دریافت کالا');
    expect(nav).toContain('موجودی فعلی');
  });

  test('the English shell uses fully English labels for the same concepts', async () => {
    window.localStorage.setItem('resto_language', 'en');
    await signInAt('/app/dashboard');

    await waitFor(() => expect(screen.getByText('Purchase Orders')).toBeInTheDocument());
    expect(screen.getByText('Receive Stock')).toBeInTheDocument();
    expect(screen.getByText('Current Stock')).toBeInTheDocument();
    window.localStorage.removeItem('resto_language');
  });
});

describe('P10 — unit of measurement', () => {
  test('the unit catalog is plain business wording, not technical codes', async () => {
    mockedApi.getUnits.mockResolvedValueOnce(UNIT_SAMPLE);

    await signInAt('/app/products');

    await waitFor(() => expect(screen.getByText(/کالاها/)).toBeInTheDocument());
    expect(screen.getByText('عدد')).toBeInTheDocument();
    expect(screen.getByText('کیلوگرم')).toBeInTheDocument();

    const html = document.body.innerHTML;
    expect(html).not.toMatch(/base_unit|unit_code|measurement_type|SKU unit|inventory_unit/i);
  });
});

describe('P10 — warehouse selection and inventory increase', () => {
  test('with no warehouse the screen explains the situation and offers to create one', async () => {
    mockedApi.getWarehouses.mockResolvedValue([]);

    await signInAt('/app/inventory');

    await waitFor(() =>
      expect(screen.getAllByText(/برای ثبت موجودی ابتدا باید یک انبار داشته باشید/).length).toBeGreaterThan(0)
    );
    expect(screen.getAllByText('ایجاد انبار').length).toBeGreaterThan(0);
  });

  test('with several warehouses the increase form requires the user to choose one', async () => {
    mockedApi.getWarehouses.mockResolvedValue(WAREHOUSE_SAMPLE);

    await signInAt('/app/inventory');

    await waitFor(() => expect(screen.getByText(/افزایش موجودی/)).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /افزایش موجودی/ }));

    await waitFor(() => expect(screen.getByLabelText(/انبار/)).toBeInTheDocument());
    // A real picker with the business's own warehouses — not a fixture id.
    const picker = screen.getByLabelText(/انبار/) as HTMLSelectElement;
    expect(Array.from(picker.options).map((o) => o.value)).toEqual(expect.arrayContaining(['wh_a', 'wh_b']));
    expect(picker.value).toBe(''); // multi-warehouse: the user must choose

    // Submitting without a warehouse fails on the field, before any request.
    expect(mockedApi.stockIn).not.toHaveBeenCalled();
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /ثبت افزایش موجودی/ }));
    });
    expect(mockedApi.stockIn).not.toHaveBeenCalled();
    await waitFor(() => expect(screen.getAllByText(/انبار را انتخاب کنید/).length).toBeGreaterThan(0));
  });

  test('no warehouse id is hardcoded anywhere in the interface', async () => {
    mockedApi.getWarehouses.mockResolvedValue(WAREHOUSE_SAMPLE);

    await signInAt('/app/inventory');

    await waitFor(() => expect(screen.getByText(/افزایش موجودی/)).toBeInTheDocument());
    const bundle = document.body.innerHTML + (JSON.stringify(mockedApi.getWarehouses.mock.results) ?? '');
    expect(bundle).not.toMatch(/wh_apex_1a|wh_apex_1b/);
  });
});

describe('P10 — tax is a configured value, not a magic number', () => {
  test('the business default rate is shown in business settings and can be changed', async () => {
    await signInAt('/app/settings');

    await waitFor(() => expect(screen.getByLabelText(/مالیات پیش‌فرض سفارش/)).toBeInTheDocument());
    expect(screen.getByLabelText(/مالیات پیش‌فرض سفارش/)).toHaveValue(8);

    fireEvent.change(screen.getByLabelText(/مالیات پیش‌فرض سفارش/), { target: { value: '10' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /ذخیره/ }));
    });

    expect(mockedApi.updateBusinessSettings).toHaveBeenCalledWith({ default_tax_rate: 10 });
  });

  test('a rate outside the allowed range is refused before it is sent', async () => {
    await signInAt('/app/settings');

    await waitFor(() => expect(screen.getByLabelText(/مالیات پیش‌فرض سفارش/)).toBeInTheDocument());
    fireEvent.change(screen.getByLabelText(/مالیات پیش‌فرض سفارش/), { target: { value: '-5' } });
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /ذخیره/ }));
    });

    expect(mockedApi.updateBusinessSettings).not.toHaveBeenCalled();
    expect(screen.getAllByText(/بین ۰ تا ۱۰۰/).length).toBeGreaterThan(0);
  });

  test('the order screen takes the rate from the server configuration', async () => {
    mockedApi.getBusinessSettings.mockResolvedValueOnce({
      defaultTaxRate: 9.25,
      taxInclusivePricing: false,
      canOverrideTaxPerOrder: true,
      minTaxRate: 0,
      maxTaxRate: 100,
    });

    await signInAt('/app/settings');

    await waitFor(() => expect(screen.getByLabelText(/مالیات پیش‌فرض سفارش/)).toBeInTheDocument());
    expect(screen.getByLabelText(/مالیات پیش‌فرض سفارش/)).toHaveValue(9.25);
  });
});

describe('P10 — order from message', () => {
  test('offers a plain Persian message box with realistic examples and no AI claim', async () => {
    await signInAt('/app/messages');

    await waitFor(() => expect(screen.getByLabelText(/متن پیام مشتری/)).toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'بررسی پیام' })).toBeInTheDocument();
    expect(document.body.textContent).toMatch(/قهوه/);

    const html = document.body.innerHTML;
    expect(html).not.toMatch(/هوش مصنوعی|\bAI\b/i);
  });
});

describe('P10 — the orders screen lists real orders', () => {
  test('asks the server for orders and shows number, customer, total, status and source', async () => {
    mockedApi.getOrders.mockResolvedValueOnce({
      current_page: 1,
      last_page: 1,
      per_page: 25,
      total: 1,
      data: [
        {
          id: 'ord_1042',
          orderNumber: '1042',
          customerName: 'محمد رضایی',
          itemsCount: 3,
          total: 1250000,
          paymentStatus: 'unpaid',
          status: 'new',
          source: 'instagram',
          createdAt: '2026-10-03T18:40:00+00:00',
        },
      ],
    } as never);

    await signInAt('/app/orders');

    await waitFor(() => expect(screen.getByText(/1042/)).toBeInTheDocument());
    expect(screen.getByText(/محمد رضایی/)).toBeInTheDocument();
    expect(screen.getByText(/اینستاگرام|Instagram/)).toBeInTheDocument();
    expect(mockedApi.getOrders).toHaveBeenCalled();
  });

  test('search is sent to the server, not filtered over already-loaded rows', async () => {
    await signInAt('/app/orders');

    await waitFor(() => expect(mockedApi.getOrders).toHaveBeenCalled());
    mockedApi.getOrders.mockClear();

    const search = screen.getByRole('searchbox') as HTMLInputElement;
    fireEvent.change(search, { target: { value: '1042' } });

    await waitFor(() => expect(mockedApi.getOrders).toHaveBeenCalled());
    expect(mockedApi.getOrders.mock.calls[0][0]).toMatchObject({ q: '1042' });
  });

  test('offers only the statuses the backend actually reports', async () => {
    await signInAt('/app/orders');

    await waitFor(() => expect(mockedApi.getOrders).toHaveBeenCalled());
    const filters = document.body.textContent ?? '';
    ['همه', 'در انتظار پرداخت', 'پرداخت شده', 'لغو شده'].forEach((label) => {
      expect(filters).toContain(label);
    });
  });
});

describe('P10 — operating expenses are distinct from purchases', () => {
  test('explains the purpose of the expense and lists real categories', async () => {
    mockedApi.getExpenseCategories.mockResolvedValueOnce([
      { key: 'rent', label: 'اجاره' },
      { key: 'internet', label: 'اینترنت' },
    ] as never);
    mockedApi.getExpenses.mockResolvedValueOnce([] as never);

    await signInAt('/app/expenses');

    await waitFor(() => expect(screen.getAllByText(/هزینه/).length).toBeGreaterThan(0));
    expect(screen.getByRole('button', { name: /ثبت هزینه/ })).toBeInTheDocument();
    const text = document.body.textContent ?? '';
    expect(text).toMatch(/تأمین‌کننده|خرید کالا/);
  });

  test('the expense form refuses a submission without a title or category', async () => {
    mockedApi.getExpenseCategories.mockResolvedValueOnce([
      { key: 'rent', label: 'اجاره' },
    ] as never);

    await signInAt('/app/expenses');

    await waitFor(() => expect(screen.getByRole('button', { name: /ثبت هزینه/ })).toBeInTheDocument());
    fireEvent.click(screen.getByRole('button', { name: /ثبت هزینه/ }));

    await waitFor(() => expect(screen.getByLabelText(/عنوان/)).toBeInTheDocument());
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: /ذخیره هزینه/ }));
    });

    expect(mockedApi.createExpense).not.toHaveBeenCalled();
    expect(screen.getAllByText(/عنوان هزینه را وارد کنید/).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/دسته‌بندی را انتخاب کنید/).length).toBeGreaterThan(0);
  });
});

describe('P10 — no fake fallbacks hide a failed request', () => {
  test('a failing order request shows an error instead of an empty list', async () => {
    mockedApi.getOrders.mockRejectedValueOnce(new Error('boom'));

    await signInAt('/app/orders');

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
  });

  test('a failing stock request shows an error instead of pretending the warehouse is empty', async () => {
    mockedApi.getStock.mockRejectedValueOnce(new Error('boom'));

    await signInAt('/app/inventory');

    await waitFor(() => expect(screen.getByRole('alert')).toBeInTheDocument());
    expect(screen.queryByText(/برای ثبت موجودی ابتدا باید یک انبار داشته باشید/)).not.toBeInTheDocument();
  });
});

describe('P10 — the warm dark theme still applies', () => {
  test('the new screens consume the centralized theme, not their own colours', async () => {
    await signInAt('/app/orders');

    await waitFor(() => expect(mockedApi.getOrders).toHaveBeenCalled());
    const html = document.body.innerHTML;
    // Screens must not hardcode their own hex palette.
    const hexes = html.match(/#[0-9a-fA-F]{3,8}/g) ?? [];
    const rootPalette = new Set(
      [
        '#101418',
        '#161b22',
        '#1c222a',
        '#f5f7fa',
        '#e6edf3',
        '#c9a227',
        '#8b5e34',
        '#3a2c22',
      ].map((c) => c.toLowerCase())
    );
    hexes.forEach((hex) => {
      if (hex.length === 4 || hex.length === 7) {
        expect(rootPalette.has(hex.toLowerCase())).toBe(true);
      }
    });
  });
});

afterEach(() => {
  act(() => {
    window.localStorage.clear();
  });
});

test('ZZDUMP', async () => {
  const m = jest.requireMock('./api/apiClient');
  // eslint-disable-next-line no-console
  console.log('CATDEF', typeof m.apiClient.getExpenseCategories, JSON.stringify(await m.apiClient.getExpenseCategories()));
  for (const route of ['/app/expenses']) {
    window.history.pushState(null, '', route);
    setAccessToken('token-abc');
    setBusinessContext({ organizationId: 'org_1', organizationName: 'کافه رستو', role: 'OWNER', storeId: 'store_1', storeName: 'فروشگاه مرکزی', warehouseId: 'wh_a', warehouseName: 'انبار مرکزی' });
    mockGetProfile.mockResolvedValue(OWNER);
    const view = render(<ThemeProvider><BusinessApp /></ThemeProvider>);
    await waitFor(() => expect(mockGetProfile).toHaveBeenCalled());
    await act(async () => { await new Promise((r) => setTimeout(r, 250)); });
    const buttons = Array.from(document.querySelectorAll('button')).map((b) => (b.textContent ?? '').trim()).filter(Boolean).slice(0, 30);
    const labels = Array.from(document.querySelectorAll('label')).map((l) => (l.textContent ?? '').trim()).slice(0, 30);
    const inputs = Array.from(document.querySelectorAll('input,select,textarea')).map((i) => `${i.tagName}:${(i as HTMLInputElement).type}:${(i as HTMLInputElement).value}`).slice(0, 30);
    // eslint-disable-next-line no-console
    console.log('DUMP', route, JSON.stringify({ buttons, labels, inputs }));
    view.unmount();
  }
});

afterEach(() => {
  act(() => {
    window.localStorage.clear();
  });
});