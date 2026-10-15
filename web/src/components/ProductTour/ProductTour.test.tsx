import React from 'react';
import { render, screen, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import { TourOverlay } from './TourOverlay';
import { TourProvider, useTourContext } from './TourProvider';
import { TourConfig, TourStep } from './tourTypes';
import { ThemeProvider } from '../../theme/ThemeContext';

const mockSteps: TourStep[] = [
  {
    target: '#test-target-1',
    title: { fa: 'گام نخست', en: 'First Step' },
    description: { fa: 'توضیحات گام نخست', en: 'Description of the first step' },
    placement: 'bottom',
  },
  {
    target: '#test-target-2',
    title: { fa: 'گام دوم', en: 'Second Step' },
    description: { fa: 'توضیحات گام دوم', en: 'Description of the second step' },
    placement: 'top',
    action: { fa: 'اقدام تستی', en: 'Test Action' },
    onAction: jest.fn(),
  },
];

const mockConfig: TourConfig = {
  id: 'test-tour',
  steps: mockSteps,
  onComplete: jest.fn(),
  onSkip: jest.fn(),
};

describe('Product Tour System', () => {
  let target1: HTMLDivElement;
  let target2: HTMLDivElement;

  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();

    target1 = document.createElement('div');
    target1.id = 'test-target-1';
    target1.setAttribute('data-tour', 'target-one');
    target1.getBoundingClientRect = jest.fn(() => ({
      top: 100,
      left: 100,
      bottom: 140,
      right: 200,
      width: 100,
      height: 40,
      x: 100,
      y: 100,
      toJSON: () => {},
    }));
    document.body.appendChild(target1);

    target2 = document.createElement('div');
    target2.id = 'test-target-2';
    target2.getBoundingClientRect = jest.fn(() => ({
      top: 300,
      left: 200,
      bottom: 350,
      right: 300,
      width: 100,
      height: 50,
      x: 200,
      y: 300,
      toJSON: () => {},
    }));
    document.body.appendChild(target2);
  });

  afterEach(() => {
    if (target1 && target1.parentNode) target1.parentNode.removeChild(target1);
    if (target2 && target2.parentNode) target2.parentNode.removeChild(target2);
  });

  describe('TourOverlay Unit Tests', () => {
    it('renders step content in English', () => {
      const onNext = jest.fn();
      const onBack = jest.fn();
      const onSkip = jest.fn();
      const onFinish = jest.fn();

      render(
        <ThemeProvider>
          <TourOverlay
            step={mockSteps[0]}
            stepIndex={0}
            totalSteps={2}
            targetElement={target1}
            language="en"
            onNext={onNext}
            onBack={onBack}
            onSkip={onSkip}
            onFinish={onFinish}
          />
        </ThemeProvider>
      );

      expect(screen.getByText('First Step')).toBeInTheDocument();
      expect(screen.getByText('Description of the first step')).toBeInTheDocument();
      expect(screen.getByText('Step 1 of 2')).toBeInTheDocument();

      // Back button should be disabled on first step
      const backBtn = screen.getByRole('button', { name: 'Back' });
      expect(backBtn).toBeDisabled();

      // Next button should be present
      const nextBtn = screen.getByRole('button', { name: 'Next' });
      fireEvent.click(nextBtn);
      expect(onNext).toHaveBeenCalledTimes(1);
    });

    it('renders step content in Persian (RTL)', () => {
      const onNext = jest.fn();
      const onBack = jest.fn();
      const onSkip = jest.fn();
      const onFinish = jest.fn();

      render(
        <ThemeProvider>
          <TourOverlay
            step={mockSteps[0]}
            stepIndex={0}
            totalSteps={2}
            targetElement={target1}
            language="fa"
            onNext={onNext}
            onBack={onBack}
            onSkip={onSkip}
            onFinish={onFinish}
          />
        </ThemeProvider>
      );

      expect(screen.getByText('گام نخست')).toBeInTheDocument();
      expect(screen.getByText('توضیحات گام نخست')).toBeInTheDocument();
      expect(screen.getByText(/مرحله 1 از 2/)).toBeInTheDocument();

      const nextBtn = screen.getByRole('button', { name: 'بعدی' });
      fireEvent.click(nextBtn);
      expect(onNext).toHaveBeenCalledTimes(1);
    });

    it('renders Finish button on final step and invokes onFinish', () => {
      const onNext = jest.fn();
      const onBack = jest.fn();
      const onSkip = jest.fn();
      const onFinish = jest.fn();

      render(
        <ThemeProvider>
          <TourOverlay
            step={mockSteps[1]}
            stepIndex={1}
            totalSteps={2}
            targetElement={target2}
            language="en"
            onNext={onNext}
            onBack={onBack}
            onSkip={onSkip}
            onFinish={onFinish}
          />
        </ThemeProvider>
      );

      expect(screen.getByText('Second Step')).toBeInTheDocument();
      expect(screen.getByText('Step 2 of 2')).toBeInTheDocument();

      const backBtn = screen.getByRole('button', { name: 'Back' });
      expect(backBtn).not.toBeDisabled();
      fireEvent.click(backBtn);
      expect(onBack).toHaveBeenCalledTimes(1);

      const finishBtn = screen.getByRole('button', { name: 'Finish' });
      fireEvent.click(finishBtn);
      expect(onFinish).toHaveBeenCalledTimes(1);
      expect(onNext).not.toHaveBeenCalled();
    });

    it('handles Skip tour button click', () => {
      const onSkip = jest.fn();

      render(
        <ThemeProvider>
          <TourOverlay
            step={mockSteps[0]}
            stepIndex={0}
            totalSteps={2}
            targetElement={target1}
            language="en"
            onNext={jest.fn()}
            onBack={jest.fn()}
            onSkip={onSkip}
            onFinish={jest.fn()}
          />
        </ThemeProvider>
      );

      const skipBtn = screen.getByRole('button', { name: 'Skip tour' });
      fireEvent.click(skipBtn);
      expect(onSkip).toHaveBeenCalledTimes(1);
    });

    it('displays fallback notice gracefully when targetElement is null', () => {
      render(
        <ThemeProvider>
          <TourOverlay
            step={mockSteps[0]}
            stepIndex={0}
            totalSteps={2}
            targetElement={null}
            language="en"
            onNext={jest.fn()}
            onBack={jest.fn()}
            onSkip={jest.fn()}
            onFinish={jest.fn()}
          />
        </ThemeProvider>
      );

      expect(screen.getByText('Target element is not currently visible on screen.')).toBeInTheDocument();
    });

    it('handles Escape key to dismiss tour', () => {
      const onSkip = jest.fn();

      render(
        <ThemeProvider>
          <TourOverlay
            step={mockSteps[0]}
            stepIndex={0}
            totalSteps={2}
            targetElement={target1}
            language="en"
            onNext={jest.fn()}
            onBack={jest.fn()}
            onSkip={onSkip}
            onFinish={jest.fn()}
          />
        </ThemeProvider>
      );

      fireEvent.keyDown(window, { key: 'Escape' });
      expect(onSkip).toHaveBeenCalledTimes(1);
    });
  });

  describe('TourProvider & Integration Flow', () => {
    const TestConsumer: React.FC = () => {
      const { startTour, isActive, resetTour } = useTourContext();
      return (
        <div>
          <button onClick={() => startTour(mockConfig)}>Launch Tour</button>
          <button onClick={() => resetTour('test-tour')}>Reset Tour</button>
          <div data-testid="active-status">{isActive ? 'ACTIVE' : 'INACTIVE'}</div>
        </div>
      );
    };

    it('launches tour, steps through next, back, and finish with localStorage persistence', () => {
      render(
        <ThemeProvider>
          <TourProvider language="en">
            <TestConsumer />
          </TourProvider>
        </ThemeProvider>
      );

      expect(screen.getByTestId('active-status').textContent).toBe('INACTIVE');

      // Click to start tour
      fireEvent.click(screen.getByText('Launch Tour'));
      expect(screen.getByTestId('active-status').textContent).toBe('ACTIVE');
      expect(screen.getByText('First Step')).toBeInTheDocument();

      // Click Next
      fireEvent.click(screen.getByRole('button', { name: 'Next' }));
      expect(screen.getByText('Second Step')).toBeInTheDocument();

      // Click Back
      fireEvent.click(screen.getByRole('button', { name: 'Back' }));
      expect(screen.getByText('First Step')).toBeInTheDocument();

      // Click Next again
      fireEvent.click(screen.getByRole('button', { name: 'Next' }));
      expect(screen.getByText('Second Step')).toBeInTheDocument();

      // Click Finish
      fireEvent.click(screen.getByRole('button', { name: 'Finish' }));
      expect(screen.getByTestId('active-status').textContent).toBe('INACTIVE');
      expect(localStorage.getItem('resto_tour_test-tour')).toBe('completed');
    });

    it('persists skip action to localStorage', () => {
      render(
        <ThemeProvider>
          <TourProvider language="en">
            <TestConsumer />
          </TourProvider>
        </ThemeProvider>
      );

      fireEvent.click(screen.getByText('Launch Tour'));
      expect(screen.getByTestId('active-status').textContent).toBe('ACTIVE');

      fireEvent.click(screen.getByRole('button', { name: 'Skip tour' }));
      expect(screen.getByTestId('active-status').textContent).toBe('INACTIVE');
      expect(localStorage.getItem('resto_tour_test-tour')).toBe('skipped');
    });
  });
});
