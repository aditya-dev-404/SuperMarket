import { FiChevronLeft, FiChevronRight } from 'react-icons/fi';

/** 1 … 4 5 [6] 7 8 … 20  → always first, last, current and its neighbours. */
function pageWindow(current, total) {
  const wanted = [...new Set([1, total, current - 1, current, current + 1])]
    .filter((p) => p >= 1 && p <= total)
    .sort((a, b) => a - b);

  const items = [];
  wanted.forEach((p, i) => {
    if (i > 0 && p - wanted[i - 1] > 1) items.push(`gap-${p}`);
    items.push(p);
  });
  return items;
}

const base =
  'inline-flex h-9 min-w-9 items-center justify-center rounded-md border px-3 text-sm font-medium';

/** `page` is one-based here. */
export default function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;

  return (
    <nav className="mt-8 flex items-center justify-center gap-1" aria-label="Pagination">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page <= 1}
        aria-label="Previous page"
        className={`${base} border-gray-300 text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40`}
      >
        <FiChevronLeft aria-hidden="true" />
      </button>

      {pageWindow(page, totalPages).map((item) =>
        typeof item === 'string' ? (
          <span key={item} className="px-1 text-gray-400" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={item}
            type="button"
            onClick={() => onChange(item)}
            aria-current={item === page ? 'page' : undefined}
            className={`${base} ${
              item === page
                ? 'border-gray-900 bg-gray-900 text-white'
                : 'border-gray-300 text-gray-700 hover:bg-gray-50'
            }`}
          >
            {item}
          </button>
        ),
      )}

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages}
        aria-label="Next page"
        className={`${base} border-gray-300 text-gray-700 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40`}
      >
        <FiChevronRight aria-hidden="true" />
      </button>
    </nav>
  );
}