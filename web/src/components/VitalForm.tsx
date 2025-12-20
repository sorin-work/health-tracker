import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import {
  createVital,
  updateVital,
  type GlucoseContext,
  type VitalPayload,
  type VitalReading,
  type VitalType,
} from '../api/vitals';
import { fromLocalInputValue, toLocalInputValue } from '../lib/dates';
import { GLUCOSE_CONTEXT_LABELS, VITAL_META, VITAL_TYPES } from '../lib/format';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

export function VitalForm({
  reading,
  defaultType = 'WEIGHT',
  onDone,
}: {
  reading?: VitalReading;
  defaultType?: VitalType;
  onDone: () => void;
}) {
  const queryClient = useQueryClient();
  const [type, setType] = useState<VitalType>(reading?.type ?? defaultType);
  const [value, setValue] = useState(reading ? String(reading.value) : '');
  const [secondary, setSecondary] = useState(reading?.secondaryValue ? String(reading.secondaryValue) : '');
  const [context, setContext] = useState<GlucoseContext>(reading?.glucoseContext ?? 'FASTING');
  const [measuredAt, setMeasuredAt] = useState(
    toLocalInputValue(reading ? new Date(reading.measuredAt) : new Date()),
  );
  const [notes, setNotes] = useState(reading?.notes ?? '');

  const mutation = useMutation({
    mutationFn: (payload: VitalPayload) => (reading ? updateVital(reading.id, payload) : createVital(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vitals'] });
      queryClient.invalidateQueries({ queryKey: ['goals'] });
      onDone();
    },
  });

  const meta = VITAL_META[type];
  const fields = mutation.error instanceof ApiError ? mutation.error.fields : {};

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({
      type,
      value: Number(value),
      secondaryValue: type === 'BLOOD_PRESSURE' ? Number(secondary) : null,
      glucoseContext: type === 'BLOOD_GLUCOSE' ? context : null,
      measuredAt: fromLocalInputValue(measuredAt),
      notes: notes.trim() || null,
    });
  }

  return (
    <form className="form" onSubmit={submit}>
      <Field label="Type">
        <select value={type} onChange={(e) => setType(e.target.value as VitalType)} disabled={!!reading}>
          {VITAL_TYPES.map((t) => (
            <option key={t} value={t}>
              {VITAL_META[t].label}
            </option>
          ))}
        </select>
      </Field>

      {type === 'BLOOD_PRESSURE' ? (
        <div className="form__row">
          <Field label="Systolic (mmHg)" error={fields.value}>
            <input type="number" inputMode="numeric" required min={50} max={260} value={value} placeholder="120" onChange={(e) => setValue(e.target.value)} />
          </Field>
          <Field label="Diastolic (mmHg)" error={fields.secondaryValue}>
            <input type="number" inputMode="numeric" required min={30} max={160} value={secondary} placeholder="80" onChange={(e) => setSecondary(e.target.value)} />
          </Field>
        </div>
      ) : (
        <Field label={`${meta.label} (${meta.unit})`} error={fields.value}>
          <input
            type="number"
            inputMode="decimal"
            step={meta.decimals > 0 ? '0.1' : '1'}
            required
            min={0}
            value={value}
            placeholder={meta.placeholder}
            onChange={(e) => setValue(e.target.value)}
          />
        </Field>
      )}

      {type === 'BLOOD_GLUCOSE' && (
        <Field label="When">
          <select value={context} onChange={(e) => setContext(e.target.value as GlucoseContext)}>
            {Object.entries(GLUCOSE_CONTEXT_LABELS).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </Field>
      )}

      <Field label={type === 'SLEEP' ? 'Woke up at' : 'Measured at'} error={fields.measuredAt}>
        <input type="datetime-local" required value={measuredAt} max={toLocalInputValue()} onChange={(e) => setMeasuredAt(e.target.value)} />
      </Field>

      <Field label="Notes" hint="Optional, e.g. “after coffee”">
        <textarea rows={2} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Field>

      <ErrorMessage error={mutation.error && Object.keys(fields).length === 0 ? mutation.error : null} />

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : reading ? 'Save changes' : 'Add reading'}
        </button>
      </div>
    </form>
  );
}
