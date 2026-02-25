import type { ReactNode } from 'react';
import type { Goal } from '../api/goals';
import { formatDate } from '../lib/dates';
import { formatNumber, GOAL_META } from '../lib/format';
import { ProgressBar } from './ProgressBar';

export function goalStatus(goal: Goal): { text: string; tone: 'success' | 'warning' | 'default' } {
  if (goal.achieved) return { text: 'Achieved', tone: 'success' };
  if (goal.currentValue === undefined || goal.currentValue === null) return { text: 'No data yet', tone: 'default' };
  if (goal.onTrack === true) return { text: 'On track', tone: 'success' };
  if (goal.onTrack === false) return { text: 'Behind schedule', tone: 'warning' };
  return { text: 'In progress', tone: 'default' };
}

export function GoalCard({ goal, children }: { goal: Goal; children?: ReactNode }) {
  const meta = GOAL_META[goal.type];
  const status = goalStatus(goal);
  const decimals = goal.type === 'WEIGHT' || goal.type === 'SLEEP_HOURS' ? 1 : 0;
  return (
    <article className={`goal${goal.archived ? ' goal--archived' : ''}`}>
      <div className="goal__header">
        <h3>{meta.describe(goal.targetValue)}</h3>
        <span className={`status status--${status.tone}`}>{status.text}</span>
      </div>
      <ProgressBar percent={goal.progressPercent} label={meta.label} tone={status.tone} />
      <div className="goal__numbers">
        <span>
          Now: <strong>{formatNumber(goal.currentValue, decimals)}</strong> {goal.unit}
          {goal.type === 'WEIGHT' && goal.startValue && (
            <span className="muted"> (started at {formatNumber(goal.startValue, 1)})</span>
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
