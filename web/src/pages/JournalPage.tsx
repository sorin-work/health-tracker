import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useDeferredValue, useState } from 'react';
import { deleteJournalEntry, listJournal, type JournalEntry } from '../api/journal';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { PlusIcon } from '../components/icons';
import { JournalForm } from '../components/JournalForm';
import { Modal } from '../components/Modal';
import { PageHeader } from '../components/PageHeader';
import { formatDate, formatWeekday } from '../lib/dates';

const PAGE_SIZE = 20;

export function JournalPage() {
  const queryClient = useQueryClient();
  const [search, setSearch] = useState('');
  const q = useDeferredValue(search.trim());
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<JournalEntry | 'new' | null>(null);

  const entries = useQuery({
    queryKey: ['journal', q, page],
    queryFn: () => listJournal({ q, page, size: PAGE_SIZE }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: deleteJournalEntry,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['journal'] }),
  });

  const items = entries.data?.items ?? [];
  const pages = Math.max(1, Math.ceil((entries.data?.total ?? 0) / PAGE_SIZE));

  return (
    <>
      <PageHeader
        title="Journal"
        description="Notes for yourself and for your next appointment."
        actions={
          <button className="button" onClick={() => setEditing('new')}>
            <PlusIcon /> New note
          </button>
        }
      />

      <section className="card">
        <div className="card__header">
          <input
            type="search"
            className="search"
            aria-label="Search notes"
            placeholder="Search notes…"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
          />
          {entries.data && <span className="muted small">{entries.data.total} notes</span>}
        </div>
        <ErrorMessage error={entries.error ?? remove.error} />
        {items.length === 0 && !entries.isPending && (
          <EmptyState title={q ? `Nothing matches “${q}”` : 'No notes yet'}>
            {!q && 'Write down how a new medication is going, or questions for your doctor.'}
          </EmptyState>
        )}
        <ul className="journal-list">
          {items.map((entry) => (
            <li key={entry.id}>
              <article>
                <header className="journal-list__header">
                  <h3>
                    <span className="muted">{formatWeekday(entry.entryDate)}</span> {formatDate(entry.entryDate)}
                    {entry.title && ` · ${entry.title}`}
                  </h3>
                  <div className="row-actions">
                    <button className="link-button" onClick={() => setEditing(entry)}>
                      Edit
                    </button>
                    <button
                      className="link-button link-button--danger"
                      onClick={() => window.confirm('Delete this note?') && remove.mutate(entry.id)}
                    >
                      Delete
                    </button>
                  </div>
                </header>
                <p className="journal-list__body">{entry.body}</p>
              </article>
            </li>
          ))}
        </ul>
        {pages > 1 && (
          <div className="pager">
            <button className="button button--ghost button--small" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Newer
            </button>
            <span className="muted">
              Page {page + 1} of {pages}
            </span>
            <button
              className="button button--ghost button--small"
              disabled={page + 1 >= pages}
              onClick={() => setPage(page + 1)}
            >
              Older
            </button>
          </div>
        )}
      </section>

      {editing && (
        <Modal title={editing === 'new' ? 'New note' : 'Edit note'} onClose={() => setEditing(null)}>
          <JournalForm entry={editing === 'new' ? undefined : editing} onDone={() => setEditing(null)} />
        </Modal>
      )}
    </>
  );
}
