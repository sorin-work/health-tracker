import type { Flag } from '../api/vitals';
import { FLAG_LABELS } from '../lib/format';

/** Coloured pill for a reading's range flag. Renders nothing for unassessed readings. */
export function FlagBadge({ flag }: { flag?: Flag | null }) {
  if (!flag) return null;
  const tone = flag === 'NORMAL' ? 'ok' : flag.startsWith('CRITICAL') ? 'critical' : 'warn';
  return <span className={`badge badge--${tone}`}>{FLAG_LABELS[flag]}</span>;
}
