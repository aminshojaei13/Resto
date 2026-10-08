/**
 * The single source of truth for "who is signed in" in the browser.
 *
 * Only two things are persisted: the bearer token and which business / store /
 * warehouse the person is working in. The person's identity is never cached
 * here — it is always re-read from the authenticated profile endpoint, so a
 * stale name can never survive a sign-out, an account switch or a server-side
 * profile change.
 */

const TOKEN_KEY = 'resto_access_token';
const CONTEXT_KEY = 'resto_business_context';
const LANGUAGE_KEY = 'resto_language';

export interface BusinessContext {
  organizationId: string;
  organizationName: string;
  role: string;
  storeId: string;
  storeName: string;
  warehouseId: string;
  warehouseName: string;
}

export const EMPTY_CONTEXT: BusinessContext = {
  organizationId: '',
  organizationName: '',
  role: '',
  storeId: '',
  storeName: '',
  warehouseId: '',
  warehouseName: '',
};

export const getAccessToken = (): string => {
  try {
    return window.localStorage.getItem(TOKEN_KEY) ?? '';
  } catch {
    return '';
  }
};

export const setAccessToken = (token: string): void => {
  try {
    if (token) {
      window.localStorage.setItem(TOKEN_KEY, token);
    } else {
      window.localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    /* storage unavailable — the session simply does not survive a reload */
  }
};

export const getBusinessContext = (): BusinessContext => {
  try {
    const raw = window.localStorage.getItem(CONTEXT_KEY);
    if (!raw) return { ...EMPTY_CONTEXT };
    const parsed = JSON.parse(raw) as Partial<BusinessContext>;
    return { ...EMPTY_CONTEXT, ...parsed };
  } catch {
    return { ...EMPTY_CONTEXT };
  }
};

export const setBusinessContext = (context: BusinessContext): void => {
  try {
    window.localStorage.setItem(CONTEXT_KEY, JSON.stringify(context));
  } catch {
    /* storage unavailable */
  }
};

export const setActiveOrganization = (
  organizationId: string,
  organizationName: string,
  role: string
): void => {
  setBusinessContext({ ...getBusinessContext(), organizationId, organizationName, role });
};

export const setActiveStore = (storeId: string, storeName: string): void => {
  setBusinessContext({ ...getBusinessContext(), storeId, storeName });
};

export const setActiveWarehouse = (warehouseId: string, warehouseName: string): void => {
  setBusinessContext({ ...getBusinessContext(), warehouseId, warehouseName });
};

/**
 * Leaving this device: the token and the selected business both go, so the next
 * person to open the app cannot see the previous person's context.
 */
export const clearSession = (): void => {
  try {
    window.localStorage.removeItem(TOKEN_KEY);
    window.localStorage.removeItem(CONTEXT_KEY);
    // Removes the legacy cached identity blob so an old name can never reappear.
    window.localStorage.removeItem('resto_auth_user');
  } catch {
    /* storage unavailable */
  }
};

export const hasToken = (): boolean => getAccessToken() !== '';
