# integrity-guard

Android integrity checks and guard interfaces.

## Owns

- Integrity guard contracts.
- Default Android implementation for reusable integrity checks.
- Current optional Hilt wiring used by legacy consumers.

## Boundaries

- Do not store app-specific certificate hashes or secrets here.
- Do not add app policy decisions; expose reusable checks only.
- Prefer optional DI bridges long term instead of direct Hilt in base libraries.

## Usage

Use this module for reusable Android integrity checks. Apps should provide their own expected values and policy decisions.
