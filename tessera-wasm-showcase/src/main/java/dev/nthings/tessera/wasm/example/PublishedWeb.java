package dev.nthings.tessera.wasm.example;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.compiler.CompiledSite;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.OutputMode;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.wasm.GraalWebImage;
import java.nio.file.Path;

/// Publishes the already built showcase entries with the public document API.
public final class PublishedWeb {
  private PublishedWeb() {}

  public static void main(String[] args) {
    var build = Path.of(args[0]);
    SiteWriter.writeToDirectory(compile(build, "/assets/demo"), build.resolve("published-web"));
  }

  public static CompiledSite compile(Path build, String assetBase) {
    var demo =
        GraalWebImage.read(build.resolve("tessera-demo.js"), build.resolve("tessera-demo.js.wasm"));
    var counter =
        GraalWebImage.read(
            build.resolve("tessera-second.js"), build.resolve("tessera-second.js.wasm"));
    var home =
        DocumentElement.of(Elements.main().child(h1().text("Published Web Images")))
            .child(
                Documents.wasmMount("app", "app", demo, a("/guide/").text("Open the static guide")))
            .child(
                Documents.wasmMount(
                    "notes", "secondary", demo, Field.of("Fallback notes", Forms.text("notes"))))
            .child(
                Documents.wasmMount(
                    "counter", "counter", counter, p().text("Counter is unavailable.")));
    var guide =
        DocumentElement.of(Elements.main().child(h1().text("Static guide")))
            .child(
                Documents.wasmMount(
                    "broken", "broken", demo, a("/").text("Return to the static home")))
            .child(
                Documents.wasmMount(
                    "counter", "counter", counter, p().text("Counter is unavailable.")));
    return Compiler.compile(
        SiteDefinition.builder()
            .outputMode(OutputMode.external(assetBase))
            .route(new SiteRoute("/", new PageDocument("Published Web Images", home)))
            .route(new SiteRoute("/guide", new PageDocument("Static guide", guide)))
            .build());
  }
}
