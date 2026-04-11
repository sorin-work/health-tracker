import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { listRanges } from '../api/ranges';
import {
  deleteVital,
  listVitals,
  vitalTrend,
  type TrendPoint,
  type VitalReading,
  type VitalType,
} from '../api/vitals';
import { TrendChart } from '../components/charts/TrendChart';
import { DateRangePicker } from '../components/DateRangePicker';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { FlagBadge } from '../components/FlagBadge';
import { PlusIcon } from '../components/icons';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { VitalForm } from '../components/VitalForm';
import { formatDateTime, presetRange, type DateRange } from '../lib/dates';
import { formatNumber, formatVitalValue, GLUCOSE_CONTEXT_LABELS, VITAL_META, VITAL_TYPES } from '../lib/format';
import { bandsFor } from '../lib/ranges';

const PAGE_SIZE = 25;

function isVitalType(value: string | null): value is VitalType {
  return !!value && (VITAL_TYPES as string[]).includes(value);
}

export function VitalsPage() {
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const type: VitalType = isVitalType(params.get('type')) ? (params.get('type') as VitalType) : 'BLOOD_PRESSURE';
  const fallback = presetRange('30d');
  const range: DateRange = { from: params.get('from') ?? fallback.from, to: params.get('to') ?? fallback.to };
  const flaggedOnly = params.get('flagged') === 'true';
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<VitalReading | 'new' | null>(null);

  // Type, range and the flagged filter live in the URL so they survive switching tabs and reloads.
  function update(next: { type?: VitalType; range?: DateRange; flagged?: boolean }) {
    const merged = { type, ...range, flagged: flaggedOnly, ...next, ...(next.range ?? {}) };
    const search = new URLSearchParams({ type: merged.type, from: merged.from, to: merged.to });
    if (merged.flagged) search.set('flagged', 'true');
    setParams(search, { replace: true });
    setPage(0);
  }

  const trend = useQuery({
    queryKey: ['vitals', 'trend', type, range.from, range.to],
    queryFn: () => vitalTrend(type, range.from, range.to),
  });
  const readings = useQuery({
    queryKey: ['vitals', 'list', type, range.from, range.to, flaggedOnly, page],
    queryFn: () => listVitals({ type, from: range.from, to: range.to, flagged: flaggedOnly, page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });
  const ranges = useQuery({ queryKey: ['ranges'], queryFn: listRanges, staleTime: 5 * 60_000 });
  const remove = useMutation({
    mutationFn: deleteVital,
    onSuccess: () => {
      // Deleting the only reading on the last page would leave an empty page behind.
      if (page > 0 && readings.data?.items.length === 1) setPage(page - 1);
      queryClient.invalidateQueries({ queryKey: ['vitals'] });
    },
  });

  const meta = VITAL_META[type];
  const [band, secondaryBand] = bandsFor(type, ranges.data);
  const points = trend.data ?? [];
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
        </div>
        <ErrorMessage error={trend.error} />
        {points.length > 0 ? (
          <TrendChart type={type} points={points} band={band} secondaryBand={secondaryBand} />
        ) : (
          !trend.isPending && <EmptyState title={`No ${meta.label.toLowerCase()} readings in this period`} />
        )}
        {points.length > 0 && <TrendSummary type={type} points={points} />}
      </section>

      <section className="card">
        <div className="card__header">
          <h2>Readings</h2>
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

function TrendSummary({ type, points }: { type: VitalType; points: TrendPoint[] }) {
  const { decimals, unit } = VITAL_META[type];
  const count = points.reduce((n, p) => n + p.count, 0);
  const flagged = points.reduce((n, p) => n + p.flagged, 0);
  const avg = points.reduce((s, p) => s + p.average * p.count, 0) / count;
  // Blood pressure days always have a diastolic average; weight them the same way as systolic.
  const withSecondary = points.filter((p) => p.averageSecondary !== undefined && p.averageSecondary !== null);
  const secondaryCount = withSecondary.reduce((n, p) => n + p.count, 0);
  const avgSecondary =
    secondaryCount > 0
      ? withSecondary.reduce((s, p) => s + (p.averageSecondary ?? 0) * p.count, 0) / secondaryCount
      : undefined;
  const min = Math.min(...points.map((p) => p.min));
  const max = Math.max(...points.map((p) => p.max));
  return (
    <dl className="stats-row">
      <div>
        <dt>Average</dt>
        <dd>
          {formatVitalValue(type, avg, avgSecondary)} {unit}
        </dd>
      </div>
      <div>
        <dt>{type === 'BLOOD_PRESSURE' ? 'Lowest systolic' : 'Lowest'}</dt>
        <dd>{formatNumber(min, decimals)}</dd>
      </div>
      <div>
        <dt>{type === 'BLOOD_PRESSURE' ? 'Highest systolic' : 'Highest'}</dt>
        <dd>{formatNumber(max, decimals)}</dd>
      </div>
      <div>
        <dt>Readings</dt>
        <dd>{count}</dd>
      </div>
      <div>
        <dt>Out of range</dt>
        <dd className={flagged > 0 ? 'text-warn' : undefined}>{flagged}</dd>
      </div>
    </dl>
  );
}
