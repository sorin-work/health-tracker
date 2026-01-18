import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useId, useState, type FormEvent } from 'react';
import { createSymptom, symptomNames, updateSymptom, type Symptom, type SymptomPayload } from '../api/symptoms';
import { fromLocalInputValue, toLocalInputValue } from '../lib/dates';
import { severityLabel } from '../lib/format';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

export function SymptomForm({ symptom, onDone }: { symptom?: Symptom; onDone: () => void }) {
  const queryClient = useQueryClient();
  const listId = useId();
  const names = useQuery({ queryKey: ['symptoms', 'names'], queryFn: symptomNames });
  const [name, setName] = useState(symptom?.name ?? '');
  const [severity, setSeverity] = useState(symptom?.severity ?? 4);
  const [occurredAt, setOccurredAt] = useState(
    toLocalInputValue(symptom ? new Date(symptom.occurredAt) : new Date()),
  );
  const [duration, setDuration] = useState(symptom?.durationMinutes ? String(symptom.durationMinutes) : '');
  const [notes, setNotes] = useState(symptom?.notes ?? '');

  const mutation = useMutation({
    mutationFn: (payload: SymptomPayload) => (symptom ? updateSymptom(symptom.id, payload) : createSymptom(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['symptoms'] });
      onDone();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({
      name: name.trim(),
      severity,
      occurredAt: fromLocalInputValue(occurredAt),
      durationMinutes: duration ? Number(duration) : null,
      notes: notes.trim() || null,
    });
  }

  return (
    <form className="form" onSubmit={submit}>
      <Field label="Symptom">
        <input required maxLength={80} list={listId} value={name} placeholder="Headache" onChange={(e) => setName(e.target.value)} />
      </Field>
      <datalist id={listId}>
        {(names.data ?? []).map((n) => (
          <option key={n} value={n} />
        ))}
      </datalist>

      <Field label={`Severity: ${severity} / 10 (${severityLabel(severity)})`}>
        <input type="range" min={1} max={10} value={severity} onChange={(e) => setSeverity(Number(e.target.value))} />
      </Field>

      <div className="form__row">
        <Field label="When">
          <input type="datetime-local" required value={occurredAt} max={toLocalInputValue()} onChange={(e) => setOccurredAt(e.target.value)} />
        </Field>
        <Field label="Lasted (minutes)" hint="Optional">
          <input type="number" min={0} max={20160} value={duration} onChange={(e) => setDuration(e.target.value)} />
        </Field>
      </div>

      <Field label="Notes" hint="Triggers, what helped…">
        <textarea rows={2} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Field>

      <ErrorMessage error={mutation.error} />

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : symptom ? 'Save changes' : 'Log symptom'}
        </button>
      </div>
    </form>
  );
}
