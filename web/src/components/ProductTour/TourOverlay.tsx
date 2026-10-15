import React, { useEffect, useRef, useState, useCallback } from 'react';
import { TourStep } from './tourTypes';
import { useTheme } from '../../theme/ThemeContext';
import { useReducedMotion } from '../../theme/animations';

export interface TourOverlayProps {
  step: TourStep;
  stepIndex: number;
  totalSteps: number;
  targetElement: HTMLElement | null;
  language: 'fa' | 'en';
  onNext: () => void;
  onBack: () => void;
  onSkip: () => void;
  onFinish: () => void;
}

export const TourOverlay: React.FC<TourOverlayProps> = ({
  step,
  stepIndex,
  totalSteps,
  targetElement,
  language,
  onNext,
  onBack,
  onSkip,
  onFinish,
}) => {
  const { theme, effectiveMode } = useTheme();
  const isDark = effectiveMode === 'warmDark';
  const reducedMotion = useReducedMotion();
  const isFa = language === 'fa';

  const tooltipRef = useRef<HTMLDivElement>(null);
  const primaryBtnRef = useRef<HTMLButtonElement>(null);

  const [targetRect, setTargetRect] = useState<DOMRect | null>(null);
  const [coords, setCoords] = useState<{ top: number; left: number }>({ top: 0, left: 0 });
  const [isVisible, setIsVisible] = useState<boolean>(false);

  const isLastStep = stepIndex >= totalSteps - 1;

  // Track target element bounding rect
  const updateTargetRect = useCallback(() => {
    if (!targetElement) {
      setTargetRect(null);
      return;
    }
    const rect = targetElement.getBoundingClientRect();
    setTargetRect(rect);
  }, [targetElement]);

  useEffect(() => {
    updateTargetRect();
    window.addEventListener('resize', updateTargetRect);
    window.addEventListener('scroll', updateTargetRect, true);

    return () => {
      window.removeEventListener('resize', updateTargetRect);
      window.removeEventListener('scroll', updateTargetRect, true);
    };
  }, [updateTargetRect, stepIndex]);

  // Compute tooltip position relative to target or viewport center
  const calculatePosition = useCallback(() => {
    const tooltipEl = tooltipRef.current;
    const tooltipWidth = tooltipEl ? tooltipEl.offsetWidth : 360;
    const tooltipHeight = tooltipEl ? tooltipEl.offsetHeight : 220;

    const viewportW = typeof window !== 'undefined' ? window.innerWidth : 1200;
    const viewportH = typeof window !== 'undefined' ? window.innerHeight : 800;
    const edgeMargin = 16;
    const gap = 12;

    if (!targetRect) {
      // Missing target: center tooltip on screen
      const top = Math.max(edgeMargin, (viewportH - tooltipHeight) / 2);
      const left = Math.max(edgeMargin, (viewportW - tooltipWidth) / 2);
      setCoords({ top, left });
      setIsVisible(true);
      return;
    }

    const spaceAbove = targetRect.top;
    const spaceBelow = viewportH - targetRect.bottom;
    const spaceLeft = targetRect.left;
    const spaceRight = viewportW - targetRect.right;

    let chosenPlacement = step.placement || 'auto';

    if (chosenPlacement === 'auto') {
      if (spaceBelow >= tooltipHeight + gap + edgeMargin) {
        chosenPlacement = 'bottom';
      } else if (spaceAbove >= tooltipHeight + gap + edgeMargin) {
        chosenPlacement = 'top';
      } else if (isFa && spaceLeft >= tooltipWidth + gap + edgeMargin) {
        chosenPlacement = 'left';
      } else if (!isFa && spaceRight >= tooltipWidth + gap + edgeMargin) {
        chosenPlacement = 'right';
      } else if (spaceRight >= tooltipWidth + gap + edgeMargin) {
        chosenPlacement = 'right';
      } else if (spaceLeft >= tooltipWidth + gap + edgeMargin) {
        chosenPlacement = 'left';
      } else {
        chosenPlacement = spaceBelow >= spaceAbove ? 'bottom' : 'top';
      }
    } else if (chosenPlacement === 'bottom' && spaceBelow < tooltipHeight + gap + edgeMargin && spaceAbove > spaceBelow) {
      chosenPlacement = 'top';
    } else if (chosenPlacement === 'top' && spaceAbove < tooltipHeight + gap + edgeMargin && spaceBelow > spaceAbove) {
      chosenPlacement = 'bottom';
    } else if (chosenPlacement === 'right' && spaceRight < tooltipWidth + gap + edgeMargin && spaceLeft > spaceRight) {
      chosenPlacement = 'left';
    } else if (chosenPlacement === 'left' && spaceLeft < tooltipWidth + gap + edgeMargin && spaceRight > spaceLeft) {
      chosenPlacement = 'right';
    }

    let top = 0;
    let left = 0;

    switch (chosenPlacement) {
      case 'bottom':
        top = targetRect.bottom + gap;
        left = targetRect.left + (targetRect.width - tooltipWidth) / 2;
        break;
      case 'top':
        top = targetRect.top - tooltipHeight - gap;
        left = targetRect.left + (targetRect.width - tooltipWidth) / 2;
        break;
      case 'right':
        top = targetRect.top + (targetRect.height - tooltipHeight) / 2;
        left = targetRect.right + gap;
        break;
      case 'left':
        top = targetRect.top + (targetRect.height - tooltipHeight) / 2;
        left = targetRect.left - tooltipWidth - gap;
        break;
      default:
        top = targetRect.bottom + gap;
        left = targetRect.left + (targetRect.width - tooltipWidth) / 2;
    }

    // Clamp coordinates to stay within the viewport
    const clampedLeft = Math.max(edgeMargin, Math.min(viewportW - tooltipWidth - edgeMargin, left));
    const clampedTop = Math.max(edgeMargin, Math.min(viewportH - tooltipHeight - edgeMargin, top));

    setCoords({ top: clampedTop, left: clampedLeft });
    setIsVisible(true);
  }, [targetRect, step.placement, isFa]);

  useEffect(() => {
    calculatePosition();
  }, [calculatePosition, targetRect, stepIndex]);

  // Close on Escape key
  useEffect(() => {
    const handleGlobalKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onSkip();
      }
    };

    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, [onSkip]);

  // Focus the primary action button when step changes
  useEffect(() => {
    const timer = setTimeout(() => {
      primaryBtnRef.current?.focus();
    }, 50);
    return () => clearTimeout(timer);
  }, [stepIndex]);

  // Focus trap within the tooltip
  const handleTooltipKeyDown = (e: React.KeyboardEvent<HTMLDivElement>) => {
    if (e.key !== 'Tab' || !tooltipRef.current) return;

    const focusableElements = tooltipRef.current.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );

    if (focusableElements.length === 0) return;

    const firstElement = focusableElements[0];
    const lastElement = focusableElements[focusableElements.length - 1];

    if (e.shiftKey) {
      if (document.activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      }
    } else {
      if (document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    }
  };

  const spotlightPadding = 6;
  const spotlightTop = targetRect ? targetRect.top - spotlightPadding : 0;
  const spotlightLeft = targetRect ? targetRect.left - spotlightPadding : 0;
  const spotlightWidth = targetRect ? targetRect.width + spotlightPadding * 2 : 0;
  const spotlightHeight = targetRect ? targetRect.height + spotlightPadding * 2 : 0;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 10000,
        pointerEvents: 'auto',
      }}
    >
      {/* Background Dimming: Spotlight cutout if element is found, otherwise full overlay scrim */}
      {targetRect ? (
        <div
          aria-hidden="true"
          style={{
            position: 'fixed',
            top: `${spotlightTop}px`,
            left: `${spotlightLeft}px`,
            width: `${spotlightWidth}px`,
            height: `${spotlightHeight}px`,
            borderRadius: theme.borderRadius.lg,
            boxShadow: `0 0 0 9999px ${theme.colors.overlay}, 0 0 16px rgba(0, 0, 0, 0.45)`,
            border: `2px solid ${theme.colors.primary}`,
            pointerEvents: 'none',
            zIndex: 10000,
            transition: reducedMotion
              ? 'none'
              : 'top 0.25s cubic-bezier(0.4, 0, 0.2, 1), left 0.25s cubic-bezier(0.4, 0, 0.2, 1), width 0.25s cubic-bezier(0.4, 0, 0.2, 1), height 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
        />
      ) : (
        <div
          aria-hidden="true"
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: theme.colors.overlay,
            zIndex: 10000,
            pointerEvents: 'none',
          }}
        />
      )}

      {/* Tooltip Dialog */}
      <div
        ref={tooltipRef}
        role="dialog"
        aria-modal="true"
        aria-label={step.title[language]}
        onKeyDown={handleTooltipKeyDown}
        tabIndex={-1}
        style={{
          position: 'fixed',
          top: `${coords.top}px`,
          left: `${coords.left}px`,
          width: 'min(380px, calc(100vw - 32px))',
          backgroundColor: theme.colors.surface,
          color: theme.colors.textPrimary,
          borderRadius: theme.borderRadius.xl,
          border: `1px solid ${theme.colors.border}`,
          boxShadow: isDark
            ? '0 16px 40px -8px rgba(0, 0, 0, 0.65), 0 0 0 1px rgba(255, 255, 255, 0.08)'
            : '0 20px 35px -10px rgba(0, 0, 0, 0.2), 0 1px 3px rgba(0, 0, 0, 0.05)',
          padding: theme.spacing.xl,
          zIndex: 10001,
          direction: isFa ? 'rtl' : 'ltr',
          textAlign: isFa ? 'right' : 'left',
          opacity: isVisible ? 1 : 0,
          transform: isVisible ? 'scale(1)' : 'scale(0.96)',
          transition: reducedMotion ? 'none' : 'opacity 0.2s ease, transform 0.2s ease',
          display: 'flex',
          flexDirection: 'column',
          gap: theme.spacing.md,
          outline: 'none',
        }}
      >
        {/* Header: Step counter badge and close button */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div
            style={{
              fontSize: '12px',
              fontWeight: 700,
              color: theme.colors.primaryDark,
              backgroundColor: theme.colors.primaryLight,
              padding: '2px 8px',
              borderRadius: theme.borderRadius.full,
            }}
          >
            {isFa
              ? `مرحله ${stepIndex + 1} از ${totalSteps}`
              : `Step ${stepIndex + 1} of ${totalSteps}`}
          </div>

          <button
            onClick={onSkip}
            aria-label={isFa ? 'بستن راهنما' : 'Close tour'}
            style={{
              background: 'transparent',
              border: 'none',
              color: theme.colors.textMuted,
              cursor: 'pointer',
              fontSize: '16px',
              lineHeight: 1,
              padding: '4px',
              borderRadius: theme.borderRadius.sm,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            ✕
          </button>
        </div>

        {/* Title */}
        <h3
          style={{
            margin: 0,
            fontSize: '16px',
            fontWeight: 800,
            color: theme.colors.textPrimary,
            lineHeight: 1.4,
          }}
        >
          {step.title[language]}
        </h3>

        {/* Description */}
        <p
          style={{
            margin: 0,
            fontSize: '13px',
            lineHeight: 1.6,
            color: theme.colors.textSecondary,
          }}
        >
          {step.description[language]}
        </p>

        {/* Target Missing Notice */}
        {!targetElement && (
          <div
            style={{
              padding: `${theme.spacing.sm} ${theme.spacing.md}`,
              backgroundColor: isDark ? theme.colors.warningLight : '#FFFBEB',
              border: `1px solid ${isDark ? theme.colors.borderStrong : '#FDE68A'}`,
              borderRadius: theme.borderRadius.md,
              color: isDark ? theme.colors.warning : '#B45309',
              fontSize: '12px',
              display: 'flex',
              alignItems: 'center',
              gap: theme.spacing.sm,
            }}
          >
            <span>ℹ️</span>
            <span>
              {isFa
                ? 'عنصر مورد نظر در حال حاضر در این صفحه نمایش داده نمی‌شود.'
                : 'Target element is not currently visible on screen.'}
            </span>
          </div>
        )}

        {/* Optional Action CTA */}
        {step.action && (
          <div style={{ marginTop: theme.spacing.xs }}>
            <button
              onClick={() => {
                step.onAction?.();
              }}
              style={{
                backgroundColor: theme.colors.surfaceHover,
                color: theme.colors.primary,
                border: `1px solid ${theme.colors.borderStrong}`,
                borderRadius: theme.borderRadius.md,
                padding: `${theme.spacing.sm} ${theme.spacing.md}`,
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: theme.spacing.xs,
              }}
            >
              <span>⚡</span>
              <span>{step.action[language]}</span>
            </button>
          </div>
        )}

        {/* Progress Dots */}
        <div
          aria-label={isFa ? 'پیشرفت راهنما' : 'Tour progress'}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            marginTop: theme.spacing.xs,
          }}
        >
          {Array.from({ length: totalSteps }).map((_, idx) => {
            const isActive = idx === stepIndex;
            return (
              <span
                key={idx}
                style={{
                  width: isActive ? '18px' : '6px',
                  height: '6px',
                  borderRadius: theme.borderRadius.full,
                  backgroundColor: isActive ? theme.colors.primary : theme.colors.borderStrong,
                  transition: reducedMotion ? 'none' : 'all 0.2s ease',
                  display: 'inline-block',
                }}
              />
            );
          })}
        </div>

        {/* Footer Actions: Skip, Back, Next / Finish */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: theme.spacing.sm,
            marginTop: theme.spacing.sm,
            paddingTop: theme.spacing.sm,
            borderTop: `1px solid ${theme.colors.border}`,
          }}
        >
          <button
            onClick={onSkip}
            style={{
              background: 'transparent',
              border: 'none',
              color: theme.colors.textMuted,
              cursor: 'pointer',
              fontSize: '12px',
              fontWeight: 500,
              padding: '6px 8px',
              borderRadius: theme.borderRadius.sm,
            }}
          >
            {isFa ? 'رد شدن از راهنما' : 'Skip tour'}
          </button>

          <div style={{ display: 'flex', gap: theme.spacing.sm }}>
            <button
              onClick={onBack}
              disabled={stepIndex === 0}
              style={{
                backgroundColor: 'transparent',
                color: stepIndex === 0 ? theme.colors.textMuted : theme.colors.textPrimary,
                border: `1px solid ${theme.colors.border}`,
                borderRadius: theme.borderRadius.md,
                padding: '6px 14px',
                fontSize: '13px',
                fontWeight: 600,
                cursor: stepIndex === 0 ? 'not-allowed' : 'pointer',
                opacity: stepIndex === 0 ? 0.45 : 1,
              }}
            >
              {isFa ? 'قبلی' : 'Back'}
            </button>

            <button
              ref={primaryBtnRef}
              onClick={isLastStep ? onFinish : onNext}
              style={{
                backgroundColor: theme.colors.primary,
                color: theme.colors.primaryTextOnBrand,
                border: 'none',
                borderRadius: theme.borderRadius.md,
                padding: '6px 18px',
                fontSize: '13px',
                fontWeight: 700,
                cursor: 'pointer',
                boxShadow: theme.shadows.sm,
              }}
            >
              {isLastStep ? (isFa ? 'پایان' : 'Finish') : isFa ? 'بعدی' : 'Next'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
