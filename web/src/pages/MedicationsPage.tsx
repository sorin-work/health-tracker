import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  adherence,
  archiveMedication,
  deleteMedication,
  dosesForDay,
  listMedications,
  type AdherenceStats,
  type Medication,
} from '../api/medications';
import { AdherenceChart } from '../components/charts/AdherenceChart';
import { DateRangePicker } from '../components/DateRangePicker';
import { DoseList } from '../components/DoseList';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { PlusIcon } from '../components/icons';
import { MedicationForm } from '../components/MedicationForm';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { RefillForm } from '../components/RefillForm';
import { ProgressBar } from '../components/ProgressBar';
import { addDays, formatDate, presetRange, today, type DateRange } from '../lib/dates';
import { adherenceTone, describeSchedule, describeSupply } from '../lib/format';

export function MedicationsPage() {
  const queryClient = useQueryClient();
  const [day, setDay] = useState(today());
  const [range, setRange] = useState<DateRange>(presetRange('30d'));
  const [showArchived, setShowArchived] = useState(false);
  const [editing, setEditing] = useState<Medication | 'new' | null>(null);
  const [refilling, setRefilling] = useState<Medication | null>(null);

  const meds = useQuery({
    queryKey: ['medications', showArchived],
    queryFn: () => listMedications(showArchived),
  });
  const doses = useQuery({ queryKey: ['doses', day], queryFn: () => dosesForDay(day) });
  const stats = useQuery({
    queryKey: ['doses', 'adherence', range.from, range.to],
    queryFn: () => adherence(range.from, range.to),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['medications'] });
    queryClient.invalidateQueries({ queryKey: ['doses'] });
  };
  const archive = useMutation({ mutationFn: archiveMedication, onSuccess: invalidate });
  const remove = useMutation({ mutationFn: deleteMedication, onSuccess: invalidate });

  const isToday = day === today();

  return (
    <>
      <PageHeader
        title="Medications"
        description="Mark doses as taken or skipped. Doses with nothing logged count as missed once the day is over."
        actions={
          <button className="button" onClick={() => setEditing('new')}>
            <PlusIcon /> Add medication
          </button>
        }
      />

      <div className="grid grid--2">
        <section className="card">
          <div className="card__header">
            <h2>{isToday ? 'Today' : formatDate(day)}</h2>
            <div className="segmented">
              <button aria-label="Previous day" onClick={() => setDay(addDays(day, -1))}>
                ‹
              </button>
              <button disabled={isToday} onClick={() => setDay(today())}>
                Today
              </button>
              <button aria-label="Next day" disabled={isToday} onClick={() => setDay(addDays(day, 1))}>
                ›
              </button>
            </div>
          </div>
          <ErrorMessage error={doses.error} />
          {doses.data && doses.data.length === 0 ? (
            <EmptyState title="Nothing scheduled">Add a medication with times of day to see it here.</EmptyState>
          ) : (
            doses.data && <DoseList doses={doses.data} />
          )}
        </section>

        <section className="card">
          <div className="card__header">
            <h2>Your medications</h2>
            <label className="checkbox">
              <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />
              Show stopped
            </label>
          </div>
          <ErrorMessage error={meds.error ?? archive.error ?? remove.error} />
          {meds.data && meds.data.length === 0 && <EmptyState title="No medications yet" />}
          <ul className="med-list">
            {meds.data?.map((m) => (
              <li key={m.id} className={m.active ? undefined : 'med--stopped'}>
                <div>
                  <strong>{m.name}</strong> {m.dosage && <span className="muted">{m.dosage}</span>}
                  <div className="muted small">
                    {describeSchedule(m)}
                    {' · since '}
                    {formatDate(m.startDate)}
                    {m.endDate && ` until ${formatDate(m.endDate)}`}
                  </div>
                  {m.instructions && <div className="small">{m.instructions}</div>}
                  {m.supply !== undefined && m.supply !== null && (
                    <div className="small">
                      <span className={m.lowSupply ? 'status status--warning' : 'muted'}>
                        {describeSupply(m)}
                      </span>
                    </div>
                  )}
                </div>
                <div className="row-actions">
                  <button className="link-button" onClick={() => setEditing(m)}>
                    Edit
                  </button>
                  {m.active && (
                    <button className="link-button" onClick={() => setRefilling(m)}>
                      Refill
                    </button>
                  )}
                  {m.active ? (
                    <button
                      className="link-button"
                      onClick={() =>
                        window.confirm(`Stop ${m.name}? Its history is kept.`) && archive.mutate(m.id)
                      }
                    >
                      Stop
                    </button>
                  ) : (
                    <button
                      className="link-button link-button--danger"
                      onClick={() =>
                        window.confirm(`Delete ${m.name} and all of its dose history?`) && remove.mutate(m.id)
                      }
                    >
                      Delete
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>
      </div>

      <section className="card">
        <div className="card__header card__header--wrap">
          <h2>Adherence</h2>
          <DateRangePicker value={range} onChange={setRange} />
        </div>
        <ErrorMessage error={stats.error} />
        {stats.data && (
          <>
            <AdherenceSummary stats={stats.data.overall} />
            {stats.data.daily.length > 0 && <AdherenceChart daily={stats.data.daily} />}
            {stats.data.medications.length > 0 && (
              <div className="table-wrap">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Medication</th>
                      <th className="num">Taken</th>
                      <th className="num">Skipped</th>
                      <th className="num">Missed</th>
                      <th>Adherence</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stats.data.medications.map((m) => (
                      <tr key={m.medicationId}>
                        <td>{m.name}</td>
                        <td className="num">{m.stats.taken}</td>
                        <td className="num">{m.stats.skipped}</td>
                        <td className="num">{m.stats.missed}</td>
                        <td className="adherence-cell">
                          <ProgressBar
                            percent={m.stats.percent ?? 0}
                            label={`${m.name} adherence`}
                            tone={adherenceTone(m.stats.percent)}
                          />
                          <span>{m.stats.percent === undefined ? '–' : `${m.stats.percent}%`}</span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </>
        )}
      </section>

      {refilling && (
        <Modal title={`Refill ${refilling.name}`} onClose={() => setRefilling(null)}>
          <RefillForm medication={refilling} onDone={() => setRefilling(null)} />
        </Modal>
      )}

      {editing && (
        <Modal title={editing === 'new' ? 'Add medication' : `Edit ${editing.name}`} onClose={() => setEditing(null)}>
          <MedicationForm medication={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}

function AdherenceSummary({ stats }: { stats: AdherenceStats }) {
  return (
    <dl className="stats-row">
      <div>
        <dt>Adherence</dt>
        <dd className="stat-large">{stats.percent === undefined ? '–' : `${stats.percent}%`}</dd>
      </div>
      <div>
        <dt>Taken</dt>
        <dd>{stats.taken}</dd>
      </div>
      <div>
        <dt>Skipped</dt>
        <dd>{stats.skipped}</dd>
      </div>
      <div>
        <dt>Missed</dt>
        <dd className={stats.missed > 0 ? 'text-warn' : undefined}>{stats.missed}</dd>
      </div>
      <div>
        <dt>Still due today</dt>
        <dd>{stats.pending}</dd>
      </div>
    </dl>
  );
}
