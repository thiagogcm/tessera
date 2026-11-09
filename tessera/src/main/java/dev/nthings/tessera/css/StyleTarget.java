package dev.nthings.tessera.css;

/// The element or builder a style trait writes its declarations to. [Styled] exposes both methods.
///
/// @param <SELF> concrete element or style builder type
interface StyleTarget<SELF extends StyleTarget<SELF>> {

  /// Returns this target, typed as `SELF`, so style methods chain.
  SELF self();

  /// The declarations this target's style methods write to.
  StyleRefinement style();
}
