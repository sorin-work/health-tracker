import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { archiveGoal, deleteGoal, listGoals, restoreGoal, type Goal } from '../api/goals';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { GoalCard } from '../components/GoalCard';
import { GoalForm } from '../components/GoalForm';
import { PlusIcon } from '../components/icons';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';

export function GoalsPage() {
  const queryClient = useQueryClient();
  const [showArchived, setShowArchived] = useState(false);
  const [editing, setEditing] = useState<Goal | 'new' | null>(null);
  const goals = useQuery({ queryKey: ['goals', showArchived], queryFn: () => listGoals(showArchived) });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['goals'] });
  const archive = useMutation({ mutationFn: archiveGoal, onSuccess: invalidate });
  const restore = useMutation({ mutationFn: restoreGoal, onSuccess: invalidate });
  const remove = useMutation({ mutationFn: deleteGoal, onSuccess: invalidate });

  return (
    <>
      <PageHeader
        title="Goals"
        description="Progress updates from your logged weight, sleep and activity."
        actions={
          <button className="button" onClick={() => setEditing('new')}>
            <PlusIcon /> New goal
          </button>
        }
      />
      <label className="checkbox">
        <input type="checkbox" checked={showArchived} onChange={(e) => setShowArchived(e.target.checked)} />
        Show archived goals
      </label>
      <ErrorMessage error={goals.error ?? archive.error ?? restore.error ?? remove.error} />
      {goals.data && goals.data.length === 0 && (
        <EmptyState title="No goals yet">Set a target weight, a daily step count or a sleep goal.</EmptyState>
      )}
      <div className="grid grid--3">
        {goals.data?.map((goal) => (
          <GoalCard key={goal.id} goal={goal}>
            <div className="row-actions">
              <button className="link-button" onClick={() => setEditing(goal)}>
                Edit
              </button>
              {goal.archived ? (
                <>
                  <button className="link-button" onClick={() => restore.mutate(goal.id)}>
                    Restore
                  </button>
                  <button
                    className="link-button link-button--danger"
                    onClick={() => window.confirm('Delete this goal?') && remove.mutate(goal.id)}
                  >
                    Delete
                  </button>
                </>
              ) : (
                <button className="link-button" onClick={() => archive.mutate(goal.id)}>
                  Archive
                </button>
              )}
            </div>
          </GoalCard>
        ))}
      </div>

      {editing && (
        <Modal title={editing === 'new' ? 'New goal' : 'Edit goal'} onClose={() => setEditing(null)}>
          <GoalForm goal={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}
