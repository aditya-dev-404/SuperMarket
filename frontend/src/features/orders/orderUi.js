export const PAYMENT_METHODS = {
  ONLINE: { label: 'Pay online', hint: 'Pay now. Free shipping.' },
  COD: { label: 'Cash on delivery', hint: 'Pay when it arrives. A shipping fee applies.' },
};

const STATUS_LABELS = {
  PENDING_PAYMENT: 'Awaiting payment',
  PLACED: 'Placed',
  PROCESSING: 'Processing',
  SHIPPED: 'Shipped',
  DELIVERED: 'Delivered',
  CANCELLED: 'Cancelled',
};

const STATUS_TONES = {
  PENDING_PAYMENT: 'bg-amber-100 text-amber-800',
  PLACED: 'bg-blue-100 text-blue-800',
  PROCESSING: 'bg-indigo-100 text-indigo-800',
  SHIPPED: 'bg-purple-100 text-purple-800',
  DELIVERED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-200 text-gray-700',
};

const PAYMENT_LABELS = {
  PENDING_PAYMENT: 'Payment pending',
  PAID: 'Paid',
  PAYMENT_FAILED: 'Payment failed',
  COD: 'Pay on delivery',
  REFUND_PENDING: 'Refund pending',
  CANCELLED: 'Cancelled',
};

/** Statuses a vendor can filter their list by (PENDING_PAYMENT is never shown to vendors). */
export const VENDOR_STATUS_FILTERS = ['PLACED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED'];

export function statusLabel(status) {
  return STATUS_LABELS[status] ?? status;
}

export function statusTone(status) {
  return STATUS_TONES[status] ?? 'bg-gray-100 text-gray-700';
}

export function paymentStatusLabel(status) {
  return PAYMENT_LABELS[status] ?? status;
}

/** The verb on the vendor's button that moves a sub-order to `nextStatus`. */
export function advanceActionLabel(nextStatus) {
  return nextStatus ? `Mark as ${statusLabel(nextStatus).toLowerCase()}` : null;
}

export function formatDateTime(isoString) {
  if (!isoString) return '';
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(
    new Date(isoString),
  );
}