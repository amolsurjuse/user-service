# ADR-005: auditable privacy-rights workflow

- Status: Accepted
- Date: 2026-08-12

Authenticated users can download the personal data owned by user-service and submit access, portability, erasure,
restriction, or rectification requests. Intake records are append-only, have a 30-day operational due date, are
returned only to their subject, and responses are marked `no-store`.

Erasure is deliberately a coordinated request rather than an immediate cascade delete. Charging invoices, payment
ledger entries, security evidence, and terms acceptance can have statutory retention duties. Fulfilment must erase or
anonymize each service's data according to the approved retention schedule and record any lawful hold. This control
does not claim legal sign-off or replace the DPIA.
