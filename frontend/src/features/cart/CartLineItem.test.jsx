import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import CartLineItem from './CartLineItem';

const baseItem = {
  id: 7,
  productName: 'Blue Tee',
  productSlug: 'blue-tee',
  size: 'M',
  colour: 'Blue',
  imageUrl: null,
  unitPrice: 500,
  quantity: 2,
  lineTotal: 1000,
  availableStock: 5,
  status: 'AVAILABLE',
};

function renderItem(overrides = {}, handlers = {}) {
  const props = {
    item: { ...baseItem, ...overrides },
    busy: false,
    onQuantityChange: vi.fn(),
    onRemove: vi.fn(),
    ...handlers,
  };
  render(
    <MemoryRouter>
      <ul>
        <CartLineItem {...props} />
      </ul>
    </MemoryRouter>,
  );
  return props;
}

describe('CartLineItem', () => {
  it('asks for quantity + 1 when increasing', async () => {
    const props = renderItem();
    await userEvent.click(screen.getByRole('button', { name: 'Increase quantity' }));
    expect(props.onQuantityChange).toHaveBeenCalledWith(7, 3);
  });

  it('cannot increase beyond the available stock', () => {
    renderItem({ quantity: 5 });
    expect(screen.getByRole('button', { name: 'Increase quantity' }).disabled).toBe(true);
  });

  it('warns when stock is lower than the cart quantity but still allows reducing', async () => {
    const props = renderItem({ quantity: 4, availableStock: 2, status: 'INSUFFICIENT_STOCK' });
    expect(screen.getByText('Only 2 left. Reduce the quantity.')).toBeTruthy();
    await userEvent.click(screen.getByRole('button', { name: 'Decrease quantity' }));
    expect(props.onQuantityChange).toHaveBeenCalledWith(7, 3);
  });

  it('locks the quantity stepper for an out-of-stock line', () => {
    renderItem({ status: 'OUT_OF_STOCK', availableStock: 0 });
    expect(screen.getByRole('button', { name: 'Increase quantity' }).disabled).toBe(true);
    expect(screen.getByRole('button', { name: 'Decrease quantity' }).disabled).toBe(true);
  });

  it('calls onRemove with the line id', async () => {
    const props = renderItem();
    await userEvent.click(screen.getByRole('button', { name: 'Remove Blue Tee' }));
    expect(props.onRemove).toHaveBeenCalledWith(7);
  });
});