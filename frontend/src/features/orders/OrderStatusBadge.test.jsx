import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import OrderStatusBadge from './OrderStatusBadge';

describe('OrderStatusBadge', () => {
  it('shows the readable label', () => {
    render(<OrderStatusBadge status="SHIPPED" />);
    expect(screen.getByText('Shipped')).toBeInTheDocument();
  });
});