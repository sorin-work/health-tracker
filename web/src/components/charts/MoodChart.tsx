import { CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { MoodDay } from '../../api/mood';
import { formatShortDate } from '../../lib/dates';
import { formatNumber, moodLabel } from '../../lib/format';

/** Daily average mood, plus energy when it was logged, on the 1-5 scale. */
export function MoodChart({ days }: { days: MoodDay[] }) {
  const hasEnergy = days.some((d) => d.energy !== undefined && d.energy !== null);
  return (
    <div className="chart" role="img" aria-label="Daily mood and energy">
      <ResponsiveContainer width="100%" height={240}>
        <LineChart data={days} margin={{ top: 8, right: 16, bottom: 0, left: -20 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis dataKey="date" tickFormatter={formatShortDate} stroke="var(--text-muted)" fontSize={12} minTickGap={24} />
          <YAxis domain={[1, 5]} ticks={[1, 2, 3, 4, 5]} stroke="var(--text-muted)" fontSize={12} />
          <Tooltip
            labelFormatter={(label) => formatShortDate(String(label))}
            formatter={(value, name) =>
              name === 'Mood'
                ? [`${formatNumber(Number(value), 1)} (${moodLabel(Number(value))})`, name]
                : [formatNumber(Number(value), 1), name]
            }
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
          />
          {hasEnergy && <Legend wrapperStyle={{ fontSize: 12 }} />}
          <Line type="monotone" dataKey="mood" name="Mood" stroke="var(--chart-1)" strokeWidth={2} dot={false} connectNulls />
          {hasEnergy && (
            <Line
              type="monotone"
              dataKey="energy"
              name="Energy"
              stroke="var(--chart-3)"
              strokeWidth={2}
              strokeDasharray="5 4"
              dot={false}
              connectNulls
            />
          )}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
