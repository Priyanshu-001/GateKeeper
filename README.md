# RateLimiter

A Kotlin rate-limiting library focused on clean architecture, explicit domain modeling, and concurrency-safe execution.

## Highlights

- Multi-dimensional rate limiting per resource (`userId`, `ip`, etc.)
- Supports keyed and global limits
- Multiple algorithms behind one interface:
  - Fixed Window
  - Sliding Window
  - Token Bucket
- Clear decision output: `Allowed` or `Denied` with exact violation details
- Thread-safe state handling for concurrent requests
- Config-driven behavior through `RateLimitContext` and `ResourceRules`

## Design Choices

### Sealed classes used intentionally

This project uses sealed classes to model constrained domain states and make control flow exhaustive:

- `RateLimitResult`: `Allowed` or `Denied`
- `Denied.Violation`: `Global` or `Dimension`
- `ResourceRules.AlgorithmConfig`: fixed set of supported algorithms
- API `Identifier` and internal `core.models.Identifier` are separated for clean boundaries

Benefits:

- Prevents invalid states
- Improves readability and maintainability
- Gives compiler-backed exhaustiveness guarantees


## Project Structure

- `src/main/kotlin/api`
  - `RateLimiter`, `RateLimitResult`, `RateLimitContext`, `ResourceRules`, `Identifier`
- `src/main/kotlin/core`
  - `RateLimiterImpl`, `RateLimiterAlgorithm`, `RateLimiterAlgorithmFactory`
  - `strategies/` for concrete algorithms
  - `models/Identifier` for internal bucket identity

## How Requests Are Evaluated

1. Rules are validated at startup.
2. Rules are registered into an internal resource registry.
3. For each incoming request:
   - resource config is resolved
   - global limiter is checked (if configured)
   - keyed dimensions are checked
4. Returns:
   - `Allowed`
   - `Denied` with specific `Violation` (resource, key, dimension/global)

## Concurrency Model

- Per-bucket state is stored in `ConcurrentHashMap`
- Mutations are synchronized per bucket state object
- Avoids a single global lock and keeps contention localized to hot keys

## Quick Build & Test

```powershell
.\gradlew.bat clean build
.\gradlew.bat test
```

## Test Coverage (Current)

- Allows then denies for same request with multiple dimensions
- Denies and then allows again after fixed-window reset

## Example Rule Setup (Conceptual)

Resource `checkout`:

- `userId`: fixed window, `limit=1`, `window=60s`
- `ip`: fixed window, `limit=2`, `window=60s`

Behavior:

- First request (`userId=u-1`, `ip=10.0.0.1`) -> `Allowed`
- Immediate second identical request -> `Denied` (user dimension violation)

## Roadmap

- Inject `Clock` end-to-end for deterministic time-based tests
- Add pluggable external state backend (Redis/DB)
- Add metrics and observability hooks
- Add bucket eviction/cleanup strategy for long-running processes
