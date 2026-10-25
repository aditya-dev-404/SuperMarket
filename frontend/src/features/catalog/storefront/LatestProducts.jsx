import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import ProductCard from './ProductCard';
import ProductCardSkeleton from './ProductCardSkeleton';
import { searchProducts } from './storefrontApi';

const COUNT = 8;

/** "New arrivals" strip for the home page. Renders nothing if there are no products / on error. */
export default function LatestProducts() {
  const [state, setState] = useState({ loading: true, products: [] });

  useEffect(() => {
    let ignore = false;
    searchProducts({ sort: 'newest', limit: COUNT })
      .then((page) => {
        if (!ignore) setState({ loading: false, products: page.content });
      })
      .catch(() => {
        if (!ignore) setState({ loading: false, products: [] });
      });
    return () => {
      ignore = true;
    };
  }, []);

  if (!state.loading && state.products.length === 0) return null;

  return (
    <section aria-labelledby="home-latest">
      <div className="mb-4 flex items-baseline justify-between">
        <h2 id="home-latest" className="text-xl font-semibold text-gray-900">
          New arrivals
        </h2>
        <Link to="/products" className="text-sm font-medium text-gray-900 underline">
          View all
        </Link>
      </div>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {state.loading
          ? Array.from({ length: COUNT }, (_, i) => <ProductCardSkeleton key={i} />)
          : state.products.map((product) => <ProductCard key={product.id} product={product} />)}
      </div>
    </section>
  );
}