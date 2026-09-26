import React, { useState } from 'react';
import { apiClient } from '../api/apiClient';

interface OnboardingWizardProps {
  language?: 'fa' | 'en';
  onComplete?: () => void;
}

export const OnboardingWizard: React.FC<OnboardingWizardProps> = ({
  language = 'fa',
  onComplete,
}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [isCompleting, setIsCompleting] = useState(false);

  const isFa = language === 'fa';

  const handleFinish = async () => {
    setIsCompleting(true);
    try {
      await apiClient.completeOnboarding();
      setIsCompleting(false);
      if (onComplete) onComplete();
    } catch {
      setIsCompleting(false);
      if (onComplete) onComplete();
    }
  };

  return (
    <div style={{ backgroundColor: '#FFF', border: '2px solid #005AC1', borderRadius: '16px', padding: '32px', maxWidth: '600px', margin: '40px auto', boxShadow: '0 8px 24px rgba(0,90,193,0.12)', fontFamily: 'system-ui, sans-serif' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', borderBottom: '1px solid #EEE', paddingBottom: '16px' }}>
        <h2 style={{ margin: 0, color: '#005AC1', fontSize: '20px', fontWeight: 'bold' }}>
          {isFa ? 'راهنمای راه‌اندازی سریع اولیه (Onboarding)' : 'Initial Resto Setup Wizard'}
        </h2>
        <span style={{ fontSize: '13px', fontWeight: 'bold', backgroundColor: '#E3F2FD', color: '#0D47A1', padding: '4px 12px', borderRadius: '12px' }}>
          {isFa ? `گام ${currentStep} از ۴` : `Step ${currentStep} of 4`}
        </span>
      </div>

      {currentStep === 1 && (
        <div>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '16px' }}>{isFa ? 'گام ۱: تایید پروفایل سازمان و اطلاعات پایه' : 'Step 1: Confirm Business Profile'}</h3>
          <p style={{ color: '#666', fontSize: '14px', lineHeight: '1.6' }}>
            {isFa
              ? 'سازمان جدید شما ایجاد شده است. در رستو هر سازمان شامل شعب (Stores) و انبارهای مستقل (Warehouses) است.'
              : 'Your new Organization is provisioned. In Resto, each organization manages stores and warehouses.'}
          </p>
          <button
            onClick={() => setCurrentStep(2)}
            style={{ width: '100%', marginTop: '20px', backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '8px', padding: '12px', fontSize: '15px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            {isFa ? 'مرحله بعد: تنظیمات شعب فروشگاه ➔' : 'Next: Store Setup ➔'}
          </button>
        </div>
      )}

      {currentStep === 2 && (
        <div>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '16px' }}>{isFa ? 'گام ۲: تایید شعبه اصلی فروشگاه (Main Store)' : 'Step 2: Confirm Primary Store'}</h3>
          <p style={{ color: '#666', fontSize: '14px', lineHeight: '1.6' }}>
            {isFa
              ? 'شعبه اصلی فروشگاه برای سازمان شما ایجاد شده است. می‌توانید کلیه ثبت فاکتورهای POS را به این شعبه اختصاص دهید.'
              : 'Your primary store is provisioned. All POS transactions and cash registers link to this store.'}
          </p>
          <button
            onClick={() => setCurrentStep(3)}
            style={{ width: '100%', marginTop: '20px', backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '8px', padding: '12px', fontSize: '15px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            {isFa ? 'مرحله بعد: تنظیم انبار مرکزی ➔' : 'Next: Warehouse Setup ➔'}
          </button>
        </div>
      )}

      {currentStep === 3 && (
        <div>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '16px' }}>{isFa ? 'گام ۳: تایید انبار مرکزی نگهداری کالا (Main Warehouse)' : 'Step 3: Confirm Main Warehouse'}</h3>
          <p style={{ color: '#666', fontSize: '14px', lineHeight: '1.6' }}>
            {isFa
              ? 'انبار مرکزی جهت نگهداری موجودی کالاها و ورود محصولات پس از تحویل خریدها (Goods Receiving) آماده است.'
              : 'Your primary warehouse is configured for stock movements and goods receiving.'}
          </p>
          <button
            onClick={() => setCurrentStep(4)}
            style={{ width: '100%', marginTop: '20px', backgroundColor: '#005AC1', color: '#FFF', border: 'none', borderRadius: '8px', padding: '12px', fontSize: '15px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            {isFa ? 'مرحله بعد: تکمیل راه‌اندازی ➔' : 'Next: Complete Setup ➔'}
          </button>
        </div>
      )}

      {currentStep === 4 && (
        <div>
          <h3 style={{ margin: '0 0 12px 0', fontSize: '16px' }}>{isFa ? 'گام ۴: آمادگی کامل برای شروع کار در رستو' : 'Step 4: Ready for Business Operations'}</h3>
          <p style={{ color: '#666', fontSize: '14px', lineHeight: '1.6' }}>
            {isFa
              ? 'تبریک! راه‌اندازی اولیه سازمان شما به پایان رسید. اکنون می‌توانید وارد پنل مدیریتی، تعریف محصولات، ثبت فاکتور و حسابداری شوید.'
              : 'Congratulations! Onboarding is complete. You can now start managing catalog, sales, inventory, and accounting.'}
          </p>
          <button
            onClick={handleFinish}
            disabled={isCompleting}
            style={{ width: '100%', marginTop: '20px', backgroundColor: '#198754', color: '#FFF', border: 'none', borderRadius: '8px', padding: '14px', fontSize: '16px', fontWeight: 'bold', cursor: 'pointer' }}
          >
            {isCompleting ? (isFa ? 'در حال ورود به داشبورد...' : 'Completing...') : (isFa ? '🎉 ورود به داشبورد اصلی رستو' : '🎉 Enter Resto Dashboard')}
          </button>
        </div>
      )}
    </div>
  );
};
