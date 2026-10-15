import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useTheme } from '../../theme/ThemeContext';
import { useReducedMotion } from '../../theme/animations';
import { helpStrings, getSectionHelp, Language } from '../../i18n/helpStrings';

export interface ContextualHelpProps {
  language: Language;
  section: string; // key to look up in helpStrings (e.g. 'products', 'inventory', '/app/pos')
  children?: React.ReactNode; // optional extra content
}

export const ContextualHelp: React.FC<ContextualHelpProps> = ({
  language,
  section,
  children,
}) => {
  const { theme } = useTheme();
  const prefersReducedMotion = useReducedMotion();
  const isFa = language === 'fa';
  const s = helpStrings(language);
  const helpData = getSectionHelp(section, language);

  const [isOpen, setIsOpen] = useState(false);
  const [position, setPosition] = useState<{ top: number; left: number } | null>(null);

  const buttonRef = useRef<HTMLButtonElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);

  const updatePosition = useCallback(() => {
    if (!buttonRef.current) return;
    const rect = buttonRef.current.getBoundingClientRect();
    const panelWidth = Math.min(360, window.innerWidth - 32);
    const estimatedHeight = 380;

    // Vertical placement: prefer below, flip above if not enough space
    let top = rect.bottom + 8;
    if (top + estimatedHeight > window.innerHeight - 16 && rect.top - estimatedHeight - 8 > 16) {
      top = Math.max(16, rect.top - estimatedHeight - 8);
    } else if (top + 160 > window.innerHeight - 16) {
      top = Math.max(16, window.innerHeight - estimatedHeight - 16);
    }

    // Horizontal placement: align to trigger button side depending on direction
    let left: number;
    if (isFa) {
      left = rect.right - panelWidth;
    } else {
      left = rect.left;
    }

    // Viewport bounds clamping
    if (left + panelWidth > window.innerWidth - 16) {
      left = window.innerWidth - panelWidth - 16;
    }
    if (left < 16) {
      left = 16;
    }

    setPosition({ top, left });
  }, [isFa]);

  const handleToggle = () => {
    if (!isOpen) {
      updatePosition();
      setIsOpen(true);
    } else {
      setIsOpen(false);
    }
  };

  useEffect(() => {
    if (!isOpen) return;

    updatePosition();

    const handleResizeOrScroll = () => {
      updatePosition();
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
        buttonRef.current?.focus();
      }
    };

    const handlePointerDown = (e: MouseEvent | TouchEvent) => {
      const target = e.target as Node;
      if (
        panelRef.current &&
        !panelRef.current.contains(target) &&
        buttonRef.current &&
        !buttonRef.current.contains(target)
      ) {
        setIsOpen(false);
      }
    };

    window.addEventListener('resize', handleResizeOrScroll);
    window.addEventListener('scroll', handleResizeOrScroll, true);
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('touchstart', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('resize', handleResizeOrScroll);
      window.removeEventListener('scroll', handleResizeOrScroll, true);
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('touchstart', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, updatePosition]);

  return (
    <div style={{ position: 'relative', display: 'inline-flex', verticalAlign: 'middle' }}>
      <button
        ref={buttonRef}
        type="button"
        onClick={handleToggle}
        aria-label={s.openContextualHelp}
        aria-haspopup="dialog"
        aria-expanded={isOpen}
        style={{
          width: '28px',
          height: '28px',
          borderRadius: '50%',
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: isOpen ? theme.colors.primaryLight : theme.colors.surfaceElevated,
          color: isOpen ? theme.colors.primaryDark : theme.colors.textSecondary,
          border: `1px solid ${isOpen ? theme.colors.primary : theme.colors.border}`,
          cursor: 'pointer',
          padding: 0,
          fontSize: '13px',
          lineHeight: 1,
          transition: prefersReducedMotion ? 'none' : 'all 150ms ease-out',
          boxShadow: theme.shadows.sm,
          userSelect: 'none',
        }}
      >
        ❓
      </button>

      {isOpen && position && (
        <div
          ref={panelRef}
          role="dialog"
          aria-modal="false"
          aria-label={helpData ? helpData.title : s.helpPanelTitle}
          style={{
            position: 'fixed',
            top: `${position.top}px`,
            left: `${position.left}px`,
            width: 'min(360px, calc(100vw - 32px))',
            maxHeight: 'calc(100vh - 48px)',
            overflowY: 'auto',
            backgroundColor: theme.colors.surface,
            border: `1px solid ${theme.colors.borderStrong}`,
            borderRadius: theme.borderRadius.xl,
            boxShadow: theme.shadows.lg,
            zIndex: 1050,
            direction: isFa ? 'rtl' : 'ltr',
            textAlign: isFa ? 'right' : 'left',
            fontFamily: theme.typography.fontFamily,
            color: theme.colors.textPrimary,
            boxSizing: 'border-box',
            animation: prefersReducedMotion ? 'none' : 'fadeIn 150ms ease-out',
            opacity: 1,
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: `${theme.spacing.md} ${theme.spacing.lg}`,
              borderBottom: `1px solid ${theme.colors.border}`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: theme.spacing.sm,
              backgroundColor: theme.colors.surfaceElevated,
              borderTopLeftRadius: theme.borderRadius.xl,
              borderTopRightRadius: theme.borderRadius.xl,
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: theme.spacing.sm }}>
              <span style={{ fontSize: '16px' }}>💡</span>
              <span style={{ fontWeight: 700, fontSize: '14px', color: theme.colors.textPrimary }}>
                {helpData ? helpData.title : s.helpPanelTitle}
              </span>
            </div>

            <button
              type="button"
              onClick={() => setIsOpen(false)}
              aria-label={s.closeHelp}
              style={{
                background: 'transparent',
                border: 'none',
                color: theme.colors.textMuted,
                cursor: 'pointer',
                fontSize: '16px',
                padding: '4px',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                borderRadius: theme.borderRadius.sm,
                lineHeight: 1,
              }}
            >
              ✕
            </button>
          </div>

          {/* Body */}
          <div style={{ padding: theme.spacing.lg, display: 'flex', flexDirection: 'column', gap: theme.spacing.md }}>
            {/* Purpose / Help text */}
            <div>
              <div
                style={{
                  fontSize: '11px',
                  fontWeight: 700,
                  textTransform: 'uppercase',
                  color: theme.colors.primaryDark,
                  marginBottom: theme.spacing.xs,
                  letterSpacing: '0.04em',
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
                {helpData ? helpData.help : s.noHelpAvailable}
              </p>
            </div>

            {/* Prerequisites */}
            {helpData?.prerequisites && (
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
                <div
                  style={{
                    fontSize: '12px',
                    lineHeight: '1.5',
                    color: theme.colors.textSecondary,
                  }}
                >
                  {helpData.prerequisites}
                </div>
              </div>
            )}

            {/* Steps */}
            {helpData?.steps && (
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
            {helpData?.importantFields && (
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
            {helpData?.afterSaving && (
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
            {helpData?.tip && (
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

            {/* Custom children if passed */}
            {children && (
              <div
                style={{
                  marginTop: theme.spacing.xs,
                  paddingTop: theme.spacing.sm,
                  borderTop: `1px solid ${theme.colors.border}`,
                }}
              >
                {children}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
