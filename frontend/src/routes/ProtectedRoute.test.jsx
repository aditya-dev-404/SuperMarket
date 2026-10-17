import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { AuthContext } from '../features/auth/AuthContext';
import ProtectedRoute from './ProtectedRoute';

function renderAt(authValue, roles) {
  return render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={['/secret']}>
        <Routes>
          <Route element={<ProtectedRoute roles={roles} />}>
            <Route path="/secret" element={<p>secret content</p>} />
          </Route>
          <Route path="/login" element={<p>login page</p>} />
          <Route path="/forbidden" element={<p>forbidden page</p>} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('ProtectedRoute', () => {
  it('shows a loader while the session is being restored', () => {
    renderAt({ status: 'loading', user: null });
    expect(screen.getByRole('status')).toBeInTheDocument();
  });

  it('redirects anonymous visitors to the login page', () => {
    renderAt({ status: 'anonymous', user: null });
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('lets signed-in users through', () => {
    renderAt({ status: 'authenticated', user: { role: 'CUSTOMER' } });
    expect(screen.getByText('secret content')).toBeInTheDocument();
  });

  it('redirects users without the required role', () => {
    renderAt({ status: 'authenticated', user: { role: 'CUSTOMER' } }, ['ADMIN']);
    expect(screen.getByText('forbidden page')).toBeInTheDocument();
  });

  it('allows users who have the required role', () => {
    renderAt({ status: 'authenticated', user: { role: 'ADMIN' } }, ['ADMIN']);
    expect(screen.getByText('secret content')).toBeInTheDocument();
  });
});
