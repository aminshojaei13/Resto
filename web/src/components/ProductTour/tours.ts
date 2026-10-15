import { TourConfig } from './tourTypes';

/**
 * Dashboard Tour: Guided tour of the Resto business control center.
 * Walks through header metrics, quick operations, onboarding progress,
 * core management pillars, and recent ledger activity.
 */
export const dashboardTour: TourConfig = {
  id: 'dashboard',
  steps: [
    {
      target: '[data-tour="dashboard-header"]',
      placement: 'bottom',
      title: {
        fa: 'مرکز مدیریت و عملیات کسب‌وکار',
        en: 'Business Control Center',
      },
      description: {
        fa: 'نمای کلی وضعیت کسب‌وکار، خلاصه درآمد روزانه و هشدارهای مهم در این بخش ارائه می‌شوند.',
        en: 'View an executive overview of daily business operations, revenue summaries, and alerts.',
      },
    },
    {
      target: '[data-tour="dashboard-quick-actions"]',
      placement: 'bottom',
      title: {
        fa: 'اقدامات سریع عملیاتی',
        en: 'Daily Quick Operations',
      },
      description: {
        fa: 'ثبت فوری سفارش صندوق (POS)، سفارش از شبکه‌های اجتماعی، تعریف کالا و ثبت خرید از تأمین‌کننده.',
        en: 'Fast-track shortcuts to register POS sales, convert social chat orders, add stock, and log purchases.',
      },
    },
    {
      target: '[data-tour="dashboard-onboarding"]',
      placement: 'bottom',
      title: {
        fa: 'چک‌لیست راه‌اندازی سریع',
        en: 'Setup Checklist',
      },
      description: {
        fa: 'چهار مرحله اصلی راه‌اندازی شامل ثبت کالاها، تأمین‌کنندگان و ثبت اولین سفارش برای تکمیل فرآیند آغازین.',
        en: 'Four core onboarding milestones: configure products, link suppliers, and register your first transactions.',
      },
    },
    {
      target: '[data-tour="dashboard-core-needs"]',
      placement: 'top',
      title: {
        fa: 'ارکان سه‌گانه عملیاتی',
        en: 'Core Management Pillars',
      },
      description: {
        fa: 'مدیریت و کنترل موجودی انبار، ردیابی سفارش‌های آنلاین و شبکه‌های اجتماعی، و وضعیت اسناد و تراز مالی.',
        en: 'Holistic control over stock counts and low inventory alerts, incoming social orders, and double-entry ledger balance.',
      },
    },
    {
      target: '[data-tour="dashboard-transactions"]',
      placement: 'top',
      title: {
        fa: 'جریان اسناد و تراکنش‌های اخیر',
        en: 'Recent Operations Stream',
      },
      description: {
        fa: 'فهرست تراکنش‌های اخیر دفتر کل، اسناد بدهکار/بستانکار خودکار حاصل از فاکتورها و ورودی‌های انبار.',
        en: 'Live audit log of recent double-entry ledger postings created by orders and stock movements.',
      },
    },
  ],
};

/**
 * Navigation Tour: Guided tour of the Resto application sidebar navigation.
 * Explains brand identity, organization switcher, sales module, purchasing module,
 * and language toggle.
 */
export const navigationTour: TourConfig = {
  id: 'navigation',
  steps: [
    {
      target: '[data-tour="nav-brand"]',
      placement: 'right',
      title: {
        fa: 'سیستم جامع رِستو',
        en: 'Resto Platform',
      },
      description: {
        fa: 'سامانه یکپارچه مدیریت فروش، انبارداری و حسابداری متمرکز برای کسب‌وکارهای مدرن.',
        en: 'Unified operating system for multi-channel sales, inventory warehouses, and financial ledger.',
      },
    },
    {
      target: '[data-tour="nav-business-switcher"]',
      placement: 'right',
      title: {
        fa: 'انتخاب سازمان، شعبه و انبار',
        en: 'Business & Warehouse Switcher',
      },
      description: {
        fa: 'مشاهده شعبه و انبار فعال و جابجایی آسان بین کسب‌وکارها، شعبات یا انبارهای مختلف.',
        en: 'Inspect your active organization, store, and warehouse, with instant switching between contexts.',
      },
    },
    {
      target: '[data-tour="nav-group-sales"]',
      placement: 'right',
      title: {
        fa: 'بخش عملیات فروش و مشتریان',
        en: 'Sales & Customer Operations',
      },
      description: {
        fa: 'دسترسی سریع به صندوق فروش (POS)، ثبت سفارش از پیام‌رسان‌ها، مدیریت سفارش‌ها و لیست مشتریان.',
        en: 'Access POS cash register, social chat ordering, active order fulfillments, and customer directory.',
      },
    },
    {
      target: '[data-tour="nav-group-purchasing"]',
      placement: 'right',
      title: {
        fa: 'بخش خرید و تأمین کالا',
        en: 'Purchasing & Stock Inflow',
      },
      description: {
        fa: 'ثبت سفارش‌های خرید (PO)، تحویل‌گیری و بازرسی کالاهای ورودی انبار، و فهرست تأمین‌کنندگان.',
        en: 'Manage purchase orders, handle warehouse stock receiving checkpoints, and vendor accounts.',
      },
    },
    {
      target: '[data-tour="nav-language-switcher"]',
      placement: 'top',
      title: {
        fa: 'تغییر زبان و چیدمان',
        en: 'Language & Layout Switcher',
      },
      description: {
        fa: 'تغییر فوری زبان و جهت رابط کاربری بین فارسی (RTL) و انگلیسی (LTR).',
        en: 'Toggle application language and layout direction between Persian (RTL) and English (LTR).'
      },
    },
  ],
};

/**
 * Retrieve a predefined tour configuration by its identifier.
 */
export function getTourById(id: string): TourConfig | undefined {
  if (id === 'dashboard' || id === dashboardTour.id) {
    return dashboardTour;
  }
  if (id === 'navigation' || id === navigationTour.id) {
    return navigationTour;
  }
  return undefined;
}
