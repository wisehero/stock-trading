# AGENTS.md

## Scope
- This file applies to the `stock-trading` project root and all subdirectories.

## Communication Rules
- If any requirement is ambiguous, missing, or has multiple valid interpretations, ask a clarifying question before implementation.
- Do not make silent assumptions for business rules, API contracts, or data model decisions.
- Keep questions short and option-based when possible.

## Workflow
- Follow this order by default:
  1. Requirements clarification
  2. Implementation plan
  3. Implementation
  4. Test
  5. Review

## General Code Best Practices
- Follow SOLID principles, especially SRP and DIP.
- Prefer simple solutions first (KISS).
- Do not add abstractions that are not currently needed (YAGNI).
- Remove duplication where it improves maintainability (DRY), but avoid premature abstraction.
- Use clear and intention-revealing names for classes, methods, variables, and constants.
- Keep functions/methods focused on a single responsibility.
- Minimize side effects; prefer immutable data and explicit state changes.
- Handle errors explicitly; avoid silent failures.
- Design for testability (dependency inversion, small units, clear boundaries).
- Keep code style and formatting consistent across the project.
- Write comments to explain *why*, not obvious *what*.
- When behavior changes, update tests and related documentation together.

## API Rules
- Standard response body format: `{ "code": "...", "message": "...", "data": ... }`
- Do not use `ResponseEntity` for controller responses.
- Keep HTTP status codes semantic (200/201/204 for success, 4xx/5xx for failure).

## Domain Rules
- Use `BigDecimal` for money and quantity values that require precision.
- Do not use `double` or `float` for business calculations.
- Scale and rounding rules must be explicitly defined per domain/API requirement.
- If scale/rounding is not specified, ask a clarifying question before implementation.

## Time Rules
- Store server-side and database timestamps in UTC by default.
- Use ISO-8601 format for API date-time fields.
- If a user-facing timezone is needed, use `Asia/Seoul` unless explicitly specified otherwise.
- If timezone handling is unclear in requirements, ask a clarifying question before implementation.

## API Versioning
- All new REST endpoints should be exposed under `/api/v1`.
- Backward-incompatible changes must be released in a new major API version (e.g. `/api/v2`).
- Do not silently break existing API contracts.

## Error Code Rules
- Use domain-scoped string error codes.
- Prefix policy:
  - `COMMON-*`: cross-cutting/common errors
  - `ORDER-*`: order domain errors
  - `ACCOUNT-*`: account/balance domain errors
- Error codes must remain stable once published.

## Error Handling
- Use centralized exception handling.
- Use string-based error codes (example: `COMMON-400`, `COMMON-404`, `COMMON-500`).

## Validation and Testing
- Add or update tests for any behavior change.
- Ensure `./gradlew test` passes before reporting completion.
- Testing baseline:
  - Domain/service logic: unit tests required
  - Controller contract and validation/error response: web layer tests required
  - Persistence/query behavior: integration tests required when repository logic changes
- No behavior change should be merged without at least one corresponding automated test.
