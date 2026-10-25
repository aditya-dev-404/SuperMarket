import { useEffect, useMemo, useState } from 'react';
import { FiFilter } from 'react-icons/fi';
import { useSearchParams } from 'react-router-dom';
import Pagination from '../features/catalog/storefront/Pagination';
import ProductCard from '../features/catalog/storefront/ProductCard';
import ProductCardSkeleton from '../features/catalog/storefront/ProductCardSkeleton';
import ProductFilters from '../features/catalog/storefront/ProductFilters';
import { getStorefrontFilters, searchProducts } from '../features/catalog/storefront/storefrontApi';

const PAGE_SIZE = 15;

const SORT_OPTIONS = [
  { value: 'newest', label: 'Newest' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
];

/** The URL is the single source of truth: shareable links, working back button. */
function readFilters(params) {
  return {
    q: params.get('q') ?? '',
    category: params.get('category') ?? '',
    minPrice: params.get('minPrice') ?? '',
    maxPrice: params.get('maxPrice') ?? '',
    size: params.get('size') ?? '',
    colour: params.get('colour') ?? '',
    inStock: params.get('inStock') === 'true',
    sort: params.get('sort') ?? '',
    page: Math.max(parseInt(params.get('page') ?? '1', 10) || 1, 1),
  };
}

export default function ProductListPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = useMemo(() => readFilters(searchParams), [searchParams]);
  const queryString = searchParams.toString();

  const [reloadKey, setReloadKey] = useState(0);
  // `key` records which request the stored data/error belongs to
  const [response, setResponse] = useState({ key: null, data: null, error: '' });
  const requestKey = `${queryString}|${reloadKey}`;
  // loading is derived (no setState in the effect body): the stored response is stale
  const status = response.key !== requestKey ? 'loading' : response.error ? 'error' : 'ready';
  const result = response.data; // last PagedResponse (kept while the next page loads)
  const errorMessage = response.error;
  const [options, setOptions] = useState(null);
  const [showFilters, setShowFilters] = useState(false);

  // products
  useEffect(() => {
    let ignore = false;
    searchProducts({
      q: filters.q,
      category: filters.category,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      size: filters.size,
      colour: filters.colour,
      inStock: filters.inStock,
      sort: filters.sort,
      page: filters.page - 1, // API is zero-based
      limit: PAGE_SIZE,
    })
      .then((data) => {
        if (!ignore) setResponse({ key: requestKey, data, error: '' });
      })
      .catch((err) => {
        if (!ignore) {
          setResponse({
            key: requestKey,
            data: null,
            error: err?.message || 'Could not load products.',
          });
        }
      });
    return () => {
      ignore = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [requestKey]);

  // filter sidebar data (scoped to the selected category)
  useEffect(() => {
    let ignore = false;
    getStorefrontFilters(filters.category)
      .then((data) => {
        if (!ignore) setOptions(data);
      })
      .catch(() => {});
    return () => {
      ignore = true;
    };
  }, [filters.category]);

  useEffect(() => {
    document.title = filters.q ? `Search: ${filters.q}` : 'Shop';
  }, [filters.q]);

  const updateFilters = (patch) => {
    const next = new URLSearchParams(searchParams);
    Object.entries(patch).forEach(([key, value]) => {
      const empty = value === '' || value === null || value === undefined || value === false;
      if (empty || (key === 'page' && Number(value) <= 1)) next.delete(key);
      else next.set(key, String(value));
    });
    if (!('page' in patch)) next.delete('page'); // any filter change goes back to page 1
    setSearchParams(next);
  };

  const clearAll = () => {
    const next = new URLSearchParams();
    if (filters.q) next.set('q', filters.q); // keep the search term
    setSearchParams(next);
  };

  const changePage = (page) => {
    updateFilters({ page });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const hasActiveFilters = Boolean(
    filters.category ||
      filters.minPrice ||
      filters.maxPrice ||
      filters.size ||
      filters.colour ||
      filters.inStock,
  );

  const sortOptions = filters.q
    ? [{ value: 'relevance', label: 'Best match' }, ...SORT_OPTIONS]
    : SORT_OPTIONS;
  const sortValue = filters.sort || (filters.q ? 'relevance' : 'newest');

  const categoryName = options?.categories?.find((c) => c.slug === filters.category)?.name;
  const heading = filters.q ? `Results for “${filters.q}”` : (categoryName ?? 'All products');

  const products = result?.content ?? [];

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{heading}</h1>
          {status === 'ready' && (
            <p className="mt-1 text-sm text-gray-500">
              {result.totalElements} {result.totalElements === 1 ? 'product' : 'products'}
            </p>
          )}
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setShowFilters((open) => !open)}
            aria-expanded={showFilters}
            className="inline-flex items-center gap-2 rounded-md border border-gray-300 px-3 py-2 text-sm font-medium text-gray-700 lg:hidden"
          >
            <FiFilter aria-hidden="true" /> Filters
          </button>
          <label className="flex items-center gap-2 text-sm text-gray-600">
            <span className="sr-only sm:not-sr-only">Sort by</span>
            <select
              value={sortValue}
              onChange={(e) => updateFilters({ sort: e.target.value })}
              className="rounded-md border border-gray-300 bg-white px-2 py-2 text-sm text-gray-900"
            >
              {sortOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      <div className="lg:grid lg:grid-cols-[16rem_1fr] lg:gap-8">
        <aside className={`${showFilters ? 'block' : 'hidden'} mb-6 lg:mb-0 lg:block`}>
          <ProductFilters
            filters={filters}
            options={options}
            onChange={updateFilters}
            onClear={clearAll}
            hasActiveFilters={hasActiveFilters}
          />
        </aside>

        <main>
          {status === 'error' ? (
            <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-6 text-center">
              <p className="text-sm text-red-700">{errorMessage}</p>
              <button
                type="button"
                onClick={() => setReloadKey((k) => k + 1)}
                className="mt-3 rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
              >
                Try again
              </button>
            </div>
          ) : status === 'loading' && !result ? (
            <ProductGrid>
              {Array.from({ length: PAGE_SIZE }, (_, i) => (
                <ProductCardSkeleton key={i} />
              ))}
            </ProductGrid>
          ) : products.length === 0 ? (
            <div className="rounded-lg border border-dashed border-gray-300 p-10 text-center">
              <p className="text-lg font-medium text-gray-900">No products found</p>
              <p className="mt-1 text-sm text-gray-500">
                Try a different search term or remove some filters.
              </p>
              {(hasActiveFilters || filters.q) && (
                <button
                  type="button"
                  onClick={() => setSearchParams(new URLSearchParams())}
                  className="mt-4 rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
                >
                  Reset search and filters
                </button>
              )}
            </div>
          ) : (
            <div className={status === 'loading' ? 'opacity-60 transition-opacity' : ''}>
              <ProductGrid>
                {products.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </ProductGrid>
              <Pagination
                page={filters.page}
                totalPages={result.totalPages}
                onChange={changePage}
              />
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

function ProductGrid({ children }) {
  return <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">{children}</div>;
}