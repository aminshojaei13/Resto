import { Language } from '../i18n/authStrings';

/**
 * The unit vocabulary, mirrored from the server catalog.
 *
 * The server is the source of truth — it decides which unit codes exist and
 * what they are called — and this table only exists so a quantity can be
 * rendered next to its product without a second request. A code this table
 * does not know is shown as it is rather than hidden.
 */
const FA: Record<string, string> = {
  piece: 'عدد',
  kilogram: 'کیلوگرم',
  gram: 'گرم',
  liter: 'لیتر',
  meter: 'متر',
  pack: 'بسته',
  box: 'جعبه',
  bottle: 'بطری',
  set: 'دست',
  serving: 'سرو',
  order: 'سفارش',
};

const EN: Record<string, string> = {
  piece: 'Piece',
  kilogram: 'Kilogram',
  gram: 'Gram',
  liter: 'Liter',
  meter: 'Meter',
  pack: 'Pack',
  box: 'Box',
  bottle: 'Bottle',
  set: 'Set',
  serving: 'Serving',
  order: 'Order',
};

export const DEFAULT_UNIT = 'piece';

export const unitLabel = (code: string | undefined | null, language: Language): string => {
  const table = language === 'fa' ? FA : EN;
  const key = (code ?? '').toLowerCase();

  if (key === '') return table[DEFAULT_UNIT];
  if (table[key]) return table[key];

  return code ?? table[DEFAULT_UNIT];
};

/** "۱۲ عدد" / "12 Kilogram" — a quantity is never shown without its unit. */
export const quantityWithUnit = (
  quantity: number,
  code: string | undefined | null,
  language: Language
): string => {
  const label = unitLabel(code, language);
  const formatted = Number(quantity).toLocaleString(language === 'fa' ? 'fa-IR' : 'en-US');

  return language === 'fa' ? `${formatted} ${label}` : `${formatted} ${label}`;
};

export const formatPercent = (rate: number): string =>
  Number(rate).toLocaleString('en-US', { maximumFractionDigits: 2 });

export const formatPercentFa = (rate: number): string =>
  `${Number(rate).toLocaleString('fa-IR', { maximumFractionDigits: 2 })}٪`;