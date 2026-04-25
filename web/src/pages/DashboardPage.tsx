import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router';
import { listGoals } from '../api/goals';
import { adherence, dosesForDay } from '../api/medications';
import { listVitals, vitalSummary, type VitalSummary } from '../api/vitals';
import { useUser } from '../auth/AuthContext';
import { DoseList } from '../components/DoseList';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { FlagBadge } from '../components/FlagBadge';
import { GoalCard } from '../components/GoalCard';
import { AlertIcon } from '../components/icons';
import { ProgressBar } from '../components/ProgressBar';
import { formatDateTime, presetRange, today } from '../lib/dates';
import { adherenceTone, formatReading, formatVitalValue, VITAL_META } from '../lib/format';

function greeting(date = new Date()): string {
  const h = date.getHours();
  if (h < 12) return 'Good morning';
  if (h < 18) return 'Good afternoon';
  return 'Good evening';
}

export function DashboardPage() {
  const user = useUser();
  const last30 = presetRange('30d');
  const summary = useQuery({ queryKey: ['vitals', 'summary'], queryFn: vitalSummary });
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

  const percent = stats.data?.overall.percent;

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

      <ErrorMessage error={summary.error} />
      <div className="vital-cards">
        {summary.data?.map((s) => <VitalCard key={s.type} summary={s} />)}
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
                  <strong>{VITAL_META[r.type].label}</strong> {formatReading(r)}
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

function VitalCard({ summary }: { summary: VitalSummary }) {
  const meta = VITAL_META[summary.type];
  const latest = summary.latest;
  return (
    <Link to={`/vitals?type=${summary.type}`} className="vital-card">
      <div className="vital-card__label">{meta.label}</div>
      {latest ? (
        <>
          <div className="vital-card__value">
            {formatVitalValue(latest.type, latest.value, latest.secondaryValue)}
            <span className="vital-card__unit">{meta.unit}</span>
          </div>
          <div className="vital-card__meta">
            <FlagBadge flag={latest.flag} />
            <span className="muted small">{formatDateTime(latest.measuredAt)}</span>
          </div>
          {summary.average30d !== undefined && (
            <div className="muted small">
              30-day avg {formatVitalValue(summary.type, summary.average30d, summary.averageSecondary30d)}
              {summary.flagged30d > 0 && ` · ${summary.flagged30d} flagged`}
            </div>
          )}
        </>
      ) : (
        <div className="muted">No readings yet</div>
      )}
    </Link>
  );
}
