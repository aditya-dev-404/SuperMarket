import { FiCheckCircle } from 'react-icons/fi';
import { Link, useParams } from 'react-router-dom';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import { paymentStatusLabel } from '../features/orders/orderUi';
import { useOrder } from '../features/orders/useOrders';

export default function OrderSuccessPage() {
  const { orderId } = useParams();
  const { data: order, isPending, isError, error, refetch } = useOrder(orderId);

  if (isPending) return <PageSkeleton rows={1} />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  const cod = order.paymentMethod === 'COD';

  return (
    <div className="mx-auto max-w-lg px-4 py-16 text-center">
      <FiCheckCircle className="mx-auto h-12 w-12 text-green-600" aria-hidden="true" />
      <h1 className="mt-4 text-2xl font-semibold text-gray-900">Thank you, your order is placed</h1>
      <p className="mt-2 text-sm text-gray-600">
        Order <span className="font-medium">{order.orderNumber}</span> · {formatPrice(order.total)} ·{' '}
        {paymentStatusLabel(order.paymentStatus)}
      </p>
      {cod && (
        <p className="mt-1 text-sm text-gray-500">Keep {formatPrice(order.total)} ready when it arrives.</p>
      )}
      <div className="mt-8 flex justify-center gap-3">
        <Link
          to={`/orders/${order.id}`}
          className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white hover:bg-indigo-700"
        >
          View order
        </Link>
        <Link
          to="/products"
          className="rounded-md border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
        >
          Continue shopping
        </Link>
      </div>
    </div>
  );
}