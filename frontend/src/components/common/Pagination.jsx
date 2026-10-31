/** Previous / next controls for a zero-based PagedResponse. */
export default function Pagination({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null;
  const buttonClass =
    'rounded-md border border-gray-300 px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40';
  return (
    <nav aria-label="Pagination" className="mt-6 flex items-center justify-center gap-4">
      <button type="button" className={buttonClass} disabled={page <= 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="text-sm text-gray-600">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className={buttonClass}
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next
      </button>
    </nav>
  );
}