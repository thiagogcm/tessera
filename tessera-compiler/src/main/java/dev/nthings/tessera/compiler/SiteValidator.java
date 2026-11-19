package dev.nthings.tessera.compiler;

import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.TransitionKey;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Cross-route contracts over prepared page metadata.
final class SiteValidator {
  /// A fragment browsers scroll to the top of the document when no element has it as its id.
  private static final String TOP_FRAGMENT = "top";

  private SiteValidator() {}

  /// `path` with the `..` segments that climb above the root dropped, as browsers resolve them.
  private static String withinRoot(String path) {
    var resolved = path;
    while (resolved.startsWith("/../") || resolved.equals("/..")) {
      resolved = resolved.substring(3);
    }
    return resolved.isEmpty() ? "/" : resolved;
  }

  static void validateViewTransitions(
      Map<String, PreparedPage> preparedByRoute, List<Diagnostic> diagnostics) {
    var locationsByKey = new LinkedHashMap<TransitionKey, List<Location>>();
    for (var entry : preparedByRoute.entrySet()) {
      var route = entry.getKey();
      entry
          .getValue()
          .transitionLocations()
          .forEach((key, location) ->
              locationsByKey
                  .computeIfAbsent(key, _ -> new ArrayList<>())
                  .add(CompilationState.atRoute(location, route)));
    }
    for (var entry : locationsByKey.entrySet()) {
      var key = entry.getKey();
      var locations = entry.getValue();
      if (locations.size() < 2) {
        var location = locations.getFirst();
        diagnostics.add(
            new Diagnostic(
                "unshared-transition-key",
                "TransitionKey '"
                    + key.name()
                    + "' declared on route '"
                    + location.route()
                    + "' must appear on at least two distinct routes site-wide to enable cross-document morphing.",
                "Use the same key on the corresponding element of another route, or remove this transition key.",
                location));
      }
    }
  }

  static void validateSiteLinks(
      Map<String, PreparedPage> preparedByRoute,
      Set<String> assetPaths,
      List<Diagnostic> diagnostics) {
    var publishedPages = new LinkedHashMap<String, PreparedPage>();
    preparedByRoute.forEach((route, page) -> {
      publishedPages.put(route, page);
      publishedPages.put(SiteRoute.outputUrlPathFor(route), page);
    });
    for (var entry : preparedByRoute.entrySet()) {
      var sourceRoute = entry.getKey();
      var sourcePage = entry.getValue();
      var base = SiteRoute.documentUriFor(sourceRoute);

      for (var link : sourcePage.outgoingLinks().entrySet()) {
        var href = link.getKey();
        var location = CompilationState.atRoute(link.getValue(), sourceRoute);
        var reference = URI.create(href);
        if (!reference.isAbsolute() && reference.getRawAuthority() == null) {
          // Empty path (query or fragment only) means the current document.
          var referencePath = Objects.requireNonNull(reference.getRawPath());
          var rawPath =
              referencePath.isEmpty()
                  ? Objects.requireNonNull(base.getPath())
                  : withinRoot(
                      Objects.requireNonNull(base.resolve(reference).normalize().getPath()));
          var fragment = reference.getFragment();
          if (assetPaths.contains(rawPath)) {
            continue;
          }
          var targetRoute = SiteRoute.normalizePath(rawPath);

          var targetPage = publishedPages.get(targetRoute);
          if (targetPage == null) {
            diagnostics.add(
                new Diagnostic(
                    "missing-route-or-asset",
                    "Route \""
                        + sourceRoute
                        + "\" references unregistered internal route or asset \""
                        + href
                        + "\"",
                    "Register the destination route or asset, or correct the URL relative to this page's published URL.",
                    location));
            continue;
          }
          if (fragment != null
              && !fragment.isEmpty()
              && !fragment.equalsIgnoreCase(TOP_FRAGMENT)
              && !targetPage.registeredDomIds().contains(fragment)) {
            diagnostics.add(
                new Diagnostic(
                    "missing-route-fragment",
                    "Route \""
                        + sourceRoute
                        + "\" links to missing target anchor #"
                        + fragment
                        + " on route \""
                        + targetRoute
                        + "\"",
                    "Correct the fragment or add the matching ID to the destination page.",
                    location));
          }
        }
      }
    }
  }
}
