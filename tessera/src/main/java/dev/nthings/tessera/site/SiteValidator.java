package dev.nthings.tessera.site;

import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.render.CompilationDiagnostic;
import dev.nthings.tessera.render.CompilationLocation;
import dev.nthings.tessera.render.PreparedPage;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Cross-route contracts over prepared page metadata.
final class SiteValidator {
  private SiteValidator() {}

  static void validateViewTransitions(
      Map<String, PreparedPage> preparedByRoute, List<CompilationDiagnostic> diagnostics) {
    var locationsByKey = new LinkedHashMap<TransitionKey, List<CompilationLocation>>();
    for (var entry : preparedByRoute.entrySet()) {
      var route = entry.getKey();
      entry
          .getValue()
          .transitionLocations()
          .forEach(
              (key, location) ->
                  locationsByKey
                      .computeIfAbsent(key, _ -> new ArrayList<>())
                      .add(location.atRoute(route)));
    }
    for (var entry : locationsByKey.entrySet()) {
      var key = entry.getKey();
      var locations = entry.getValue();
      if (locations.size() < 2) {
        var location = locations.getFirst();
        diagnostics.add(
            new CompilationDiagnostic(
                "unshared-transition-key",
                location,
                "TransitionKey '"
                    + key.name()
                    + "' declared on route '"
                    + location.route()
                    + "' must appear on at least two distinct routes site-wide to enable cross-document morphing.",
                "Use the same key on the corresponding element of another route, or remove this transition key."));
      }
    }
  }

  static void validateSiteLinks(
      Map<String, PreparedPage> preparedByRoute,
      Set<String> assetPaths,
      List<CompilationDiagnostic> diagnostics) {
    var publishedPages = new LinkedHashMap<String, PreparedPage>();
    preparedByRoute.forEach(
        (route, page) -> {
          publishedPages.put(route, page);
          publishedPages.put(
              "/" + SiteRoute.outputPathFor(route).toString().replace('\\', '/'), page);
        });
    for (var entry : preparedByRoute.entrySet()) {
      var sourceRoute = entry.getKey();
      var sourcePage = entry.getValue();
      var base = SiteRoute.documentUriFor(sourceRoute);

      for (var link : sourcePage.outgoingLinks().entrySet()) {
        var href = link.getKey();
        var location = link.getValue().atRoute(sourceRoute);
        var reference = URI.create(href);
        if (!reference.isAbsolute() && reference.getRawAuthority() == null) {
          // Empty paths (query-only and fragment-only references) retain the current document.
          var referencePath = Objects.requireNonNull(reference.getRawPath());
          var rawPath =
              Objects.requireNonNull(
                  referencePath.isEmpty()
                      ? base.getPath()
                      : base.resolve(reference).normalize().getPath());
          var fragment = reference.getFragment();
          if (assetPaths.contains(rawPath)) {
            continue;
          }
          var targetRoute = SiteRoute.normalizeRoutePath(rawPath);

          var targetPage = publishedPages.get(targetRoute);
          if (targetPage == null) {
            diagnostics.add(
                new CompilationDiagnostic(
                    "missing-route-or-asset",
                    location,
                    "Route \""
                        + sourceRoute
                        + "\" references unregistered internal route or asset \""
                        + href
                        + "\"",
                    "Register the destination route or asset, or correct the URL relative to this page's published URL."));
            continue;
          }
          if (fragment != null
              && !fragment.isEmpty()
              && !targetPage.registeredDomIds().contains(fragment)) {
            diagnostics.add(
                new CompilationDiagnostic(
                    "missing-route-fragment",
                    location,
                    "Route \""
                        + sourceRoute
                        + "\" links to missing target anchor #"
                        + fragment
                        + " on route \""
                        + targetRoute
                        + "\"",
                    "Correct the fragment or add the matching ID to the destination page."));
          }
        }
      }
    }
  }
}
