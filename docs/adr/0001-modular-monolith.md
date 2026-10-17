# ADR 0001: Modular monolith

Status: accepted

Context: a multi-vendor marketplace has many domains, but a single team and a single deployable
keep development, debugging and transactions (stock, orders, payments) simple.

Decision: one Spring Boot application with package-by-feature modules and strict boundaries.

Consequences: simple transactions and deployment; modules can be extracted later if scale demands.
