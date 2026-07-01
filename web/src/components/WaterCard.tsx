import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { addWater, deleteWater, waterForDay } from '../api/water';
import { useUnitPrefs } from '../auth/AuthContext';
import { formatTime } from '../lib/dates';
import { formatWater } from '../lib/format';
import { waterFromDisplay, waterUnit, type UnitPrefs } from '../lib/units';
import { ErrorMessage } from './ErrorMessage';
import { ProgressBar } from './ProgressBar';

/** Quick-add buttons in the unit the user sees: a glass and a bottle. */
const QUICK_AMOUNTS: Record<UnitPrefs['unitSystem'], number[]> = { METRIC: [250, 500], IMPERIAL: [8, 16] };
export const DEFAULT_WATER_TARGET_ML = 2000;


/** Today's water with quick-add buttons. The target comes from a water goal when there is one. */
export function WaterCard({ targetMl = DEFAULT_WATER_TARGET_ML }: { targetMl?: number }) {
  const queryClient = useQueryClient();
  const prefs = useUnitPrefs();
  const unit = waterUnit(prefs);
  const [custom, setCustom] = useState('');
  const today = useQuery({ queryKey: ['water', 'today'], queryFn: () => waterForDay() });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['water'] });
    queryClient.invalidateQueries({ queryKey: ['goals'] });
  };
  const add = useMutation({ mutationFn: (ml: number) => addWater(ml), onSuccess: invalidate });
  const remove = useMutation({ mutationFn: deleteWater, onSuccess: invalidate });

  const total = today.data?.totalMl ?? 0;
  const percent = Math.min(100, Math.round((total / targetMl) * 100));
  const last = today.data?.entries.at(-1);

  function addCustom(e: FormEvent) {
    e.preventDefault();
    const ml = waterFromDisplay(Number(custom), prefs);
    if (ml > 0) {
      add.mutate(ml, { onSuccess: () => setCustom('') });
    }
  }

  return (
    <div className="water">
      <div className="water__total">
        <strong>{formatWater(total, prefs)}</strong>
        <span className="muted"> of {formatWater(targetMl, prefs)}</span>
      </div>
      <ProgressBar percent={percent} label="Water today" tone={total >= targetMl ? 'success' : 'default'} />
      <div className="water__actions">
        {QUICK_AMOUNTS[prefs.unitSystem].map((amount) => (
          <button
            key={amount}
            type="button"
            className="button button--ghost button--small"
            disabled={add.isPending}
            onClick={() => add.mutate(waterFromDisplay(amount, prefs))}
          >
            + {amount} {unit}
          </button>
        ))}
        <form className="water__custom" onSubmit={addCustom}>
          <input
            type="number"
            min={1}
            max={5000}
            inputMode="numeric"
            aria-label={`Other amount in ${unit}`}
            placeholder={unit}
            value={custom}
            onChange={(e) => setCustom(e.target.value)}
          />
          <button type="submit" className="button button--small" disabled={!custom || add.isPending}>
            Add
          </button>
        </form>
      </div>
      {last && (
        <p className="muted small">
          Last: {formatWater(last.amountMl, prefs)} at {formatTime(last.loggedAt)} ·{' '}
          <button className="link-button" disabled={remove.isPending} onClick={() => remove.mutate(last.id)}>
            Undo
          </button>
        </p>
      )}
      <ErrorMessage error={today.error ?? add.error ?? remove.error} />
    </div>
  );
}
