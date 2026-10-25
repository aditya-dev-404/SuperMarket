import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getStorefrontFilters } from './storefrontApi';

/** Top-level category links for the home page. Renders nothing if there are none / on error. */
export default function HomeCategories() {
  const [roots, setRoots] = useState([]);

  useEffect(() => {
    let ignore = false;
    getStorefrontFilters()
      .then((data) => {
        if (!ignore) setRoots((data.categories ?? []).filter((c) => !c.parentSlug));
      })
      .catch(() => {});
    return () => {
      ignore = true;
    };
  }, []);

  if (roots.length === 0) return null;

  return (
    <section aria-labelledby="home-categories">
      <h2 id="home-categories" className="mb-4 text-xl font-semibold text-gray-900">
        Shop by category
      </h2>
      <ul className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
        {roots.map((category) => (
          <li key={category.slug}>
            <Link
              to={`/products?category=${encodeURIComponent(category.slug)}`}
              className="flex h-20 items-center justify-center rounded-lg border border-gray-200 px-3 text-center text-sm font-medium text-gray-800 hover:border-gray-900 hover:bg-gray-50"
            >
              {category.name}
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}