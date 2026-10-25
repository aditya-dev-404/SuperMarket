import { useState } from 'react';
import { FiSearch } from 'react-icons/fi';
import { useLocation, useNavigate } from 'react-router-dom';

/** Search box for the Header. Submits to /products?q=... (Enter or the button). */
export default function HeaderSearch({ className = '' }) {
  const { pathname, search } = useLocation();

  // keep the box in sync with the URL while the shopper is on the product list
  const urlTerm = pathname === '/products' ? (new URLSearchParams(search).get('q') ?? '') : '';

  // key => the form remounts with a fresh value whenever the URL's search term changes
  return <SearchForm key={urlTerm} initialValue={urlTerm} className={className} />;
}

function SearchForm({ initialValue, className }) {
  const navigate = useNavigate();
  const [value, setValue] = useState(initialValue);

  const submit = (event) => {
    event.preventDefault();
    const term = value.trim();
    navigate(term ? `/products?q=${encodeURIComponent(term)}` : '/products');
  };

  return (
    <form onSubmit={submit} role="search" className={`relative ${className}`}>
      <input
        type="search"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="Search products"
        aria-label="Search products"
        maxLength={100}
        className="w-full rounded-full border border-gray-300 py-2 pl-4 pr-10 text-sm focus:border-gray-900 focus:outline-none"
      />
      <button
        type="submit"
        aria-label="Search"
        className="absolute right-1 top-1/2 -translate-y-1/2 rounded-full p-2 text-gray-600 hover:text-gray-900"
      >
        <FiSearch aria-hidden="true" />
      </button>
    </form>
  );
}