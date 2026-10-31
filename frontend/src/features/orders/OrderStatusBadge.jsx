import { statusLabel, statusTone } from './orderUi';

export default function OrderStatusBadge({ status }) {
  return (
    <span
      className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${statusTone(status)}`}
    >
      {statusLabel(status)}
    </span>
  );
}