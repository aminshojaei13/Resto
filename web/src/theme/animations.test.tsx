import React from 'react';
import { render, screen, act } from '@testing-library/react';
import {
  useReducedMotion,
  TRANSITIONS,
  getTransition,
  KEYFRAMES,
  injectKeyframes,
  useAnimationStyle,
  SkeletonLine,
  FadeIn,
} from './animations';
import { ThemeProvider } from './ThemeContext';

describe('theme/animations.ts', () => {
  beforeEach(() => {
    // Clear any injected style elements
    document.head.querySelectorAll('style[data-resto-keyframes]').forEach((el) => el.remove());
    document.head.querySelectorAll('style[data-resto-animations]').forEach((el) => el.remove());
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.restoreAllMocks();
  });

  describe('TRANSITIONS', () => {
    it('defines required transition strings', () => {
      expect(TRANSITIONS.fast).toBe('150ms ease');
      expect(TRANSITIONS.normal).toBe('250ms ease');
      expect(TRANSITIONS.slow).toBe('350ms ease-out');
      expect(TRANSITIONS.fadeIn).toContain('opacity');
      expect(TRANSITIONS.slideUp).toContain('transform');
      expect(TRANSITIONS.slideUp).toContain('opacity');
      expect(TRANSITIONS.slideDown).toContain('transform');
      expect(TRANSITIONS.slideDown).toContain('opacity');
      expect(TRANSITIONS.scaleIn).toContain('transform');
      expect(TRANSITIONS.scaleIn).toContain('opacity');
      expect(TRANSITIONS.cardHover).toContain('box-shadow');
      expect(TRANSITIONS.buttonPress).toContain('transform');
    });
  });

  describe('getTransition', () => {
    it('returns "none" when reduced motion is enabled', () => {
      expect(getTransition(true)).toBe('none');
      expect(getTransition(true, TRANSITIONS.slideUp)).toBe('none');
      expect(getTransition(TRANSITIONS.fadeIn, true)).toBe('none');
    });

    it('returns the transition string when reduced motion is disabled', () => {
      expect(getTransition(false, TRANSITIONS.slideUp)).toBe(TRANSITIONS.slideUp);
      expect(getTransition(false)).toBe(TRANSITIONS.normal);
      expect(getTransition(TRANSITIONS.fadeIn, false)).toBe(TRANSITIONS.fadeIn);
    });
  });

  describe('KEYFRAMES and injectKeyframes', () => {
    it('contains all required keyframes', () => {
      expect(KEYFRAMES.fadeIn).toContain('@keyframes fadeIn');
      expect(KEYFRAMES.slideUp).toContain('translateY(16px)');
      expect(KEYFRAMES.slideDown).toContain('translateY(-16px)');
      expect(KEYFRAMES.scaleIn).toContain('scale(0.95)');
      expect(KEYFRAMES.pulse).toContain('scale(1.02)');
      expect(KEYFRAMES.shimmer).toContain('background-position');
    });

    it('injects style element idempotently into document.head', () => {
      expect(document.head.querySelector('style[data-resto-keyframes]')).toBeNull();

      injectKeyframes();
      const style1 = document.head.querySelector('style[data-resto-keyframes]');
      expect(style1).not.toBeNull();
      expect(style1?.textContent).toContain('@keyframes fadeIn');
      expect(style1?.textContent).toContain('@keyframes shimmer');

      // Second call should not duplicate
      injectKeyframes();
      const styles = document.head.querySelectorAll('style[data-resto-keyframes]');
      expect(styles.length).toBe(1);
    });
  });

  describe('useReducedMotion', () => {
    it('returns false when media query does not match', () => {
      let listener: ((e: any) => void) | null = null;
      window.matchMedia = jest.fn().mockImplementation((query) => ({
        matches: false,
        media: query,
        addEventListener: jest.fn((_, cb) => {
          listener = cb;
        }),
        removeEventListener: jest.fn(),
      }));

      const TestComponent = () => {
        const reduced = useReducedMotion();
        return <div data-testid="reduced">{reduced ? 'true' : 'false'}</div>;
      };

      render(<TestComponent />);
      expect(screen.getByTestId('reduced').textContent).toBe('false');

      // Trigger change
      act(() => {
        if (listener) listener({ matches: true });
      });
      expect(screen.getByTestId('reduced').textContent).toBe('true');
    });

    it('returns true when media query matches', () => {
      window.matchMedia = jest.fn().mockImplementation((query) => ({
        matches: true,
        media: query,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      }));

      const TestComponent = () => {
        const reduced = useReducedMotion();
        return <div data-testid="reduced">{reduced ? 'true' : 'false'}</div>;
      };

      render(<TestComponent />);
      expect(screen.getByTestId('reduced').textContent).toBe('true');
    });
  });

  describe('useAnimationStyle', () => {
    it('returns animation style when reduced motion is false', () => {
      window.matchMedia = jest.fn().mockReturnValue({
        matches: false,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      });

      const TestComponent = () => {
        const style = useAnimationStyle('fadeIn', '300ms ease');
        return <div data-testid="box" style={style}>Content</div>;
      };

      render(<TestComponent />);
      const el = screen.getByTestId('box');
      expect(el.style.animation).toBe('fadeIn 300ms ease');
      expect(document.head.querySelector('style[data-resto-keyframes]')).not.toBeNull();
    });

    it('returns empty object {} when reduced motion is true', () => {
      window.matchMedia = jest.fn().mockReturnValue({
        matches: true,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      });

      const TestComponent = () => {
        const style = useAnimationStyle('fadeIn');
        return <div data-testid="box" style={style}>Content</div>;
      };

      render(<TestComponent />);
      const el = screen.getByTestId('box');
      expect(el.style.animation).toBe('');
    });
  });

  describe('SkeletonLine', () => {
    it('renders with shimmer animation and proper sizing when motion is enabled', () => {
      window.matchMedia = jest.fn().mockReturnValue({
        matches: false,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      });

      const { container } = render(
        <ThemeProvider>
          <SkeletonLine width="200px" height="24px" borderRadius="8px" />
        </ThemeProvider>
      );

      const skeleton = container.firstElementChild as HTMLElement;
      expect(skeleton.style.width).toBe('200px');
      expect(skeleton.style.height).toBe('24px');
      expect(skeleton.style.borderRadius).toBe('8px');
      expect(skeleton.style.animation).toContain('shimmer');
      expect(skeleton.style.backgroundColor).not.toBe('');
    });

    it('renders static background without shimmer when reduced motion is active', () => {
      window.matchMedia = jest.fn().mockReturnValue({
        matches: true,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      });

      const { container } = render(
        <ThemeProvider>
          <SkeletonLine width="100px" height="12px" />
        </ThemeProvider>
      );

      const skeleton = container.firstElementChild as HTMLElement;
      expect(skeleton.style.width).toBe('100px');
      expect(skeleton.style.animation).toBe('');
      expect(skeleton.style.backgroundColor).not.toBe('');
    });
  });

  describe('FadeIn', () => {
    it('starts invisible and flips to visible on mount with CSS transitions', () => {
      window.matchMedia = jest.fn().mockReturnValue({
        matches: false,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      });

      const { container } = render(
        <FadeIn direction="up" duration={200} delay={50}>
          <p>Fade in content</p>
        </FadeIn>
      );

      const div = container.firstElementChild as HTMLElement;
      expect(div.style.opacity).toBe('0');
      expect(div.style.transform).toBe('translateY(16px)');
      expect(div.style.transition).toContain('opacity 200ms ease');

      act(() => {
        jest.advanceTimersByTime(60);
      });

      expect(div.style.opacity).toBe('1');
      expect(div.style.transform).toBe('translateY(0)');
    });

    it('renders immediately visible with no transition if reduced motion is active', () => {
      window.matchMedia = jest.fn().mockReturnValue({
        matches: true,
        addEventListener: jest.fn(),
        removeEventListener: jest.fn(),
      });

      const { container } = render(
        <FadeIn direction="down" duration={300} delay={100}>
          <p>Instant content</p>
        </FadeIn>
      );

      const div = container.firstElementChild as HTMLElement;
      expect(div.style.opacity).toBe('1');
      expect(div.style.transform).toBe('none');
      expect(div.style.transition).toBe('none');
    });
  });
});
