import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { ProgressBar } from './ProgressBar';

describe('ProgressBar', () => {
  it('exposes progress to assistive technology', () => {
    render(<ProgressBar percent={42.4} label="Daily steps" />);
    const bar = screen.getByRole('progressbar', { name: 'Daily steps' });
    expect(bar).toHaveAttribute('aria-valuenow', '42');
  });

  it('clamps values outside 0-100', () => {
    render(<ProgressBar percent={130} label="Over" />);
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '100');
  });
});
