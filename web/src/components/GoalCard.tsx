import type { ReactNode } from 'react';
import type { Goal } from '../api/goals';
import { useUnitPrefs } from '../auth/AuthContext';
import { formatDate } from '../lib/dates';
import { describeGoal, formatNumber, GOAL_META } from '../lib/format';
import { goalStatus } from '../lib/goals';
import { goalToDisplay, goalUnit } from '../lib/units';
import { ProgressBar } from './ProgressBar';

export function GoalCard({ goal, children }: { goal: Goal; children?: ReactNode }) {
  const prefs = useUnitPrefs();
  const meta = GOAL_META[goal.type];
  const status = goalStatus(goal);
  const shown = (value: number | undefined) =>
    value === undefined || value === null ? undefined : goalToDisplay(goal.type, value, prefs);
  return (
    <article className={`goal${goal.archived ? ' goal--archived' : ''}`}>
      <div className="goal__header">
        <h3>{describeGoal(goal.type, goal.targetValue, prefs)}</h3>
        <span className={`status status--${status.tone}`}>{status.text}</span>
      </div>
      <ProgressBar percent={goal.progressPercent} label={meta.label} tone={status.tone} />
      <div className="goal__numbers">
        <span>
          Now: <strong>{formatNumber(shown(goal.currentValue), meta.decimals)}</strong>{' '}
          {goalUnit(goal.type, goal.unit, prefs)}
          {goal.type === 'WEIGHT' && goal.startValue && (
            <span className="muted"> (started at {formatNumber(shown(goal.startValue), 1)})</span>
          )}
        </span>
        <span className="muted">{Math.round(goal.progressPercent)}%</span>
      </div>
      <div className="muted small">
        {goal.type === 'WEIGHT' ? `Since ${formatDate(goal.startDate)}` : 'Last 7 days'}
        {goal.targetDate && ` · by ${formatDate(goal.targetDate)}`}
      </div>
      {goal.notes && <p className="small">{goal.notes}</p>}
      {children}
    </article>
  );
}
