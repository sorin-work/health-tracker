import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { presetRange } from '../lib/dates';
import { DateRangePicker } from './DateRangePicker';

describe('DateRangePicker', () => {
  it('highlights the matching preset', () => {
    render(<DateRangePicker value={presetRange('30d')} onChange={() => {}} />);
    expect(screen.getByRole('button', { name: '30 days' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: '7 days' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('switches presets', async () => {
    const onChange = vi.fn();
    render(<DateRangePicker value={presetRange('30d')} onChange={onChange} />);

    await userEvent.click(screen.getByRole('button', { name: '90 days' }));

    expect(onChange).toHaveBeenCalledWith(presetRange('90d'));
  });

  it('shows the custom dates', () => {
    render(<DateRangePicker value={{ from: '2026-01-01', to: '2026-01-31' }} onChange={() => {}} />);
    expect(screen.getByLabelText('From')).toHaveValue('2026-01-01');
    expect(screen.getByLabelText('To')).toHaveValue('2026-01-31');
    expect(screen.getAllByRole('button').every((b) => b.getAttribute('aria-pressed') === 'false')).toBe(true);
  });

  it('moves the other end when a typed date crosses it', () => {
    const onChange = vi.fn();
    render(<DateRangePicker value={{ from: '2026-01-01', to: '2026-01-31' }} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-02-10' } });
    expect(onChange).toHaveBeenLastCalledWith({ from: '2026-02-10', to: '2026-02-10' });

    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2025-12-24' } });
    expect(onChange).toHaveBeenLastCalledWith({ from: '2025-12-24', to: '2025-12-24' });
  });
});
