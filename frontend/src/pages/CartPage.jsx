import { Link } from 'react-router-dom';
import ErrorMessage from '../components/common/ErrorMessage';
import CartLineItem from '../features/cart/CartLineItem';
import {
  useCart,
  useClearCart,
  useRemoveCartItem,
  useUpdateCartItem,
} from '../features/cart/useCart';
import { formatPrice } from '../features/catalog/storefront/priceFormat';

export default function CartPage() {
  const { data: cart, isPending, isError, error, refetch } = useCart();
  const updateItem = useUpdateCartItem();
  const removeItem = useRemoveCartItem();
  const clear = useClearCart();

  const busy = updateItem.isPending || removeItem.isPending || clear.isPending;
  const actionError = updateItem.error ?? removeItem.error ?? clear.error;

  const handleClear = () => {
    if (window.confirm('Remove all items from your cart?')) clear.mutate();
  };

  if (isPending) {
    return (
      <div className="mx-auto max-w-5xl animate-pulse px-4 py-8" aria-hidden="true">
        <div className="h-7 w-40 rounded bg-gray-200" />
        <div className="mt-6 h-32 rounded-lg bg-gray-200" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-8">
        <ErrorMessage message={error.message} />
        <button
          type="button"
          onClick={() => refetch()}
          className="mt-4 rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          Try again
        </button>
      </div>
    );
  }

  if (cart.vendors.length === 0) {
    return (
      <div className="mx-auto max-w-xl px-4 py-20 text-center">
        <h1 className="text-2xl font-semibold text-gray-900">Your cart is empty</h1>
        <p className="mt-2 text-sm text-gray-500">Looks like you haven&apos;t added anything yet.</p>
        <Link
          to="/products"
          className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          Continue shopping
        </Link>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold text-gray-900">Your cart</h1>
        <button
          type="button"
          onClick={handleClear}
          disabled={busy}
          className="text-sm text-gray-500 hover:text-red-600 disabled:opacity-40"
        >
          Clear cart
        </button>
      </div>

      {actionError && (
        <div className="mt-4">
          <ErrorMessage message={actionError.message} />
        </div>
      )}

      <div className="mt-6 grid gap-8 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {cart.vendors.map((group) => (
            <section
              key={group.vendorId}
              aria-label={`Items from ${group.storeName}`}
              className="rounded-lg border border-gray-200 px-4"
            >
              <h2 className="border-b border-gray-100 py-3 text-sm font-medium text-gray-600">
                Sold by {group.storeName}
              </h2>
              <ul className="divide-y divide-gray-100">
                {group.items.map((item) => (
                  <CartLineItem
                    key={item.id}
                    item={item}
                    busy={busy}
                    onQuantityChange={(itemId, quantity) => updateItem.mutate({ itemId, quantity })}
                    onRemove={(itemId) => removeItem.mutate(itemId)}
                  />
                ))}
              </ul>
            </section>
          ))}
        </div>

        <aside className="h-fit rounded-lg border border-gray-200 p-4">
          <h2 className="text-sm font-semibold text-gray-900">Order summary</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-600">Items</dt>
              <dd>{cart.totalQuantity}</dd>
            </div>
            <div className="flex justify-between text-base font-semibold text-gray-900">
              <dt>Subtotal</dt>
              <dd>{formatPrice(cart.subtotal)}</dd>
            </div>
          </dl>

          {!cart.checkoutReady && (
            <p className="mt-3 text-xs text-red-600" role="alert">
              Some items need your attention before you can check out.
            </p>
          )}

          <p className="mt-3 text-xs text-gray-500">
            Shipping is free for online payment. Cash on delivery adds a small shipping fee at checkout.
          </p>

          {cart.checkoutReady ? (
            <Link
              to="/checkout"
              className="mt-4 block w-full rounded-md bg-indigo-600 px-4 py-3 text-center text-sm font-medium text-white hover:bg-indigo-700"
            >
              Proceed to checkout
            </Link>
          ) : (
            <button
              type="button"
              disabled
              className="mt-4 w-full cursor-not-allowed rounded-md bg-gray-900 px-4 py-3 text-sm font-medium text-white opacity-40"
            >
              Proceed to checkout
            </button>
          )}
        </aside>
      </div>
    </div>
  );
}