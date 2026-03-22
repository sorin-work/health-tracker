import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { FlagBadge } from './FlagBadge';

describe('FlagBadge', () => {
  it('shows a readable label with a tone', () => {
    render(<FlagBadge flag="CRITICAL_HIGH" />);
    const badge = screen.getByText('Very high');
    expect(badge).toHaveClass('badge--critical');
  });

  it('uses the warning tone for plain high/low', () => {
    render(<FlagBadge flag="LOW" />);
    expect(screen.getByText('Low')).toHaveClass('badge--warn');
  });

  it('renders nothing for readings that are not assessed', () => {
    const { container } = render(<FlagBadge flag={undefined} />);
    expect(container).toBeEmptyDOMElement();
  });
});
