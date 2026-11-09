import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { forgotPassword } from '../features/auth/authApi';
import ForgotPasswordPage from './ForgotPasswordPage';

vi.mock('../features/auth/authApi', () => ({ forgotPassword: vi.fn() }));

describe('ForgotPasswordPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the same neutral message the server returns', async () => {
    forgotPassword.mockResolvedValue({ message: 'If an account exists, we sent a link.' });
    render(
      <MemoryRouter>
        <ForgotPasswordPage />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText('Email'), 'jane@example.com');
    await userEvent.click(screen.getByRole('button', { name: 'Send reset link' }));

    expect(forgotPassword).toHaveBeenCalledWith('jane@example.com');
    expect(await screen.findByText('If an account exists, we sent a link.')).toBeInTheDocument();
  });
});