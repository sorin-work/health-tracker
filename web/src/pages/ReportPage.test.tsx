import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { DoctorReport } from '../api/report';
import * as reportApi from '../api/report';
import { renderWithProviders } from '../test/utils';
import { ReportPage } from './ReportPage';

vi.mock('../api/report', () => ({ doctorReport: vi.fn() }));

const report: DoctorReport = {
  patient: 'Alex Demo',
  timezone: 'Europe/Berlin',
  from: '2026-07-01',
  to: '2026-09-28',
  generatedAt: '2026-09-28T18:00:00Z',
  vitals: [
    { type: 'BLOOD_PRESSURE', unit: 'mmHg', count: 120, average: 124, averageSecondary: 79, min: 108, max: 162, flagged: 4 },
  ],
  flaggedReadings: [
    {
      id: 1,
      type: 'BLOOD_PRESSURE',
      value: 162,
      secondaryValue: 101,
      unit: 'mmHg',
      measuredAt: '2026-08-14T18:00:00Z',
      flag: 'HIGH',
      notes: 'Stressful day',
    },
  ],
  flaggedTotal: 4,
  adherence: { scheduled: 100, taken: 95, skipped: 2, missed: 3, pending: 0, percent: 95 },
  medications: [
    {
      name: 'Vitamin D3',
      dosage: '20 000 IU',
      schedule: 'WEEKLY',
      times: ['09:00:00'],
      daysOfWeek: ['SUNDAY'],
      startDate: '2026-01-01',
      asNeeded: false,
      adherence: { scheduled: 13, taken: 12, skipped: 0, missed: 1, pending: 0, percent: 92.3 },
      asNeededTaken: 0,
    },
    {
      name: 'Ibuprofen',
      schedule: 'AS_NEEDED',
      times: [],
      daysOfWeek: [],
      startDate: '2026-01-01',
      asNeeded: true,
      asNeededTaken: 6,
    },
  ],
  symptoms: [{ name: 'Headache', count: 9, averageSeverity: 4.2, maxSeverity: 7 }],
  mood: { checkIns: 70, averageMood: 3.8, averageEnergy: 3.4 },
  activity: { sessions: 80, activeMinutes: 3600, averageMinutesPerWeek: 280 },
  notes: [
    {
      id: 3,
      entryDate: '2026-09-20',
      title: 'Next appointment',
      body: 'Ask about the metformin dose',
      createdAt: '',
      updatedAt: '',
    },
  ],
};

describe('ReportPage', () => {
  beforeEach(() => {
    vi.mocked(reportApi.doctorReport).mockReset().mockResolvedValue(report);
  });

  it('summarises the period', async () => {
    renderWithProviders(<ReportPage />);

    expect(await screen.findByRole('heading', { name: 'Health summary: Alex Demo' })).toBeInTheDocument();
    expect(screen.getByText('124/79 mmHg')).toBeInTheDocument();
    expect(screen.getByText('162/101 mmHg')).toBeInTheDocument();
    expect(screen.getByText(/latest 1 of 4/)).toBeInTheDocument();
    expect(screen.getByText('Sun at 09:00')).toBeInTheDocument();
    expect(screen.getByText('92% (1 missed)')).toBeInTheDocument();
    expect(screen.getByText('taken 6×')).toBeInTheDocument();
    expect(screen.getByText('Ask about the metformin dose')).toBeInTheDocument();
    expect(screen.getAllByText(/not medical advice/).length).toBeGreaterThan(0);
  });

  it('prints', async () => {
    const print = vi.spyOn(window, 'print').mockImplementation(() => undefined);
    renderWithProviders(<ReportPage />);

    await screen.findByRole('heading', { name: /Health summary/ });
    await userEvent.click(screen.getByRole('button', { name: 'Print or save as PDF' }));

    expect(print).toHaveBeenCalled();
  });
});
