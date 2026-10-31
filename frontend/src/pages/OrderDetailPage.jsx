import { Link, useParams } from 'react-router-dom';
import Alert from '../components/common/Alert';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import AddressSummary from '../features/address/AddressSummary';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import OrderItemList from '../features/orders/OrderItemList';
import OrderStatusBadge from '../features/orders/OrderStatusBadge';
import { PAYMENT_METHODS, formatDateTime, paymentStatusLabel } from '../features/orders/orderUi';
import { useCancelOrder, useOrder } from '../features/orders/useOrders';

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const { data: order, isPending, isError, error, refetch } = useOrder(orderId);
  const cancel = useCancelOrder();

  if (isPending) return <PageSkeleton />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  const handleCancel = () => {
    if (window.confirm('Cancel this order? This cannot be undone.')) cancel.mutate(order.id);
  };

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link to="/orders" className="text-sm text-indigo-600 hover:underline">
        ← My orders
      </Link>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{order.orderNumber}</h1>
          <p className="text-sm text-gray-500">Placed on {formatDateTime(order.createdAt)}</p>
        </div>
        <OrderStatusBadge status={order.status} />
      </div>

      <Alert message={cancel.error?.message} className="mt-4" />
      {order.paymentStatus === 'REFUND_PENDING' && (
        <Alert tone="info" className="mt-4" message="Your refund is pending." />
      )}
      {order.payable && (
        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4">
          <p className="text-sm text-amber-800">This order is waiting for payment.</p>
          <Link
            to={`/orders/${order.id}/pay`}
            className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
          >
            Pay now
          </Link>
        </div>
      )}

      <div className="mt-6 grid gap-8 lg:grid-cols-3">
        <div className="space-y-6 lg:col-span-2">
          {order.subOrders.map((subOrder) => (
            <section
              key={subOrder.id}
              aria-label={`Items from ${subOrder.storeName}`}
              className="rounded-lg border border-gray-200 px-4"
            >
              <div className="flex items-center justify-between border-b border-gray-100 py-3">
                <h2 className="text-sm font-medium text-gray-600">Sold by {subOrder.storeName}</h2>
                <OrderStatusBadge status={subOrder.status} />
              </div>
              <OrderItemList items={subOrder.items} />
              {(subOrder.shippedAt || subOrder.deliveredAt || subOrder.cancelledAt) && (
                <p className="border-t border-gray-100 py-2 text-xs text-gray-500">
                  {subOrder.deliveredAt
                    ? `Delivered ${formatDateTime(subOrder.deliveredAt)}`
                    : subOrder.cancelledAt
                      ? `Cancelled ${formatDateTime(subOrder.cancelledAt)}`
                      : `Shipped ${formatDateTime(subOrder.shippedAt)}`}
                </p>
              )}
            </section>
          ))}
        </div>

        <aside className="space-y-6">
          <section className="rounded-lg border border-gray-200 p-4">
            <h2 className="text-sm font-semibold text-gray-900">Summary</h2>
            <dl className="mt-3 space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-gray-600">Subtotal</dt>
                <dd>{formatPrice(order.subtotal)}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-gray-600">Shipping</dt>
                <dd>{Number(order.shippingFee) > 0 ? formatPrice(order.shippingFee) : 'Free'}</dd>
              </div>
              <div className="flex justify-between border-t border-gray-100 pt-2 font-semibold text-gray-900">
                <dt>Total</dt>
                <dd>{formatPrice(order.total)}</dd>
              </div>
            </dl>
            <p className="mt-3 text-xs text-gray-500">
              {PAYMENT_METHODS[order.paymentMethod]?.label} · {paymentStatusLabel(order.paymentStatus)}
            </p>
          </section>

          <section className="rounded-lg border border-gray-200 p-4">
            <h2 className="mb-2 text-sm font-semibold text-gray-900">Delivering to</h2>
            <AddressSummary address={order.shippingAddress} />
          </section>

          {order.cancellable && (
            <button
              type="button"
              onClick={handleCancel}
              disabled={cancel.isPending}
              className="w-full rounded-md border border-red-300 px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
            >
              {cancel.isPending ? 'Cancelling...' : 'Cancel order'}
            </button>
          )}
        </aside>
      </div>
    </div>
  );
}