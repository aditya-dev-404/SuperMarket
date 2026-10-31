import { Link, useParams } from 'react-router-dom';
import Alert from '../components/common/Alert';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import AddressSummary from '../features/address/AddressSummary';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import OrderItemList from '../features/orders/OrderItemList';
import OrderStatusBadge from '../features/orders/OrderStatusBadge';
import { advanceActionLabel, formatDateTime } from '../features/orders/orderUi';
import { useUpdateSubOrderStatus, useVendorSubOrder } from '../features/orders/useVendorOrders';

export default function VendorOrderDetailPage() {
  const { subOrderId } = useParams();
  const { data: subOrder, isPending, isError, error, refetch } = useVendorSubOrder(subOrderId);
  const update = useUpdateSubOrderStatus();

  if (isPending) return <PageSkeleton />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  const action = advanceActionLabel(subOrder.nextStatus);
  const cod = subOrder.paymentMethod === 'COD';

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link to="/vendor/orders" className="text-sm text-indigo-600 hover:underline">
        ← All orders
      </Link>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900">{subOrder.orderNumber}</h1>
          <p className="text-sm text-gray-500">Ordered {formatDateTime(subOrder.orderedAt)}</p>
        </div>
        <OrderStatusBadge status={subOrder.status} />
      </div>

      <Alert message={update.error?.message} className="mt-4" />
      {cod && subOrder.status !== 'CANCELLED' && (
        <Alert
          tone="info"
          className="mt-4"
          message={`Cash on delivery: the customer pays on arrival. Order value ${formatPrice(subOrder.subtotal)}.`}
        />
      )}

      <div className="mt-6 grid gap-8 lg:grid-cols-3">
        <section className="rounded-lg border border-gray-200 px-4 lg:col-span-2">
          <h2 className="border-b border-gray-100 py-3 text-sm font-medium text-gray-600">Items to pack</h2>
          <OrderItemList items={subOrder.items} />
          <p className="border-t border-gray-100 py-3 text-right text-sm font-semibold text-gray-900">
            Subtotal {formatPrice(subOrder.subtotal)}
          </p>
        </section>

        <aside className="space-y-6">
          <section className="rounded-lg border border-gray-200 p-4">
            <h2 className="mb-2 text-sm font-semibold text-gray-900">Ship to</h2>
            <AddressSummary address={subOrder.shippingAddress} />
          </section>

          {action ? (
            <button
              type="button"
              disabled={update.isPending}
              onClick={() => update.mutate({ subOrderId: subOrder.id, status: subOrder.nextStatus })}
              className="w-full rounded-md bg-indigo-600 px-4 py-3 text-sm font-medium text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {update.isPending ? 'Updating...' : action}
            </button>
          ) : (
            <p className="text-center text-sm text-gray-500">No further updates for this order.</p>
          )}
        </aside>
      </div>
    </div>
  );
}