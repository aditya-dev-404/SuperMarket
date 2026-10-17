import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../features/auth/AuthContext';
import LoginPage from './LoginPage';

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

describe('LoginPage', () => {
  it('submits the credentials and goes home on success', async () => {
    const login = vi.fn().mockResolvedValue({ firstName: 'Jane' });
    renderLogin(login);

    await userEvent.type(screen.getByLabelText('Email'), 'jane@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'Passw0rd1');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(login).toHaveBeenCalledWith({ email: 'jane@example.com', password: 'Passw0rd1' });
    expect(await screen.findByText('home page')).toBeInTheDocument();
  });

  it('shows the error message when login fails', async () => {
    const login = vi.fn().mockRejectedValue(new Error('Invalid email or password'));
    renderLogin(login);

    await userEvent.type(screen.getByLabelText('Email'), 'jane@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
  });
});
