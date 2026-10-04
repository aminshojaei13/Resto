import React from 'react';
import { useTheme } from '../theme/ThemeContext';

interface EmptyStateProps {
  title: string;
  description: string;
  actionText?: string;
  onAction?: () => void;
  icon?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title,
  description,
  actionText,
  onAction,
  icon = '📂',
}) => {
  const { theme } = useTheme();
  return (
    <div
      style={{
        backgroundColor: theme.colors.surface,
        borderRadius: theme.borderRadius.xl,
        padding: theme.spacing['4xl'],
        border: `1px solid ${theme.colors.border}`,
        boxShadow: theme.shadows.card,
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        maxWidth: '480px',
        margin: `${theme.spacing['3xl']} auto`,
      }}
    >
      <div
        style={{
          fontSize: '36px',
          width: '64px',
          height: '64px',
          borderRadius: theme.borderRadius.xl,
          backgroundColor: theme.colors.primaryLight,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          marginBottom: theme.spacing.lg,
        }}
      >
        {icon}
      </div>
      <h3
        style={{
          fontSize: '18px',
          fontWeight: 700,
          color: theme.colors.textPrimary,
          margin: 0,
          marginBottom: theme.spacing.xs,
        }}
      >
        {title}
      </h3>
      <p
        style={{
          fontSize: '14px',
          color: theme.colors.textSecondary,
          margin: 0,
          marginBottom: actionText ? theme.spacing.xl : 0,
          lineHeight: 1.5,
        }}
      >
        {description}
      </p>

      {actionText && onAction && (
        <button
          onClick={onAction}
          style={{
            backgroundColor: theme.colors.primary,
            color: '#FFF',
            border: 'none',
            borderRadius: theme.borderRadius.md,
            padding: `${theme.spacing.md} ${theme.spacing.xl}`,
            fontWeight: 600,
            fontSize: '14px',
            cursor: 'pointer',
            transition: 'background-color 0.2s ease',
          }}
        >
          {actionText}
        </button>
      )}
    </div>
  );
};
