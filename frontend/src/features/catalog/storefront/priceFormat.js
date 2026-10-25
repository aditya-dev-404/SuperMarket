const CURRENCY = 'INR';

const formatter = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: CURRENCY,
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

export function formatPrice(value) {
  if (value === null || value === undefined || value === '') return '';
  return formatter.format(Number(value));
}