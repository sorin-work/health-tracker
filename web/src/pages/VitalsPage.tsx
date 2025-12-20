import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { deleteVital, listVitals, type VitalReading, type VitalType } from '../api/vitals';
import { DateRangePicker } from '../components/DateRangePicker';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { FlagBadge } from '../components/FlagBadge';
import { PlusIcon } from '../components/icons';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { VitalForm } from '../components/VitalForm';
import { formatDateTime, presetRange, type DateRange } from '../lib/dates';
import { formatVitalValue, GLUCOSE_CONTEXT_LABELS, VITAL_META, VITAL_TYPES } from '../lib/format';

const PAGE_SIZE = 25;

export function VitalsPage() {
  const queryClient = useQueryClient();
  const [type, setType] = useState<VitalType>('BLOOD_PRESSURE');
  const [range, setRange] = useState<DateRange>(presetRange('30d'));
  const [flaggedOnly, setFlaggedOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<VitalReading | 'new' | null>(null);

  function update(next: { type?: VitalType; range?: DateRange; flagged?: boolean }) {
    if (next.type) {
      setType(next.type);
      setRange(presetRange('30d'));
    }
    if (next.range) setRange(next.range);
    if (next.flagged !== undefined) setFlaggedOnly(next.flagged);
    setPage(0);
  }

  const readings = useQuery({
    queryKey: ['vitals', 'list', type, range.from, range.to, flaggedOnly, page],
    queryFn: () => listVitals({ type, from: range.from, to: range.to, flagged: flaggedOnly, page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: deleteVital,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['vitals'] }),
  });

  const meta = VITAL_META[type];
  const total = readings.data?.total ?? 0;
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <>
      <PageHeader
        title="Vitals"
        description="Readings are flagged when they fall outside your reference ranges."
        actions={
          <button className="button" onClick={() => setEditing('new')}>
            <PlusIcon /> Add reading
          </button>
        }
      />

      <div className="tabs" role="tablist">
        {VITAL_TYPES.map((t) => (
          <button
            key={t}
            role="tab"
            aria-selected={t === type}
            className={t === type ? 'tab tab--active' : 'tab'}
            onClick={() => update({ type: t })}
          >
            {VITAL_META[t].label}
          </button>
        ))}
      </div>

      <section className="card">
        <div className="card__header card__header--wrap">
          <h2>
            {meta.label} <span className="muted">({meta.unit})</span>
          </h2>
          <DateRangePicker value={range} onChange={(r) => update({ range: r })} />
          <label className="checkbox">
            <input type="checkbox" checked={flaggedOnly} onChange={(e) => update({ flagged: e.target.checked })} />
            Out of range only
          </label>
        </div>
        <ErrorMessage error={readings.error ?? remove.error} />
        {readings.data && readings.data.items.length === 0 ? (
          <EmptyState title={flaggedOnly ? 'No out-of-range readings in this period' : 'No readings yet'} />
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>When</th>
                  <th className="num">Value</th>
                  <th>Status</th>
                  <th>Notes</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {readings.data?.items.map((r) => (
                  <tr key={r.id}>
                    <td>{formatDateTime(r.measuredAt)}</td>
                    <td className="num">
                      {formatVitalValue(r.type, r.value, r.secondaryValue)} <span className="muted">{r.unit}</span>
                      {r.glucoseContext && <div className="muted small">{GLUCOSE_CONTEXT_LABELS[r.glucoseContext]}</div>}
                    </td>
                    <td>
                      <FlagBadge flag={r.flag} />
                    </td>
                    <td className="muted">{r.notes}</td>
                    <td className="row-actions">
                      <button className="link-button" onClick={() => setEditing(r)}>
                        Edit
                      </button>
                      <button
                        className="link-button link-button--danger"
                        onClick={() => window.confirm('Delete this reading?') && remove.mutate(r.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {pages > 1 && (
          <div className="pager">
            <button className="button button--ghost button--small" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span className="muted">
              Page {page + 1} of {pages}
            </span>
            <button
              className="button button--ghost button--small"
              disabled={page + 1 >= pages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </div>
        )}
      </section>

      {editing && (
        <Modal title={editing === 'new' ? 'Add reading' : 'Edit reading'} onClose={() => setEditing(null)}>
          <VitalForm
            reading={editing === 'new' ? undefined : editing}
            defaultType={type}
            onDone={() => setEditing(null)}
          />
        </Modal>
      )}
    </>
  );
}
