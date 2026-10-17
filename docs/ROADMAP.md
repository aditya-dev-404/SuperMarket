# Roadmap

Each phase includes backend + frontend and ends with something runnable.

| #  | Phase                        | Scope |
|----|------------------------------|-------|
| 0  | Foundation                   | Project skeletons, Docker MySQL, Flyway, error handling, OpenAPI, React base |
| 1  | Auth & Users                 | Register/login, JWT + refresh, roles, profile, route guards |
| 2  | Vendor onboarding & admin    | Vendor application, admin approve/reject/suspend, dashboard shells |
| 3  | Vendor catalog               | Categories, products, size/colour variants, Cloudinary images, vendor CRUD |
| 4  | Public storefront            | Home, search/filter/sort/pagination, product detail |
| 5  | Cart & wishlist              | Server-side cart and wishlist |
| 6  | Addresses, checkout & orders | Order + vendor sub-orders, mock payment, stock locking, order history |
| 7  | Email notifications          | Async emails, HTML templates, Mailpit |
| 8  | Commission & payouts         | Commission config, vendor ledger, payout records, dashboards |
| 9  | Reviews & ratings            | Verified-purchase reviews |
| 10 | Real payment gateway         | Replace mock (Razorpay test mode), idempotent webhooks |
| 11 | Hardening & release          | Security review, rate limiting, caching, CI, Dockerfiles, deployment |

Status: **Phase 0 and Phase 1 delivered.** Next: Phase 2.
