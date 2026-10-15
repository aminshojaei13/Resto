export interface TourStep {
  /** CSS selector or data-tour attribute value for the target element */
  target: string;
  /** Title of the step - supports both fa and en */
  title: { fa: string; en: string };
  /** Description/explanation */
  description: { fa: string; en: string };
  /** Preferred position of the tooltip relative to the target */
  placement?: 'top' | 'bottom' | 'left' | 'right' | 'auto';
  /** Optional action label for a CTA within the step */
  action?: { fa: string; en: string };
  /** Callback when action button is clicked */
  onAction?: () => void;
}

export interface TourConfig {
  /** Unique ID for persistence */
  id: string;
  /** Tour steps */
  steps: TourStep[];
  /** Callback when tour completes */
  onComplete?: () => void;
  /** Callback when tour is skipped */
  onSkip?: () => void;
}

export interface TourState {
  isActive: boolean;
  currentStep: number;
  totalSteps: number;
}
