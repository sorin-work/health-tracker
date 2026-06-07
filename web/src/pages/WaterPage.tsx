import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { listGoals } from '../api/goals';
import { waterDaily } from '../api/water';
import { WaterChart } from '../components/charts/WaterChart';
import { DateRangePicker } from '../components/DateRangePicker';
import { ErrorMessage } from '../components/ErrorMessage';
import { PageHeader } from '../components/PageHeader';
import { DEFAULT_WATER_TARGET_ML, WaterCard } from '../components/WaterCard';
import { presetRange, type DateRange } from '../lib/dates';
import { formatNumber } from '../lib/format';

export function WaterPage() {
  const [range, setRange] = useState<DateRange>(presetRange('30d'));
  const goals = useQuery({ queryKey: ['goals', false], queryFn: () => listGoals(false) });
  const daily = useQuery({
    queryKey: ['water', 'daily', range.from, range.to],
    queryFn: () => waterDaily(range.from, range.to),
  });

  const target = goals.data?.find((g) => g.type === 'DAILY_WATER')?.targetValue ?? DEFAULT_WATER_TARGET_ML;
  const days = daily.data ?? [];
  const logged = days.filter((d) => d.totalMl > 0);
  const average = logged.length > 0 ? logged.reduce((s, d) => s + d.totalMl, 0) / logged.length : undefined;

  return (
    <>
      <PageHeader
        title="Water"
        description="Log what you drink through the day. Set a water goal to change the daily target."
      />
      <div className="grid grid--2">
        <section className="card">
          <h2>Today</h2>
          <WaterCard targetMl={target} />
        </section>
        <section className="card">
          <h2>In this period</h2>
          <dl className="stats-row">
            <div>
              <dt>Average on days logged</dt>
              <dd>{average === undefined ? '–' : `${formatNumber(average)} ml`}</dd>
            </div>
            <div>
              <dt>Days at target</dt>
              <dd>
                {days.filter((d) => d.totalMl >= target).length} / {days.length}
              </dd>
            </div>
          </dl>
        </section>
      </div>
      <section className="card">
        <div className="card__header card__header--wrap">
          <h2>Per day</h2>
          <DateRangePicker value={range} onChange={setRange} />
        </div>
        <ErrorMessage error={daily.error} />
        {days.length > 0 && <WaterChart days={days} target={target} />}
      </section>
    </>
  );
}
