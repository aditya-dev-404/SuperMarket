import { describe, expect, it } from 'vitest';
import { advanceActionLabel, paymentStatusLabel, statusLabel } from './orderUi';

describe('orderUi', () => {
  it('labels statuses and falls back to the raw value', () => {
    expect(statusLabel('PENDING_PAYMENT')).toBe('Awaiting payment');
    expect(statusLabel('SOMETHING_NEW')).toBe('SOMETHING_NEW');
    expect(paymentStatusLabel('REFUND_PENDING')).toBe('Refund pending');
  });

  it('builds the vendor button label from the next status', () => {
    expect(advanceActionLabel('PROCESSING')).toBe('Mark as processing');
    expect(advanceActionLabel(null)).toBeNull();
  });
});