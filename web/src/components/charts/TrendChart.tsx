import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ReferenceArea,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import type { TrendPoint, VitalType } from '../../api/vitals';
import { formatShortDate } from '../../lib/dates';
import { yDomain } from '../../lib/charts';
import { formatNumber, VITAL_META } from '../../lib/format';

export interface Band {
  low: number;
  high: number;
}

/**
 * Daily averages for one vital type. The normal range is shaded so out-of-range days stand
 * out; for blood pressure the diastolic limit is drawn as a dashed line.
 */
export function TrendChart({
  type,
  points,
  band,
  secondaryBand,
  unit,
  decimals,
}: {
  type: VitalType;
  /** Already converted to the units the chart should show. */
  points: TrendPoint[];
  band?: Band;
  secondaryBand?: Band;
  unit?: string;
  decimals?: number;
}) {
  const meta = VITAL_META[type];
  const isBp = type === 'BLOOD_PRESSURE';
  const values = points.flatMap((p) => [p.average, p.averageSecondary ?? p.average]);
  if (band) values.push(band.low, band.high);
  const domain = yDomain(values, decimals ?? meta.decimals);

  return (
    <div
      className="chart"
      role="img"
      aria-label={
        points.length > 0
          ? `${meta.label} trend from ${formatShortDate(points[0].date)} to ${formatShortDate(points[points.length - 1].date)}`
          : `${meta.label} trend`
      }
    >
      <ResponsiveContainer width="100%" height={280}>
        <LineChart data={points} margin={{ top: 8, right: 16, bottom: 0, left: -8 }}>
          <CartesianGrid stroke="var(--border)" vertical={false} />
          <XAxis
            dataKey="date"
            tickFormatter={formatShortDate}
            stroke="var(--text-muted)"
            fontSize={12}
            minTickGap={24}
          />
          <YAxis domain={domain} stroke="var(--text-muted)" fontSize={12} allowDecimals={(decimals ?? meta.decimals) > 0} />
          {band && (
            <ReferenceArea y1={band.low} y2={band.high} fill="var(--ok)" fillOpacity={0.08} stroke="none" ifOverflow="extendDomain" />
          )}
          {secondaryBand && (
            <ReferenceLine y={secondaryBand.high} stroke="var(--chart-2)" strokeDasharray="4 4" strokeOpacity={0.6} />
          )}
          <Tooltip
            labelFormatter={(label) => formatShortDate(String(label))}
            formatter={(value, name) => [
              `${formatNumber(Number(value), decimals ?? meta.decimals)} ${unit ?? meta.unit}`,
              name,
            ]}
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 8 }}
          />
          {isBp && <Legend verticalAlign="top" height={28} iconType="plainline" />}
          <Line
            type="monotone"
            dataKey="average"
            name={isBp ? 'Systolic' : meta.label}
            stroke="var(--chart-1)"
            strokeWidth={2}
            dot={points.length < 40 ? { r: 3 } : false}
            activeDot={{ r: 5 }}
          />
          {isBp && (
            <Line
              type="monotone"
              dataKey="averageSecondary"
              name="Diastolic"
              stroke="var(--chart-2)"
              strokeWidth={2}
              dot={points.length < 40 ? { r: 3 } : false}
            />
          )}
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
