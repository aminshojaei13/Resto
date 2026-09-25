import React from 'react';
import { theme } from '../theme/tokens';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
}

export const PageHeader: React.FC<PageHeaderProps> = ({ title, description, actions }) => {
  return (
    <div
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
