import { CartesianGrid, ResponsiveContainer, Scatter, ScatterChart, Tooltip, XAxis, YAxis, ZAxis } from 'recharts';
import type { Symptom } from '../../api/symptoms';
import { formatDateTime } from '../../lib/dates';
import { OTHER, symptomSeriesNames } from '../../lib/symptoms';

/** One colour per series, as many as SERIES_LIMIT in lib/symptoms. */
const COLORS = ['var(--chart-1)', 'var(--chart-2)', 'var(--chart-3)', 'var(--chart-4)', 'var(--chart-5)'];

/** Each entry as a dot: time on x, severity on y, one colour per symptom. */
export function SymptomChart({ symptoms }: { symptoms: Symptom[] }) {
  const names = symptomSeriesNames(symptoms);
  const own = new Set(names.filter((n) => n !== OTHER));
  const seriesOf = (name: string) => (own.has(name) ? name : OTHER);
  const series = names.map((name) => ({
    name,
    data: symptoms
      .filter((s) => seriesOf(s.name) === name)
      .map((s) => ({ t: new Date(s.occurredAt).getTime(), severity: s.severity, at: s.occurredAt })),
  }));

  return (
    <div className="chart" role="img" aria-label="Symptom severity over time">
      <ResponsiveContainer width="100%" height={240}>
        <ScatterChart margin={{ top: 8, right: 16, bottom: 0, left: -16 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis
            dataKey="t"
            type="number"
            domain={['dataMin', 'dataMax']}
            tickFormatter={(t: number) => new Date(t).toLocaleDateString(undefined, { day: 'numeric', month: 'short' })}
            stroke="var(--text-muted)"
            fontSize={12}
          />
          <YAxis dataKey="severity" domain={[0, 10]} ticks={[0, 2, 4, 6, 8, 10]} stroke="var(--text-muted)" fontSize={12} />
          <ZAxis range={[50, 50]} />
          <Tooltip
            cursor={false}
            formatter={(value, name) => (name === 't' ? [formatDateTime(new Date(Number(value)).toISOString()), 'When'] : [value, 'Severity'])}
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
          />
          {series.map((s, i) => (
            <Scatter key={s.name} name={s.name} data={s.data} fill={COLORS[i]} />
          ))}
        </ScatterChart>
      </ResponsiveContainer>
      <ul className="legend">
        {names.map((name, i) => (
          <li key={name}>
            <span className="legend__swatch" style={{ background: COLORS[i] }} />
            {name}
          </li>
        ))}
      </ul>
    </div>
  );
}
