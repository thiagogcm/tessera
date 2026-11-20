# Roadmap

This roadmap tracks upcoming capabilities and release readiness criteria for Tessera. All currently implemented features are documented directly in the [user guides](../README.md#documentation) and [Architecture](architecture.md).

## Upcoming work

### Streaming host requests (`tessera-wasm`)

Extend `Host` in `dev.nthings.tessera.wasm` with incremental response streaming scoped to a `BrowserRoot` or effect `Lifetime`:

- Deliver decoded chunks incrementally to application callbacks and trigger bounded re-renders as chunks arrive.
- Cancel the underlying native reader and abort the HTTP request automatically on explicit cancellation, effect disposal, or root closure.
- Guarantee that late chunks arriving after cancellation or disposal never invoke application callbacks or invalidate the root.
- Verify incremental DOM updates, mid-stream cancellation, network errors, and post-close isolation in Chromium and Firefox under the full deployment Content Security Policy.

## Release readiness criteria

Before tagging the first stable release (`0.1.0`) covering both static compilation and live browser roots, verify a representative application (combining forms, validation, adaptive widgets, and a filterable keyed list) through the complete published distribution:

| Area | Required evidence |
| --- | --- |
| Static compilation | `just verify`, `just no-suppress-warnings`, `just site`, and `just guide` pass cleanly |
| Browser runtime | `just wasm-verify` passes in Chromium and Firefox covering interaction, keyed identity, IME composition, adaptive widgets, disposal, streaming cancellation, and failure recovery |
| Dynamic accessibility | Keyboard navigation, accessibility-tree checks, and axe-core WCAG A/AA audits pass after live DOM mutations, including reduced-motion and forced-colors modes |
| Published distribution | Published sites contain every referenced resource with valid SHA-384 integrity, exclude compiler intermediates, load zero unexpected remote dependencies, and produce zero CSP violations |
| Documentation | Every guide matches shipped APIs, module boundaries, and runnable commands |

## Acceptance rules for changes

Every upcoming feature and refactor must satisfy these rules:

- Read existing tests first and extend shared fixtures before creating a new suite.
- Assert observable HTML, DOM, accessibility, and deployment behavior; avoid pinning private class names, internal tree layouts, or exact diagnostic prose.
- Verify JPMS module visibility through real consumer compilation on the module path.
- Verify browser interaction, lifecycle, and CSP behavior against HTTP-served builds in Chromium and Firefox; JVM unit tests alone are not sufficient.
- Replace superseded APIs directly without leaving compatibility shims or parallel legacy paths.
