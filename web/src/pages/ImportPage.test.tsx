import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ImportResult } from '../api/imports';
import * as importsApi from '../api/imports';
import { renderWithProviders } from '../test/utils';
import { ImportPage } from './ImportPage';

vi.mock('../api/imports', () => ({ importVitals: vi.fn() }));

const file = new File(['measured_at,type,value\n2026-09-01 07:30,WEIGHT,82.4\n'], 'vitals.csv', { type: 'text/csv' });

const clean: ImportResult = {
  dryRun: true,
  rows: 3,
  imported: 0,
  duplicates: 1,
  errors: [],
  preview: [
    { type: 'WEIGHT', value: 82.4, measuredAt: '2026-09-01T05:30:00Z' },
    { type: 'BLOOD_PRESSURE', value: 128, secondaryValue: 82, measuredAt: '2026-09-01T06:00:00Z' },
  ],
};

describe('ImportPage', () => {
  beforeEach(() => {
    vi.mocked(importsApi.importVitals).mockReset();
  });

  it('previews the file and imports it on confirmation', async () => {
    vi.mocked(importsApi.importVitals)
      .mockResolvedValueOnce(clean)
      .mockResolvedValueOnce({ ...clean, dryRun: false, imported: 2 });
    renderWithProviders(<ImportPage />);

    await userEvent.upload(screen.getByLabelText('CSV file'), file);

    expect(await screen.findByText('128/82 mmHg')).toBeInTheDocument();
    expect(importsApi.importVitals).toHaveBeenLastCalledWith(file, true);
    await userEvent.click(screen.getByRole('button', { name: 'Import 2 readings' }));

    await waitFor(() => expect(importsApi.importVitals).toHaveBeenLastCalledWith(file, false));
    expect(await screen.findByText(/Imported 2 readings, skipped 1/)).toBeInTheDocument();
  });

  it('lists the problems and does not offer to import', async () => {
    vi.mocked(importsApi.importVitals).mockResolvedValue({
      ...clean,
      errors: [{ line: 4, message: 'Weight must be between 20 and 400' }],
    });
    renderWithProviders(<ImportPage />);

    await userEvent.upload(screen.getByLabelText('CSV file'), file);

    expect(await screen.findByText('Line 4: Weight must be between 20 and 400')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^Import/ })).toBeDisabled();
  });
});
