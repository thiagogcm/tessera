package dev.nthings.tessera.compiler;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/// How compiled CSS and page scripts are packaged: [#inline()] or [#external(String)].
public final class OutputMode {

  /// The default site asset base, which inline output also uses for browser bundles.
  static final String DEFAULT_EXTERNAL_BASE = "/assets";

  private static final OutputMode INLINE = new OutputMode(null);

  /// `null` for inline output; otherwise the normalized external asset base (empty string for `/`).
  private final @Nullable String assetBase;

  private OutputMode(@Nullable String assetBase) {
    this.assetBase = assetBase;
  }

  /// Inline output: the stylesheet is embedded in a `<style>` element in `<head>`, and each page
  /// script in an inline `<script type="module">`. Browser bundles still publish separate assets.
  public static OutputMode inline() {
    return INLINE;
  }

  /// External output: the page links `<assetBase>/styles-<hash>.css` from `<head>` and loads each
  /// page script from `<assetBase>/scripts/<sanitized id>-<hash>.js`. Hashes derive from the
  /// content, so a changed file is never served stale from a cache. The stylesheet and modules are
  /// returned as [Asset]s, which the caller must publish at their paths.
  ///
  /// `assetBase` must be an absolute URL path such as `/assets`; trailing slashes and `.` path
  /// segments are normalized. Throws IllegalArgumentException if it does not start with `/`, or
  /// contains `//` or `..`.
  public static OutputMode external(String assetBase) {
    if (!assetBase.startsWith("/") || assetBase.contains("//") || assetBase.contains("..")) {
      throw new IllegalArgumentException(
          "External asset base must be an absolute path such as /assets: " + assetBase);
    }
    var normalized = assetBase.replaceAll("/\\.(?=/|$)", "");
    if (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return new OutputMode(normalized);
  }

  boolean isExternal() {
    return assetBase != null;
  }

  @Nullable String assetBase() {
    return assetBase;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof OutputMode mode && Objects.equals(assetBase, mode.assetBase);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(assetBase);
  }

  @Override
  public String toString() {
    return assetBase == null ? "OutputMode.inline()" : "OutputMode.external(" + assetBase + ")";
  }
}
