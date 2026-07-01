import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { deleteActivity, listActivities, weeklyActivity, type Activity } from '../api/activities';
import { listGoals } from '../api/goals';
import { useUnitPrefs } from '../auth/AuthContext';
import { ActivityForm } from '../components/ActivityForm';
import { WeeklyActivityChart } from '../components/charts/WeeklyActivityChart';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { PlusIcon } from '../components/icons';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { addDays, formatDateTime, today } from '../lib/dates';
import { ACTIVITY_LABELS, formatMinutes, formatNumber } from '../lib/format';
import { distanceToDisplay, distanceUnit } from '../lib/units';

const PAGE_SIZE = 20;

export function ActivityPage() {
  const prefs = useUnitPrefs();
  const queryClient = useQueryClient();
  const [weeks, setWeeks] = useState(12);
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Activity | 'new' | null>(null);

  const from = addDays(today(), -(weeks * 7 - 1));
  const weekly = useQuery({ queryKey: ['activities', 'weekly', weeks], queryFn: () => weeklyActivity(from) });
  const list = useQuery({
    queryKey: ['activities', 'list', page],
    queryFn: () => listActivities({ page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });
  const goals = useQuery({ queryKey: ['goals', false], queryFn: () => listGoals(false) });
  const remove = useMutation({
    mutationFn: deleteActivity,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activities'] });
      queryClient.invalidateQueries({ queryKey: ['goals'] });
    },
  });

  const minutesGoal = goals.data?.find((g) => g.type === 'WEEKLY_ACTIVE_MINUTES')?.targetValue;
  const thisWeek = weekly.data?.at(-1);
  const total = list.data?.total ?? 0;
  const pages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  return (
    <>
      <PageHeader
        title="Activity"
        description="Workouts, walks and steps. Adults are generally advised to get 150 active minutes a week."
        actions={
          <button className="button" onClick={() => setEditing('new')}>
            <PlusIcon /> Log activity
          </button>
        }
      />

      <section className="card">
        <div className="card__header">
          <h2>Active minutes per week</h2>
          <div className="segmented" role="group" aria-label="Weeks shown">
            {[4, 12, 26, 52].map((w) => (
              <button key={w} className={w === weeks ? 'active' : undefined} aria-pressed={w === weeks} onClick={() => setWeeks(w)}>
                {w} weeks
              </button>
            ))}
          </div>
        </div>
        <ErrorMessage error={weekly.error} />
        {weekly.data && <WeeklyActivityChart weeks={weekly.data} target={minutesGoal} />}
        {thisWeek && (
          <dl className="stats-row">
            <div>
              <dt>This week</dt>
              <dd>{formatMinutes(thisWeek.activeMinutes)}</dd>
            </div>
            <div>
              <dt>Sessions</dt>
              <dd>{thisWeek.sessions}</dd>
            </div>
            <div>
              <dt>Distance</dt>
              <dd>
                {formatNumber(distanceToDisplay(thisWeek.distanceKm, prefs), 1)} {distanceUnit(prefs)}
              </dd>
            </div>
            <div>
              <dt>Steps</dt>
              <dd>{formatNumber(thisWeek.steps)}</dd>
            </div>
          </dl>
        )}
      </section>

      <section className="card">
        <div className="card__header">
          <h2>History</h2>
        </div>
        <ErrorMessage error={list.error ?? remove.error} />
        {list.data && list.data.items.length === 0 ? (
          <EmptyState title="No activities yet">Log a walk or a workout to get started.</EmptyState>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Activity</th>
                  <th className="num">Duration</th>
                  <th className="num">Distance</th>
                  <th className="num">Steps</th>
                  <th className="num">kcal</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {list.data?.items.map((a) => (
                  <tr key={a.id}>
                    <td>{formatDateTime(a.startedAt)}</td>
                    <td>
                      {ACTIVITY_LABELS[a.type]}
                      {a.notes && <div className="muted small">{a.notes}</div>}
                    </td>
                    <td className="num">{formatMinutes(a.durationMinutes)}</td>
                    <td className="num">
                      {a.distanceKm
                        ? `${formatNumber(distanceToDisplay(a.distanceKm, prefs), 1)} ${distanceUnit(prefs)}`
                        : '–'}
                    </td>
                    <td className="num">{a.steps ? formatNumber(a.steps) : '–'}</td>
                    <td className="num">{a.caloriesKcal ?? '–'}</td>
                    <td className="row-actions">
                      <button className="link-button" onClick={() => setEditing(a)}>
                        Edit
                      </button>
                      <button
                        className="link-button link-button--danger"
                        onClick={() => window.confirm('Delete this activity?') && remove.mutate(a.id)}
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {pages > 1 && (
          <div className="pager">
            <button className="button button--ghost button--small" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span className="muted">
              Page {page + 1} of {pages}
            </span>
            <button className="button button--ghost button--small" disabled={page + 1 >= pages} onClick={() => setPage(page + 1)}>
              Next
            </button>
          </div>
        )}
      </section>

      {editing && (
        <Modal title={editing === 'new' ? 'Log activity' : 'Edit activity'} onClose={() => setEditing(null)}>
          <ActivityForm activity={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}
