import React, { useEffect, useState } from 'react';
import { ApiError, apiClient } from '../api/apiClient';
import { PageHeader } from '../components/PageHeader';
import { FormError, InfoNote, PrimaryButton, SecondaryButton, TextInput } from '../components/Field';
import { BusinessSettings } from '../types';
import { useTheme } from '../theme/ThemeContext';
import { opsStrings } from '../i18n/opsStrings';
import { formatPercent, formatPercentFa } from '../util/units';

interface SettingsPageProps {
  language?: 'fa' | 'en';
}

/**
 * Business settings.
 *
 * The default order tax rate lives here. This is the only place it is set;
 * every order takes it from this value unless an authorised manager overrides
 * it for a single order.
 */
export const SettingsPage: React.FC<SettingsPageProps> = ({ language = 'fa' }) => {
  const s = opsStrings(language);
  const isFa = language === 'fa';
  const { theme } = useTheme();

  const [settings, setSettings] = useState<BusinessSettings | null>(null);
  /** Kept as text while typing so a half-typed decimal is not fought with. */
  const [rateText, setRateText] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [notice, setNotice] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    apiClient
      .getBusinessSettings()
      .then((value) => {
        setSettings(value);
        setRateText(String(value.defaultTaxRate));
      })
      .catch((err) =>
        setFormError(err instanceof ApiError ? err.message : isFa ? 'دریافت تنظیمات ناموفق بود.' : 'Could not load settings.')
      );
  }, [isFa]);

  const save = async (e: React.FormEvent) => {
    e.preventDefault();

    const parsed = Number(rateText);

    if (rateText.trim() === '' || Number.isNaN(parsed) || !Number.isFinite(parsed)) {
      setErrors({ rate: isFa ? 'نرخ مالیات باید عدد باشد.' : 'The tax rate must be a number.' });
      return;
    }

    if (parsed < 0 || parsed > 100) {
      setErrors({ rate: isFa ? 'نرخ مالیات باید بین ۰ تا ۱۰۰ باشد.' : 'The tax rate must be between 0 and 100.' });
      return;
    }

    setIsSaving(true);
    setErrors({});
    setFormError('');
    setNotice('');

    try {
      const updated = await apiClient.updateBusinessSettings({ default_tax_rate: parsed });
      setSettings(updated);
      setRateText(String(updated.defaultTaxRate));
      setNotice(s.settingsSaved);
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(toFieldErrors(err.fieldErrors));
        setFormError(err.message);
      } else {
        setFormError(isFa ? 'ذخیره تنظیمات ناموفق بود.' : 'Could not save settings.');
      }
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: theme.spacing.xl, maxWidth: 640 }}>
      <PageHeader title={s.businessSettingsTitle} description={s.businessSettingsSubtitle} />

      <FormError message={formError} />
      <FormError message={notice} />

      <form
        onSubmit={save}
        style={{
          backgroundColor: theme.colors.surface,
          border: `1px solid ${theme.colors.border}`,
          borderRadius: theme.borderRadius.xl,
          padding: theme.spacing['2xl'],
          display: 'flex',
          flexDirection: 'column',
          gap: theme.spacing.lg,
          boxShadow: theme.shadows.card,
        }}
      >
        <TextInput
          label={`${s.defaultOrderTax} (%)`}
          required
          type="number"
          min={0}
          max={100}
          step="0.01"
          value={rateText}
          error={errors.default_tax_rate ?? errors.rate}
          hint={s.defaultOrderTaxHint}
          onChange={(e) => setRateText(e.target.value)}
        />

        {settings && (
          <InfoNote>
            {settings.canOverrideTaxPerOrder ? s.taxOverridable : s.taxNotOverridable}
            {isFa ? ` نرخ فعلی: ${formatPercentFa(settings.defaultTaxRate)}` : ` Current rate: ${formatPercent(settings.defaultTaxRate)}%`}
          </InfoNote>
        )}

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: theme.spacing.md }}>
          <SecondaryButton
            type="button"
            onClick={() => settings && setRateText(String(settings.defaultTaxRate))}
            disabled={isSaving}
          >
            {isFa ? 'بازگردانی' : 'Reset'}
          </SecondaryButton>
          <PrimaryButton type="submit" disabled={isSaving}>
            {isSaving ? s.saving : s.saveSettings}
          </PrimaryButton>
        </div>
      </form>
    </div>
  );
};
const toFieldErrors = (fieldErrors: Record<string, string[]>): Record<string, string> =>
  Object.fromEntries(Object.entries(fieldErrors).map(([key, value]) => [key, value[0] ?? '']));
