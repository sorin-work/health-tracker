import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import * as vitalsApi from '../api/vitals';
import { renderWithProviders } from '../test/utils';
import { VitalForm } from './VitalForm';

vi.mock('../api/vitals', () => ({
  createVital: vi.fn(),
  updateVital: vi.fn(),
}));

describe('VitalForm', () => {
  beforeEach(() => {
    vi.mocked(vitalsApi.createVital).mockReset();
  });

  it('asks for both numbers for blood pressure', async () => {
    vi.mocked(vitalsApi.createVital).mockResolvedValue({} as vitalsApi.VitalReading);
    const onDone = vi.fn();
    renderWithProviders(<VitalForm defaultType="BLOOD_PRESSURE" onDone={onDone} />);

    await userEvent.type(screen.getByLabelText('Systolic (mmHg)'), '128');
    await userEvent.type(screen.getByLabelText('Diastolic (mmHg)'), '82');
    await userEvent.click(screen.getByRole('button', { name: 'Add reading' }));

    expect(vitalsApi.createVital).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'BLOOD_PRESSURE', value: 128, secondaryValue: 82, glucoseContext: null }),
    );
    expect(onDone).toHaveBeenCalled();
  });

  it('asks when a glucose reading was taken', async () => {
    vi.mocked(vitalsApi.createVital).mockResolvedValue({} as vitalsApi.VitalReading);
    renderWithProviders(<VitalForm defaultType="BLOOD_GLUCOSE" onDone={() => {}} />);

    await userEvent.type(screen.getByLabelText('Blood glucose (mg/dL)'), '104');
    await userEvent.selectOptions(screen.getByLabelText('When'), 'AFTER_MEAL');
    await userEvent.click(screen.getByRole('button', { name: 'Add reading' }));

    expect(vitalsApi.createVital).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'BLOOD_GLUCOSE', value: 104, secondaryValue: null, glucoseContext: 'AFTER_MEAL' }),
    );
  });

  it('shows server errors', async () => {
    vi.mocked(vitalsApi.createVital).mockRejectedValue(new ApiError(400, 'Weight must be between 20 and 400'));
    const onDone = vi.fn();
    renderWithProviders(<VitalForm defaultType="WEIGHT" onDone={onDone} />);

    await userEvent.type(screen.getByLabelText('Weight (kg)'), '824');
    await userEvent.click(screen.getByRole('button', { name: 'Add reading' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Weight must be between 20 and 400');
    expect(onDone).not.toHaveBeenCalled();
  });
});
