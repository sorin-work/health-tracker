import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import { renderWithProviders } from '../test/utils';
import { RegisterPage } from './RegisterPage';

const register = vi.fn();

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ register }),
}));

function renderRegister() {
  return renderWithProviders(
    <Routes>
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<p>Dashboard</p>} />
    </Routes>,
    { route: '/register' },
  );
}

async function fillIn() {
  await userEvent.type(screen.getByLabelText('Your name'), 'Sam Lee');
  await userEvent.type(screen.getByLabelText('Email'), 'sam@example.com');
  await userEvent.type(screen.getByLabelText('Password'), 'long-enough');
  await userEvent.click(screen.getByRole('button', { name: /Create account/ }));
}

describe('RegisterPage', () => {
  beforeEach(() => {
    register.mockReset();
  });

  it('creates the account with the browser time zone and opens the dashboard', async () => {
    register.mockResolvedValue({ id: 1 });
    renderRegister();

    await fillIn();

    expect(register).toHaveBeenCalledWith(
      expect.objectContaining({ email: 'sam@example.com', displayName: 'Sam Lee', password: 'long-enough' }),
    );
    expect(register.mock.calls[0][0].timezone).toBeTruthy();
    expect(await screen.findByText('Dashboard')).toBeInTheDocument();
  });

  it('shows field errors next to the field', async () => {
    register.mockRejectedValue(
      new ApiError(400, 'Some fields are invalid', 'validation_failed', { email: 'must be a well-formed email address' }),
    );
    renderRegister();

    await fillIn();

    expect(await screen.findByText('must be a well-formed email address')).toBeInTheDocument();
    expect(screen.queryByText('Some fields are invalid')).not.toBeInTheDocument();
  });

  it('shows other errors at the top', async () => {
    register.mockRejectedValue(new ApiError(409, 'An account with this email already exists', 'conflict'));
    renderRegister();

    await fillIn();

    expect(await screen.findByRole('alert')).toHaveTextContent('An account with this email already exists');
  });
});
