package dev.nthings.tessera.compiler;

import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.document.WasmBundle;
import dev.nthings.tessera.document.WasmBundle.Kind;
import dev.nthings.tessera.document.WasmBundle.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/// Resolves one compilation's bundle resources to immutable published URLs.
final class BundlePublication {
  static final String CSP =
      "default-src 'self'; script-src 'self' 'wasm-unsafe-eval'; style-src 'self'; object-src 'none'";
  static final Map<String, String> HEADERS = Map.of("Content-Security-Policy", CSP);

  /// Marks a mount host; the bootstrap module starts every element carrying it.
  static final String MOUNT_ATTRIBUTE = "data-tessera-bundle";

  private static final Pattern SAFE_BASE = Pattern.compile("(?:/[A-Za-z0-9._-]+)*");

  record File(Asset asset, String integrity) {}

  record Entry(File launcher, String image, String resources, List<Asset> assets) {}

  private final String base;
  private @Nullable File bootstrap;
  private final Map<WasmBundle, Entry> entries = new ConcurrentHashMap<>();

  BundlePublication(OutputMode output) {
    var externalBase = output.assetBase();
    base = externalBase != null ? externalBase : OutputMode.DEFAULT_EXTERNAL_BASE;
  }

  /// The shared bootstrap module, published once per compilation on first use.
  synchronized File bootstrap() {
    var published = bootstrap;
    if (published == null) {
      published = file(Bootstrap.RESOURCE);
      bootstrap = published;
    }
    return published;
  }

  private static final class Bootstrap {
    private static final Resource RESOURCE =
        Resource.of(
            "bootstrap.js",
            Kind.LAUNCHER,
            PageScript.fromResource("wasm-bootstrap", BundlePublication.class, "wasm-bootstrap.js")
                .module()
                .getBytes(StandardCharsets.UTF_8));
  }

  Entry entry(WasmBundle bundle) {
    return entries.computeIfAbsent(bundle, this::publish);
  }

  private Entry publish(WasmBundle bundle) {
    var launcher = file(Objects.requireNonNull(bundle.resources().get(bundle.launcher())));
    var assets = new ArrayList<Asset>();
    assets.add(launcher.asset());
    var manifest = new ArrayList<String>();
    for (var resource :
        bundle.resources().values().stream()
            .sorted(Comparator.comparing(Resource::name))
            .toList()) {
      if (resource.kind() != Kind.LAUNCHER) {
        var published = file(resource);
        assets.add(published.asset());
        manifest.add(resource.name());
        manifest.add(published.asset().path());
        manifest.add(published.integrity());
      }
    }
    return new Entry(launcher, bundle.image(), String.join(" ", manifest), List.copyOf(assets));
  }

  private File file(Resource resource) {
    if (!SAFE_BASE.matcher(base).matches()) {
      throw new IllegalArgumentException("A Wasm bundle requires a URL-safe asset base: " + base);
    }
    var integrity = resource.integrity();
    var hash =
        HexFormat.of()
            .formatHex(Base64.getDecoder().decode(integrity.substring("sha384-".length())));
    var path = base + "/wasm/" + hash + resource.kind().extension();
    return new File(Asset.of(path, resource.content()), integrity);
  }
}
