package dev.nthings.tessera.wasm;

import dev.nthings.tessera.wasm.example.PublishedWeb;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The distribution the build writes to `published-web`, checked as a static host would serve it.
@DisplayName("Bundle distribution")
class BundleDistributionIT {
  private static final Pattern CSS_URL = Pattern.compile("url\\(\\s*['\"]?([^'\")]+)");

  private final Path build = Path.of(System.getProperty("wasm.build"));
  private final Path site = build.resolve("published-web");

  @Test
  @DisplayName("every page reference resolves to a published file whose integrity matches")
  void referencesResolveToVerifiedFiles() throws IOException {
    var problems = new ArrayList<String>();
    var referenced = new HashSet<Path>();
    var pages = files().stream().filter(file -> file.toString().endsWith(".html")).toList();
    Assertions.assertThat(pages).isNotEmpty();
    for (var page : pages) {
      var html = Jsoup.parse(page.toFile());
      var references = new ArrayList<Reference>();
      for (var element : html.select("[src], [href]")) {
        var url = element.hasAttr("src") ? element.attr("src") : element.attr("href");
        references.add(new Reference(url, element.attr("integrity")));
      }
      for (var host : html.select("[data-tessera-bundle]")) {
        references.add(
            new Reference(host.attr("data-tessera-entry"), host.attr("data-tessera-integrity")));
        var manifest = host.attr("data-tessera-resources").split(" ");
        for (var index = 0; index + 2 < manifest.length; index += 3) {
          references.add(new Reference(manifest[index + 1], manifest[index + 2]));
        }
      }
      for (var reference : references) {
        check(page, reference, referenced, problems);
      }
    }
    // Response headers a deployment applies to the pages can reference files too.
    var compiled = PublishedWeb.compile(build, "/assets/demo");
    for (var route : compiled.pageHeaders().entrySet()) {
      var page = site.resolve(route.getKey().substring(1)).resolve("index.html");
      var rules = route.getValue().get("Speculation-Rules");
      if (rules != null) {
        check(page, new Reference(rules.replace("\"", ""), ""), referenced, problems);
      }
    }
    for (var stylesheet :
        files().stream().filter(file -> file.toString().endsWith(".css")).toList()) {
      var matcher = CSS_URL.matcher(Files.readString(stylesheet));
      while (matcher.find()) {
        check(stylesheet, new Reference(matcher.group(1), ""), referenced, problems);
      }
    }
    Assertions.assertThat(problems).isEmpty();
    // Nothing else is published but the site's search index: no compiler intermediates or stray
    // build outputs.
    Assertions.assertThat(files())
        .filteredOn(file -> !file.toString().endsWith(".html"))
        .filteredOn(file -> !file.equals(site.resolve("search-index.json")))
        .allSatisfy(file -> Assertions.assertThat(referenced).contains(file));
  }

  private record Reference(String url, String integrity) {}

  private void check(Path from, Reference reference, Set<Path> referenced, List<String> problems)
      throws IOException {
    var url = reference.url();
    if (url.isEmpty() || url.startsWith("#") || url.startsWith("data:")) {
      return;
    }
    if (url.contains("//") || url.contains(":")) {
      problems.add(site.relativize(from) + " references a remote URL: " + url);
      return;
    }
    var path = url.split("[?#]", 2)[0];
    var target =
        path.startsWith("/")
            ? site.resolve(path.substring(1))
            : from.getParent().resolve(path).normalize();
    if (path.endsWith("/")) {
      target = target.resolve("index.html");
    }
    if (!Files.isRegularFile(target)) {
      problems.add(site.relativize(from) + " references a missing file: " + url);
      return;
    }
    referenced.add(target);
    var integrity = reference.integrity();
    if (!integrity.isEmpty() && !integrity.equals(sha384(Files.readAllBytes(target)))) {
      problems.add(site.relativize(from) + " has a mismatched integrity for " + url);
    }
  }

  private List<Path> files() throws IOException {
    try (var paths = Files.walk(site)) {
      return paths.filter(Files::isRegularFile).toList();
    }
  }

  private static String sha384(byte[] content) {
    try {
      return "sha384-"
          + Base64.getEncoder()
              .encodeToString(MessageDigest.getInstance("SHA-384").digest(content));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
