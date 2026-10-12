import React from 'react';
import { ModalShell, PrimaryButton, SecondaryButton } from './Field';
import { useTheme } from '../theme/ThemeContext';

interface OnboardingWelcomeModalProps {
  language?: 'fa' | 'en';
  userName?: string;
  onClose: () => void;
  onStart: () => void;
}

export const OnboardingWelcomeModal: React.FC<OnboardingWelcomeModalProps> = ({
  language = 'fa',
  userName = '',
  onClose,
  onStart,
}) => {
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const steps = [
    {
      num: isFa ? '۱' : '1',
      icon: '📦',
      title: isFa ? '۱. ثبت اولین کالا و موجودی انبار' : '1. Products & Initial Stock',
      desc: isFa
        ? 'تعریف کالاها با قیمت فروش و تخصیص موجودی اولیه در انبار'
        : 'Define your products with prices and set initial stock levels.',
    },
    {
      num: isFa ? '۲' : '2',
      icon: '👥',
      title: isFa ? '۲. تأمین‌کنندگان و خرید (اختیاری)' : '2. Suppliers & Purchases (Optional)',
      desc: isFa
        ? 'ثبت طرف‌های تجاری تأمین برای مدیریت فاکتورهای خرید و بستانکاری'
        : 'Register your suppliers to track purchase bills and vendor debt.',
    },
    {
      num: isFa ? '۳' : '3',
      icon: '💳',
      title: isFa ? '۳. ثبت اولین فروش و صدور فاکتور' : '3. First Sale & Invoice',
      desc: isFa
        ? 'ثبت فروش حضوری در صندوق POS یا ثبت هوشمند از متن پیامک و چت شبکه‌های اجتماعی'
        : 'Checkout via POS terminal or automatically parse customer chats and SMS.',
    },
    {
      num: isFa ? '۴' : '4',
      icon: '🚚',
      title: isFa ? '۴. مراحل آماده‌سازی، ارسال و تکمیل' : '4. Order Fulfillment & Delivery',
      desc: isFa
        ? 'پیگیری وضعیت سفارشات، شروع آماده‌سازی، تحویل به مشتری و ثبت تسویه حساب'
        : 'Track orders, move from processing to shipping, and record payment.',
    },
  ];

  const footer = (
    <div style={{ display: 'flex', gap: theme.spacing.sm, justifyContent: 'flex-end', width: '100%', flexWrap: 'wrap' }}>
      <SecondaryButton onClick={onClose} style={{ flex: '1 1 140px' }}>
        {isFa ? 'بعداً یادآوری کن' : 'Remind Later'}
      </SecondaryButton>
      <PrimaryButton onClick={onStart} style={{ flex: '1 1 140px' }}>
        {isFa ? 'شروع مراحل راه‌اندازی' : 'Get Started'}
      </PrimaryButton>
    </div>
  );

  return (
    <ModalShell
      title={isFa ? 'به نرم‌افزار رستو (Resto) خوش آمدید' : 'Welcome to Resto'}
      onClose={onClose}
      footer={footer}
      maxWidth={620}
    >
      <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.lg, direction: isFa ? 'rtl' : 'ltr' }}>
        <p style={{ margin: 0, fontSize: '14px', lineHeight: 1.7, color: theme.colors.textSecondary }}>
          {isFa
            ? `${userName ? `سلام ${userName} گرامی، ` : ''}برای شروع کار با سیستم مدیریت کسب‌وکار و فروشگاه، ۴ مرحله ساده زیر طراحی شده تا به راحتی بر نرم‌افزار مسلط شوید:`
            : `Welcome to Resto! Follow these 4 simple steps to set up your business and learn the workflow:`}
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
          {steps.map((step, idx) => (
            <div
              key={idx}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: theme.spacing.md,
                padding: theme.spacing.md,
                backgroundColor: theme.colors.background,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.lg,
              }}
            >
              <div
                style={{
                  width: '42px',
                  height: '42px',
                  borderRadius: theme.borderRadius.md,
                  backgroundColor: theme.colors.primaryLight,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '20px',
                  flexShrink: 0,
                }}
              >
                {step.icon}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: 1 }}>
                <div style={{ fontSize: '14px', fontWeight: 700, color: theme.colors.textPrimary }}>
                  {step.title}
                </div>
                <div style={{ fontSize: '12px', color: theme.colors.textSecondary, lineHeight: 1.5 }}>
                  {step.desc}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </ModalShell>
  );
};
