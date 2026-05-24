import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { MoodEntry } from '../api/mood';
import * as moodApi from '../api/mood';
import { renderWithProviders } from '../test/utils';
import { MoodForm } from './MoodForm';

vi.mock('../api/mood', () => ({ createMood: vi.fn(), updateMood: vi.fn() }));

describe('MoodForm', () => {
  beforeEach(() => {
    vi.mocked(moodApi.createMood).mockReset().mockResolvedValue({} as MoodEntry);
    vi.mocked(moodApi.updateMood).mockReset().mockResolvedValue({} as MoodEntry);
  });

  it('needs a mood before it can be saved', async () => {
    renderWithProviders(<MoodForm />);

    expect(screen.getByRole('button', { name: 'Check in' })).toBeDisabled();
    await userEvent.click(screen.getByRole('button', { name: /Good$/ }));
    expect(screen.getByRole('button', { name: 'Check in' })).toBeEnabled();
  });

  it('sends the mood, energy and note', async () => {
    renderWithProviders(<MoodForm />);

    await userEvent.click(screen.getByRole('button', { name: /Very low/ }));
    await userEvent.click(screen.getByRole('button', { name: /Tired/ }));
    await userEvent.type(screen.getByLabelText('Note'), 'Migraine all morning');
    await userEvent.click(screen.getByRole('button', { name: 'Check in' }));

    await waitFor(() => expect(moodApi.createMood).toHaveBeenCalled());
    expect(vi.mocked(moodApi.createMood).mock.calls[0][0]).toMatchObject({
      mood: 1,
      energy: 2,
      note: 'Migraine all morning',
    });
  });

  it('clicking the selected energy again clears it', async () => {
    renderWithProviders(<MoodForm />);
    const tired = screen.getByRole('button', { name: /Tired/ });

    await userEvent.click(tired);
    expect(tired).toHaveAttribute('aria-pressed', 'true');
    await userEvent.click(tired);
    expect(tired).toHaveAttribute('aria-pressed', 'false');
  });

  it('updates an existing entry', async () => {
    const entry: MoodEntry = { id: 4, mood: 3, recordedAt: '2026-05-20T08:00:00Z' };
    renderWithProviders(<MoodForm entry={entry} onDone={vi.fn()} />);

    await userEvent.click(screen.getByRole('button', { name: /Very good/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }));

    await waitFor(() => expect(moodApi.updateMood).toHaveBeenCalled());
    expect(vi.mocked(moodApi.updateMood).mock.calls[0][0]).toBe(4);
    expect(vi.mocked(moodApi.updateMood).mock.calls[0][1]).toMatchObject({ mood: 5 });
  });
});
