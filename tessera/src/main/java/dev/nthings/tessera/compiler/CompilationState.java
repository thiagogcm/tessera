package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.internal.view.ViewExpansion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// What one page validates and publishes, once its expansion is linked: its DOM ids and
/// transitions, outgoing links and diagnostics.
final class CompilationState {
  final Set<String> registeredDomIds;
  final Set<String> ambiguousDomIds;
  final Map<TransitionKey, Location> transitionLocations;
  final Map<String, Location> outgoingLinks = new LinkedHashMap<>();
  final List<Diagnostic> diagnostics = new ArrayList<>();
  private final String pageTitle;

  /// Starts from a linked expansion: its identities, transitions and diagnostics.
  CompilationState(String pageTitle, ViewExpansion<?> expansion) {
    this.pageTitle = pageTitle;
    this.registeredDomIds = Collections.unmodifiableSet(expansion.ids());
    this.ambiguousDomIds = Collections.unmodifiableSet(expansion.ambiguousIds());
    var transitions = new LinkedHashMap<TransitionKey, Location>();
    expansion.transitions().forEach((key, location) -> transitions.put(key, location(location)));
    this.transitionLocations = Collections.unmodifiableMap(transitions);
    for (var diagnostic : expansion.diagnostics()) {
      diagnostics.add(atPage(diagnostic, pageTitle));
    }
  }

  Location location(Location view) {
    return atPage(view, pageTitle);
  }

  void report(Location location, String rule, String message, String suggestion) {
    diagnostics.add(new Diagnostic(rule, message, suggestion, location(location)));
  }

  static Location siteLocation(@Nullable String route, @Nullable String page) {
    return new Location(route, page, List.of(), "", null, null);
  }

  static Location atPage(Location location, String title) {
    return new Location(
        location.route(),
        title,
        location.components(),
        location.tag(),
        location.domId(),
        location.source());
  }

  static Location atRoute(Location location, String path) {
    return new Location(
        path,
        location.page(),
        location.components(),
        location.tag(),
        location.domId(),
        location.source());
  }

  static Diagnostic atPage(Diagnostic diagnostic, String title) {
    return new Diagnostic(
        diagnostic.rule(),
        diagnostic.message(),
        diagnostic.fix(),
        atPage(diagnostic.location(), title));
  }

  static Diagnostic atRoute(Diagnostic diagnostic, String path) {
    return new Diagnostic(
        diagnostic.rule(),
        diagnostic.message(),
        diagnostic.fix(),
        atRoute(diagnostic.location(), path));
  }
}
