import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { renderWithProviders } from '../test/utils';
import { LoginPage } from './LoginPage';

const login = vi.fn();

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ login }),
}));

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/" element={<p>Dashboard</p>} />
    </Routes>,
    { route: '/login' },
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    login.mockReset();
  });

  it('signs in and goes to the dashboard', async () => {
    login.mockResolvedValue({ id: 1 });
    renderLogin();

    await userEvent.type(screen.getByLabelText('Email'), 'demo@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'demo-password');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(login).toHaveBeenCalledWith('demo@example.com', 'demo-password');
    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
  });

  it('shows the error from the server', async () => {
    login.mockRejectedValue(new ApiError(401, 'Email or password is incorrect', 'invalid_credentials'));
    renderLogin();

    await userEvent.type(screen.getByLabelText('Email'), 'demo@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Email or password is incorrect');
    expect(screen.getByRole('button', { name: 'Sign in' })).toBeEnabled();
  });

  it('includes the medical disclaimer', () => {
    renderLogin();
    expect(screen.getByText(/not medical advice/)).toBeInTheDocument();
  });
});
