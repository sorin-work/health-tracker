import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import type { Goal } from '../api/goals';
import { GoalCard, goalStatus } from './GoalCard';

const goal: Goal = {
  id: 1,
  type: 'WEIGHT',
  unit: 'kg',
  targetValue: 80,
  startValue: 90,
  startDate: '2026-01-01',
  targetDate: '2026-06-01',
  archived: false,
  currentValue: 86,
  progressPercent: 40,
  achieved: false,
  onTrack: true,
};

describe('GoalCard', () => {
  it('shows progress towards a weight goal', () => {
    render(<GoalCard goal={goal} />);

    expect(screen.getByRole('heading', { name: 'Reach 80.0 kg' })).toBeInTheDocument();
    expect(screen.getByText('On track')).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '40');
    expect(screen.getByText(/started at 90.0/)).toBeInTheDocument();
  });

  it('works out a status', () => {
    expect(goalStatus({ ...goal, achieved: true }).text).toBe('Achieved');
    expect(goalStatus({ ...goal, onTrack: false }).text).toBe('Behind schedule');
    expect(goalStatus({ ...goal, onTrack: undefined }).text).toBe('In progress');
    expect(goalStatus({ ...goal, currentValue: undefined }).text).toBe('No data yet');
  });
});
