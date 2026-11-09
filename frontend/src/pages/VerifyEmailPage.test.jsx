import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { verifyEmail } from '../features/auth/authApi';
import VerifyEmailPage from './VerifyEmailPage';

vi.mock('../features/auth/authApi', () => ({ verifyEmail: vi.fn() }));

const renderAt = (url) =>
  render(
    <MemoryRouter initialEntries={[url]}>
      <VerifyEmailPage />
    </MemoryRouter>,
  );

describe('VerifyEmailPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('verifies the token from the link and offers to log in', async () => {
    verifyEmail.mockResolvedValue();
    renderAt('/verify-email?token=abc');

    expect(await screen.findByRole('heading', { name: 'Email verified' })).toBeInTheDocument();
    expect(verifyEmail).toHaveBeenCalledTimes(1);
    expect(verifyEmail).toHaveBeenCalledWith('abc');
  });

  it('shows the server message when the link is invalid or expired', async () => {
    verifyEmail.mockRejectedValue(new Error('This link is invalid or has expired.'));
    renderAt('/verify-email?token=old');

    expect(await screen.findByRole('alert')).toHaveTextContent('This link is invalid or has expired.');
  });

  it('does not call the server when the link has no token', () => {
    renderAt('/verify-email');

    expect(screen.getByRole('alert')).toHaveTextContent('incomplete');
    expect(verifyEmail).not.toHaveBeenCalled();
  });
});