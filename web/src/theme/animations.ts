import React, { useState, useEffect, CSSProperties, ReactNode } from 'react';
import { useTheme } from './ThemeContext';

// ============================================================================
// Resto Animation Utilities & Tokens
// ============================================================================

/**
 * 1. Hook to detect whether the user has requested reduced motion.
 * Returns true if prefers-reduced-motion: reduce is active, false otherwise.
 */
export function useReducedMotion(): boolean {
  const [reducedMotion, setReducedMotion] = useState<boolean>(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return false;
    }
    try {
      return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    } catch {
      return false;
    }
  });

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      return;
    }

    try {
      const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
      setReducedMotion(mediaQuery.matches);

      const handleChange = (event: MediaQueryListEvent | MediaQueryList) => {
        setReducedMotion(event.matches);
      };

      if (typeof mediaQuery.addEventListener === 'function') {
        mediaQuery.addEventListener('change', handleChange);
        return () => {
          mediaQuery.removeEventListener('change', handleChange);
        };
      } else if (typeof (mediaQuery as any).addListener === 'function') {
        (mediaQuery as any).addListener(handleChange);
        return () => {
          (mediaQuery as any).removeListener(handleChange);
        };
      }
    } catch {
      // Graceful fallback if matchMedia errors
    }
  }, []);

  return reducedMotion;
}

/**
 * 2. Commonly-used CSS transition strings.
 */
export const TRANSITIONS = {
  fadeIn: 'opacity 250ms ease',
  slideUp: 'transform 250ms ease, opacity 250ms ease',
  slideDown: 'transform 250ms ease, opacity 250ms ease',
  scaleIn: 'transform 250ms ease, opacity 250ms ease',
  cardHover: 'transform 200ms ease, box-shadow 200ms ease',
  buttonPress: 'transform 100ms ease',
  fast: '150ms ease',
  normal: '250ms ease',
  slow: '350ms ease-out',
} as const;

export type TransitionKey = keyof typeof TRANSITIONS;

/**
 * 3. Returns 'none' if reduced motion is enabled, or the given transition string otherwise.
 * Supports both signatures:
 *   getTransition(reducedMotion: boolean, transition?: string)
 *   getTransition(transition: string, reducedMotion?: boolean)
 */
export function getTransition(reducedMotion: boolean, transition?: string): string;
export function getTransition(transition: string, reducedMotion?: boolean): string;
export function getTransition(
  arg1: boolean | string,
  arg2?: boolean | string
): string {
  if (typeof arg1 === 'boolean') {
    if (arg1) return 'none';
    return typeof arg2 === 'string' ? arg2 : TRANSITIONS.normal;
  }
  // arg1 is transition string
  if (arg2 === true) return 'none';
  return arg1;
}

/**
 * 4. CSS @keyframes definitions for common Resto animations.
 */
export const KEYFRAMES = {
  fadeIn: `@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}`,
  slideUp: `@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}`,
  slideDown: `@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}`,
  scaleIn: `@keyframes scaleIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}`,
  pulse: `@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.02);
  }
  100% {
    transform: scale(1);
  }
}`,
  shimmer: `@keyframes shimmer {
  0% {
    background-position: -200% 0;
  }
  100% {
    background-position: 200% 0;
  }
}`,
} as const;

export type KeyframeName = keyof typeof KEYFRAMES;

const KEYFRAMES_STYLE_ATTR = 'data-resto-keyframes';

/**
 * 5. Injects a <style> element with all Resto keyframe definitions into <head>.
 * Operation is idempotent and checks for existing style attribute before adding.
 */
export function injectKeyframes(): void {
  if (typeof document === 'undefined' || !document.head) return;

  if (document.querySelector(`style[${KEYFRAMES_STYLE_ATTR}]`)) {
    return;
  }

  const style = document.createElement('style');
  style.setAttribute(KEYFRAMES_STYLE_ATTR, 'true');
  style.setAttribute('data-resto-animations', 'true');
  style.textContent = Object.values(KEYFRAMES).join('\n\n');
  document.head.appendChild(style);
}

