package dev.nthings.tessera.compiler;

/// How compiled CSS and page scripts are packaged: [Inline] or [External].
public sealed interface OutputMode permits OutputMode.Inline, OutputMode.External {

  /// The default site asset base, which inline output also uses for browser bundles.
  String DEFAULT_EXTERNAL_BASE = "/assets";

  /// Inline output: the stylesheet is embedded in a `<style>` element in `<head>`, and each page
  /// script in an inline `<script type="module">`. Browser bundles still publish separate assets.
  record Inline() implements OutputMode {
    private static final Inline INSTANCE = new Inline();
  }

  /// External output: the page links `<assetBase>/styles-<hash>.css` from `<head>` and loads each
  /// page script from `<assetBase>/scripts/<sanitized id>-<hash>.js`. Hashes derive from the
  /// content, so a changed file is never served stale from a cache. The stylesheet and modules are
  /// returned as [Asset]s, which the caller must publish at their paths.
  ///
  /// `assetBase` must be an absolute URL path such as `/assets`; trailing slashes and `.` path
  /// segments are normalized (the root path `/` normalizes to `""`). Throws
  /// IllegalArgumentException if it does not start with `/`, or contains `//` or `..`.
  record External(String assetBase) implements OutputMode {
    public External {
      if (!assetBase.startsWith("/") || assetBase.contains("//") || assetBase.contains("..")) {
        throw new IllegalArgumentException(
            "External asset base must be an absolute path such as /assets: " + assetBase);
      }
      var normalized = assetBase.replaceAll("/\\.(?=/|$)", "");
      if (normalized.endsWith("/")) {
        normalized = normalized.substring(0, normalized.length() - 1);
      }
      assetBase = normalized;
    }
  }

  /// Returns inline output mode.
  static Inline inline() {
    return Inline.INSTANCE;
  }

  /// Returns external output mode under `assetBase`.
  static External external(String assetBase) {
    return new External(assetBase);
  }
}
