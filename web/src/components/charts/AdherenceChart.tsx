import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { Adherence } from '../../api/medications';
import { formatShortDate } from '../../lib/dates';

/** Daily adherence %, coloured by how well the day went. */
export function AdherenceChart({ daily }: { daily: Adherence['daily'] }) {
  const data = daily
    .filter((d) => d.stats.percent !== undefined && d.stats.percent !== null)
    .map((d) => ({ date: d.date, percent: d.stats.percent ?? 0, taken: d.stats.taken, due: d.stats.taken + d.stats.skipped + d.stats.missed }));

  return (
    <div className="chart" role="img" aria-label="Daily adherence">
      <ResponsiveContainer width="100%" height={220}>
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -16 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis dataKey="date" tickFormatter={formatShortDate} stroke="var(--text-muted)" fontSize={12} minTickGap={24} />
          <YAxis domain={[0, 100]} unit="%" stroke="var(--text-muted)" fontSize={12} />
          <Tooltip
            labelFormatter={(label) => formatShortDate(String(label))}
            formatter={(_value, _name, item) => {
              const p = item.payload as (typeof data)[number];
              return [`${p.percent}% (${p.taken} of ${p.due})`, 'Taken'];
            }}
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
          />
          <Bar dataKey="percent" radius={[3, 3, 0, 0]}>
            {data.map((d) => (
              <Cell
                key={d.date}
                fill={d.percent >= 90 ? 'var(--ok)' : d.percent >= 60 ? 'var(--warn)' : 'var(--critical)'}
              />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
