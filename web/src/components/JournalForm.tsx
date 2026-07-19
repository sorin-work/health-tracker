import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { createJournalEntry, updateJournalEntry, type JournalEntry, type JournalPayload } from '../api/journal';
import { today } from '../lib/dates';
import { ErrorMessage } from './ErrorMessage';
import { Field } from './Field';

const MAX_BODY = 5000;

export function JournalForm({ entry, onDone }: { entry?: JournalEntry; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [entryDate, setEntryDate] = useState(entry?.entryDate ?? today());
  const [title, setTitle] = useState(entry?.title ?? '');
  const [body, setBody] = useState(entry?.body ?? '');

  const mutation = useMutation({
    mutationFn: (payload: JournalPayload) =>
      entry ? updateJournalEntry(entry.id, payload) : createJournalEntry(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['journal'] });
      onDone();
    },
  });
  const fields = mutation.error instanceof ApiError ? mutation.error.fields : {};

  function submit(e: FormEvent) {
    e.preventDefault();
    mutation.mutate({ entryDate, title: title.trim() || null, body: body.trim() });
  }

  return (
    <form className="form" onSubmit={submit}>
      <div className="form__row">
        <Field label="Day" error={fields.entryDate}>
          <input type="date" required value={entryDate} max={today()} onChange={(e) => setEntryDate(e.target.value)} />
        </Field>
        <Field label="Title" hint="Optional">
          <input maxLength={120} value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
      </div>
      <Field label="Note" error={fields.body} hint={`${body.length} / ${MAX_BODY}`}>
        <textarea
          required
          rows={8}
          maxLength={MAX_BODY}
          value={body}
          placeholder="How did the day go? Anything to bring up at the next appointment?"
          onChange={(e) => setBody(e.target.value)}
        />
      </Field>
      <ErrorMessage error={mutation.error && Object.keys(fields).length === 0 ? mutation.error : null} />
      <div className="form__actions">
        <button type="button" className="button button--ghost" onClick={onDone}>
          Cancel
        </button>
        <button type="submit" className="button" disabled={!body.trim() || mutation.isPending}>
          {mutation.isPending ? 'Saving…' : entry ? 'Save changes' : 'Save note'}
        </button>
      </div>
    </form>
  );
}
