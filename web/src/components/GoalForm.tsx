import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { createGoal, updateGoal, type Goal, type GoalPayload, type GoalType } from '../api/goals';
import { useUnitPrefs } from '../auth/AuthContext';
import { addDays, today } from '../lib/dates';
import { GOAL_META } from '../lib/format';
import { goalFromDisplay, goalToDisplay, goalUnit, vitalUnit, type UnitPrefs } from '../lib/units';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

const DEFAULT_TARGETS: Record<GoalType, string> = {
  WEIGHT: '',
  DAILY_STEPS: '8000',
  WEEKLY_ACTIVE_MINUTES: '150',
  WEEKLY_WORKOUTS: '3',
  SLEEP_HOURS: '7.5',
  DAILY_WATER: '2000',
};

/** A stored value as the user would type it, rounded so 80 kg shows as 176.4 lb, not 176.369809. */
function shown(type: GoalType, value: number, prefs: UnitPrefs): string {
  return String(Number(goalToDisplay(type, value, prefs).toFixed(1)));
}

export function GoalForm({ goal, onDone }: { goal?: Goal; onDone: () => void }) {
  const queryClient = useQueryClient();
  const prefs = useUnitPrefs();
  const [type, setType] = useState<GoalType>(goal?.type ?? 'WEIGHT');
  const [target, setTarget] = useState(goal ? shown(goal.type, goal.targetValue, prefs) : DEFAULT_TARGETS.WEIGHT);
  const [start, setStart] = useState(goal?.startValue ? shown('WEIGHT', goal.startValue, prefs) : '');
  const [targetDate, setTargetDate] = useState(goal?.targetDate ?? '');
  const [notes, setNotes] = useState(goal?.notes ?? '');

  const mutation = useMutation({
    mutationFn: (payload: GoalPayload) => (goal ? updateGoal(goal.id, payload) : createGoal(payload)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['goals'] });
      onDone();
    },
  });

  function changeType(next: GoalType) {
    setType(next);
    const preset = DEFAULT_TARGETS[next];
    setTarget(preset ? shown(next, Number(preset), prefs) : '');
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({
      type,
      targetValue: goalFromDisplay(type, Number(target), prefs),
      startValue: type === 'WEIGHT' && start ? goalFromDisplay('WEIGHT', Number(start), prefs) : null,
      targetDate: targetDate || null,
      notes: notes.trim() || null,
    });
  }

  const meta = GOAL_META[type];
  return (
    <form className="form" onSubmit={submit}>
      <Field label="Goal">
        <select value={type} onChange={(e) => changeType(e.target.value as GoalType)} disabled={!!goal}>
          {Object.entries(GOAL_META).map(([key, m]) => (
            <option key={key} value={key}>
              {m.label}
            </option>
          ))}
        </select>
      </Field>
      <div className="form__row">
        <Field label={`Target (${goalUnit(type, meta.unit, prefs)})`}>
          <input type="number" required min={0} step="any" value={target} onChange={(e) => setTarget(e.target.value)} />
        </Field>
        {type === 'WEIGHT' && (
          <Field label={`Starting weight (${vitalUnit('WEIGHT', prefs)})`} hint="Defaults to your latest weight">
            <input type="number" min={0} step="0.1" value={start} onChange={(e) => setStart(e.target.value)} />
          </Field>
        )}
      </div>
      {type === 'WEIGHT' && (
        <Field label="Target date" hint="Optional; used to tell whether you are on track">
          <input type="date" min={addDays(today(), 1)} value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
        </Field>
      )}
      <Field label="Notes">
        <textarea rows={2} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </Field>

      <ErrorMessage error={mutation.error} />

      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : goal ? 'Save changes' : 'Create goal'}
        </button>
      </div>
    </form>
  );
}
