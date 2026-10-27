import { FiImage } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import WishlistButton from '../../wishlist/WishlistButton';
import { thumbnail } from './imageUrl';
import { formatPrice } from './priceFormat';

export default function ProductCard({ product }) {
  const { id, slug, name, price, priceVaries, imageUrl, inStock } = product;

  return (
    <div className="relative">
      <Link
        to={`/products/${slug}`}
        className="group block rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-gray-900"
      >
        <div className="relative aspect-square overflow-hidden rounded-lg bg-gray-100">
          {imageUrl ? (
            <img
              src={thumbnail(imageUrl)}
              alt={name}
              loading="lazy"
              className={`h-full w-full object-cover transition duration-300 group-hover:scale-105 ${
                inStock ? '' : 'opacity-50 grayscale'
              }`}
            />
          ) : (
            <div className="flex h-full w-full items-center justify-center text-gray-300">
              <FiImage className="h-10 w-10" aria-hidden="true" />
            </div>
          )}
          {!inStock && (
            <span className="absolute left-2 top-2 rounded bg-gray-900/80 px-2 py-1 text-xs font-medium text-white">
              Out of stock
            </span>
          )}
        </div>

        <h3
          className={`mt-2 line-clamp-2 text-sm font-medium ${
            inStock ? 'text-gray-900' : 'text-gray-400'
          }`}
        >
          {name}
        </h3>
        <p className={`mt-1 text-sm ${inStock ? 'font-semibold text-gray-900' : 'text-gray-400'}`}>
          {priceVaries && <span className="font-normal text-gray-500">From </span>}
          {formatPrice(price)}
        </p>
      </Link>

      {/* a sibling of the link (not inside it) so clicking the heart never navigates */}
      <WishlistButton productId={id} variant="overlay" />
    </div>
  );
}