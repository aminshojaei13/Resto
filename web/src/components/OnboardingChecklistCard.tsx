import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { StatusBadge } from './StatusBadge';

interface OnboardingChecklistCardProps {
  language?: 'fa' | 'en';
  hasProducts: boolean;
  hasSuppliers: boolean;
  isSupplierSkipped: boolean;
  hasOrders: boolean;
  hasCompletedOrders: boolean;
  onNavigate: (path: string) => void;
  onSkipSupplier: () => void;
  onDismiss: () => void;
  onOpenWelcomeModal: () => void;
}

export const OnboardingChecklistCard: React.FC<OnboardingChecklistCardProps> = ({
  language = 'fa',
  hasProducts,
  hasSuppliers,
  isSupplierSkipped,
  hasOrders,
  hasCompletedOrders,
  onNavigate,
  onSkipSupplier,
  onDismiss,
  onOpenWelcomeModal,
}) => {
  const isFa = language === 'fa';
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';

  const [isExpanded, setIsExpanded] = useState(true);

  const step1Done = hasProducts;
  const step2Done = hasSuppliers || isSupplierSkipped;
  const step3Done = hasOrders;
  const step4Done = hasCompletedOrders;

  const completedCount = (step1Done ? 1 : 0) + (step2Done ? 1 : 0) + (step3Done ? 1 : 0) + (step4Done ? 1 : 0);
  const totalSteps = 4;
  const progressPercent = Math.round((completedCount / totalSteps) * 100);
  const isAllDone = completedCount >= totalSteps;

  const toDigits = (num: number | string) => {
    if (!isFa) return String(num);
    const faDigits = ['۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹'];
    return String(num).replace(/[0-9]/g, (w) => faDigits[+w]);
  };

  const steps = [
    {
      index: 1,
      title: isFa ? '۱. تعریف کالا و موجودی انبار' : '1. Add Products & Initial Stock',
      desc: isFa
        ? 'کالاهای خود را همراه با قیمت فروش و موجودی اولیه ثبت کنید تا آماده فروش شوند.'
        : 'Add your products with selling prices and stock levels to get started.',
      isDone: step1Done,
      primaryAction: {
        label: isFa ? 'ثبت کالا و موجودی' : 'Add Product',
        onClick: () => onNavigate('/app/products'),
      },
    },
    {
      index: 2,
      title: isFa ? '۲. تعریف تأمین‌کننده و خرید (اختیاری)' : '2. Add Suppliers & Purchases (Optional)',
      desc: isFa
        ? 'تأمین‌کنندگان را مشخص کنید تا فاکتورهای خرید و بدهی‌های تجاری دقیق ثبت شوند.'
        : 'Register your vendors to track purchase invoices and supplier debts.',
      isDone: step2Done,
      isOptional: true,
      canSkip: !hasSuppliers && !isSupplierSkipped,
      primaryAction: {
        label: isFa ? 'ثبت تأمین‌کننده' : 'Add Supplier',
        onClick: () => onNavigate('/app/suppliers'),
      },
      onSkip: onSkipSupplier,
    },
    {
      index: 3,
      title: isFa ? '۳. ثبت اولین فروش و صدور فاکتور' : '3. Record First Sale & Invoice',
      desc: isFa
        ? 'سفارش مشتری را می‌توانید مستقیماً از صندوق یا هوشمندانه از متن پیامک و چت اینستاگرام ثبت کنید.'
        : 'Create an order via POS terminal or automatically parse customer chat/SMS.',
      isDone: step3Done,
      primaryAction: {
        label: isFa ? 'صندوق فروش (POS)' : 'Open POS',
        onClick: () => onNavigate('/app/pos'),
      },
      secondaryAction: {
        label: isFa ? 'ثبت از پیام / چت' : 'Parse Chat/SMS',
        onClick: () => onNavigate('/app/messages'),
      },
    },
    {
      index: 4,
      title: isFa ? '۴. مراحل آماده‌سازی، ارسال و تکمیل' : '4. Order Fulfillment & Delivery',
      desc: isFa
        ? 'سفارشات ثبت‌شده را پیگیری کنید، مرحله آماده‌سازی و ارسال را ثبت و فاکتور را نهایی کنید.'
        : 'Track order progress, update fulfillment status to processing and completed.',
      isDone: step4Done,
      primaryAction: {
        label: isFa ? 'مشاهده و مدیریت سفارشات' : 'Manage Orders',
        onClick: () => onNavigate('/app/orders'),
      },
    },
  ];

  return (
    <div
      style={{
        backgroundColor: theme.colors.surface,
        border: `1.5px solid ${theme.colors.primary}`,
        borderRadius: theme.borderRadius.xl,
        padding: theme.spacing.xl,
        boxShadow: theme.shadows.card,
        direction: isFa ? 'rtl' : 'ltr',
        display: 'flex',
        flexDirection: 'column',
        gap: theme.spacing.lg,
      }}
    >
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: theme.spacing.md }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
          <div
            style={{
              width: '40px',
              height: '40px',
              borderRadius: '50%',
              backgroundColor: theme.colors.primaryLight,
              color: theme.colors.primary,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '20px',
              flexShrink: 0,
            }}
          >
            🎓
          </div>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
              <span style={{ fontSize: '16px', fontWeight: 700, color: theme.colors.textPrimary }}>
                {isFa ? 'راهنمای شروع به کار و راه‌اندازی سیستم' : 'Getting Started Guide'}
              </span>
              {isAllDone && (
                <StatusBadge label={isFa ? 'تکمیل شد' : 'Completed'} variant="success" />
              )}
            </div>
            <div style={{ fontSize: '13px', color: theme.colors.textSecondary, marginTop: '2px' }}>
              {isFa
                ? `پیشرفت شما: ${toDigits(completedCount)} از ${toDigits(totalSteps)} مرحله (${toDigits(progressPercent)}٪)`
                : `Progress: ${completedCount} of ${totalSteps} steps (${progressPercent}%)`}
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
          <button
            onClick={onOpenWelcomeModal}
            style={{
              backgroundColor: 'transparent',
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.md,
              padding: '6px 12px',
              fontSize: '12px',
              fontWeight: 600,
              color: theme.colors.textPrimary,
              cursor: 'pointer',
            }}
          >
            {isFa ? '📖 مشاهده راهنما' : '📖 View Guide'}
          </button>
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            style={{
              backgroundColor: 'transparent',
              border: `1px solid ${theme.colors.border}`,
              borderRadius: theme.borderRadius.md,
              padding: '6px 10px',
              fontSize: '13px',
              color: theme.colors.textSecondary,
              cursor: 'pointer',
            }}
            title={isExpanded ? (isFa ? 'جمع کردن' : 'Collapse') : (isFa ? 'باز کردن' : 'Expand')}
          >
            {isExpanded ? '▲' : '▼'}
          </button>
          <button
            onClick={onDismiss}
            style={{
              backgroundColor: 'transparent',
              border: 'none',
              borderRadius: theme.borderRadius.md,
              padding: '6px 8px',
              fontSize: '14px',
              color: theme.colors.textSecondary,
              cursor: 'pointer',
            }}
            title={isFa ? 'بستن راهنما' : 'Dismiss'}
          >
            ✕
          </button>
        </div>
      </div>

      {/* Progress Bar */}
      <div
        style={{
          width: '100%',
          height: '8px',
          backgroundColor: theme.colors.background,
          borderRadius: '999px',
          overflow: 'hidden',
          border: `1px solid ${theme.colors.border}`,
        }}
      >
        <div
          style={{
            width: `${progressPercent}%`,
            height: '100%',
            backgroundColor: theme.colors.primary,
            borderRadius: '999px',
            transition: 'width 0.3s ease',
          }}
        />
      </div>

      {/* Expanded Step Cards */}
      {isExpanded && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.md, marginTop: theme.spacing.xs }}>
          {steps.map((step) => (
            <div
              key={step.index}
              style={{
                display: 'flex',
                flexDirection: 'column',
                gap: theme.spacing.xs,
                padding: theme.spacing.md,
                backgroundColor: step.isDone
                  ? theme.colors.surfaceElevated
                  : theme.colors.background,
                border: `1px solid ${step.isDone ? theme.colors.border : theme.colors.primary}`,
                borderRadius: theme.borderRadius.lg,
                opacity: step.isDone ? 0.75 : 1,
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
                  <div
                    style={{
                      width: '24px',
                      height: '24px',
                      borderRadius: '50%',
                      backgroundColor: step.isDone ? theme.colors.successLight : theme.colors.primaryLight,
                      color: step.isDone ? theme.colors.success : theme.colors.primary,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: '12px',
                      fontWeight: 700,
                    }}
                  >
                    {step.isDone ? '✓' : toDigits(step.index)}
                  </div>
                  <span
                    style={{
                      fontSize: '14px',
                      fontWeight: 700,
                      color: step.isDone ? theme.colors.textSecondary : theme.colors.textPrimary,
                      textDecoration: step.isDone ? 'line-through' : 'none',
                    }}
                  >
                    {step.title}
                  </span>
                </div>

                <div>
                  {step.isDone ? (
                    <StatusBadge label={isFa ? 'انجام شد' : 'Done'} variant="success" />
                  ) : step.isOptional ? (
                    <StatusBadge label={isFa ? 'اختیاری' : 'Optional'} variant="neutral" />
                  ) : null}
                </div>
              </div>

              {!step.isDone && (
                <>
                  <div style={{ fontSize: '13px', color: theme.colors.textSecondary, marginRight: '32px', lineHeight: 1.6 }}>
                    {step.desc}
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm, marginRight: '32px', marginTop: theme.spacing.xs }}>
                    <button
                      onClick={step.primaryAction.onClick}
                      style={{
                        backgroundColor: theme.colors.primary,
                        color: theme.colors.surface,
                        border: 'none',
                        borderRadius: theme.borderRadius.md,
                        padding: '6px 14px',
                        fontSize: '13px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      {step.primaryAction.label}
                    </button>

                    {step.secondaryAction && (
                      <button
                        onClick={step.secondaryAction.onClick}
                        style={{
                          backgroundColor: 'transparent',
                          color: theme.colors.textPrimary,
                          border: `1px solid ${theme.colors.border}`,
                          borderRadius: theme.borderRadius.md,
                          padding: '6px 14px',
                          fontSize: '13px',
                          fontWeight: 600,
                          cursor: 'pointer',
                        }}
                      >
                        {step.secondaryAction.label}
                      </button>
                    )}

                    {step.canSkip && step.onSkip && (
                      <button
                        onClick={step.onSkip}
                        style={{
                          backgroundColor: 'transparent',
                          color: theme.colors.textSecondary,
                          border: 'none',
                          padding: '6px 10px',
                          fontSize: '13px',
                          cursor: 'pointer',
                          textDecoration: 'underline',
                        }}
                      >
                        {isFa ? 'رد شدن از این مرحله' : 'Skip this step'}
                      </button>
                    )}
                  </div>
                </>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
