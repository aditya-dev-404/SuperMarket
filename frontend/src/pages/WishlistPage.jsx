import { Link, Navigate, useSearchParams } from 'react-router-dom';
import ErrorMessage from '../components/common/ErrorMessage';
import Pagination from '../features/catalog/storefront/Pagination';
import ProductCard from '../features/catalog/storefront/ProductCard';
import ProductCardSkeleton from '../features/catalog/storefront/ProductCardSkeleton';
import MoveToCart from '../features/wishlist/MoveToCart';
import { WISHLIST_PAGE_SIZE, useWishlistPage } from '../features/wishlist/useWishlist';

function parsePage(value) {
  const page = Number.parseInt(value ?? '1', 10);
  return Number.isNaN(page) || page < 1 ? 1 : page;
}

const GRID = 'mt-6 grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4';

export default function WishlistPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const page = parsePage(searchParams.get('page'));
  const { data, isPending, isError, error } = useWishlistPage(page);

  const goToPage = (nextPage) => setSearchParams({ page: String(nextPage) });

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <h1 className="text-2xl font-semibold text-gray-900">Your wishlist</h1>

      {isPending && (
        <div className={GRID} aria-hidden="true">
          {Array.from({ length: WISHLIST_PAGE_SIZE }, (_, index) => (
            <ProductCardSkeleton key={index} />
          ))}
        </div>
      )}

      {isError && (
        <div className="mt-6">
          <ErrorMessage message={error.message} />
        </div>
      )}

      {data && data.content.length === 0 && page > 1 && (
        // the last item of the last page was removed: step back to the last page that exists
        <Navigate to={`/wishlist?page=${Math.max(1, data.totalPages)}`} replace />
      )}

      {data && data.content.length === 0 && page === 1 && (
        <div className="py-16 text-center">
          <p className="text-sm text-gray-500">Nothing saved yet. Tap the heart on a product.</p>
          <Link
            to="/products"
            className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
          >
            Browse products
          </Link>
        </div>
      )}

      {data && data.content.length > 0 && (
        <>
          <ul className={GRID}>
            {data.content.map(({ product }) => (
              <li key={product.id}>
                <ProductCard product={product} />
                <MoveToCart product={product} />
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={data.totalPages} onChange={goToPage} />
        </>
      )}
    </div>
  );
}