import { render, screen } from '@testing-library/react';
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
});
