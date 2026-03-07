import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { updateProfile } from '../api/auth';
import { listRanges, resetRange, updateRange, type ReferenceRange } from '../api/ranges';
import { ME_QUERY_KEY, useUser } from '../auth/AuthContext';
import { ErrorMessage } from '../components/ErrorMessage';
import { Field } from '../components/Field';
import { PageHeader } from '../components/PageHeader';
import { formatNumber } from '../lib/format';

function timezones(): string[] {
  const intl = Intl as unknown as { supportedValuesOf?: (key: string) => string[] };
  try {
    return intl.supportedValuesOf?.('timeZone') ?? [];
  } catch {
    return [];
  }
}

export function SettingsPage() {
  return (
    <>
      <PageHeader title="Settings" />
      <div className="grid grid--2">
        <ProfileForm />
        <RangesCard />
      </div>
    </>
  );
}

function ProfileForm() {
  const user = useUser();
  const queryClient = useQueryClient();
  const [displayName, setDisplayName] = useState(user.displayName);
  const [timezone, setTimezone] = useState(user.timezone);
  const [saved, setSaved] = useState(false);
  const zones = timezones();

  const mutation = useMutation({
    mutationFn: updateProfile,
    onSuccess: (updated) => {
      queryClient.setQueryData(ME_QUERY_KEY, updated);
      // Day boundaries depend on the zone, so everything date-based is stale now.
      queryClient.invalidateQueries({ predicate: (q) => q.queryKey[0] !== ME_QUERY_KEY[0] });
      setSaved(true);
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    setSaved(false);
    mutation.mutate({ displayName: displayName.trim(), timezone });
  }

  return (
    <section className="card">
      <h2>Profile</h2>
      <form className="form" onSubmit={submit}>
        <Field label="Name">
          <input required maxLength={100} value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
        </Field>
        <Field label="Email">
          <input value={user.email} disabled />
        </Field>
        <Field label="Time zone" hint="Used for daily totals, medication times and exports">
          {zones.length > 0 ? (
            <select value={timezone} onChange={(e) => setTimezone(e.target.value)}>
              {!zones.includes(timezone) && <option value={timezone}>{timezone}</option>}
              {zones.map((z) => (
                <option key={z} value={z}>
                  {z}
                </option>
              ))}
            </select>
          ) : (
            <input required value={timezone} onChange={(e) => setTimezone(e.target.value)} />
          )}
        </Field>
        <ErrorMessage error={mutation.error} />
        {saved && (
          <div className="alert alert--success" role="status">
            Saved
          </div>
        )}
        <div className="form__actions">
          <button className="button" type="submit" disabled={mutation.isPending}>
            Save
          </button>
        </div>
      </form>
    </section>
  );
}

function RangesCard() {
  const ranges = useQuery({ queryKey: ['ranges'], queryFn: listRanges });
  return (
    <section className="card">
      <h2>Reference ranges</h2>
      <p className="muted small">
        Readings outside these ranges are flagged. The defaults are general guidance for adults; if your doctor gave
        you different targets, enter them here. Critical limits always apply.
      </p>
      <ErrorMessage error={ranges.error} />
      <div className="ranges">
        {ranges.data?.map((r) => (
          <RangeRow key={r.metric} range={r} />
        ))}
      </div>
    </section>
  );
}

function RangeRow({ range }: { range: ReferenceRange }) {
  const queryClient = useQueryClient();
  const [low, setLow] = useState(String(range.low));
  const [high, setHigh] = useState(String(range.high));
  const onSuccess = (updated: ReferenceRange | void) => {
    queryClient.invalidateQueries({ queryKey: ['ranges'] });
    queryClient.invalidateQueries({ queryKey: ['vitals'] });
    if (!updated) {
      setLow(String(range.defaultLow));
      setHigh(String(range.defaultHigh));
    }
  };
  const save = useMutation({ mutationFn: () => updateRange(range.metric, Number(low), Number(high)), onSuccess });
  const reset = useMutation({ mutationFn: () => resetRange(range.metric), onSuccess: () => onSuccess() });
  const dirty = Number(low) !== range.low || Number(high) !== range.high;

  return (
    <form
      className="range-row"
      onSubmit={(e) => {
        e.preventDefault();
        save.mutate();
      }}
    >
      <div className="range-row__label">
        <strong>{range.label}</strong>
        <span className="muted small">
          {range.custom
            ? `Custom (default ${formatNumber(range.defaultLow)}–${formatNumber(range.defaultHigh)})`
            : 'Default'}
        </span>
      </div>
      <div className="range-row__inputs">
        <input type="number" step="any" aria-label={`${range.label} low`} value={low} onChange={(e) => setLow(e.target.value)} />
        <span aria-hidden>–</span>
        <input type="number" step="any" aria-label={`${range.label} high`} value={high} onChange={(e) => setHigh(e.target.value)} />
        <span className="muted small">{range.unit}</span>
      </div>
      <div className="range-row__actions">
        <button className="button button--small" type="submit" disabled={!dirty || save.isPending}>
          Save
        </button>
        {range.custom && (
          <button className="link-button" type="button" onClick={() => reset.mutate()}>
            Reset
          </button>
        )}
      </div>
      <ErrorMessage error={save.error ?? reset.error} />
    </form>
  );
}
