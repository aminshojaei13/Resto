import React, { useState, useEffect } from 'react';
import { useTheme } from '../../theme/ThemeContext';
import { useReducedMotion } from '../../theme/animations';
import { helpStrings, getSectionHelp, normalizeSectionKey, Language } from '../../i18n/helpStrings';

export interface SectionHelpProps {
  language: Language;
  sectionKey: string;
  // Whether to show a tip icon instead of full card
  compact?: boolean;
}

export const SectionHelp: React.FC<SectionHelpProps> = ({
  language,
  sectionKey,
  compact = false,
}) => {
  const { theme } = useTheme();
  const prefersReducedMotion = useReducedMotion();
  const isFa = language === 'fa';
  const s = helpStrings(language);
  const helpData = getSectionHelp(sectionKey, language);

  const normalizedKey = normalizeSectionKey(sectionKey);
  const storageKey = `resto_help_collapsed_${normalizedKey}`;

  const [isCollapsed, setIsCollapsed] = useState<boolean>(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved !== null) {
        return saved === 'true';
      }
    } catch {
      // Ignore storage access errors
    }
    return false;
  });

  useEffect(() => {
    try {
      const saved = localStorage.getItem(storageKey);
      if (saved !== null) {
        setIsCollapsed(saved === 'true');
      }
    } catch {
      // Ignore
    }
  }, [storageKey]);

  if (!helpData) {
    return null;
  }

  const toggleCollapsed = () => {
    setIsCollapsed((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(storageKey, String(next));
      } catch {
        // Ignore
      }
      return next;
    });
  };

  // Compact variant: focused tip/guidance badge
  if (compact) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: theme.spacing.sm,
          backgroundColor: theme.colors.surfaceElevated,
          border: `1px solid ${theme.colors.border}`,
          borderRadius: theme.borderRadius.md,
          padding: `${theme.spacing.xs} ${theme.spacing.md}`,
          marginBottom: theme.spacing.lg,
          direction: isFa ? 'rtl' : 'ltr',
          textAlign: isFa ? 'right' : 'left',
          fontFamily: theme.typography.fontFamily,
          boxShadow: theme.shadows.sm,
        }}
      >
        <span style={{ fontSize: '15px', flexShrink: 0 }}>💡</span>
        <div style={{ flex: 1, fontSize: '12px', color: theme.colors.textSecondary, lineHeight: '1.4' }}>
          <strong style={{ color: theme.colors.textPrimary }}>{helpData.title}: </strong>
          {helpData.tip || helpData.help}
        </div>
      </div>
    );
  }

  // Full Collapsible Section Help Card
  return (
    <aside
      aria-label={`${helpData.title} ${s.helpPanelTitle}`}
      style={{
        backgroundColor: theme.colors.surface,
        border: `1px solid ${theme.colors.border}`,
        borderRadius: theme.borderRadius.lg,
        marginBottom: theme.spacing.xl,
        direction: isFa ? 'rtl' : 'ltr',
        textAlign: isFa ? 'right' : 'left',
        fontFamily: theme.typography.fontFamily,
        boxShadow: theme.shadows.sm,
        overflow: 'hidden',
        transition: prefersReducedMotion ? 'none' : 'border-color 150ms ease, box-shadow 150ms ease',
      }}
    >
      {/* Header bar / Collapsible toggle */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: `${theme.spacing.sm} ${theme.spacing.lg}`,
          backgroundColor: theme.colors.surfaceElevated,
          borderBottom: isCollapsed ? 'none' : `1px solid ${theme.colors.border}`,
          cursor: 'pointer',
          userSelect: 'none',
        }}
        onClick={toggleCollapsed}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
          <span style={{ fontSize: '16px' }}>💡</span>
          <span
            style={{
              fontWeight: 700,
              fontSize: '13px',
              color: theme.colors.textPrimary,
            }}
          >
            {helpData.title} — {s.helpPanelTitle}
          </span>
        </div>

        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            toggleCollapsed();
          }}
          aria-expanded={!isCollapsed}
          aria-label={isCollapsed ? s.expandHelp : s.collapseHelp}
          style={{
            background: 'transparent',
            border: `1px solid ${theme.colors.border}`,
            borderRadius: theme.borderRadius.sm,
            padding: `2px ${theme.spacing.sm}`,
            fontSize: '12px',
            fontWeight: 600,
            color: theme.colors.textSecondary,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: theme.spacing.xs,
          }}
        >
          <span>{isCollapsed ? s.expandHelp : s.collapseHelp}</span>
          <span
            style={{
              display: 'inline-block',
              transition: prefersReducedMotion ? 'none' : 'transform 200ms ease',
              transform: isCollapsed ? 'rotate(0deg)' : 'rotate(180deg)',
              fontSize: '10px',
            }}
          >
            ▼
          </span>
        </button>
      </div>

      {/* Expandable Content Area */}
      <div
        style={{
          maxHeight: isCollapsed ? '0px' : '1200px',
          opacity: isCollapsed ? 0 : 1,
          overflow: 'hidden',
          transition: prefersReducedMotion
            ? 'none'
            : 'max-height 250ms cubic-bezier(0.4, 0, 0.2, 1), opacity 200ms ease',
        }}
      >
        <div
          style={{
            padding: theme.spacing.lg,
            display: 'flex',
            flexDirection: 'column',
            gap: theme.spacing.md,
          }}
        >
          {/* Section Purpose */}
          <div>
            <div
              style={{
                fontSize: '11px',
                fontWeight: 700,
                textTransform: 'uppercase',
                color: theme.colors.primaryDark,
                marginBottom: theme.spacing.xs,
                letterSpacing: '0.03em',
              }}
            >
              {s.whatIsThisSection}
            </div>
            <p
              style={{
                fontSize: '13px',
                lineHeight: '1.6',
                color: theme.colors.textSecondary,
                margin: 0,
              }}
            >
              {helpData.help}
            </p>
          </div>

          {/* Prerequisites */}
          {helpData.prerequisites && (
            <div
              style={{
                backgroundColor: theme.colors.surfaceElevated,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.sm,
              }}
            >
              <div
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  color: theme.colors.textPrimary,
                  marginBottom: '4px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: theme.spacing.xs,
                }}
              >
                <span>📋</span>
                <span>{s.prerequisites}</span>
              </div>
              <div style={{ fontSize: '12px', lineHeight: '1.5', color: theme.colors.textSecondary }}>
                {helpData.prerequisites}
              </div>
            </div>
          )}

          {/* Steps */}
          {helpData.steps && (
            <div>
              <div
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  color: theme.colors.primaryDark,
                  marginBottom: theme.spacing.xs,
                  display: 'flex',
                  alignItems: 'center',
                  gap: theme.spacing.xs,
                }}
              >
                <span>🔢</span>
                <span>{s.steps}</span>
              </div>
              <div
                style={{
                  fontSize: '12px',
                  lineHeight: '1.6',
                  color: theme.colors.textSecondary,
                  whiteSpace: 'pre-line',
                  backgroundColor: theme.colors.surfaceElevated,
                  padding: theme.spacing.sm,
                  borderRadius: theme.borderRadius.md,
                  border: `1px solid ${theme.colors.border}`,
                }}
              >
                {helpData.steps}
              </div>
            </div>
          )}

          {/* Important Fields */}
          {helpData.importantFields && (
            <div>
              <div
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  color: theme.colors.textPrimary,
                  marginBottom: theme.spacing.xs,
                }}
              >
                📝 {s.importantFields}
              </div>
              <div style={{ fontSize: '12px', lineHeight: '1.5', color: theme.colors.textSecondary }}>
                {helpData.importantFields}
              </div>
            </div>
          )}

          {/* After Saving */}
          {helpData.afterSaving && (
            <div>
              <div
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  color: theme.colors.textPrimary,
                  marginBottom: theme.spacing.xs,
                }}
              >
                💾 {s.afterSaving}
              </div>
              <div style={{ fontSize: '12px', lineHeight: '1.5', color: theme.colors.textSecondary }}>
                {helpData.afterSaving}
              </div>
            </div>
          )}

          {/* Practical Tip */}
          {helpData.tip && (
            <div
              style={{
                backgroundColor: theme.colors.primaryLight,
                border: `1px solid ${theme.colors.primary}`,
                borderRadius: theme.borderRadius.md,
                padding: theme.spacing.sm,
                display: 'flex',
                alignItems: 'flex-start',
                gap: theme.spacing.xs,
              }}
            >
              <span style={{ fontSize: '14px', flexShrink: 0 }}>💡</span>
              <div style={{ fontSize: '12px', lineHeight: '1.5', color: theme.colors.primaryDark }}>
                <strong>{s.tipPrefix}:</strong> {helpData.tip}
              </div>
            </div>
          )}
        </div>
      </div>
    </aside>
  );
};
