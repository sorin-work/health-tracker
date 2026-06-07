import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { WaterEntry } from '../api/water';
import * as waterApi from '../api/water';
import { renderWithProviders } from '../test/utils';
import { WaterCard } from './WaterCard';

vi.mock('../api/water', () => ({ waterForDay: vi.fn(), addWater: vi.fn(), deleteWater: vi.fn() }));

describe('WaterCard', () => {
  beforeEach(() => {
    vi.mocked(waterApi.waterForDay)
      .mockReset()
      .mockResolvedValue({
        date: '2026-06-10',
        totalMl: 750,
        entries: [
          { id: 1, amountMl: 250, loggedAt: '2026-06-10T06:00:00Z' },
          { id: 2, amountMl: 500, loggedAt: '2026-06-10T09:00:00Z' },
        ],
      });
    vi.mocked(waterApi.addWater).mockReset().mockResolvedValue({} as WaterEntry);
    vi.mocked(waterApi.deleteWater).mockReset().mockResolvedValue(undefined);
  });

  it('shows the total against the target', async () => {
    renderWithProviders(<WaterCard targetMl={1500} />);

    expect(await screen.findByText('750 ml')).toBeInTheDocument();
    expect(screen.getByText(/of 1,500 ml/)).toBeInTheDocument();
    expect(screen.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '50');
  });

  it('adds a glass with one click', async () => {
    renderWithProviders(<WaterCard />);

    await userEvent.click(screen.getByRole('button', { name: '+ 250 ml' }));

    await waitFor(() => expect(waterApi.addWater).toHaveBeenCalledWith(250));
  });

  it('adds a custom amount', async () => {
    renderWithProviders(<WaterCard />);

    await userEvent.type(screen.getByLabelText('Other amount in ml'), '330');
    await userEvent.click(screen.getByRole('button', { name: 'Add' }));

    await waitFor(() => expect(waterApi.addWater).toHaveBeenCalledWith(330));
    await waitFor(() => expect(screen.getByLabelText('Other amount in ml')).toHaveValue(null));
  });

  it('undoes the last entry', async () => {
    renderWithProviders(<WaterCard />);

    await userEvent.click(await screen.findByRole('button', { name: 'Undo' }));

    await waitFor(() => expect(waterApi.deleteWater).toHaveBeenCalled());
    expect(vi.mocked(waterApi.deleteWater).mock.calls[0][0]).toBe(2);
  });
});
