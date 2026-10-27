import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import HomePage from './HomePage';
import * as systemApi from '../api/systemApi';

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('HomePage', () => {
  it('shows backend status when ping succeeds', async () => {
    vi.spyOn(systemApi, 'fetchPing').mockResolvedValue({
      status: 'UP',
      application: 'supermarket-backend',
    });
    renderPage();
    expect(await screen.findByText('UP')).toBeInTheDocument();
  });

  it('shows an error when the backend is unreachable', async () => {
    vi.spyOn(systemApi, 'fetchPing').mockRejectedValue(new Error('Server is unreachable'));
    renderPage();
    expect(await screen.findByRole('alert')).toHaveTextContent('Server is unreachable');
  });
});