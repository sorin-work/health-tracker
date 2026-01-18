import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { deleteSymptom, listSymptoms, symptomNames, type Symptom } from '../api/symptoms';
import { SymptomChart } from '../components/charts/SymptomChart';
import { DateRangePicker } from '../components/DateRangePicker';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { PlusIcon } from '../components/icons';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { SymptomForm } from '../components/SymptomForm';
import { formatDateTime, presetRange, type DateRange } from '../lib/dates';
import { formatMinutes, severityLabel } from '../lib/format';

export function SymptomsPage() {
  const queryClient = useQueryClient();
  const [range, setRange] = useState<DateRange>(presetRange('90d'));
  const [name, setName] = useState('');
  const [editing, setEditing] = useState<Symptom | 'new' | null>(null);

  const names = useQuery({ queryKey: ['symptoms', 'names'], queryFn: symptomNames });
  const symptoms = useQuery({
    queryKey: ['symptoms', 'list', range.from, range.to, name],
    queryFn: () => listSymptoms({ from: range.from, to: range.to, name, size: 500 }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: deleteSymptom,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['symptoms'] }),
  });

  const items = symptoms.data?.items ?? [];

  return (
    <>
      <PageHeader
        title="Symptoms"
        description="Log how you feel. Patterns are easier to spot (and to explain to a doctor) over a few weeks."
        actions={
          <button className="button" onClick={() => setEditing('new')}>
            <PlusIcon /> Log symptom
          </button>
        }
      />

      <section className="card">
        <div className="card__header card__header--wrap">
          <div className="filters">
            <select aria-label="Symptom" value={name} onChange={(e) => setName(e.target.value)}>
              <option value="">All symptoms</option>
              {names.data?.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </div>
          <DateRangePicker value={range} onChange={setRange} />
        </div>
        <ErrorMessage error={symptoms.error ?? remove.error} />
        {items.length > 0 ? (
          <SymptomChart symptoms={items} />
        ) : (
          !symptoms.isPending && <EmptyState title="No symptoms logged in this period" />
        )}
      </section>

      {items.length > 0 && (
        <section className="card">
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Symptom</th>
                  <th>Severity</th>
                  <th>Duration</th>
                  <th>Notes</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {items.map((s) => (
                  <tr key={s.id}>
                    <td>{formatDateTime(s.occurredAt)}</td>
                    <td>{s.name}</td>
                    <td>
                      <span className={`severity severity--${Math.ceil(s.severity / 3.4)}`}>{s.severity}</span>{' '}
                      <span className="muted small">{severityLabel(s.severity)}</span>
                    </td>
                    <td>{s.durationMinutes ? formatMinutes(s.durationMinutes) : '–'}</td>
                    <td className="muted">{s.notes}</td>
                    <td className="row-actions">
                      <button className="link-button" onClick={() => setEditing(s)}>
                        Edit
                      </button>
                      <button
                        className="link-button link-button--danger"
                        onClick={() => window.confirm('Delete this entry?') && remove.mutate(s.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      {editing && (
        <Modal title={editing === 'new' ? 'Log symptom' : 'Edit symptom'} onClose={() => setEditing(null)}>
          <SymptomForm symptom={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}
