import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { createMood, updateMood, type MoodEntry, type MoodPayload } from '../api/mood';
import { fromLocalInputValue, toLocalInputValue } from '../lib/dates';
import { ENERGY_LABELS, MOOD_LABELS } from '../lib/format';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

const SCALE = [1, 2, 3, 4, 5];

/** Five big buttons for the mood, an optional energy level and a note. */
export function MoodForm({ entry, onDone }: { entry?: MoodEntry; onDone?: () => void }) {
  const queryClient = useQueryClient();
  const [mood, setMood] = useState<number | null>(entry?.mood ?? null);
  const [energy, setEnergy] = useState<number | null>(entry?.energy ?? null);
  const [recordedAt, setRecordedAt] = useState(toLocalInputValue(entry ? new Date(entry.recordedAt) : new Date()));
  const [note, setNote] = useState(entry?.note ?? '');

  const mutation = useMutation({
    mutationFn: (payload: MoodPayload) => (entry ? updateMood(entry.id, payload) : createMood(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mood'] });
      if (!entry) {
        setMood(null);
        setEnergy(null);
        setNote('');
        setRecordedAt(toLocalInputValue());
      }
      onDone?.();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    if (mood === null) return;
    mutation.mutate({ mood, energy, recordedAt: fromLocalInputValue(recordedAt), note: note.trim() || null });
  }

  return (
    <form className="form" onSubmit={submit}>
      <fieldset className="scale">
        <legend>How are you feeling?</legend>
        <div className="scale__options">
          {SCALE.map((n) => (
            <button
              key={n}
              type="button"
              className={`scale__option scale__option--mood-${n}`}
              aria-pressed={mood === n}
              onClick={() => setMood(n)}
            >
              <span className="scale__value">{n}</span>
              <span className="scale__label">{MOOD_LABELS[n]}</span>
            </button>
          ))}
        </div>
      </fieldset>

      <fieldset className="scale">
        <legend>Energy (optional)</legend>
        <div className="scale__options">
          {SCALE.map((n) => (
            <button
              key={n}
              type="button"
              className="scale__option"
              aria-pressed={energy === n}
              onClick={() => setEnergy(energy === n ? null : n)}
            >
              <span className="scale__value">{n}</span>
              <span className="scale__label">{ENERGY_LABELS[n]}</span>
            </button>
          ))}
        </div>
      </fieldset>

      <div className="form__row">
        <Field label="When">
          <input
            type="datetime-local"
            required
            value={recordedAt}
            max={toLocalInputValue()}
            onChange={(e) => setRecordedAt(e.target.value)}
          />
        </Field>
        <Field label="Note" hint="Optional, e.g. “bad night”">
          <input maxLength={500} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </div>

      <ErrorMessage error={mutation.error} />

      <div className="form__actions">
        {onDone && entry && (
          <button type="button" className="button button--ghost" onClick={onDone}>
            Cancel
          </button>
        )}
        <button type="submit" className="button" disabled={mood === null || mutation.isPending}>
          {mutation.isPending ? 'Saving…' : entry ? 'Save changes' : 'Check in'}
        </button>
      </div>
    </form>
  );
}
