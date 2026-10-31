/** Tailwind-only error/notice banner (the older ErrorMessage still relies on vanilla CSS). */
const tones = {
  error: 'border-red-200 bg-red-50 text-red-700',
  info: 'border-indigo-200 bg-indigo-50 text-indigo-700',
};

export default function Alert({ message, tone = 'error', className = '' }) {
  if (!message) return null;
  return (
    <p
      role={tone === 'error' ? 'alert' : 'status'}
      className={`rounded-md border px-3 py-2 text-sm ${tones[tone]} ${className}`}
    >
      {message}
    </p>
  );
}