import React, { useState, useRef, useEffect, useCallback } from 'react';
import { useTheme } from '../../theme/ThemeContext';
import { useReducedMotion } from '../../theme/animations';
import { helpStrings, getSectionHelp, Language } from '../../i18n/helpStrings';

export interface HelpButtonProps {
  language: Language;
  currentSection?: string;
  onStartTour?: () => void;
  onRestartTour?: () => void;
}

export const HelpButton: React.FC<HelpButtonProps> = ({
  language,
  currentSection,
  onStartTour,
  onRestartTour,
}) => {
  const { theme } = useTheme();
  const prefersReducedMotion = useReducedMotion();
  const isFa = language === 'fa';
  const s = helpStrings(language);

  const [isOpen, setIsOpen] = useState(false);
  const [dropdownPos, setDropdownPos] = useState<{ top: number; left: number } | null>(null);

  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const currentHelpData = currentSection ? getSectionHelp(currentSection, language) : undefined;

  const updatePosition = useCallback(() => {
    if (!buttonRef.current) return;
    const rect = buttonRef.current.getBoundingClientRect();
    const menuWidth = Math.min(320, window.innerWidth - 32);

    let top = rect.bottom + 6;
    if (top + 340 > window.innerHeight - 16 && rect.top - 340 > 16) {
      top = Math.max(16, rect.top - 340);
    }

    let left: number;
    if (isFa) {
      left = rect.right - menuWidth;
    } else {
      left = rect.left;
    }

    if (left + menuWidth > window.innerWidth - 16) {
      left = window.innerWidth - menuWidth - 16;
    }
    if (left < 16) {
      left = 16;
    }

    setDropdownPos({ top, left });
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

    const handleResize = () => updatePosition();
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
        buttonRef.current?.focus();
      }
    };
    const handlePointerDown = (e: MouseEvent | TouchEvent) => {
      const target = e.target as Node;
      if (
        menuRef.current &&
        !menuRef.current.contains(target) &&
        buttonRef.current &&
        !buttonRef.current.contains(target)
      ) {
        setIsOpen(false);
      }
    };

    window.addEventListener('resize', handleResize);
    window.addEventListener('scroll', handleResize, true);
    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('touchstart', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('scroll', handleResize, true);
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('touchstart', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, updatePosition]);

  return (
    <div style={{ position: 'relative', display: 'inline-block' }}>
      <button
        ref={buttonRef}
        type="button"
        onClick={handleToggle}
        aria-label={s.helpMenuLabel}
        aria-haspopup="menu"
        aria-expanded={isOpen}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: theme.spacing.xs,
          backgroundColor: isOpen ? theme.colors.surfaceSelected : theme.colors.surfaceHover,
          border: `1px solid ${isOpen ? theme.colors.primary : theme.colors.border}`,
          borderRadius: theme.borderRadius.md,
          padding: '6px 12px',
          fontSize: '13px',
          fontWeight: 600,
          color: theme.colors.textPrimary,
          cursor: 'pointer',
          transition: prefersReducedMotion ? 'none' : 'background-color 150ms ease, border-color 150ms ease',
          lineHeight: 1.2,
          boxShadow: theme.shadows.sm,
          userSelect: 'none',
        }}
      >
        <span style={{ fontSize: '14px' }}>❓</span>
        <span>{s.helpMenuLabel}</span>
      </button>

      {isOpen && dropdownPos && (
        <div
          ref={menuRef}
          role="menu"
          aria-label={s.helpMenuLabel}
          style={{
            position: 'fixed',
            top: `${dropdownPos.top}px`,
            left: `${dropdownPos.left}px`,
            width: 'min(320px, calc(100vw - 32px))',
            maxHeight: 'calc(100vh - 48px)',
            overflowY: 'auto',
            backgroundColor: theme.colors.surface,
            border: `1px solid ${theme.colors.borderStrong}`,
            borderRadius: theme.borderRadius.lg,
            boxShadow: theme.shadows.lg,
            zIndex: 1050,
            direction: isFa ? 'rtl' : 'ltr',
            textAlign: isFa ? 'right' : 'left',
            fontFamily: theme.typography.fontFamily,
            padding: theme.spacing.sm,
            boxSizing: 'border-box',
            animation: prefersReducedMotion ? 'none' : 'fadeIn 150ms ease-out',
          }}
        >
          {/* Guided Tour Actions */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setIsOpen(false);
                onStartTour?.();
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: theme.spacing.sm,
                width: '100%',
                padding: `${theme.spacing.sm} ${theme.spacing.md}`,
                backgroundColor: 'transparent',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                color: theme.colors.textPrimary,
                fontSize: '13px',
                fontWeight: 500,
                cursor: 'pointer',
                textAlign: isFa ? 'right' : 'left',
                transition: prefersReducedMotion ? 'none' : 'background-color 120ms ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = theme.colors.surfaceHover;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
              }}
            >
              <span style={{ fontSize: '15px' }}>🧭</span>
              <span style={{ flex: 1 }}>{s.startTour}</span>
            </button>

            <button
              type="button"
              role="menuitem"
              onClick={() => {
                setIsOpen(false);
                onRestartTour?.();
              }}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: theme.spacing.sm,
                width: '100%',
                padding: `${theme.spacing.sm} ${theme.spacing.md}`,
                backgroundColor: 'transparent',
                border: 'none',
                borderRadius: theme.borderRadius.md,
                color: theme.colors.textPrimary,
                fontSize: '13px',
                fontWeight: 500,
                cursor: 'pointer',
                textAlign: isFa ? 'right' : 'left',
                transition: prefersReducedMotion ? 'none' : 'background-color 120ms ease',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.backgroundColor = theme.colors.surfaceHover;
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.backgroundColor = 'transparent';
              }}
            >
              <span style={{ fontSize: '15px' }}>🔄</span>
              <span style={{ flex: 1 }}>{s.restartTour}</span>
            </button>
          </div>

          {/* Divider */}
          <div
            style={{
              height: '1px',
              backgroundColor: theme.colors.border,
              margin: `${theme.spacing.sm} 0`,
            }}
          />

          {/* Contextual Section Help Details */}
          {currentHelpData ? (
            <div
              style={{
                padding: theme.spacing.sm,
                backgroundColor: theme.colors.surfaceElevated,
                borderRadius: theme.borderRadius.md,
                border: `1px solid ${theme.colors.border}`,
                display: 'flex',
                flexDirection: 'column',
                gap: theme.spacing.xs,
              }}
            >
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: theme.spacing.xs,
                  fontWeight: 700,
                  fontSize: '12px',
                  color: theme.colors.primaryDark,
                }}
              >
                <span>📖</span>
                <span>{currentHelpData.title}</span>
              </div>
              <p
                style={{
                  fontSize: '12px',
                  lineHeight: '1.5',
                  color: theme.colors.textSecondary,
                  margin: 0,
                }}
              >
                {currentHelpData.help}
              </p>
              {currentHelpData.tip && (
                <div
                  style={{
                    fontSize: '11px',
                    lineHeight: '1.4',
                    color: theme.colors.textPrimary,
                    backgroundColor: theme.colors.primaryLight,
                    padding: `${theme.spacing.xs} ${theme.spacing.sm}`,
                    borderRadius: theme.borderRadius.sm,
                    marginTop: '2px',
                  }}
                >
                  <strong>💡 {s.tipPrefix}:</strong> {currentHelpData.tip}
                </div>
              )}
            </div>
          ) : (
            <div
              style={{
                padding: `${theme.spacing.xs} ${theme.spacing.sm}`,
                fontSize: '12px',
                color: theme.colors.textMuted,
              }}
            >
              ℹ️ {s.helpPanelTitle}
            </div>
          )}

          {/* Divider */}
          <div
            style={{
              height: '1px',
              backgroundColor: theme.colors.border,
              margin: `${theme.spacing.sm} 0`,
            }}
          />

          {/* About Resto & Version Info */}
          <div
            style={{
              padding: `${theme.spacing.xs} ${theme.spacing.sm}`,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '11px',
              color: theme.colors.textMuted,
            }}
          >
            <span style={{ fontWeight: 600 }}>🍽️ Resto OS</span>
            <span>{s.versionInfo}</span>
          </div>
        </div>
      )}
    </div>
  );
};
