import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { listGoals } from '../api/goals';
import { moodDaily, type MoodDay } from '../api/mood';
import { adherence, dosesForDay, lowSupplyMedications } from '../api/medications';
import { listVitals, vitalSummary, weeklyChange, type VitalSummary, type WeeklyChange } from '../api/vitals';
import { useUnitPrefs, useUser } from '../auth/AuthContext';
import { DoseList } from '../components/DoseList';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { FlagBadge } from '../components/FlagBadge';
import { GoalCard } from '../components/GoalCard';
import { AlertIcon } from '../components/icons';
import { ProgressBar } from '../components/ProgressBar';
import { RemindersCard } from '../components/RemindersCard';
import { DEFAULT_WATER_TARGET_ML, WaterCard } from '../components/WaterCard';
import { vitalUnit } from '../lib/units';
import { addDays, formatDateTime, formatShortDate, formatWeekday, presetRange, today } from '../lib/dates';
import {
  adherenceTone,
  describeSupply,
  describeWeeklyChange,
  formatNumber,
  formatReading,
  formatVitalValue,
  moodLabel,
  VITAL_META,
} from '../lib/format';

function greeting(date = new Date()): string {
  const h = date.getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

export function DashboardPage() {
  const user = useUser();
  const prefs = useUnitPrefs();
  const last30 = presetRange('30d');
  const summary = useQuery({ queryKey: ['vitals', 'summary'], queryFn: vitalSummary });
  const changes = useQuery({ queryKey: ['vitals', 'weekly-change'], queryFn: weeklyChange });
  const doses = useQuery({ queryKey: ['doses', today()], queryFn: () => dosesForDay(today()) });
  const stats = useQuery({
    queryKey: ['doses', 'adherence', last30.from, last30.to],
    queryFn: () => adherence(last30.from, last30.to),
  });
  const goals = useQuery({ queryKey: ['goals', false], queryFn: () => listGoals(false) });
  const flagged = useQuery({
    queryKey: ['vitals', 'list', 'flagged-recent', last30.from],
    queryFn: () => listVitals({ from: last30.from, to: last30.to, flagged: true, size: 5 }),
  });

  const lowSupply = useQuery({ queryKey: ['medications', 'low-supply'], queryFn: lowSupplyMedications });
  const last7 = presetRange('7d');
  const mood = useQuery({
    queryKey: ['mood', 'daily', last7.from, last7.to],
    queryFn: () => moodDaily(last7.from, last7.to),
  });

  const percent = stats.data?.overall.percent;
  const waterTarget = goals.data?.find((g) => g.type === 'DAILY_WATER')?.targetValue ?? DEFAULT_WATER_TARGET_ML;

  return (
    <>
      <div className="page-header">
        <div>
          <h1>
            {greeting()}, {user.displayName.split(' ')[0]}
          </h1>
          <p className="muted">Here is how the last 30 days look.</p>
        </div>
      </div>

      {lowSupply.data && lowSupply.data.length > 0 && (
        <div className="alert alert--warning" role="status">
          <AlertIcon />
          <div>
            <strong>Running low:</strong>{' '}
            {lowSupply.data.map((m, i) => (
              <span key={m.id}>
                {i > 0 && ', '}
                {m.name} ({describeSupply(m)})
              </span>
            ))}
            . <Link to="/medications">Log a refill</Link>
          </div>
        </div>
      )}

      <ErrorMessage error={summary.error} />
      <div className="vital-cards">
        {summary.data?.map((s) => (
          <VitalCard key={s.type} summary={s} change={changes.data?.find((c) => c.type === s.type)} />
        ))}
      </div>

      <div className="grid grid--2">
        <section className="card">
          <div className="card__header">
            <h2>Today’s medications</h2>
            <Link to="/medications" className="link">
              All medications
            </Link>
          </div>
          <ErrorMessage error={doses.error} />
          {doses.data && doses.data.length === 0 && <EmptyState title="Nothing scheduled today" />}
          {doses.data && doses.data.length > 0 && <DoseList doses={doses.data} />}
          {percent !== undefined && percent !== null && (
            <div className="adherence-inline">
              <span>30-day adherence</span>
              <ProgressBar percent={percent} label="30-day adherence" tone={adherenceTone(percent)} />
              <strong>{percent}%</strong>
            </div>
          )}
        </section>

        <section className="card">
          <div className="card__header">
            <h2>
              <AlertIcon className="text-warn" /> Out-of-range readings
            </h2>
            <Link to="/vitals?flagged=true" className="link">
              See all
            </Link>
          </div>
          <ErrorMessage error={flagged.error} />
          {flagged.data && flagged.data.items.length === 0 && (
            <EmptyState title="Nothing flagged in the last 30 days" />
          )}
          <ul className="flagged-list">
            {flagged.data?.items.map((r) => (
              <li key={r.id}>
                <span>
                  <strong>{VITAL_META[r.type].label}</strong> {formatReading(r, prefs)}
                  <div className="muted small">{formatDateTime(r.measuredAt)}</div>
                </span>
                <FlagBadge flag={r.flag} />
              </li>
            ))}
          </ul>
          {flagged.data && flagged.data.total > flagged.data.items.length && (
            <p className="muted small">{flagged.data.total} readings flagged in total.</p>
          )}
        </section>
      </div>

      <div className="grid grid--3">
        <section className="card">
          <div className="card__header">
            <h2>Coming up</h2>
          </div>
          {doses.data && goals.data && lowSupply.data && (
            <RemindersCard doses={doses.data} lowSupply={lowSupply.data} goals={goals.data} />
          )}
        </section>

        <section className="card">
          <div className="card__header">
            <h2>Water today</h2>
            <Link to="/water" className="link">
              History
            </Link>
          </div>
          <WaterCard targetMl={waterTarget} />
        </section>

        <section className="card">
          <div className="card__header">
            <h2>Mood this week</h2>
            <Link to="/mood" className="link">
              Check in
            </Link>
          </div>
          <ErrorMessage error={mood.error} />
          {mood.data && <MoodWeek days={mood.data} from={last7.from} />}
        </section>
      </div>

      <section className="card">
        <div className="card__header">
          <h2>Goals</h2>
          <Link to="/goals" className="link">
            Manage goals
          </Link>
        </div>
        {goals.data && goals.data.length === 0 && (
          <EmptyState title="No goals yet">
            <Link to="/goals">Set one</Link> to track your progress here.
          </EmptyState>
        )}
        <div className="grid grid--3">
          {goals.data?.map((g) => <GoalCard key={g.id} goal={g} />)}
        </div>
      </section>
    </>
  );
}

function VitalCard({ summary, change }: { summary: VitalSummary; change?: WeeklyChange }) {
  const prefs = useUnitPrefs();
  const meta = VITAL_META[summary.type];
  const weekly = change ? describeWeeklyChange(change, prefs) : null;
  const latest = summary.latest;
  return (
    <Link to={`/vitals?type=${summary.type}`} className="vital-card">
      <div className="vital-card__label">{meta.label}</div>
      {latest ? (
        <>
          <div className="vital-card__value">
            {formatVitalValue(latest.type, latest.value, latest.secondaryValue, prefs)}
            <span className="vital-card__unit">{vitalUnit(summary.type, prefs)}</span>
          </div>
          <div className="vital-card__meta">
            <FlagBadge flag={latest.flag} />
            <span className="muted small">{formatDateTime(latest.measuredAt)}</span>
          </div>
          {summary.average30d !== undefined && (
            <div className="muted small">
              30-day avg {formatVitalValue(summary.type, summary.average30d, summary.averageSecondary30d, prefs)}
              {summary.flagged30d > 0 && ` · ${summary.flagged30d} flagged`}
            </div>
          )}
          {weekly && (
            <div className={`trend-change trend-change--${weekly.direction} small`} title={weekly.label}>
              <span aria-hidden>{weekly.text}</span>
              <span className="sr-only">{weekly.label}</span>
              <span className="muted"> vs last week</span>
            </div>
          )}
        </>
      ) : (
        <div className="muted">No readings yet</div>
      )}
    </Link>
  );
}

function MoodWeek({ days, from }: { days: MoodDay[]; from: string }) {
  if (days.length === 0) {
    return <EmptyState title="No check-ins this week">A quick check-in takes a few seconds.</EmptyState>;
  }
  const average = days.reduce((sum, d) => sum + d.mood * d.count, 0) / days.reduce((n, d) => n + d.count, 0);
  const byDate = new Map(days.map((d) => [d.date, d]));
  const week = Array.from({ length: 7 }, (_, i) => addDays(from, i));
  return (
    <>
      <p>
        <strong className="stat-large">{formatNumber(average, 1)}</strong>{' '}
        <span className="muted">/ 5 on average · {moodLabel(average)}</span>
      </p>
      <ol className="mood-week" aria-label="Mood per day">
        {week.map((date) => {
          const day = byDate.get(date);
          const score = day ? Math.round(day.mood) : null;
          return (
            <li key={date} title={day ? `${formatShortDate(date)}: ${moodLabel(day.mood)}` : formatShortDate(date)}>
              <span className={`mood-dot${score ? ` mood-dot--${score}` : ' mood-dot--empty'}`}>{score ?? '·'}</span>
              <span className="muted small">{formatWeekday(date)}</span>
            </li>
          );
        })}
      </ol>
    </>
  );
}
