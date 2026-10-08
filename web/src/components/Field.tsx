import React from 'react';
import { useTheme } from '../theme/ThemeContext';

/**
 * Small, shared form pieces.
 *
 * Every operational screen validates per field and shows the message next to
 * the field it belongs to, so nobody has to guess which value was wrong. All
 * colours come from the central theme, which is what keeps the Warm Dark
 * theme intact without any per-screen colours.
 */

/**
 * Server field errors arrive as lists. A field shows one message, and the list
 * is preserved rather than discarded.
 */
export const firstFieldError = (
  fieldErrors: Record<string, string[]> | undefined,
  field: string
): string | undefined => {
  const value = fieldErrors?.[field];
  return Array.isArray(value) && value.length > 0 ? value[0] : undefined;
};

interface FieldShellProps {
  label: string;
  error?: string;
  hint?: string;
  required?: boolean;
  children: React.ReactNode;
}

export const FieldShell: React.FC<FieldShellProps> = ({ label, error, hint, required, children }) => {
  const { theme } = useTheme();
  // The label has to be tied to the control it names: otherwise the field has
  // no accessible name, clicking the label does not focus it, and assistive
  // technology reads the form as a wall of unlabelled inputs.
  const generatedId = React.useId();
  const controlId = (children as React.ReactElement<{ id?: string; 'aria-labelledby'?: string }>)?.props?.id;
  const labelledById = `${generatedId}-label`;
  const fieldId = controlId ?? generatedId;

  const child =
    controlId || (children as React.ReactElement<{ 'aria-labelledby'?: string }>)?.props?.['aria-labelledby'] ? (
      children
    ) : (
      React.cloneElement(children as React.ReactElement<Record<string, unknown>>, {
        id: fieldId,
        'aria-labelledby': labelledById,
        'aria-invalid': error ? true : undefined,
        'aria-describedby': hint && !error ? `${generatedId}-hint` : undefined,
      })
    );

  return (
    <div>
      <label
        id={labelledById}
        htmlFor={fieldId}
        style={{
          display: 'block',
          fontSize: '13px',
          fontWeight: 700,
          marginBottom: '4px',
          color: error ? theme.colors.error : theme.colors.textPrimary,
        }}
      >
        {label}
        {required ? ' *' : ''}
      </label>

      {child}

      {hint && !error && (
        <p
          id={`${generatedId}-hint`}
          style={{ margin: '4px 0 0', fontSize: '12px', color: theme.colors.textSecondary }}
        >
          {hint}
        </p>
      )}

      {error && (
        <p
          role="alert"
          style={{ margin: '4px 0 0', fontSize: '12px', fontWeight: 600, color: theme.colors.error }}
        >
          {error}
        </p>
      )}
    </div>
  );
};

interface TextInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

export const TextInput: React.FC<TextInputProps> = ({ label, error, hint, required, ...rest }) => {
  const { theme } = useTheme();

  return (
    <FieldShell label={label} error={error} hint={hint} required={required}>
      <input
        {...rest}
        style={{
          width: '100%',
          padding: '10px 12px',
          borderRadius: theme.borderRadius.md,
          border: `1px solid ${error ? theme.colors.error : theme.colors.border}`,
          fontSize: '14px',
          boxSizing: 'border-box',
          backgroundColor: theme.colors.surfaceElevated,
          color: theme.colors.textPrimary,
          ...rest.style,
        }}
      />
    </FieldShell>
  );
};

interface SelectInputProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label: string;
  error?: string;
  hint?: string;
  children: React.ReactNode;
}

export const SelectInput: React.FC<SelectInputProps> = ({ label, error, hint, required, children, ...rest }) => {
  const { theme } = useTheme();

  return (
    <FieldShell label={label} error={error} hint={hint} required={required}>
      <select
        {...rest}
        style={{
          width: '100%',
          padding: '10px 12px',
          borderRadius: theme.borderRadius.md,
          border: `1px solid ${error ? theme.colors.error : theme.colors.border}`,
          fontSize: '14px',
          boxSizing: 'border-box',
          backgroundColor: theme.colors.surfaceElevated,
          color: theme.colors.textPrimary,
          ...rest.style,
        }}
      >
        {children}
      </select>
    </FieldShell>
  );
};

