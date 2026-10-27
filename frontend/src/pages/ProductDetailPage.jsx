import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import AddToCartButton from '../features/cart/AddToCartButton';
import Breadcrumb from '../features/catalog/storefront/Breadcrumb';
import ImageGallery from '../features/catalog/storefront/ImageGallery';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import { getProductBySlug } from '../features/catalog/storefront/storefrontApi';
import { useVariantSelection } from '../features/catalog/storefront/useVariantSelection';
import VariantSelector from '../features/catalog/storefront/VariantSelector';
import WishlistButton from '../features/wishlist/WishlistButton';

const LOW_STOCK_THRESHOLD = 5;

function isNotFound(err) {
  return err?.status === 404 || err?.response?.status === 404;
}

export default function ProductDetailPage() {
  const { slug } = useParams();
  // key => fresh "loading" state whenever the slug changes
  return <ProductLoader key={slug} slug={slug} />;
}

function ProductLoader({ slug }) {
  const [state, setState] = useState({ status: 'loading', product: null, message: '' });

  useEffect(() => {
    let ignore = false;
    getProductBySlug(slug)
      .then((product) => {
        if (!ignore) setState({ status: 'ready', product, message: '' });
      })
      .catch((err) => {
        if (ignore) return;
        setState({
          status: isNotFound(err) ? 'notfound' : 'error',
          product: null,
          message: err?.message || 'Could not load this product.',
        });
      });
    return () => {
      ignore = true;
    };
  }, [slug]);

  useEffect(() => {
    if (state.product) document.title = state.product.name;
  }, [state.product]);

  if (state.status === 'loading') return <DetailSkeleton />;

  if (state.status === 'notfound') {
    return (
      <Message title="Product not found" text="This product is unavailable or no longer exists." />
    );
  }
  if (state.status === 'error') {
    return <Message title="Something went wrong" text={state.message} />;
  }

  // key => selection state resets when navigating between products
  return <ProductView key={state.product.id} product={state.product} />;
}

function ProductView({ product }) {
  const selection = useVariantSelection(product.variants);
  const { selected } = selection;

  const price = selected?.price ?? product.basePrice;
  const outOfStock = !selected || !selected.inStock;

  const crumbs = [
    { label: 'Home', to: '/' },
    { label: 'Shop', to: '/products' },
    ...product.breadcrumb.map((c) => ({
      label: c.name,
      to: `/products?category=${encodeURIComponent(c.slug)}`,
    })),
    { label: product.name },
  ];

  let stockText = 'In stock';
  let stockClass = 'text-green-700';
  if (outOfStock) {
    stockText = 'Out of stock';
    stockClass = 'text-red-600';
  } else if (selected.stockQuantity <= LOW_STOCK_THRESHOLD) {
    stockText = `Only ${selected.stockQuantity} left`;
    stockClass = 'text-amber-600';
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <Breadcrumb items={crumbs} />

      <div className="mt-6 grid gap-8 lg:grid-cols-2">
        <ImageGallery images={product.images} alt={product.name} />

        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{product.name}</h1>
          {product.vendor?.name && (
            <p className="mt-1 text-sm text-gray-500">Sold by {product.vendor.name}</p>
          )}

          <p className="mt-4 text-3xl font-semibold text-gray-900">{formatPrice(price)}</p>
          <p className={`mt-1 text-sm font-medium ${stockClass}`}>{stockText}</p>

          <div className="mt-6">
            <VariantSelector selection={selection} />
          </div>

          {selected?.sku && <p className="mt-4 text-xs text-gray-400">SKU: {selected.sku}</p>}

          <div className="mt-6 flex items-start gap-3">
            {/* key => the "added" / error message resets when another variant is chosen */}
            <AddToCartButton key={selected?.id} variantId={selected?.id} disabled={outOfStock} />
            <WishlistButton productId={product.id} variant="bordered" />
          </div>

          {product.description && (
            <section className="mt-8 border-t border-gray-200 pt-6">
              <h2 className="text-sm font-semibold text-gray-900">Description</h2>
              <p className="mt-2 whitespace-pre-line text-sm leading-6 text-gray-600">
                {product.description}
              </p>
            </section>
          )}
        </div>
      </div>
    </div>
  );
}

function Message({ title, text }) {
  return (
    <div className="mx-auto max-w-xl px-4 py-20 text-center">
      <h1 className="text-2xl font-semibold text-gray-900">{title}</h1>
      <p className="mt-2 text-sm text-gray-500">{text}</p>
      <Link
        to="/products"
        className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
      >
        Back to shop
      </Link>
    </div>
  );
}

function DetailSkeleton() {
  return (
    <div className="mx-auto max-w-7xl animate-pulse px-4 py-6 sm:px-6 lg:px-8" aria-hidden="true">
      <div className="h-4 w-1/3 rounded bg-gray-200" />
      <div className="mt-6 grid gap-8 lg:grid-cols-2">
        <div className="aspect-square rounded-lg bg-gray-200" />
        <div className="space-y-4">
          <div className="h-7 w-3/4 rounded bg-gray-200" />
          <div className="h-8 w-1/4 rounded bg-gray-200" />
          <div className="h-10 w-1/2 rounded bg-gray-200" />
          <div className="h-12 w-full rounded bg-gray-200" />
        </div>
      </div>
    </div>
  );
}