import React from 'react';
import { useTheme } from '../theme/ThemeContext';
import { ContextualHelp } from './HelpSystem';
import { Language } from '../i18n/authStrings';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  helpSection?: string;
  language?: Language;
  dataTour?: string;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  description,
  actions,
  helpSection,
  language = 'fa',
  dataTour,
}) => {
  const { theme } = useTheme();
  return (
    <div
      data-tour={dataTour}
      style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: theme.spacing['2xl'],
        flexWrap: 'wrap',
        gap: theme.spacing.lg,
      }}
    >
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
          <h1
            style={{
              fontSize: '24px',
              fontWeight: 700,
              color: theme.colors.textPrimary,
              margin: 0,
              letterSpacing: '-0.02em',
            }}
          >
            {title}
          </h1>
          {helpSection && (
            <ContextualHelp language={language} section={helpSection} />
          )}
        </div>
        {description && (
          <p
            style={{
              fontSize: '14px',
              color: theme.colors.textSecondary,
              margin: `${theme.spacing.xs} 0 0 0`,
            }}
          >
            {description}
          </p>
        )}
      </div>

      {actions && (
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.md }}>
          {actions}
        </div>
      )}
    </div>
  );
};

