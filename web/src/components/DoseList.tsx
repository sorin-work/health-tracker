import { useMutation, useQueryClient } from '@tanstack/react-query';
import { recordDose, undoDose, type Dose, type DoseStatus } from '../api/medications';
import { formatTime } from '../lib/dates';
import { ErrorMessage } from './ErrorMessage';

const STATUS_LABELS: Record<DoseStatus, string> = {
  TAKEN: 'Taken',
  SKIPPED: 'Skipped',
  MISSED: 'Missed',
  PENDING: 'Due',
};

/** A day's doses with buttons to mark each one taken or skipped (or undo that). */
export function DoseList({ doses, now = new Date() }: { doses: Dose[]; now?: Date }) {
  const queryClient = useQueryClient();
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['doses'] });
    // Taking (or undoing) a dose changes what is left of a tracked supply.
    queryClient.invalidateQueries({ queryKey: ['medications'] });
  };
  const record = useMutation({
    mutationFn: ({ dose, status }: { dose: Dose; status: 'TAKEN' | 'SKIPPED' }) =>
      recordDose(dose.medicationId, dose.scheduledAt, status),
    onSuccess: invalidate,
  });
  const undo = useMutation({ mutationFn: (logId: number) => undoDose(logId), onSuccess: invalidate });
  const busy = record.isPending || undo.isPending;

  return (
    <>
      <ErrorMessage error={record.error ?? undo.error} />
      <ul className="dose-list">
        {doses.map((dose) => {
          const overdue = dose.status === 'PENDING' && new Date(dose.scheduledAt) < now;
          const statusLabel = overdue ? 'Overdue' : STATUS_LABELS[dose.status];
          return (
            <li key={`${dose.medicationId}-${dose.scheduledAt}`} className={`dose dose--${dose.status.toLowerCase()}`}>
              <span className="dose__time">{dose.asNeeded ? 'As needed' : formatTime(dose.scheduledAt)}</span>
              <span className="dose__name">
                <strong>{dose.medicationName}</strong>
                {dose.dosage && <span className="muted"> · {dose.dosage}</span>}
              </span>
              <span className={`status status--${overdue ? 'overdue' : dose.status.toLowerCase()}`}>{statusLabel}</span>
              <span className="dose__actions">
                {dose.status === 'PENDING' || dose.status === 'MISSED' ? (
                  <>
                    <button
                      className="button button--small"
                      disabled={busy}
                      onClick={() => record.mutate({ dose, status: 'TAKEN' })}
                      aria-label={`Mark ${dose.medicationName} at ${formatTime(dose.scheduledAt)} as taken`}
                    >
                      Take
                    </button>
                    <button
                      className="button button--ghost button--small"
                      disabled={busy}
                      onClick={() => record.mutate({ dose, status: 'SKIPPED' })}
                      aria-label={`Skip ${dose.medicationName} at ${formatTime(dose.scheduledAt)}`}
                    >
                      Skip
                    </button>
                  </>
                ) : (
                  dose.logId !== undefined && (
                    <button
                      className="button button--ghost button--small"
                      disabled={busy}
                      onClick={() => undo.mutate(dose.logId!)}
                      aria-label={`Undo ${dose.medicationName} at ${formatTime(dose.scheduledAt)}`}
                    >
                      Undo
                    </button>
                  )
                )}
              </span>
            </li>
          );
        })}
      </ul>
    </>
  );
}
