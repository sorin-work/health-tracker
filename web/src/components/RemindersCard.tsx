import type { Goal } from '../api/goals';
import type { Dose, Medication } from '../api/medications';
import { useUnitPrefs } from '../auth/AuthContext';
import { formatShortDate, formatTime, today } from '../lib/dates';
import { buildReminders, type Reminder } from '../lib/reminders';
import { EmptyState } from './EmptyState';
import { PillIcon, TargetIcon } from './icons';

function when(r: Reminder): string {
  if (r.kind === 'dose') return formatTime(r.due);
  return r.due === today() ? 'Today' : formatShortDate(r.due);
}

/** Today's remaining doses, refills that are due and goals with a close target date. */
export function RemindersCard({
  doses,
  lowSupply,
  goals,
  now = new Date(),
}: {
  doses: Dose[];
  lowSupply: Medication[];
  goals: Goal[];
  now?: Date;
}) {
  const prefs = useUnitPrefs();
  const reminders = buildReminders(doses, lowSupply, goals, now, prefs);
  if (reminders.length === 0) return <EmptyState title="Nothing else today">You are all caught up.</EmptyState>;
  return (
    <ul className="reminders">
      {reminders.map((r) => (
        <li key={r.key} className={r.overdue ? 'reminders__item reminders__item--overdue' : 'reminders__item'}>
          <span className="reminders__icon" aria-hidden>
            {r.kind === 'goal' ? <TargetIcon /> : <PillIcon />}
          </span>
          <span>
            <strong>{r.title}</strong>
            {r.detail && <span className="muted"> · {r.detail}</span>}
          </span>
          <span className={r.overdue ? 'status status--overdue' : 'muted small'}>{r.overdue ? 'Overdue' : when(r)}</span>
        </li>
      ))}
    </ul>
  );
}
