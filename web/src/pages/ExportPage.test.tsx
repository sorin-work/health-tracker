import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import * as exportsApi from '../api/exports';
import { presetRange } from '../lib/dates';
import { renderWithProviders } from '../test/utils';
import { ExportPage } from './ExportPage';

vi.mock('../api/exports', () => ({ downloadExport: vi.fn() }));

describe('ExportPage', () => {
  beforeEach(() => {
    vi.mocked(exportsApi.downloadExport).mockReset().mockResolvedValue('health-tracker-vitals.csv');
  });

  it('downloads a dataset for the chosen period', async () => {
    renderWithProviders(<ExportPage />);
    const range = presetRange('90d');

    const vitals = screen.getByText('Vitals').closest('li')!;
    await userEvent.click(vitals.querySelector('button')!);

    await waitFor(() => expect(exportsApi.downloadExport).toHaveBeenCalledWith('vitals', range.from, range.to));
    expect(await screen.findByText('Downloaded health-tracker-vitals.csv')).toBeInTheDocument();
  });

  it('downloads the medication list without a period', async () => {
    renderWithProviders(<ExportPage />);

    const list = screen.getByText('Medication list').closest('li')!;
    await userEvent.click(list.querySelector('button')!);

    await waitFor(() => expect(exportsApi.downloadExport).toHaveBeenCalledWith('medications'));
  });

  it('offers every dataset the api can export', () => {
    renderWithProviders(<ExportPage />);

    expect(screen.getAllByRole('button', { name: /CSV/ })).toHaveLength(8);
  });
});
