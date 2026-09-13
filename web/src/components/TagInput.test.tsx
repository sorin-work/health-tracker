import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it } from 'vitest';
import { TagInput } from './TagInput';

function Harness({ initial = [] as string[] }) {
  const [tags, setTags] = useState(initial);
  return (
    <>
      <TagInput value={tags} onChange={setTags} />
      <output>{tags.join('|')}</output>
    </>
  );
}

describe('TagInput', () => {
  it('adds tags on enter and comma, normalised', async () => {
    render(<Harness />);
    const input = screen.getByLabelText('Tags');

    await userEvent.type(input, 'Stress{Enter}after  Coffee,');

    expect(screen.getByRole('status')).toHaveTextContent('stress|after coffee');
    expect(input).toHaveValue('');
  });

  it('ignores duplicates', async () => {
    render(<Harness initial={['stress']} />);

    await userEvent.type(screen.getByLabelText('Tags'), 'STRESS{Enter}');

    expect(screen.getByRole('status')).toHaveTextContent(/^stress$/);
  });

  it('removes a tag with its button or backspace', async () => {
    render(<Harness initial={['stress', 'weather']} />);

    await userEvent.click(screen.getByRole('button', { name: 'Remove stress' }));
    expect(screen.getByRole('status')).toHaveTextContent(/^weather$/);

    await userEvent.type(screen.getByLabelText('Tags'), '{Backspace}');
    expect(screen.getByRole('status')).toBeEmptyDOMElement();
  });
});
