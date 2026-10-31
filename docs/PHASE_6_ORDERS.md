# Phase 6: addresses, checkout and orders (mock payment)

## Decisions

- Payment methods: **ONLINE** (mock gateway) and **COD**.
- Shipping: COD orders pay a flat fee (`app.checkout.cod-shipping-fee`, default 40, per order); online orders ship free. No tax.
- A cart is split by vendor into **sub-orders**; each vendor handles only their own.
- Orders keep **snapshots** (address, product name, price, size, colour, image) so later edits never change an order.
- Customers can cancel while every sub-order is before shipping. Vendors cannot cancel in this phase.
- A failed mock payment is final: the order is cancelled, stock is restored, and the items go back into the cart (best effort).
- An unpaid online order can be paid later from its detail page. Automatic expiry of unpaid orders is deferred to the hardening phase.
- Addresses are India-only: 10-digit mobile starting 6-9, 6-digit PIN not starting with 0, max 10 per user (`app.address.max-per-user`), one default.
- No admin view of all orders yet.

## Database (Flyway `V7__addresses_and_orders.sql`, next migration is V8)

`addresses`, `orders` (with `shipping_*` snapshot columns), `sub_orders` (unique per order and vendor), `order_items`
(nullable `variant_id` / `product_id`, `ON DELETE SET NULL`), `payments` (one row per payment attempt).

## Statuses

- `PaymentMethod`: ONLINE, COD.
- `PaymentStatus`: PENDING_PAYMENT, PAID, PAYMENT_FAILED, COD, REFUND_PENDING, CANCELLED.
- `SubOrderStatus`: PENDING_PAYMENT, PLACED, PROCESSING, SHIPPED, DELIVERED, CANCELLED.
- Overall order status = the least advanced non-cancelled sub-order.
- COD sub-orders start PLACED, online sub-orders start PENDING_PAYMENT and become PLACED when paid.
- A COD order becomes PAID when all its sub-orders are delivered.
- Cancelling a PAID order gives REFUND_PENDING; any other cancel gives CANCELLED.
- Vendors see COD sub-orders, and online sub-orders only when payment is PAID or REFUND_PENDING.

## Backend (`com.aditya.supermarket`)

- `address`: `AddressController`, `AddressService`, `Address`, `AddressRepository`, DTOs, `AddressProperties`.
- `order`: `OrderController`, `CheckoutController`, `VendorOrderController`; `CheckoutService`, `OrderService`,
  `VendorOrderService`, `OrderInventory`, `OrderNumberGenerator` (`SM-yyyyMMdd-XXXXXX`), `OrderCartRestorer`;
  entities `Order` (JPA name `CustomerOrder`, because ORDER is reserved), `SubOrder`, `OrderItem`, `ShippingAddress`.
- `inventory`: `InventoryService`, `StockRepository` (atomic conditional UPDATEs).
- `payment`: `PaymentController`, `PaymentService`, `PaymentGateway` interface, `MockPaymentGateway`, `Payment`.
  Phase 10 replaces the mock behind the interface (`app.payment.provider=mock`).

### How placing an order works

1. Re-validate every cart line (must be AVAILABLE), group by vendor, snapshot the data.
2. Deduct stock with a guarded UPDATE, in variant-id order (avoids deadlocks). Any shortage rolls everything back and the cart stays intact.
3. Save the order and sub-orders, clear the cart, all in one transaction.
4. Payment, cancel and vendor status updates lock the order row (`PESSIMISTIC_WRITE`) to stop races.

## API

See the Phase 6 section in `docs/API.md`.

## Frontend

- `features/address`, `features/orders`; shared `Alert`, `PageError`, `PageSkeleton`, `Pagination`, `TextField`.
- Pages: `/account/addresses`, `/checkout` (single page), `/orders`, `/orders/:id`, `/orders/:id/pay`, `/orders/:id/success`,
  `/vendor/orders`, `/vendor/orders/:subOrderId`.
- Cart and wishlist caches are refreshed after placing an order and after a failed payment.

## Testing

Manual plan: `docs/PHASE_6_TEST_PLAN.md`. Automated: service and entity unit tests under `src/test/java/.../{address,order,payment,inventory}`
and Vitest tests in `features/address` and `features/orders`.

## Out of scope (later phases)

- Emails for order events (Phase 7).
- Commission and payouts (Phase 8).
- Real payment gateway and refunds (Phase 10).
- Auto-expiry of unpaid orders, admin all-orders view, controller (MockMvc) tests (hardening phase).