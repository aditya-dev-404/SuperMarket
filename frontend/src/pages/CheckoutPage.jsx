import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Alert from '../components/common/Alert';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import AddressForm from '../features/address/AddressForm';
import AddressSummary from '../features/address/AddressSummary';
import { useAddresses, useCreateAddress } from '../features/address/useAddresses';
import { useCart } from '../features/cart/useCart';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import { PAYMENT_METHODS } from '../features/orders/orderUi';
import { useCheckoutOptions, usePlaceOrder } from '../features/orders/useOrders';

export default function CheckoutPage() {
  const navigate = useNavigate();
  const cartQuery = useCart();
  const addressesQuery = useAddresses();
  const optionsQuery = useCheckoutOptions();
  const createAddress = useCreateAddress();
  const placeOrder = usePlaceOrder();

  const [selectedId, setSelectedId] = useState(null);
  const [addingAddress, setAddingAddress] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('ONLINE');

  const queries = [cartQuery, addressesQuery, optionsQuery];
  if (queries.some((query) => query.isPending)) return <PageSkeleton rows={2} />;
  const failed = queries.find((query) => query.isError);
  if (failed) {
    return <PageError error={failed.error} onRetry={() => queries.forEach((query) => query.refetch())} />;
  }

  const cart = cartQuery.data;
  const addresses = addressesQuery.data;

  // Nothing to buy: also what a customer sees after a successful order, if they come back here.
  if (cart.vendors.length === 0) {
    return (
      <div className="mx-auto max-w-xl px-4 py-20 text-center">
        <h1 className="text-2xl font-semibold text-gray-900">Your cart is empty</h1>
        <p className="mt-2 text-sm text-gray-500">Add something to your cart to check out.</p>
        <Link
          to="/products"
          className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          Continue shopping
        </Link>
      </div>
    );
  }

  const chosen =
    addresses.find((address) => address.id === selectedId) ??
    addresses.find((address) => address.defaultAddress) ??
    addresses[0];
  const showForm = addingAddress || addresses.length === 0;

  const shippingFee = paymentMethod === 'COD' ? Number(optionsQuery.data.codShippingFee) : 0;
  const total = Number(cart.subtotal) + shippingFee;
  const canPlace = cart.checkoutReady && Boolean(chosen) && !placeOrder.isPending;

  const handlePlaceOrder = async () => {
    try {
      const order = await placeOrder.mutateAsync({ addressId: chosen.id, paymentMethod });
      navigate(paymentMethod === 'COD' ? `/orders/${order.id}/success` : `/orders/${order.id}/pay`, {
        replace: true,
      });
    } catch {
      // the error is shown from placeOrder.error below
    }
  };

  return (
    <div className="mx-auto max-w-5xl px-4 py-8">
      <h1 className="text-2xl font-semibold text-gray-900">Checkout</h1>

      <div className="mt-6 grid gap-8 lg:grid-cols-3">
        <div className="space-y-8 lg:col-span-2">
          <section aria-labelledby="delivery-heading">
            <h2 id="delivery-heading" className="text-sm font-semibold text-gray-900">
              1. Delivery address
            </h2>

            {addresses.length > 0 && (
              <ul className="mt-3 space-y-3">
                {addresses.map((address) => (
                  <li key={address.id}>
                    <label
                      className={`flex cursor-pointer items-start gap-3 rounded-lg border p-4 ${
                        chosen?.id === address.id ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'
                      }`}
                    >
                      <input
                        type="radio"
                        name="address"
                        checked={chosen?.id === address.id}
                        onChange={() => setSelectedId(address.id)}
                        className="mt-1"
                      />
                      <AddressSummary address={address} />
                    </label>
                  </li>
                ))}
              </ul>
            )}

            {showForm ? (
              <div className="mt-4 rounded-lg border border-gray-200 p-4">
                <h3 className="mb-4 text-sm font-medium text-gray-900">Add a new address</h3>
                <AddressForm
                  submitLabel="Save and use this address"
                  onSubmit={async (request) => {
                    const created = await createAddress.mutateAsync(request);
                    setSelectedId(created.id);
                    setAddingAddress(false);
                  }}
                  onCancel={addresses.length > 0 ? () => setAddingAddress(false) : undefined}
                />
              </div>
            ) : (
              <button
                type="button"
                onClick={() => setAddingAddress(true)}
                className="mt-3 text-sm font-medium text-indigo-600 hover:underline"
              >
                + Add a new address
              </button>
            )}
          </section>

          <section aria-labelledby="payment-heading">
            <h2 id="payment-heading" className="text-sm font-semibold text-gray-900">
              2. Payment method
            </h2>
            <div className="mt-3 space-y-3">
              {Object.entries(PAYMENT_METHODS).map(([method, { label, hint }]) => (
                <label
                  key={method}
                  className={`flex cursor-pointer items-start gap-3 rounded-lg border p-4 ${
                    paymentMethod === method ? 'border-indigo-500 bg-indigo-50' : 'border-gray-200'
                  }`}
                >
                  <input
                    type="radio"
                    name="payment"
                    value={method}
                    checked={paymentMethod === method}
                    onChange={() => setPaymentMethod(method)}
                    className="mt-1"
                  />
                  <span>
                    <span className="block text-sm font-medium text-gray-900">{label}</span>
                    <span className="block text-xs text-gray-500">
                      {method === 'COD'
                        ? `Pay when it arrives. A ${formatPrice(optionsQuery.data.codShippingFee)} shipping fee applies.`
                        : hint}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </section>

          <section aria-labelledby="items-heading">
            <h2 id="items-heading" className="text-sm font-semibold text-gray-900">
              3. Review items
            </h2>
            <div className="mt-3 space-y-4">
              {cart.vendors.map((group) => (
                <div key={group.vendorId} className="rounded-lg border border-gray-200 px-4">
                  <p className="border-b border-gray-100 py-3 text-sm text-gray-600">
                    Sold by {group.storeName}
                  </p>
                  <ul className="divide-y divide-gray-100">
                    {group.items.map((item) => (
                      <li key={item.id} className="flex justify-between gap-4 py-3 text-sm">
                        <span className="text-gray-900">
                          {item.productName}
                          <span className="block text-xs text-gray-500">
                            {[item.size, item.colour].filter(Boolean).join(' · ')} × {item.quantity}
                          </span>
                        </span>
                        <span className="font-medium text-gray-900">{formatPrice(item.lineTotal)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              ))}
            </div>
            <Link to="/cart" className="mt-3 inline-block text-sm text-indigo-600 hover:underline">
              Change items
            </Link>
          </section>
        </div>

        <aside className="h-fit rounded-lg border border-gray-200 p-4">
          <h2 className="text-sm font-semibold text-gray-900">Order summary</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-600">Subtotal ({cart.totalQuantity} items)</dt>
              <dd>{formatPrice(cart.subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-600">Shipping</dt>
              <dd>{shippingFee > 0 ? formatPrice(shippingFee) : 'Free'}</dd>
            </div>
            <div className="flex justify-between border-t border-gray-100 pt-2 text-base font-semibold text-gray-900">
              <dt>Total</dt>
              <dd>{formatPrice(total)}</dd>
            </div>
          </dl>

          {!cart.checkoutReady && (
            <Alert
              className="mt-3"
              message="Some items in your cart need attention. Go back to your cart to fix them."
            />
          )}
          <Alert message={placeOrder.error?.message} className="mt-3" />

          <button
            type="button"
            onClick={handlePlaceOrder}
            disabled={!canPlace}
            className="mt-4 w-full rounded-md bg-indigo-600 px-4 py-3 text-sm font-medium text-white hover:bg-indigo-700 disabled:cursor-not-allowed disabled:opacity-40"
          >
            {placeOrder.isPending
              ? 'Placing order...'
              : paymentMethod === 'COD'
                ? 'Place order'
                : 'Continue to payment'}
          </button>
          {!chosen && <p className="mt-2 text-xs text-gray-500">Add a delivery address to continue.</p>}
        </aside>
      </div>
    </div>
  );
}