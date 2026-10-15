import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import { ContextualHelp } from './ContextualHelp';
import { HelpButton } from './HelpButton';
import { SectionHelp } from './SectionHelp';
import { ThemeProvider } from '../../theme/ThemeContext';

describe('Help System Components', () => {
  beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
  });

  describe('ContextualHelp', () => {
    it('renders help trigger button with accessible label in English', () => {
      render(
        <ThemeProvider>
          <ContextualHelp language="en" section="products" />
        </ThemeProvider>
      );

      const trigger = screen.getByRole('button', { name: /view help for this section/i });
      expect(trigger).toBeInTheDocument();
      expect(trigger).toHaveAttribute('aria-haspopup', 'dialog');
      expect(trigger).toHaveAttribute('aria-expanded', 'false');
    });

    it('opens contextual help dialog with section content upon click and closes on outside click or Escape', () => {
      render(
        <ThemeProvider>
          <div>
            <div data-testid="outside-area">Outside</div>
            <ContextualHelp language="en" section="products" />
          </div>
        </ThemeProvider>
      );

      const trigger = screen.getByRole('button', { name: /view help for this section/i });
      fireEvent.click(trigger);

      // Dialog should be visible
      const dialog = screen.getByRole('dialog');
      expect(dialog).toBeInTheDocument();
      expect(screen.getByText(/Define and manage your product catalog/i)).toBeInTheDocument();

      // Press Escape to dismiss
      fireEvent.keyDown(window, { key: 'Escape' });
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('renders contextual help in Persian', () => {
      render(
        <ThemeProvider>
          <ContextualHelp language="fa" section="inventory" />
        </ThemeProvider>
      );

      const trigger = screen.getByRole('button', { name: /مشاهده راهنمای این بخش/i });
      fireEvent.click(trigger);

      expect(screen.getByText(/مشاهده لحظه‌ای موجودی کالاها/i)).toBeInTheDocument();
    });
  });

  describe('HelpButton (Shell Entry Point)', () => {
    it('renders header help button and opens dropdown menu with tour triggers', () => {
      const onStartTour = jest.fn();
      const onRestartTour = jest.fn();

      render(
        <ThemeProvider>
          <HelpButton
            language="en"
            currentSection="dashboard"
            onStartTour={onStartTour}
            onRestartTour={onRestartTour}
          />
        </ThemeProvider>
      );

      const button = screen.getByRole('button', { name: /help/i });
      expect(button).toBeInTheDocument();

      fireEvent.click(button);

      // Dropdown menu should show items
      const startTourItem = screen.getByText('Start guided tour');
      expect(startTourItem).toBeInTheDocument();

      const restartTourItem = screen.getByText('Restart guided tour');
      expect(restartTourItem).toBeInTheDocument();

      expect(screen.getByText(/Version 1.0.0/i)).toBeInTheDocument();

      // Click start tour
      fireEvent.click(startTourItem);
      expect(onStartTour).toHaveBeenCalledTimes(1);
    });

    it('renders Persian labels in dropdown menu', () => {
      const onRestartTour = jest.fn();

      render(
        <ThemeProvider>
          <HelpButton
            language="fa"
            currentSection="products"
            onStartTour={jest.fn()}
            onRestartTour={onRestartTour}
          />
        </ThemeProvider>
      );

      const button = screen.getByRole('button', { name: /راهنما/i });
      fireEvent.click(button);

      expect(screen.getByText('شروع تور راهنما')).toBeInTheDocument();
      const restartItem = screen.getByText('شروع مجدد تور راهنما');
      fireEvent.click(restartItem);
      expect(onRestartTour).toHaveBeenCalledTimes(1);
    });
  });

  describe('SectionHelp (Inline Accordion)', () => {
    it('renders inline section help and allows collapsing with localStorage persistence', () => {
      render(
        <ThemeProvider>
          <SectionHelp language="en" sectionKey="customers" />
        </ThemeProvider>
      );

      expect(screen.getByText(/Maintain customer contact directories/i)).toBeInTheDocument();

      // Find collapse/expand button
      const toggleBtn = screen.getByRole('button', { name: /hide help|show help/i });
      fireEvent.click(toggleBtn);

      // Check localStorage updated
      expect(localStorage.getItem('resto_help_collapsed_customers')).toBe('true');
    });

    it('renders Persian inline guidance properly', () => {
      render(
        <ThemeProvider>
          <SectionHelp language="fa" sectionKey="purchases" />
        </ThemeProvider>
      );

      expect(screen.getByText(/صدور و بایگانی سفارش‌های خرید از تأمین‌کنندگان/i)).toBeInTheDocument();
    });

    it('returns null gracefully for non-existent section key', () => {
      const { container } = render(
        <ThemeProvider>
          <SectionHelp language="en" sectionKey="non_existent_key" />
        </ThemeProvider>
      );

      expect(container.firstChild).toBeNull();
    });
  });
});
