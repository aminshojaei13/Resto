import { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { TourConfig, TourState, TourStep } from './tourTypes';

/**
 * Helper to locate an element by CSS selector, data-tour attribute, or ID.
 */
export function findTargetElement(target: string): HTMLElement | null {
  if (typeof document === 'undefined' || !target) return null;

  // 1. Try selector directly
  try {
    const el = document.querySelector<HTMLElement>(target);
    if (el) return el;
  } catch {
    // If target is not a standard selector string, continue to fallbacks
  }

  // 2. Try as a data-tour value
  try {
    const el = document.querySelector<HTMLElement>(`[data-tour="${target}"]`);
    if (el) return el;
  } catch {}

  // 3. Try as an ID
  try {
    const el = document.getElementById(target);
    if (el) return el;
  } catch {}

  return null;
}

export interface UseTourReturn {
  state: TourState;
  start: (overrideConfig?: TourConfig) => void;
  next: () => void;
  back: () => void;
  skip: () => void;
  finish: () => void;
  targetElement: HTMLElement | null;
  currentStepData: TourStep | null;
  isCompleted: boolean;
  reset: (id?: string) => void;
}

export function useTour(config?: TourConfig | null): UseTourReturn {
  const [activeConfig, setActiveConfig] = useState<TourConfig | null>(config ?? null);
  const [state, setState] = useState<TourState>({
    isActive: false,
    currentStep: 0,
    totalSteps: config?.steps?.length ?? 0,
  });
  const [targetElement, setTargetElement] = useState<HTMLElement | null>(null);
  const [storageVersion, setStorageVersion] = useState<number>(0);

  // Keep activeConfig in sync if incoming config prop changes
  useEffect(() => {
    if (config) {
      setActiveConfig(config);
      setState((prev) => ({
        ...prev,
        totalSteps: config.steps.length,
      }));
    }
  }, [config]);

  const currentStepData = useMemo<TourStep | null>(() => {
    if (!activeConfig || !activeConfig.steps.length) return null;
    return activeConfig.steps[state.currentStep] ?? null;
  }, [activeConfig, state.currentStep]);

  // Check if tour is already completed in localStorage
  const isCompleted = useMemo<boolean>(() => {
    if (!activeConfig?.id || typeof window === 'undefined') return false;
    try {
      const val = localStorage.getItem(`resto_tour_${activeConfig.id}`);
      return val === 'completed' || val === 'true';
    } catch {
      return false;
    }
  }, [activeConfig?.id, storageVersion]);

  // Scroll and update target element when step or active state changes
  useEffect(() => {
    if (!state.isActive || !currentStepData) {
      setTargetElement(null);
      return;
    }

    const resolveElement = () => {
      const el = findTargetElement(currentStepData.target);
      setTargetElement(el);

      if (el && typeof el.scrollIntoView === 'function') {
        try {
          const prefersReduced =
            typeof window !== 'undefined' &&
            window.matchMedia &&
            window.matchMedia('(prefers-reduced-motion: reduce)').matches;

          el.scrollIntoView({
            behavior: prefersReduced ? 'auto' : 'smooth',
            block: 'center',
            inline: 'center',
          });
        } catch {
          // Fallback if browser throws on scrollIntoView options
        }
      }
    };

    resolveElement();

    // Check on subsequent frame in case component is currently mounting
    const frameId = requestAnimationFrame(resolveElement);
    const timerId = setTimeout(resolveElement, 100);

    return () => {
      cancelAnimationFrame(frameId);
      clearTimeout(timerId);
    };
  }, [state.isActive, state.currentStep, currentStepData]);

  const finish = useCallback(() => {
    if (activeConfig) {
      try {
        localStorage.setItem(`resto_tour_${activeConfig.id}`, 'completed');
        setStorageVersion((v) => v + 1);
      } catch {}
      activeConfig.onComplete?.();
    }
    setState((prev) => ({ ...prev, isActive: false }));
  }, [activeConfig]);

  const skip = useCallback(() => {
    if (activeConfig) {
      try {
        localStorage.setItem(`resto_tour_${activeConfig.id}`, 'skipped');
        setStorageVersion((v) => v + 1);
      } catch {}
      activeConfig.onSkip?.();
    }
    setState((prev) => ({ ...prev, isActive: false }));
  }, [activeConfig]);

  const start = useCallback(
    (overrideConfig?: TourConfig) => {
      const effectiveConfig = overrideConfig || activeConfig;
      if (!effectiveConfig || !effectiveConfig.steps.length) return;

      if (overrideConfig) {
        setActiveConfig(overrideConfig);
      }

      setState({
        isActive: true,
        currentStep: 0,
        totalSteps: effectiveConfig.steps.length,
      });
    },
    [activeConfig]
  );

  const next = useCallback(() => {
    if (state.currentStep >= state.totalSteps - 1) {
      finish();
    } else {
      setState((prev) => ({
        ...prev,
        currentStep: prev.currentStep + 1,
      }));
    }
  }, [state.currentStep, state.totalSteps, finish]);

  const back = useCallback(() => {
    if (state.currentStep > 0) {
      setState((prev) => ({
        ...prev,
        currentStep: prev.currentStep - 1,
      }));
    }
  }, [state.currentStep]);

  const reset = useCallback(
    (id?: string) => {
      const targetId = id || activeConfig?.id;
      if (targetId && typeof window !== 'undefined') {
        try {
          localStorage.removeItem(`resto_tour_${targetId}`);
          setStorageVersion((v) => v + 1);
        } catch {}
      }
      setState((prev) => ({
        ...prev,
        isActive: false,
        currentStep: 0,
      }));
    },
    [activeConfig?.id]
  );

  return {
    state,
    start,
    next,
    back,
    skip,
    finish,
    targetElement,
    currentStepData,
    isCompleted,
    reset,
  };
}
