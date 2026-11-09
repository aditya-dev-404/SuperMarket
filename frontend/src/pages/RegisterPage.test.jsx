import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { resendVerification } from '../features/auth/authApi';
import { AuthContext } from '../features/auth/AuthContext';
import RegisterPage from './RegisterPage';

vi.mock('../features/auth/authApi', () => ({ resendVerification: vi.fn() }));

function renderRegister(register) {
  return render(
    <AuthContext.Provider value={{ status: 'anonymous', user: null, register }}>
      <MemoryRouter>
        <RegisterPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

async function fillAndSubmit() {
  await userEvent.type(screen.getByLabelText('First name'), 'Jane');
  await userEvent.type(screen.getByLabelText('Last name'), 'Doe');
  await userEvent.type(screen.getByLabelText('Email'), 'jane@example.com');
  await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd1');
  await userEvent.click(screen.getByRole('button', { name: 'Create account' }));
}

describe('RegisterPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('asks the user to check their inbox instead of logging them in', async () => {
    const register = vi.fn().mockResolvedValue({ message: 'ok' });
    renderRegister(register);

    await fillAndSubmit();

    expect(register).toHaveBeenCalledWith({
      firstName: 'Jane',
      lastName: 'Doe',
      email: 'jane@example.com',
      password: 'Passw0rd1',
    });
    expect(await screen.findByRole('heading', { name: 'Check your inbox' })).toBeInTheDocument();
    expect(screen.getByText('jane@example.com')).toBeInTheDocument();
  });

  it('can send the verification email again', async () => {
    resendVerification.mockResolvedValue({ message: 'New link sent.' });
    renderRegister(vi.fn().mockResolvedValue({ message: 'ok' }));

    await fillAndSubmit();
    await userEvent.click(await screen.findByRole('button', { name: 'Resend verification email' }));

    expect(resendVerification).toHaveBeenCalledWith('jane@example.com');
    expect(await screen.findByText('New link sent.')).toBeInTheDocument();
  });

  it('shows field errors returned by the server', async () => {
    const error = Object.assign(new Error('Validation failed'), {
      fieldErrors: { password: 'Password must be 8-72 characters and contain a letter and a digit' },
    });
    renderRegister(vi.fn().mockRejectedValue(error));

    await fillAndSubmit();

    expect(await screen.findByText(/Password must be 8-72 characters/)).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Check your inbox' })).not.toBeInTheDocument();
  });
});