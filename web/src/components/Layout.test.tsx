import { screen } from '@testing-library/react';
import { Route, Routes } from 'react-router';
import { describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../test/utils';
import { Layout } from './Layout';

vi.mock('../auth/AuthContext', () => ({
  useUser: () => ({ id: 1, email: 'sam@example.com', displayName: 'Sam', timezone: 'UTC', createdAt: '' }),
  useAuth: () => ({ logout: vi.fn() }),
}));

function renderLayout(route = '/vitals') {
  return renderWithProviders(
    <Routes>
      <Route element={<Layout />}>
        <Route path="/vitals" element={<h1>Vitals</h1>} />
      </Route>
    </Routes>,
    { route },
  );
}

describe('Layout', () => {
  it('starts with a link that skips the navigation', () => {
    renderLayout();

    const skip = screen.getByRole('link', { name: 'Skip to content' });
    expect(skip).toHaveAttribute('href', '#main');
    expect(document.getElementById('main')).toContainElement(screen.getByRole('heading', { name: 'Vitals' }));
  });

  it('marks the current page in the navigation', () => {
    renderLayout();

    expect(screen.getByRole('link', { name: 'Vitals' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
  });
});
