package dev.nthings.tessera.compiler;

/// [OutputMode#external(String)]; `base` has no trailing `/`, so the root base is empty.
record ExternalOutput(String base) implements OutputMode {
  ExternalOutput {
    if (!base.startsWith("/") || base.contains("//") || base.contains("..")) {
      throw new IllegalArgumentException(
          "External asset base must be an absolute path such as /assets: " + base);
    }
    if (base.endsWith("/")) {
      base = base.substring(0, base.length() - 1);
    }
  }
}
