import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { resendVerification } from '../features/auth/authApi';
import { AuthContext } from '../features/auth/AuthContext';
import LoginPage from './LoginPage';

vi.mock('../features/auth/authApi', () => ({ resendVerification: vi.fn() }));

function renderLogin(login) {
  return render(
    <AuthContext.Provider value={{ status: 'anonymous', user: null, login }}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<p>home page</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

async function fillAndSubmit(email = 'jane@example.com', password = 'Passw0rd1') {
  await userEvent.type(screen.getByLabelText('Email'), email);
  await userEvent.type(screen.getByLabelText('Password'), password);
  await userEvent.click(screen.getByRole('button', { name: 'Log in' }));
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('submits the credentials and goes home on success', async () => {
    const login = vi.fn().mockResolvedValue({ firstName: 'Jane' });
    renderLogin(login);

    await fillAndSubmit();

    expect(login).toHaveBeenCalledWith({ email: 'jane@example.com', password: 'Passw0rd1' });
    expect(await screen.findByText('home page')).toBeInTheDocument();
  });

  it('shows the error message when login fails', async () => {
    const login = vi.fn().mockRejectedValue(new Error('Invalid email or password'));
    renderLogin(login);

    await fillAndSubmit('jane@example.com', 'wrong');

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
    expect(screen.queryByRole('button', { name: 'Resend verification email' })).not.toBeInTheDocument();
  });

  it('offers to resend the verification email when the account is not verified', async () => {
    const notVerified = Object.assign(new Error('Please verify your email address'), {
      code: 'EMAIL_NOT_VERIFIED',
    });
    resendVerification.mockResolvedValue({ message: 'We have sent a new link.' });
    renderLogin(vi.fn().mockRejectedValue(notVerified));

    await fillAndSubmit();
    await userEvent.click(await screen.findByRole('button', { name: 'Resend verification email' }));

    expect(resendVerification).toHaveBeenCalledWith('jane@example.com');
    expect(await screen.findByText('We have sent a new link.')).toBeInTheDocument();
  });

  it('links to the forgot-password page', () => {
    renderLogin(vi.fn());

    expect(screen.getByRole('link', { name: 'Forgot password?' })).toHaveAttribute('href', '/forgot-password');
  });
});