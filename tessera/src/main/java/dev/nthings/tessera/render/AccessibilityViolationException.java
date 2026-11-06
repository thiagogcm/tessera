package dev.nthings.tessera.render;

/// Thrown during Phase 2 (`prepaint`) when a semantic HTML5, WCAG accessibility, or Invoker
/// Command target invariant is violated.
public final class AccessibilityViolationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AccessibilityViolationException(String message) {
    super(message);
  }
}
