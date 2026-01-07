import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { archiveMedication, deleteMedication, dosesForDay, listMedications, type Medication } from '../api/medications';
import { DoseList } from '../components/DoseList';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { PlusIcon } from '../components/icons';
import { MedicationForm } from '../components/MedicationForm';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { addDays, formatDate, shortTime, today } from '../lib/dates';

export function MedicationsPage() {
  const queryClient = useQueryClient();
  const [day, setDay] = useState(today());
  const [showArchived, setShowArchived] = useState(false);
  const [editing, setEditing] = useState<Medication | 'new' | null>(null);

  const meds = useQuery({
    queryKey: ['medications', showArchived],
    queryFn: () => listMedications(showArchived),
  });
  const doses = useQuery({ queryKey: ['doses', day], queryFn: () => dosesForDay(day) });

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
                    {m.asNeeded ? 'As needed' : m.times.map(shortTime).join(', ')}
                    {' · since '}
                    {formatDate(m.startDate)}
                    {m.endDate && ` until ${formatDate(m.endDate)}`}
                  </div>
                  {m.instructions && <div className="small">{m.instructions}</div>}
                </div>
                <div className="row-actions">
                  <button className="link-button" onClick={() => setEditing(m)}>
                    Edit
                  </button>
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

      {editing && (
        <Modal title={editing === 'new' ? 'Add medication' : `Edit ${editing.name}`} onClose={() => setEditing(null)}>
          <MedicationForm medication={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}
