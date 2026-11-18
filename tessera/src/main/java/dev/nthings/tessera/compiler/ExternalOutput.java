package dev.nthings.tessera.compiler;

/// [OutputMode#external(String)]; `base` has no trailing `/`, so the root base is empty.
record ExternalOutput(String base) implements OutputMode {
  /// The default site asset base, which inline output also uses for browser bundles.
  static final String DEFAULT_BASE = "/assets";

  ExternalOutput {
    if (!base.startsWith("/") || base.contains("//") || base.contains("..")) {
      throw new IllegalArgumentException(
          "External asset base must be an absolute path such as /assets: " + base);
    }
    base = base.replaceAll("/\\.(?=/|$)", "");
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
  }
}
