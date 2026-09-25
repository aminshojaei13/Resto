import React from 'react';
import { theme } from '../theme/tokens';

interface LoadingStateProps {
  message?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({ message = 'در حال بارگذاری اطلاعات...' }) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: theme.spacing['4xl'],
        backgroundColor: theme.colors.surface,
        borderRadius: theme.borderRadius.xl,
        border: `1px solid ${theme.colors.border}`,
        boxShadow: theme.shadows.card,
        margin: `${theme.spacing['2xl']} 0`,
      }}
    >
      <div
        style={{
          width: '40px',
          height: '40px',
          border: `3px solid ${theme.colors.primaryLight}`,
          borderTop: `3px solid ${theme.colors.primary}`,
          borderRadius: '50%',
          animation: 'spin 0.8s linear infinite',
          marginBottom: theme.spacing.lg,
        }}
      />
      <style>{`
        @keyframes spin {
          0% { transform: rotate(0deg); }
          100% { transform: rotate(360deg); }
        }
      `}</style>
      <span style={{ fontSize: '14px', color: theme.colors.textSecondary, fontWeight: 500 }}>
        {message}
      </span>
    </div>
  );
};
