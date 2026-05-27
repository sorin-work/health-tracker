import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { deleteMood, listMood, moodDaily, type MoodEntry } from '../api/mood';
import { MoodChart } from '../components/charts/MoodChart';
import { DateRangePicker } from '../components/DateRangePicker';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { Modal } from '../components/Modal';
import { MoodForm } from '../components/MoodForm';
import { PageHeader } from '../components/PageHeader';
import { formatDateTime, presetRange, type DateRange } from '../lib/dates';
import { ENERGY_LABELS, MOOD_LABELS } from '../lib/format';

export function MoodPage() {
  const queryClient = useQueryClient();
  const [range, setRange] = useState<DateRange>(presetRange('30d'));
  const [editing, setEditing] = useState<MoodEntry | null>(null);

  const entries = useQuery({
    queryKey: ['mood', 'list', range.from, range.to],
    queryFn: () => listMood({ from: range.from, to: range.to, size: 200 }),
    placeholderData: keepPreviousData,
  });
  const daily = useQuery({
    queryKey: ['mood', 'daily', range.from, range.to],
    queryFn: () => moodDaily(range.from, range.to),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: deleteMood,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['mood'] }),
  });

  const items = entries.data?.items ?? [];

  return (
    <>
      <PageHeader
        title="Mood"
        description="A quick daily check-in. Over a few weeks it shows how sleep, medication changes and activity affect how you feel."
      />

      <section className="card">
        <div className="card__header card__header--wrap">
          <h2>Over time</h2>
          <DateRangePicker value={range} onChange={setRange} />
        </div>
        <ErrorMessage error={daily.error} />
        {daily.data && daily.data.length > 0 ? (
          <MoodChart days={daily.data} />
        ) : (
          !daily.isPending && <EmptyState title="No check-ins in this period" />
        )}
      </section>

      <div className="grid grid--2">
        <section className="card">
          <h2>Check in</h2>
          <MoodForm />
        </section>

        <section className="card">
          <h2>Check-ins</h2>
          <ErrorMessage error={entries.error ?? remove.error} />
          {items.length === 0 && !entries.isPending && <EmptyState title="No check-ins in this period" />}
          <ul className="mood-list">
            {items.map((e) => (
              <li key={e.id}>
                <span className={`mood-dot mood-dot--${e.mood}`} aria-hidden>
                  {e.mood}
                </span>
                <div>
                  <strong>{MOOD_LABELS[e.mood]}</strong>
                  {e.energy !== undefined && <span className="muted"> · {ENERGY_LABELS[e.energy]}</span>}
                  <div className="muted small">{formatDateTime(e.recordedAt)}</div>
                  {e.note && <div className="small">{e.note}</div>}
                </div>
                <div className="row-actions">
                  <button className="link-button" onClick={() => setEditing(e)}>
                    Edit
                  </button>
                  <button
                    className="link-button link-button--danger"
                    onClick={() => window.confirm('Delete this check-in?') && remove.mutate(e.id)}
                  >
                    Delete
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </section>
      </div>

      {editing && (
        <Modal title="Edit check-in" onClose={() => setEditing(null)}>
          <MoodForm entry={editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}
