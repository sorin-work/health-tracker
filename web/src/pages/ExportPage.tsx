import { useState } from 'react';
import { downloadExport, type ExportDataset } from '../api/exports';
import { DateRangePicker } from '../components/DateRangePicker';
import { ErrorMessage } from '../components/ErrorMessage';
import { DownloadIcon } from '../components/icons';
import { PageHeader } from '../components/PageHeader';
import { presetRange, type DateRange } from '../lib/dates';

const DATASETS: { key: ExportDataset; title: string; description: string; ranged: boolean }[] = [
  {
    key: 'vitals',
    title: 'Vitals',
    description: 'Every reading with its unit, meal context and range flag.',
    ranged: true,
  },
  {
    key: 'doses',
    title: 'Dose log',
    description: 'Each scheduled dose and whether it was taken, skipped or missed.',
    ranged: true,
  },
  { key: 'medications', title: 'Medication list', description: 'Names, doses, schedules and dates.', ranged: false },
  { key: 'symptoms', title: 'Symptoms', description: 'Symptom entries with severity and duration.', ranged: true },
  { key: 'activities', title: 'Activity', description: 'Workouts with duration, distance and steps.', ranged: true },
  { key: 'mood', title: 'Mood', description: 'Mood and energy check-ins with notes.', ranged: true },
  { key: 'water', title: 'Water', description: 'Every glass with the time it was logged.', ranged: true },
];

export function ExportPage() {
  const [range, setRange] = useState<DateRange>(presetRange('90d'));
  const [busy, setBusy] = useState<ExportDataset | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [last, setLast] = useState<string | null>(null);

  async function download(dataset: ExportDataset, ranged: boolean) {
    setBusy(dataset);
    setError(null);
    try {
      const name = ranged ? await downloadExport(dataset, range.from, range.to) : await downloadExport(dataset);
      setLast(name);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      <PageHeader
        title="Export"
        description="Download your data as CSV to share with your doctor or open in a spreadsheet. Times are in your time zone."
      />
      <section className="card">
        <div className="card__header card__header--wrap">
          <h2>Period</h2>
          <DateRangePicker value={range} onChange={setRange} />
        </div>
        <ErrorMessage error={error} />
        {last && (
          <div className="alert alert--success" role="status">
            Downloaded {last}
          </div>
        )}
        <ul className="export-list">
          {DATASETS.map((d) => (
            <li key={d.key}>
              <div>
                <strong>{d.title}</strong>
                <div className="muted small">
                  {d.description}
                  {!d.ranged && ' Not limited to the period.'}
                </div>
              </div>
              <button className="button button--ghost" disabled={busy !== null} onClick={() => download(d.key, d.ranged)}>
                <DownloadIcon /> {busy === d.key ? 'Preparing…' : 'CSV'}
              </button>
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
