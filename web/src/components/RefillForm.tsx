import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { refillMedication, type Medication } from '../api/medications';
import { formatNumber } from '../lib/format';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

/** Adds a new pack to a medication's supply. */
export function RefillForm({ medication, onDone }: { medication: Medication; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [quantity, setQuantity] = useState('30');
  const mutation = useMutation({
    mutationFn: () => refillMedication(medication.id, Number(quantity)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['medications'] });
      onDone();
    },
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate();
  }

  const after = (medication.supply ?? 0) + (Number(quantity) || 0);
  return (
    <form className="form" onSubmit={submit}>
      <Field label="Units in the new pack" hint={`You will have ${formatNumber(after, after % 1 ? 1 : 0)} afterwards`}>
        <input type="number" required min={1} step="any" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
      </Field>
      <ErrorMessage error={mutation.error} />
      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={mutation.isPending}>
          {mutation.isPending ? 'Saving…' : 'Add to supply'}
        </button>
      </div>
    </form>
  );
}
