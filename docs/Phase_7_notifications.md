# Phase 7 – Email notifications

Transactional emails for accounts, vendor onboarding and orders. Wishlist emails are intentionally not sent.

## 1. How an email is sent

```
Service (AccountService, VendorService, CheckoutService, ...)
   | publishes a small event inside its transaction
   v
Event listener (AccountEmailListener / VendorEmailListener / OrderEmailListener)
   | runs AFTER the transaction committed, on the "notificationExecutor" thread pool
   v
NotificationService.send(request)        never throws
   | 1. render HTML + plain-text from a Thymeleaf template
   | 2. write a row in notification_log (PENDING)
   | 3. deliver through EmailSender (SMTP)
   v
notification_log row becomes SENT, or stays PENDING for the retry job
```

Why it is built this way:
- A rolled-back signup or order never sends an email (listeners wait for the commit).
- The HTTP request never waits for the mail server (listeners are async).
- A mail problem never breaks a business action (`send` catches everything).
- Failed emails are retried: `NotificationRetryJob` runs every `retry-interval`, retries due rows up to
  `max-attempts` (linear back-off, `retry-delay` x attempt number), then marks them FAILED.
- Rendered bodies are cleared once an email is SENT or FAILED, because they can contain one-time links.
- Retries assume a single running backend instance (no row locking between instances).

## 2. Emails

| Type | Trigger | To |
|---|---|---|
| EMAIL_VERIFICATION | register / resend verification | the new user |
| WELCOME | email verified | the user |
| PASSWORD_RESET | forgot password | the user |
| PASSWORD_CHANGED | change or reset password | the user |
| VENDOR_APPLICATION_RECEIVED | apply / resubmit after rejection | the applicant |
| VENDOR_APPLICATION_ADMIN_ALERT | apply / resubmit after rejection | every active, verified ADMIN (one email each) |
| VENDOR_APPROVED / VENDOR_REJECTED | admin decision (rejection includes the reason) | the applicant |
| ORDER_CONFIRMATION | COD order placed, or online order paid | the customer |
| VENDOR_NEW_ORDER | same moment | one email per vendor sub-order |
| PAYMENT_FAILED | online payment failed | the customer |
| ORDER_CANCELLED | customer cancelled | the customer |
| VENDOR_ORDER_CANCELLED | customer cancelled | vendors, only for COD orders or paid online orders |
| ORDER_SHIPPED / ORDER_DELIVERED | vendor sets SHIPPED / DELIVERED | the customer (one per vendor sub-order) |

Nothing is sent for PROCESSING, for online orders still waiting for payment, or for edits of a pending
vendor application.

## 3. Events (package notification.event)

| Event | Published by | Carries |
|---|---|---|
| EmailVerificationRequestedEvent, PasswordResetRequestedEvent | AccountService | user, raw token (hidden in toString) |
| PasswordChangedEvent | AuthService, AccountService | user |
| EmailVerifiedEvent | AccountService | user |
| VendorApplicationSubmittedEvent | VendorService.apply / updateMine (rejected only) | applicant and store details |
| VendorApplicationApprovedEvent / RejectedEvent | VendorService.approve / reject | applicant, store, reason |
| OrderPlacedEvent | CheckoutService (COD), PaymentService (paid) | order id |
| OrderPaymentFailedEvent | PaymentService | order id |
| OrderCancelledEvent | OrderService.cancel | order id |
| SubOrderStatusChangedEvent | VendorOrderService (SHIPPED, DELIVERED) | order id, sub-order id, status |

Order events carry only ids; `OrderEmailDataLoader` reads the committed order, so emails always show real data.

## 4. Templates (resources/templates/email)

Every template has an HTML file in `html/` and a plain-text twin in `text/` with the same name.

| Template | Used for | Model |
|---|---|---|
| generic | account and vendor emails | heading, greeting, paragraphs, actionLabel + actionUrl, footnote |
| order | order emails | as generic, plus items, summary, addressTitle + addressLines |

