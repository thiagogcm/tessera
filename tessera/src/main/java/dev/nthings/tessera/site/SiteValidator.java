package dev.nthings.tessera.site;

import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.render.AccessibilityViolationException;
import dev.nthings.tessera.render.PreparedPage;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
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
    for (var entry : preparedByRoute.entrySet()) {
      var sourceRoute = entry.getKey();
      var sourcePage = entry.getValue();

      for (var href : sourcePage.outgoingHrefs()) {
        // Same-page `#fragment` links are already validated by PageCompiler.prepare.
        if (href.startsWith("/") && !href.startsWith("//")) {
          var hashIdx = href.indexOf('#');
          var rawPath = hashIdx >= 0 ? href.substring(0, hashIdx) : href;
          var fragment = hashIdx >= 0 ? href.substring(hashIdx + 1) : "";
          if (assetPaths.contains(rawPath)) {
            continue;
          }
          var targetRoute = SiteRoute.normalizeRoutePath(rawPath);

          var targetPage = preparedByRoute.get(targetRoute);
          if (targetPage == null) {
            throw new AccessibilityViolationException(
                "Route \""
                    + sourceRoute
                    + "\" references unregistered internal route or asset \""
                    + href
                    + "\"");
          }
          if (!fragment.isEmpty() && !targetPage.registeredDomIds().contains(fragment)) {
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
