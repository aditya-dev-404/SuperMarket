import { Link, useNavigate, useParams } from 'react-router-dom';
import Alert from '../components/common/Alert';
import PageError from '../components/common/PageError';
import PageSkeleton from '../components/common/PageSkeleton';
import { formatPrice } from '../features/catalog/storefront/priceFormat';
import { useOrder, usePayOrder } from '../features/orders/useOrders';

/** Stand-in for a payment provider's page; Phase 10 replaces it with the real gateway. */
export default function MockPaymentPage() {
  const { orderId } = useParams();
  const navigate = useNavigate();
  const { data: order, isPending, isError, error, refetch } = useOrder(orderId);
  const pay = usePayOrder();

  if (isPending) return <PageSkeleton rows={1} />;
  if (isError) return <PageError error={error} onRetry={refetch} />;

  const failed = pay.data?.paymentStatus === 'PAYMENT_FAILED';
  if (failed) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <h1 className="text-2xl font-semibold text-gray-900">Payment failed</h1>
        <p className="mt-2 text-sm text-gray-600">
          Order {order.orderNumber} was cancelled and nothing was charged. Your items are back in your
          cart (anything that is no longer available was skipped).
        </p>
        <Link
          to="/cart"
          className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          Back to my cart
        </Link>
      </div>
    );
  }

  if (!order.payable) {
    return (
      <div className="mx-auto max-w-lg px-4 py-16 text-center">
        <h1 className="text-2xl font-semibold text-gray-900">Nothing to pay</h1>
        <p className="mt-2 text-sm text-gray-600">Order {order.orderNumber} is not waiting for payment.</p>
        <Link
          to={`/orders/${order.id}`}
          className="mt-6 inline-block rounded-md bg-gray-900 px-4 py-2 text-sm font-medium text-white"
        >
          View order
        </Link>
      </div>
    );
  }

  const handlePay = async (outcome) => {
    try {
      const result = await pay.mutateAsync({ orderId: order.id, outcome });
      if (result.paymentStatus === 'PAID') navigate(`/orders/${order.id}/success`, { replace: true });
    } catch {
      // the error is shown from pay.error below
    }
  };

  return (
    <div className="mx-auto max-w-lg px-4 py-16">
      <h1 className="text-2xl font-semibold text-gray-900">Complete your payment</h1>
      <p className="mt-2 text-sm text-gray-600">
        Order {order.orderNumber} · <span className="font-semibold">{formatPrice(order.total)}</span>
      </p>

      <Alert
        tone="info"
        className="mt-6"
        message="This is a test payment page. Choose how the payment should end."
      />
      <Alert message={pay.error?.message} className="mt-4" />

      <div className="mt-6 flex flex-col gap-3 sm:flex-row">
        <button
          type="button"
          disabled={pay.isPending}
          onClick={() => handlePay('SUCCESS')}
          className="flex-1 rounded-md bg-green-600 px-4 py-3 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
        >
          {pay.isPending ? 'Processing...' : 'Pay successfully'}
        </button>
        <button
          type="button"
          disabled={pay.isPending}
          onClick={() => handlePay('FAILURE')}
          className="flex-1 rounded-md border border-red-300 px-4 py-3 text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50"
        >
          Simulate failure
        </button>
      </div>
      <p className="mt-6 text-center text-sm">
        <Link to={`/orders/${order.id}`} className="text-gray-500 hover:underline">
          Pay later from my orders
        </Link>
      </p>
    </div>
  );
}