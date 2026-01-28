import { Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { WeeklyActivity } from '../../api/activities';
import { formatShortDate } from '../../lib/dates';
import { formatMinutes } from '../../lib/format';

/** Active minutes per week, with an optional target line (e.g. 150 minutes). */
export function WeeklyActivityChart({ weeks, target }: { weeks: WeeklyActivity[]; target?: number }) {
  return (
    <div className="chart" aria-label="Active minutes per week">
      <ResponsiveContainer width="100%" height={240}>
        <BarChart data={weeks} margin={{ top: 8, right: 16, bottom: 0, left: -12 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis dataKey="weekStart" tickFormatter={formatShortDate} stroke="var(--text-muted)" fontSize={12} />
          <YAxis stroke="var(--text-muted)" fontSize={12} />
          <Tooltip
            labelFormatter={(label) => `Week of ${formatShortDate(String(label))}`}
            formatter={(value, _name, item) => {
              const w = item.payload as WeeklyActivity;
              return [`${formatMinutes(Number(value))} · ${w.sessions} sessions`, 'Active'];
            }}
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
          />
          {target && <ReferenceLine y={target} stroke="var(--ok)" strokeDasharray="4 4" label={{ value: 'Goal', fill: 'var(--text-muted)', fontSize: 12, position: 'insideTopRight' }} />}
          <Bar dataKey="activeMinutes" fill="var(--chart-1)" radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
