package dev.nthings.tessera.css;

/// The element or builder a style trait writes its declarations to.
///
/// @param <SELF> concrete element or style builder type
public interface StyleTarget<SELF extends StyleTarget<SELF>> {

  SELF self();

  StyleRefinement style();
}
