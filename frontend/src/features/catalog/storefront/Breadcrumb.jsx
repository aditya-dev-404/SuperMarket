import { FiChevronRight } from 'react-icons/fi';
import { Link } from 'react-router-dom';

/** items: [{ label, to? }] — the last item is the current page (no link). */
export default function Breadcrumb({ items }) {
  return (
    <nav aria-label="Breadcrumb" className="text-sm text-gray-500">
      <ol className="flex flex-wrap items-center gap-1">
        {items.map((item, index) => {
          const isLast = index === items.length - 1;
          return (
            <li key={`${item.label}-${index}`} className="flex items-center gap-1">
              {item.to && !isLast ? (
                <Link to={item.to} className="hover:text-gray-900 hover:underline">
                  {item.label}
                </Link>
              ) : (
                <span className={isLast ? 'text-gray-900' : ''} aria-current={isLast ? 'page' : undefined}>
                  {item.label}
                </span>
              )}
              {!isLast && <FiChevronRight className="h-3 w-3" aria-hidden="true" />}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}