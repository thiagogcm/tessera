package dev.nthings.tessera.compiler;

import dev.nthings.tessera.document.WasmBundle;
import dev.nthings.tessera.document.WasmBundle.Kind;
import dev.nthings.tessera.document.WasmBundle.Resource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/// Imports output from the pinned GraalVM Web Image toolchain built with `-H:-AutoRunVM`. The
/// compiler consumes the returned bundle without invoking the toolchain.
public final class GraalWebImage {
  private static final String LOADER =
      """
      async function wasmInstantiate(config, args) {
          const wasmPath = config.wasm_path || runtime.getCurrentFile() + ".wasm";
          const file = await runtime.fetchData(wasmPath);
          const result = await WebAssembly.instantiate(file, wasmImports);
      """;

  private static final String VERIFIED_LOADER =
      """
      async function wasmInstantiate(config, args) {
          const file = config.tesseraWasmBytes;
          if (!(file instanceof ArrayBuffer)) throw new Error("Tessera requires a verified WebAssembly image.");
          const result = await WebAssembly.instantiate(file, wasmImports);
      """;

  private static final String EXPORT =
      """
      (function() {
      globalThis["GraalVM"] = GraalVM;
      })();\
      """;

  private GraalWebImage() {}

  /// Reads the classic `js` launcher and its corresponding `wasm` image. Only the launcher and
  /// binary are imported. Throws IllegalArgumentException for an unsupported launcher layout and
  /// UncheckedIOException if the supplied files cannot be read.
  public static WasmBundle read(Path launcher, Path image) {
    try {
      var source = Files.readString(launcher);
      var position = source.indexOf(LOADER);
      if (position < 0
          || position != source.lastIndexOf(LOADER)
          || !source.stripTrailing().endsWith(EXPORT)
          || !source.contains("GraalVM.Config = Config;")
          || !source.contains("data.wasm = await wasmInstantiate(config, vmArgs);")) {
        throw new IllegalArgumentException(
            "Unsupported GraalVM Web Image launcher layout: "
                + launcher
                + ". Build with the pinned Web Image toolchain and -H:-AutoRunVM.");
      }
      var verified =
          source.substring(0, position)
              + VERIFIED_LOADER
              + source.substring(position + LOADER.length());
      var js = "launcher.js";
      var wasm = "image.wasm";
      return new WasmBundle(
          js,
          wasm,
          List.of(
              Resource.of(js, Kind.LAUNCHER, verified.getBytes(StandardCharsets.UTF_8), wasm),
              Resource.of(wasm, Kind.WASM, Files.readAllBytes(image))));
    } catch (IOException failure) {
      throw new UncheckedIOException(
          "Cannot read GraalVM Web Image " + launcher + " and " + image, failure);
    }
  }
}
