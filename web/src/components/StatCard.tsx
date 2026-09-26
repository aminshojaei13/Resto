import React from 'react';
import { theme } from '../theme/tokens';

interface StatCardProps {
  title: string;
  value: string;
  subtitle?: string;
  trend?: string;
  isPositive?: boolean;
  icon?: string;
  badgeText?: string;
  badgeColor?: 'success' | 'warning' | 'error' | 'info';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  trend,
  isPositive = true,
  icon,
  badgeText,
  badgeColor = 'info',
}) => {
  const getBadgeStyle = () => {
    switch (badgeColor) {
      case 'success':
        return { bg: theme.colors.successLight, text: theme.colors.success };
      case 'warning':
        return { bg: theme.colors.warningLight, text: theme.colors.warning };
      case 'error':
        return { bg: theme.colors.errorLight, text: theme.colors.error };
      default:
        return { bg: theme.colors.primaryLight, text: theme.colors.primaryDark };
    }
  };

  const badgeStyle = getBadgeStyle();

  return (
    <div
      style={{
        backgroundColor: theme.colors.surface,
        borderRadius: theme.borderRadius.xl,
        padding: theme.spacing['2xl'],
        border: `1px solid ${theme.colors.border}`,
        boxShadow: theme.shadows.card,
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        position: 'relative',
        transition: 'all 0.2s ease',
      }}
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: theme.spacing.md }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
          {icon && (
            <span
              style={{
                fontSize: '18px',
                width: '36px',
                height: '36px',
                borderRadius: theme.borderRadius.md,
                backgroundColor: theme.colors.primaryLight,
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: theme.colors.primaryDark,
              }}
            >
              {icon}
            </span>
          )}
          <span style={{ fontSize: '14px', fontWeight: 600, color: theme.colors.textSecondary }}>{title}</span>
        </div>

        {badgeText && (
          <span
            style={{
              fontSize: '11px',
              fontWeight: 700,
              padding: '2px 8px',
              borderRadius: theme.borderRadius.full,
              backgroundColor: badgeStyle.bg,
              color: badgeStyle.text,
            }}
          >
            {badgeText}
          </span>
        )}
      </div>

      <div style={{ fontSize: '28px', fontWeight: 700, color: theme.colors.textPrimary, letterSpacing: '-0.02em', marginBottom: theme.spacing.xs }}>
        {value}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm, marginTop: theme.spacing.xs }}>
        {trend && (
          <span
            style={{
              fontSize: '12px',
              fontWeight: 700,
              color: isPositive ? theme.colors.success : theme.colors.error,
              display: 'inline-flex',
              alignItems: 'center',
              gap: '2px',
            }}
          >
            {isPositive ? '↑' : '↓'} {trend}
          </span>
        )}
        {subtitle && <span style={{ fontSize: '12px', color: theme.colors.textMuted }}>{subtitle}</span>}
      </div>
    </div>
  );
};
