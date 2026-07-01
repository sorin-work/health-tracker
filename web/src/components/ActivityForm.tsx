import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import {
  createActivity,
  updateActivity,
  type Activity,
  type ActivityPayload,
  type ActivityType,
} from '../api/activities';
import { useUnitPrefs } from '../auth/AuthContext';
import { fromLocalInputValue, toLocalInputValue } from '../lib/dates';
import { ACTIVITY_LABELS } from '../lib/format';
import { distanceFromDisplay, distanceToDisplay, distanceUnit } from '../lib/units';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

const DISTANCE_TYPES: ActivityType[] = ['WALK', 'RUN', 'CYCLE', 'SWIM'];

function optionalNumber(value: string): number | null {
  return value.trim() === '' ? null : Number(value);
}

export function ActivityForm({ activity, onDone }: { activity?: Activity; onDone: () => void }) {
  const queryClient = useQueryClient();
  const prefs = useUnitPrefs();
  const [type, setType] = useState<ActivityType>(activity?.type ?? 'WALK');
  const [startedAt, setStartedAt] = useState(
    toLocalInputValue(activity ? new Date(activity.startedAt) : new Date()),
  );
  const [duration, setDuration] = useState(activity ? String(activity.durationMinutes) : '30');
  const [distance, setDistance] = useState(
    activity?.distanceKm ? String(Number(distanceToDisplay(activity.distanceKm, prefs).toFixed(2))) : '',
  );
  const [steps, setSteps] = useState(activity?.steps ? String(activity.steps) : '');
  const [calories, setCalories] = useState(activity?.caloriesKcal ? String(activity.caloriesKcal) : '');
  const [notes, setNotes] = useState(activity?.notes ?? '');

  const mutation = useMutation({
    mutationFn: (payload: ActivityPayload) =>
      activity ? updateActivity(activity.id, payload) : createActivity(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] });
      queryClient.invalidateQueries({ queryKey: ['goals'] });
      onDone();
    },
  });

  const toKm = (value: number | null) => (value === null ? null : distanceFromDisplay(value, prefs));

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({
      type,
      startedAt: fromLocalInputValue(startedAt),
      durationMinutes: Number(duration),
      distanceKm: DISTANCE_TYPES.includes(type) ? toKm(optionalNumber(distance)) : null,
      steps: optionalNumber(steps),
      caloriesKcal: optionalNumber(calories),
      notes: notes.trim() || null,
    });
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form__row">
        <Field label="Activity">
          <select value={type} onChange={(e) => setType(e.target.value as ActivityType)}>
            {Object.entries(ACTIVITY_LABELS).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Started">
          <input type="datetime-local" required value={startedAt} max={toLocalInputValue()} onChange={(e) => setStartedAt(e.target.value)} />
        </Field>
      </div>
      <div className="form__row">
        <Field label="Duration (min)">
          <input type="number" required min={1} max={1440} value={duration} onChange={(e) => setDuration(e.target.value)} />
        </Field>
        {DISTANCE_TYPES.includes(type) && (
          <Field label={`Distance (${distanceUnit(prefs)})`}>
            <input type="number" min={0} step="0.01" value={distance} onChange={(e) => setDistance(e.target.value)} />
          </Field>
        )}
      </div>
      <div className="form__row">
        <Field label="Steps" hint="Counts towards the daily steps goal">
          <input type="number" min={0} value={steps} onChange={(e) => setSteps(e.target.value)} />
        </Field>
        <Field label="Calories (kcal)">
          <input type="number" min={0} value={calories} onChange={(e) => setCalories(e.target.value)} />
        </Field>
      </div>
      <Field label="Notes">
        <textarea rows={2} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Field>

      <ErrorMessage error={mutation.error} />

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : activity ? 'Save changes' : 'Log activity'}
        </button>
      </div>
    </form>
  );
}