/**
 * 6. Returns a CSSProperties object with the specified animation applied.
 * Injects keyframes on mount and returns an empty style object {} if reduced motion is active.
 */
export function useAnimationStyle(
  animationName: string,
  duration?: string
): CSSProperties {
  const reducedMotion = useReducedMotion();

  useEffect(() => {
    injectKeyframes();
  }, []);

  if (reducedMotion) {
    return {};
  }

  const animDuration = duration ?? TRANSITIONS.normal;

  return {
    animation: `${animationName} ${animDuration}`,
  };
}

// ============================================================================
// React Animation Components
// ============================================================================

export interface SkeletonLineProps {
  width?: string;
  height?: string;
  borderRadius?: string;
  style?: CSSProperties;
  className?: string;
}

/**
 * 7. Shimmer loading placeholder component.
 * Uses theme colors for gradient shimmer, respects prefers-reduced-motion
 * with a static background, and uses inline styles.
 */
export const SkeletonLine: React.FC<SkeletonLineProps> = ({
  width = '100%',
  height = '16px',
  borderRadius,
  style,
  className,
}) => {
  const { theme } = useTheme();
  const reducedMotion = useReducedMotion();

  useEffect(() => {
    injectKeyframes();
  }, []);

  const resolvedRadius = borderRadius ?? theme.borderRadius?.sm ?? '6px';
  const baseColor = theme.colors.surfaceHover;
  const highlightColor = theme.mode === 'warmDark' ? theme.colors.border : theme.colors.surface;

  const skeletonStyle: CSSProperties = {
    width,
    height,
    borderRadius: resolvedRadius,
    display: 'block',
    backgroundColor: baseColor,
    ...(reducedMotion
      ? {}
      : {
          backgroundImage: `linear-gradient(90deg, ${baseColor} 0%, ${highlightColor} 50%, ${baseColor} 100%)`,
          backgroundSize: '200% 100%',
          animation: 'shimmer 1.5s infinite linear',
        }),
    ...style,
  };

  return React.createElement('div', {
    style: skeletonStyle,
    className,
    'aria-hidden': true,
  });
};

export interface FadeInProps {
  children: ReactNode;
  delay?: number;
  duration?: number;
  direction?: 'up' | 'down' | 'none';
  style?: CSSProperties;
  className?: string;
}

/**
 * 8. FadeIn wrapper component that animates opacity (and optional translation) on mount.
 * Uses inline styles with CSS transitions (not @keyframes), flips to visible via useEffect,
 * and renders immediately visible without motion if reduced motion is active.
 */
export const FadeIn: React.FC<FadeInProps> = ({
  children,
  delay = 0,
  duration = 250,
  direction = 'none',
  style,
  className,
}) => {
  const reducedMotion = useReducedMotion();
  const [isVisible, setIsVisible] = useState<boolean>(() => reducedMotion);

  useEffect(() => {
    if (reducedMotion) {
      setIsVisible(true);
      return;
    }

    const timer = setTimeout(() => {
      setIsVisible(true);
    }, Math.max(delay, 10));

    return () => clearTimeout(timer);
  }, [delay, reducedMotion]);

  const getTransform = (): string => {
    if (reducedMotion) return 'none';
    if (isVisible) return 'translateY(0)';
    if (direction === 'up') return 'translateY(16px)';
    if (direction === 'down') return 'translateY(-16px)';
    return 'none';
  };

  const getTransitionStyle = (): string => {
    if (reducedMotion) return 'none';
    if (direction === 'none') {
      return `opacity ${duration}ms ease`;
    }
    return `opacity ${duration}ms ease, transform ${duration}ms ease`;
  };

  const containerStyle: CSSProperties = {
    opacity: reducedMotion || isVisible ? 1 : 0,
    transform: getTransform(),
    transition: getTransitionStyle(),
    ...style,
  };

  return React.createElement(
    'div',
    {
      style: containerStyle,
      className,
    },
    children
  );
};
