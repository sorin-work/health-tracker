import { screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Goal } from '../api/goals';
import * as goalsApi from '../api/goals';
import * as waterApi from '../api/water';
import { renderWithProviders } from '../test/utils';
import { WaterPage } from './WaterPage';

vi.mock('../api/goals', () => ({ listGoals: vi.fn() }));
vi.mock('../api/water', () => ({
  waterDaily: vi.fn(),
  waterForDay: vi.fn(),
  addWater: vi.fn(),
  deleteWater: vi.fn(),
}));

const goal = { id: 1, type: 'DAILY_WATER', targetValue: 1500 } as Goal;

describe('WaterPage', () => {
  beforeEach(() => {
    vi.mocked(goalsApi.listGoals).mockReset().mockResolvedValue([goal]);
    vi.mocked(waterApi.waterForDay).mockReset().mockResolvedValue({ date: '2026-06-10', totalMl: 0, entries: [] });
    vi.mocked(waterApi.waterDaily)
      .mockReset()
      .mockResolvedValue([
        { date: '2026-06-08', totalMl: 2000 },
        { date: '2026-06-09', totalMl: 0 },
        { date: '2026-06-10', totalMl: 1000 },
      ]);
  });

  it('averages the days that have entries and counts the days at the goal', async () => {
    renderWithProviders(<WaterPage />);

    expect(await screen.findByText('1,500 ml')).toBeInTheDocument();
    expect(screen.getByText('1 / 3')).toBeInTheDocument();
  });

  it('uses the water goal as the target', async () => {
    renderWithProviders(<WaterPage />);

    expect(await screen.findByText(/of 1,500 ml/)).toBeInTheDocument();
  });
});
