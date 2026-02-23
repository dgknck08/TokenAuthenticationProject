# Flyway Migration Policy

## Strategy
- Forward-only migrations.
- Never edit already-applied migration files.
- Add new `V{N}__description.sql` for each schema change.

## Rollback Plan
- Application rollback is code-level.
- Data rollback uses compensating forward migration (`V{N+1}__revert_...sql`) when necessary.
- High-risk migrations must be validated in staging before production.

## Guardrails
- Keep migrations idempotent where possible.
- Add indexes/constraints with compatibility checks for existing data.
- Prefer additive changes before destructive refactors.

## Current Order Schema Hardening
- `V1` order tables
- `V2` payment columns
- `V3` cancel/refund timestamps
- `V4` indexes + data constraints
