import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Dose } from '../api/medications';
import * as medicationsApi from '../api/medications';
import { renderWithProviders } from '../test/utils';
import { DoseList } from './DoseList';

vi.mock('../api/medications', () => ({
  recordDose: vi.fn(),
  undoDose: vi.fn(),
}));

const base: Omit<Dose, 'status' | 'scheduledAt'> = {
  medicationId: 3,
  medicationName: 'Metformin',
  dosage: '500 mg',
  asNeeded: false,
};

describe('DoseList', () => {
  beforeEach(() => {
    vi.mocked(medicationsApi.recordDose).mockReset().mockResolvedValue({} as Dose);
    vi.mocked(medicationsApi.undoDose).mockReset().mockResolvedValue(undefined);
  });

  it('marks a pending dose as taken', async () => {
    const dose: Dose = { ...base, scheduledAt: '2026-01-20T19:00:00Z', status: 'PENDING' };
    renderWithProviders(<DoseList doses={[dose]} />);

    expect(screen.getByText('Due')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /as taken/ }));

    expect(medicationsApi.recordDose).toHaveBeenCalledWith(3, '2026-01-20T19:00:00Z', 'TAKEN');
  });

  it('can skip a dose', async () => {
    const dose: Dose = { ...base, scheduledAt: '2026-01-20T07:00:00Z', status: 'PENDING' };
    renderWithProviders(<DoseList doses={[dose]} />);

    await userEvent.click(screen.getByRole('button', { name: /^Skip/ }));

    expect(medicationsApi.recordDose).toHaveBeenCalledWith(3, '2026-01-20T07:00:00Z', 'SKIPPED');
  });

  it('undoes a logged dose', async () => {
    const dose: Dose = { ...base, scheduledAt: '2026-01-20T07:00:00Z', status: 'TAKEN', logId: 11 };
    renderWithProviders(<DoseList doses={[dose]} />);

    expect(screen.getByText('Taken')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /as taken/ })).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /^Undo/ }));

    expect(medicationsApi.undoDose).toHaveBeenCalledWith(11);
  });

  it('labels as-needed doses', () => {
    const dose: Dose = { ...base, asNeeded: true, scheduledAt: '2026-01-20T10:00:00Z', status: 'TAKEN', logId: 2 };
    renderWithProviders(<DoseList doses={[dose]} />);

    expect(screen.getByText('As needed')).toBeInTheDocument();
  });
});
