package dev.nthings.tessera.site;

import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PreparedPage;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/// Cross-route contracts over prepared page metadata.
final class SiteValidator {
  private SiteValidator() {}

  static void validateViewTransitions(Map<String, PreparedPage> preparedByRoute) {
    var routesByKey = new LinkedHashMap<TransitionKey, Set<String>>();
    for (var entry : preparedByRoute.entrySet()) {
      var routePath = entry.getKey();
      for (var key : entry.getValue().transitionKeys()) {
        routesByKey.computeIfAbsent(key, _ -> new LinkedHashSet<>()).add(routePath);
      }
    }
    for (var entry : routesByKey.entrySet()) {
      var key = entry.getKey();
      var routePaths = entry.getValue();
      if (routePaths.size() < 2) {
        throw new AccessibilityViolationException(
            "TransitionKey '"
                + key.name()
                + "' declared on route '"
                + routePaths.iterator().next()
                + "' must appear on at least two distinct routes site-wide to enable cross-document morphing.");
      }
    }
  }

  static void validateSiteLinks(Map<String, PreparedPage> preparedByRoute, Set<String> assetPaths) {
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

      for (var href : sourcePage.outgoingHrefs()) {
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
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" references unregistered internal route or asset \""
                    + href
                    + "\"");
          }
          if (fragment != null
              && !fragment.isEmpty()
              && !targetPage.registeredDomIds().contains(fragment)) {
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" links to missing target anchor #"
                    + fragment
                    + " on route \""
                    + targetRoute
                    + "\"");
          }
        }
      }
    }
  }
}
