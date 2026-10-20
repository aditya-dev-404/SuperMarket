import { useEffect, useState } from 'react';
import { FiEdit2, FiPlus } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import { listMyProducts } from '../features/catalog/catalogApi';
import { errorMessage, statusBadge, ui } from '../features/catalog/catalogUi';

export default function VendorProductsPage() {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let ignore = false;
    listMyProducts({ page, size: 10, ...(status && { status }) })
      .then((data) => {
        if (!ignore) {
          setResult(data);
          setError('');
        }
      })
      .catch((err) => !ignore && setError(errorMessage(err)))
      .finally(() => !ignore && setLoading(false));
    return () => {
      ignore = true;
    };
  }, [page, status]);

  const products = result?.content ?? [];

  return (
    <div className="mx-auto max-w-5xl space-y-6 px-4 py-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold text-gray-900">My products</h1>
        <div className="flex items-center gap-3">
          <select
            className={`${ui.input} w-40`}
            value={status}
            onChange={(e) => {
              setLoading(true);
              setPage(0);
              setStatus(e.target.value);
            }}
          >
            <option value="">All statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="ACTIVE">Active</option>
            <option value="ARCHIVED">Archived</option>
          </select>
          <Link to="/vendor/products/new" className={ui.btn}>
            <FiPlus /> New product
          </Link>
        </div>
      </div>

      {error && <div className={ui.error}>{error}</div>}

      <div className={ui.card}>
        {loading ? (
          <p className="text-sm text-gray-500">Loading...</p>
        ) : products.length === 0 ? (
          <p className="text-sm text-gray-500">No products found.</p>
        ) : (
          <ul className="divide-y">
            {products.map((p) => (
              <li key={p.id} className="flex items-center gap-4 py-3">
                {p.primaryImageUrl ? (
                  <img src={p.primaryImageUrl} alt="" className="h-14 w-14 rounded object-cover" />
                ) : (
                  <div className="flex h-14 w-14 items-center justify-center rounded bg-gray-100 text-xs text-gray-400">
                    No image
                  </div>
                )}
                <div className="min-w-0 flex-1">
                  <p className="truncate font-medium text-gray-900">{p.name}</p>
                  <p className="text-sm text-gray-500">
                    {p.categoryName} · {p.variantCount} variant{p.variantCount === 1 ? '' : 's'} · ₹{p.basePrice}
                  </p>
                </div>
                <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusBadge[p.status]}`}>
                  {p.status}
                </span>
                <Link to={`/vendor/products/${p.id}/edit`} className={ui.iconBtn} aria-label="Edit product">
                  <FiEdit2 />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>

      {result && result.totalPages > 1 && (
        <div className="flex items-center justify-center gap-4 text-sm">
          <button className={ui.btnGhost} disabled={result.first} onClick={() => setPage((p) => p - 1)}>
            Previous
          </button>
          <span className="text-gray-600">
            Page {result.number + 1} of {result.totalPages}
          </span>
          <button className={ui.btnGhost} disabled={result.last} onClick={() => setPage((p) => p + 1)}>
            Next
          </button>
        </div>
      )}
    </div>
  );
}