import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { createMedication, updateMedication, type Medication, type MedicationPayload } from '../api/medications';
import { shortTime, today } from '../lib/dates';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';
import { CloseIcon, PlusIcon } from './icons';

export function MedicationForm({ medication, onDone }: { medication?: Medication; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(medication?.name ?? '');
  const [dosage, setDosage] = useState(medication?.dosage ?? '');
  const [instructions, setInstructions] = useState(medication?.instructions ?? '');
  const [asNeeded, setAsNeeded] = useState(medication?.asNeeded ?? false);
  const [times, setTimes] = useState<string[]>(
    medication && medication.times.length > 0 ? medication.times.map(shortTime) : ['08:00'],
  );
  const [startDate, setStartDate] = useState(medication?.startDate ?? today());
  const [endDate, setEndDate] = useState(medication?.endDate ?? '');

  const mutation = useMutation({
    mutationFn: (payload: MedicationPayload) =>
      medication ? updateMedication(medication.id, payload) : createMedication(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medications'] });
      queryClient.invalidateQueries({ queryKey: ['doses'] });
      onDone();
    },
  });
  const fields = mutation.error instanceof ApiError ? mutation.error.fields : {};

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({
      name: name.trim(),
      dosage: dosage.trim() || null,
      instructions: instructions.trim() || null,
      asNeeded,
      times: asNeeded ? [] : times.filter(Boolean),
      startDate,
      endDate: endDate || null,
    });
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form__row">
        <Field label="Name" error={fields.name}>
          <input required maxLength={120} value={name} placeholder="Metformin" onChange={(e) => setName(e.target.value)} />
        </Field>
        <Field label="Dose" hint="e.g. 500 mg, 2 puffs">
          <input maxLength={60} value={dosage} onChange={(e) => setDosage(e.target.value)} />
        </Field>
      </div>

      <label className="checkbox">
        <input type="checkbox" checked={asNeeded} onChange={(e) => setAsNeeded(e.target.checked)} />
        Take only when needed (no schedule)
      </label>

      {!asNeeded && (
        <fieldset className="times">
          <legend>Times of day</legend>
          {times.map((time, i) => (
            <div className="times__row" key={i}>
              <input
                type="time"
                required
                aria-label={`Time ${i + 1}`}
                value={time}
                onChange={(e) => setTimes(times.map((t, j) => (j === i ? e.target.value : t)))}
              />
              {times.length > 1 && (
                <button
                  type="button"
                  className="icon-button"
                  aria-label={`Remove time ${i + 1}`}
                  onClick={() => setTimes(times.filter((_, j) => j !== i))}
                >
                  <CloseIcon />
                </button>
              )}
            </div>
          ))}
          {times.length < 12 && (
            <button type="button" className="button button--ghost button--small" onClick={() => setTimes([...times, '20:00'])}>
              <PlusIcon /> Add a time
            </button>
          )}
        </fieldset>
      )}

      <div className="form__row">
        <Field label="Start date" error={fields.startDate}>
          <input type="date" required value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </Field>
        <Field label="End date" hint="Leave empty if ongoing">
          <input type="date" value={endDate} min={startDate} onChange={(e) => setEndDate(e.target.value)} />
        </Field>
      </div>

      <Field label="Instructions">
        <textarea rows={2} maxLength={500} value={instructions} placeholder="With food" onChange={(e) => setInstructions(e.target.value)} />
      </Field>

      <ErrorMessage error={mutation.error && Object.keys(fields).length === 0 ? mutation.error : null} />

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : medication ? 'Save changes' : 'Add medication'}
        </button>
      </div>
    </form>
  );
}