To add an email: add an event, add a listener method that builds a `NotificationRequest` (type, to, subject,
template, model, referenceId), add a value to `NotificationType` if it is a new kind, and add a render test.
User text is HTML-escaped by Thymeleaf in the HTML version.

## 5. Account security rules
- Verification and reset links hold a random 256-bit token; only its SHA-256 hash is stored (`account_tokens`).
- Tokens are single-use, expire (24 h verification, 30 min reset) and an older unused token of the same type is
  cancelled when a new one is issued.
- Resend / forgot-password answer identically for known and unknown emails (no account enumeration) and are
  rate-limited by a cooldown (default 60 s).
- Login checks the password first, then the verified status.
- Re-registering an unverified email never overwrites the account; it only re-sends the link.
- Unverified accounts older than 7 days are deleted daily at 03:30 (`UnverifiedAccountCleanupJob`).
- Existing users and the seeded admin were backfilled as verified by migration V9.

## 6. Configuration

Defaults point at Mailpit (docker-compose: SMTP `localhost:1025`, inbox http://localhost:8025). No credentials
are needed in development.

| Property | Default / env var | Meaning |
|---|---|---|
| spring.mail.host / port | `MAIL_HOST` / `MAIL_PORT` (localhost / 1025) | SMTP server |
| spring.mail.username / password | `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP login (empty for Mailpit) |
| mail.smtp.auth / starttls | `MAIL_SMTP_AUTH` / `MAIL_SMTP_STARTTLS` (false) | set both to true for a real provider |
| app.notification.enabled | true | master switch; false = nothing rendered, stored or sent |
| app.notification.from-address | `MAIL_FROM` | the single sender mailbox |
| app.notification.from-name | superMarket | display name |
| app.notification.frontend-base-url | `FRONTEND_BASE_URL` (http://localhost:5173) | base of links inside emails |
| app.notification.max-attempts | 3 | tries before FAILED |
| app.notification.retry-delay | PT2M | base back-off |
| app.notification.retry-interval | PT1M | how often the retry job runs |
| app.notification.retry-batch-size | 20 | emails per retry run |
| app.account.verification-token-ttl | PT24H | verification link validity |
| app.account.password-reset-token-ttl | PT30M | reset link validity |
| app.account.email-cooldown | PT1M | minimum gap between resend / forgot emails |
| app.account.unverified-retention | P7D | age after which unverified accounts are deleted |

Gotcha: `${MAIL_FROM:default}` only falls back when the variable is *undefined*. An env var set to an empty
string wins and fails validation at startup, so unset it instead of leaving it empty.

Production (Phase 11): use a transactional SMTP provider (for example Brevo, Mailjet, SendGrid, Resend, Zoho or
Amazon SES), set the `MAIL_*` variables, `MAIL_SMTP_AUTH=true`, `MAIL_SMTP_STARTTLS=true`, a sender address on
your own domain (with SPF / DKIM set up at the provider) and `FRONTEND_BASE_URL` to the real site.

## 7. Database
- V8 `notification_log` (type, to_address, subject, bodies, status PENDING/SENT/FAILED, attempts, reference_id,
  last_error, next_attempt_at, sent_at). `reference_id` is the user id, vendor id, order id or sub-order id.
- V9 `users.email_verified`, `users.email_verified_at`, and `account_tokens`.
- Next migration: V10.

Useful queries:
```sql
SELECT id, type, to_address, status, attempts, last_error FROM notification_log ORDER BY id DESC LIMIT 20;
SELECT COUNT(*) FROM notification_log WHERE status = 'FAILED';
```

## 8. Known limits
- Real SMTP is not exercised yet (Mailpit only).
- No unsubscribe or per-user email preferences (all emails are transactional).
- Single-instance retry job; a multi-instance deployment needs row locking or an external queue.
- A render failure is logged as an error and the email is not stored (so it is not retried); the order or signup
  itself is unaffected. Watch the log for "Could not queue".
- Refunds are not real yet: the cancellation email says a paid order will be refunded (Phase 10).