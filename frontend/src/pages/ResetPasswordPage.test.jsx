import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { resetPassword } from '../features/auth/authApi';
import ResetPasswordPage from './ResetPasswordPage';

vi.mock('../features/auth/authApi', () => ({ resetPassword: vi.fn() }));

const renderAt = (url) =>
  render(
    <MemoryRouter initialEntries={[url]}>
      <ResetPasswordPage />
    </MemoryRouter>,
  );

async function fill(password, confirmation) {
  await userEvent.type(screen.getByLabelText('New password'), password);
  await userEvent.type(screen.getByLabelText('Confirm new password'), confirmation);
  await userEvent.click(screen.getByRole('button', { name: 'Save new password' }));
}

describe('ResetPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('sends the token and new password, then offers to log in', async () => {
    resetPassword.mockResolvedValue();
    renderAt('/reset-password?token=abc');

    await fill('NewPassw0rd', 'NewPassw0rd');

    expect(resetPassword).toHaveBeenCalledWith({ token: 'abc', newPassword: 'NewPassw0rd' });
    expect(await screen.findByRole('heading', { name: 'Password updated' })).toBeInTheDocument();
  });

  it('does not call the server when the two passwords differ', async () => {
    renderAt('/reset-password?token=abc');

    await fill('NewPassw0rd', 'Different1');

    expect(await screen.findByText('Passwords do not match')).toBeInTheDocument();
    expect(resetPassword).not.toHaveBeenCalled();
  });

  it('shows the server error and a way to request a new link', async () => {
    resetPassword.mockRejectedValue(new Error('This link is invalid or has expired.'));
    renderAt('/reset-password?token=old');

    await fill('NewPassw0rd', 'NewPassw0rd');

    expect(await screen.findByRole('alert')).toHaveTextContent('invalid or has expired');
    expect(screen.getByRole('link', { name: 'Request a new one' })).toHaveAttribute('href', '/forgot-password');
  });

  it('explains a link without a token', () => {
    renderAt('/reset-password');

    expect(screen.getByRole('alert')).toHaveTextContent('incomplete');
  });
});