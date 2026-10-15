import React, { useState } from 'react';
import { useTheme } from '../theme/ThemeContext';
import { useReducedMotion } from '../theme/animations';

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
  const prefersReducedMotion = useReducedMotion();
  const [btnHovered, setBtnHovered] = useState(false);

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
        transition: prefersReducedMotion ? 'none' : 'box-shadow 200ms ease',
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
          transition: prefersReducedMotion ? 'none' : 'transform 200ms ease',
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
          onMouseEnter={() => setBtnHovered(true)}
          onMouseLeave={() => setBtnHovered(false)}
          style={{
            backgroundColor: btnHovered ? theme.colors.primaryHover : theme.colors.primary,
            color: '#FFF',
            border: 'none',
            borderRadius: theme.borderRadius.md,
            padding: `${theme.spacing.md} ${theme.spacing.xl}`,
            fontWeight: 600,
            fontSize: '14px',
            cursor: 'pointer',
            transition: prefersReducedMotion ? 'none' : 'background-color 0.2s ease, transform 0.15s ease',
            transform: btnHovered && !prefersReducedMotion ? 'scale(1.02)' : 'none',
          }}
        >
          {actionText}
        </button>
      )}
    </div>
  );
};

