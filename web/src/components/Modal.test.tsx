import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { Modal } from './Modal';

describe('Modal', () => {
  it('focuses the first field when it opens', () => {
    render(
      <Modal title="Add reading" onClose={vi.fn()}>
        <input aria-label="First" />
        <input aria-label="Second" />
      </Modal>,
    );

    expect(screen.getByLabelText('First')).toHaveFocus();
  });

  it('falls back to the close button when there is nothing to fill in', () => {
    render(
      <Modal title="Details" onClose={vi.fn()}>
        <p>Nothing to edit</p>
      </Modal>,
    );

    expect(screen.getByRole('button', { name: 'Close' })).toHaveFocus();
  });

  it('keeps focus where it is when the parent re-renders', async () => {
    const dialog = (label: string) => (
      <Modal title="Add reading" onClose={() => undefined}>
        <input aria-label="First" />
        <input aria-label="Second" />
        <span>{label}</span>
      </Modal>
    );
    const { rerender } = render(dialog('a'));
    await userEvent.click(screen.getByLabelText('Second'));

    rerender(dialog('b'));

    expect(screen.getByLabelText('Second')).toHaveFocus();
  });

  it('closes on escape with the latest handler', async () => {
    const first = vi.fn();
    const latest = vi.fn();
    const { rerender } = render(<Modal title="Edit" onClose={first}>x</Modal>);
    rerender(<Modal title="Edit" onClose={latest}>x</Modal>);

    await userEvent.keyboard('{Escape}');

    expect(latest).toHaveBeenCalledTimes(1);
    expect(first).not.toHaveBeenCalled();
  });
});
