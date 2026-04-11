import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as rangesApi from '../api/ranges';
import type { VitalReading } from '../api/vitals';
import * as vitalsApi from '../api/vitals';
import { renderWithProviders } from '../test/utils';
import { VitalsPage } from './VitalsPage';

vi.mock('../api/vitals', () => ({
  listVitals: vi.fn(),
  vitalTrend: vi.fn(),
  deleteVital: vi.fn(),
  createVital: vi.fn(),
  updateVital: vi.fn(),
}));
vi.mock('../api/ranges', () => ({ listRanges: vi.fn() }));

function reading(id: number): VitalReading {
  return { id, type: 'WEIGHT', value: 80 + id / 10, unit: 'kg', measuredAt: '2026-03-01T07:00:00Z', flag: 'NORMAL' };
}

describe('VitalsPage', () => {
  beforeEach(() => {
    vi.mocked(vitalsApi.vitalTrend).mockReset().mockResolvedValue([]);
    vi.mocked(vitalsApi.deleteVital).mockReset().mockResolvedValue(undefined);
    vi.mocked(rangesApi.listRanges).mockReset().mockResolvedValue([]);
    vi.mocked(vitalsApi.listVitals)
      .mockReset()
      .mockImplementation(async ({ page = 0 }) =>
        page === 0
          ? { items: Array.from({ length: 25 }, (_, i) => reading(i + 1)), page: 0, size: 25, total: 26 }
          : { items: [reading(26)], page: 1, size: 25, total: 26 },
      );
    vi.spyOn(window, 'confirm').mockReturnValue(true);
  });

  it('goes back a page after deleting the last reading on the last page', async () => {
    renderWithProviders(<VitalsPage />, { route: '/vitals?type=WEIGHT' });

    await userEvent.click(await screen.findByRole('button', { name: 'Next' }));
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(vi.mocked(vitalsApi.deleteVital).mock.calls[0][0]).toBe(26);
    await waitFor(() => expect(screen.queryByText(/Page 2/)).not.toBeInTheDocument());
    expect(vi.mocked(vitalsApi.listVitals).mock.lastCall?.[0]).toMatchObject({ page: 0 });
  });

  it('includes the diastolic average in the blood pressure summary', async () => {
    vi.mocked(vitalsApi.vitalTrend).mockResolvedValue([
      { date: '2026-03-01', average: 130, min: 125, max: 135, averageSecondary: 85, count: 2, flagged: 0 },
      { date: '2026-03-02', average: 120, min: 120, max: 120, averageSecondary: 80, count: 2, flagged: 0 },
    ]);
    renderWithProviders(<VitalsPage />, { route: '/vitals?type=BLOOD_PRESSURE' });

    expect(await screen.findByText('125/83 mmHg')).toBeInTheDocument();
    expect(screen.getByText('Lowest systolic')).toBeInTheDocument();
  });
});
