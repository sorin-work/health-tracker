import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { JournalEntry } from '../api/journal';
import * as journalApi from '../api/journal';
import { renderWithProviders } from '../test/utils';
import { JournalPage } from './JournalPage';

vi.mock('../api/journal', () => ({
  listJournal: vi.fn(),
  createJournalEntry: vi.fn(),
  updateJournalEntry: vi.fn(),
  deleteJournalEntry: vi.fn(),
}));

const note: JournalEntry = {
  id: 1,
  entryDate: '2026-07-01',
  title: 'Appointment',
  body: 'Ask about the dizziness\nand the new dose',
  createdAt: '2026-07-01T18:00:00Z',
  updatedAt: '2026-07-01T18:00:00Z',
};

describe('JournalPage', () => {
  beforeEach(() => {
    vi.mocked(journalApi.listJournal)
      .mockReset()
      .mockImplementation(async ({ q }) =>
        q === 'nothing' ? { items: [], page: 0, size: 20, total: 0 } : { items: [note], page: 0, size: 20, total: 1 },
      );
    vi.mocked(journalApi.createJournalEntry).mockReset().mockResolvedValue(note);
  });

  it('lists notes and searches them', async () => {
    renderWithProviders(<JournalPage />);

    expect(await screen.findByText(/Ask about the dizziness/)).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Search notes'), 'nothing');

    expect(await screen.findByText('Nothing matches “nothing”')).toBeInTheDocument();
    expect(vi.mocked(journalApi.listJournal).mock.lastCall?.[0]).toMatchObject({ q: 'nothing', page: 0 });
  });

  it('writes a new note', async () => {
    renderWithProviders(<JournalPage />);

    await userEvent.click(screen.getByRole('button', { name: /New note/ }));
    const save = screen.getByRole('button', { name: 'Save note' });
    expect(save).toBeDisabled();
    await userEvent.type(screen.getByLabelText('Note'), 'Felt much better after the walk');
    await userEvent.click(save);

    await waitFor(() => expect(journalApi.createJournalEntry).toHaveBeenCalled());
    expect(vi.mocked(journalApi.createJournalEntry).mock.calls[0][0]).toMatchObject({
      title: null,
      body: 'Felt much better after the walk',
    });
  });
});
