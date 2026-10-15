import React, { createContext, useContext, useState, useCallback, useMemo, useEffect } from 'react';
import { TourConfig } from './tourTypes';
import { useTour } from './useTour';
import { TourOverlay } from './TourOverlay';

export interface TourContextValue {
  startTour: (config: TourConfig) => void;
  isActive: boolean;
  resetTour: (tourId: string) => void;
}

const TourContext = createContext<TourContextValue | undefined>(undefined);

export interface TourProviderProps {
  children: React.ReactNode;
  language?: 'fa' | 'en';
}

export const TourProvider: React.FC<TourProviderProps> = ({ children, language: propLanguage }) => {
  const [currentConfig, setCurrentConfig] = useState<TourConfig | null>(null);

  const [currentLanguage, setCurrentLanguage] = useState<'fa' | 'en'>(() => {
    if (propLanguage) return propLanguage;
    if (typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem('resto_language');
        if (stored === 'en' || stored === 'fa') return stored;
      } catch {}
    }
    return 'fa';
  });

  useEffect(() => {
    if (propLanguage) {
      setCurrentLanguage(propLanguage);
    }
  }, [propLanguage]);

  const {
    state,
    start,
    next,
    back,
    skip,
    finish,
    targetElement,
    currentStepData,
    reset,
  } = useTour(currentConfig);

  const startTour = useCallback(
    (config: TourConfig) => {
      setCurrentConfig(config);
      start(config);
    },
    [start]
  );

  const resetTour = useCallback(
    (tourId: string) => {
      reset(tourId);
    },
    [reset]
  );

  const contextValue = useMemo<TourContextValue>(
    () => ({
      startTour,
      isActive: state.isActive,
      resetTour,
    }),
    [startTour, state.isActive, resetTour]
  );

  return (
    <TourContext.Provider value={contextValue}>
      {children}
      {state.isActive && currentStepData && (
        <TourOverlay
          step={currentStepData}
          stepIndex={state.currentStep}
          totalSteps={state.totalSteps}
          targetElement={targetElement}
          language={currentLanguage}
          onNext={next}
          onBack={back}
          onSkip={skip}
          onFinish={finish}
        />
      )}
    </TourContext.Provider>
  );
};

export function useTourContext(): TourContextValue {
  const context = useContext(TourContext);
  if (!context) {
    throw new Error('useTourContext must be used within a TourProvider');
  }
  return context;
}
