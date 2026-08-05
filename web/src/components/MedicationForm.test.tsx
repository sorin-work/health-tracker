import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { Medication } from '../api/medications';
import * as medicationsApi from '../api/medications';
import { renderWithProviders } from '../test/utils';
import { MedicationForm } from './MedicationForm';

vi.mock('../api/medications', () => ({ createMedication: vi.fn(), updateMedication: vi.fn() }));

describe('MedicationForm', () => {
  beforeEach(() => {
    vi.mocked(medicationsApi.createMedication).mockReset().mockResolvedValue({} as Medication);
    vi.mocked(medicationsApi.updateMedication).mockReset().mockResolvedValue({} as Medication);
  });

  async function fillName(name: string) {
    await userEvent.type(screen.getByLabelText('Name'), name);
  }

  it('creates a daily medication by default', async () => {
    renderWithProviders(<MedicationForm onDone={vi.fn()} />);

    await fillName('Metformin');
    await userEvent.click(screen.getByRole('button', { name: 'Add medication' }));

    await waitFor(() => expect(medicationsApi.createMedication).toHaveBeenCalled());
    expect(vi.mocked(medicationsApi.createMedication).mock.calls[0][0]).toMatchObject({
      name: 'Metformin',
      times: ['08:00'],
      scheduleType: 'DAILY',
      intervalDays: null,
      daysOfWeek: null,
    });
  });

  it('sends the chosen days for a weekly schedule', async () => {
    renderWithProviders(<MedicationForm onDone={vi.fn()} />);

    await fillName('Vitamin D3');
    await userEvent.selectOptions(screen.getByLabelText('How often'), 'WEEKLY');
    // Start from a clean selection: unpress whatever day is pre-selected, then pick two.
    for (const day of screen.getAllByRole('button', { pressed: true })) await userEvent.click(day);
    expect(screen.getByRole('button', { name: 'Add medication' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: 'Thu' }));
    await userEvent.click(screen.getByRole('button', { name: 'Mon' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add medication' }));

    await waitFor(() => expect(medicationsApi.createMedication).toHaveBeenCalled());
    expect(vi.mocked(medicationsApi.createMedication).mock.calls[0][0]).toMatchObject({
      scheduleType: 'WEEKLY',
      daysOfWeek: ['THURSDAY', 'MONDAY'],
      intervalDays: null,
    });
  });

  it('sends the interval for an every-few-days schedule', async () => {
    renderWithProviders(<MedicationForm onDone={vi.fn()} />);

    await fillName('Prednisolone');
    await userEvent.selectOptions(screen.getByLabelText('How often'), 'EVERY_N_DAYS');
    const interval = screen.getByLabelText('Days between doses');
    await userEvent.clear(interval);
    await userEvent.type(interval, '3');
    await userEvent.click(screen.getByRole('button', { name: 'Add medication' }));

    await waitFor(() => expect(medicationsApi.createMedication).toHaveBeenCalled());
    expect(vi.mocked(medicationsApi.createMedication).mock.calls[0][0]).toMatchObject({
      scheduleType: 'EVERY_N_DAYS',
      intervalDays: 3,
      daysOfWeek: null,
    });
  });

  it('hides the schedule for as-needed medications', async () => {
    renderWithProviders(<MedicationForm onDone={vi.fn()} />);

    await userEvent.click(screen.getByLabelText(/only when needed/));

    expect(screen.queryByLabelText('How often')).not.toBeInTheDocument();
  });
});
