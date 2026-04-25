import type { Goal } from '../api/goals';

/** The badge shown on a goal card. */
export function goalStatus(goal: Goal): { text: string; tone: 'success' | 'warning' | 'default' } {
  if (goal.achieved) return { text: 'Achieved', tone: 'success' };
  if (goal.currentValue === undefined || goal.currentValue === null) return { text: 'No data yet', tone: 'default' };
  if (goal.onTrack === true) return { text: 'On track', tone: 'success' };
  if (goal.onTrack === false) return { text: 'Behind schedule', tone: 'warning' };
  return { text: 'In progress', tone: 'default' };
}
