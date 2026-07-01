import { Bar, BarChart, CartesianGrid, ReferenceLine, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import type { WaterTotal } from '../../api/water';
import { formatShortDate } from '../../lib/dates';
import { formatNumber } from '../../lib/format';
import { waterToDisplay, waterUnit, type UnitPrefs } from '../../lib/units';

export function WaterChart({ days, target, prefs }: { days: WaterTotal[]; target?: number; prefs: UnitPrefs }) {
  const data = days.map((d) => ({ date: d.date, amount: waterToDisplay(d.totalMl, prefs) }));
  const unit = waterUnit(prefs);
  return (
    <div className="chart" role="img" aria-label="Water per day">
      <ResponsiveContainer width="100%" height={240}>
        <BarChart data={data} margin={{ top: 8, right: 16, bottom: 0, left: -4 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis dataKey="date" tickFormatter={formatShortDate} stroke="var(--text-muted)" fontSize={12} minTickGap={24} />
          <YAxis stroke="var(--text-muted)" fontSize={12} />
          <Tooltip
            labelFormatter={(label) => formatShortDate(String(label))}
            formatter={(value) => [`${formatNumber(Number(value))} ${unit}`, 'Water']}
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
          />
          {target && <ReferenceLine y={waterToDisplay(target, prefs)} stroke="var(--ok)" strokeDasharray="4 4" />}
          <Bar dataKey="amount" fill="var(--chart-4)" radius={[3, 3, 0, 0]} />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