interface ModalShellProps {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
  maxWidth?: number;
}

export const ModalShell: React.FC<ModalShellProps> = ({ title, onClose, children, footer, maxWidth = 560 }) => {
  const { theme } = useTheme();

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: theme.colors.overlay,
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'center',
        padding: '24px',
        overflowY: 'auto',
        zIndex: 1000,
      }}
      onClick={onClose}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          backgroundColor: theme.colors.surface,
          border: `1px solid ${theme.colors.border}`,
          borderRadius: theme.borderRadius.xl,
          width: '100%',
          maxWidth,
          padding: theme.spacing['2xl'],
          margin: 'auto',
          boxShadow: theme.shadows.lg,
          fontFamily: theme.typography.fontFamily,
        }}
      >
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: theme.spacing.xl,
          }}
        >
          <h3 style={{ margin: 0, fontSize: '18px', fontWeight: 800, color: theme.colors.textPrimary }}>{title}</h3>
          <button
            onClick={onClose}
            aria-label="close"
            style={{ background: 'none', border: 'none', fontSize: '18px', cursor: 'pointer', color: theme.colors.textMuted }}
          >
            ✕
          </button>
        </div>

        {children}

        {footer && <div style={{ marginTop: theme.spacing.xl }}>{footer}</div>}
      </div>
    </div>
  );
};

export const PrimaryButton: React.FC<React.ButtonHTMLAttributes<HTMLButtonElement>> = ({ children, disabled, ...rest }) => {
  const { theme } = useTheme();

  return (
    <button
      {...rest}
      disabled={disabled}
      style={{
        backgroundColor: disabled ? theme.colors.surfaceHover : theme.colors.primary,
        color: disabled ? theme.colors.textMuted : theme.colors.primaryTextOnBrand,
        border: 'none',
        borderRadius: theme.borderRadius.md,
        padding: '10px 20px',
        fontWeight: 700,
        fontSize: '14px',
        cursor: disabled ? 'not-allowed' : 'pointer',
        ...rest.style,
      }}
    >
      {children}
    </button>
  );
};

export const SecondaryButton: React.FC<React.ButtonHTMLAttributes<HTMLButtonElement>> = ({ children, disabled, ...rest }) => {
  const { theme } = useTheme();

  return (
    <button
      {...rest}
      disabled={disabled}
      style={{
        backgroundColor: theme.colors.background,
        color: theme.colors.textPrimary,
        border: `1px solid ${theme.colors.border}`,
        borderRadius: theme.borderRadius.md,
        padding: '10px 20px',
        fontWeight: 600,
        fontSize: '14px',
        cursor: disabled ? 'not-allowed' : 'pointer',
        ...rest.style,
      }}
    >
      {children}
    </button>
  );
};

/**
 * A form-level failure. It is never swallowed: if the server refused the
 * request, the screen says so instead of pretending the work was done.
 */
export const FormError: React.FC<{ message: string }> = ({ message }) => {
  const { theme } = useTheme();

  if (!message) return null;

  return (
    <div
      role="alert"
      style={{
        backgroundColor: theme.colors.errorLight,
        color: theme.colors.error,
        padding: theme.spacing.md,
        borderRadius: theme.borderRadius.md,
        marginBottom: theme.spacing.lg,
        fontSize: '13px',
        fontWeight: 700,
      }}
    >
      {message}
    </div>
  );
};

export const InfoNote: React.FC<{ title?: string; children: React.ReactNode }> = ({ title, children }) => {
  const { theme } = useTheme();

  return (
    <div
      style={{
        backgroundColor: theme.colors.primaryLight,
        border: `1px solid ${theme.colors.border}`,
        borderRadius: theme.borderRadius.lg,
        padding: theme.spacing.lg,
        color: theme.colors.textPrimary,
      }}
    >
      {title && (
        <strong style={{ display: 'block', marginBottom: 4, fontSize: '13px' }}>{title}</strong>
      )}
      <p style={{ margin: 0, fontSize: '13px', lineHeight: 1.7, color: theme.colors.textSecondary }}>{children}</p>
    </div>
  );
};