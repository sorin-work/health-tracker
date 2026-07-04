import { render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Goal } from '../api/goals';
import { goalStatus } from '../lib/goals';
import { GoalCard } from './GoalCard';

const prefs = vi.hoisted(() => ({ current: { unitSystem: 'METRIC', glucoseUnit: 'MG_DL' } }));
vi.mock('../auth/AuthContext', () => ({ useUnitPrefs: () => prefs.current }));

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
  beforeEach(() => {
    prefs.current = { unitSystem: 'METRIC', glucoseUnit: 'MG_DL' };
  });

  it('shows weight goals in pounds for imperial users', () => {
    prefs.current = { unitSystem: 'IMPERIAL', glucoseUnit: 'MG_DL' };
    render(<GoalCard goal={goal} />);

    expect(screen.getByRole('heading', { name: 'Reach 176.4 lb' })).toBeInTheDocument();
    expect(screen.getByText('189.6')).toBeInTheDocument();
  });

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
