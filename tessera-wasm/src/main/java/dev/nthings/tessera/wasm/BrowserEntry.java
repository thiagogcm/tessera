package dev.nthings.tessera.wasm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

/// Mounts the hosts a page declares for one compiled entry point.
///
/// A host element declares the launcher of its entry with `data-tessera-entry` and the view to
/// mount with `data-tessera-view` (its id by default). The bootstrap loads each entry once and
/// passes that entry's hosts to `main` as `host=view` arguments, in document order.
public final class BrowserEntry {
  private BrowserEntry() {}

  /// Mounts every host declared in `args` with the function `views` maps its view name to, which
  /// receives the host id, and returns the mounted roots in declaration order. A host whose view
  /// this entry does not provide, or whose mount fails, is reported and keeps its fallback
  /// content; the other hosts still mount.
  public static List<BrowserRoot> start(
      String[] args, Map<String, Function<String, BrowserRoot>> views) {
    var roots = new ArrayList<BrowserRoot>();
    for (var declaration : args) {
      var separator = declaration.indexOf('=');
      if (separator <= 0 || separator == declaration.length() - 1) {
        Dom.reportError("Invalid browser mount declaration '" + declaration + "': use host=view");
        continue;
      }
      var host = declaration.substring(0, separator);
      var view = declaration.substring(separator + 1);
      var mount = views.get(view);
      if (mount == null) {
        Dom.reportError(
            "Browser host '"
                + host
                + "' declares view '"
                + view
                + "', which this entry does not provide; available views: "
                + new TreeSet<>(views.keySet()));
        continue;
      }
      Failures.run("Mounting view '" + view + "'", host, () -> roots.add(mount.apply(host)));
    }
    return roots;
  }
}
