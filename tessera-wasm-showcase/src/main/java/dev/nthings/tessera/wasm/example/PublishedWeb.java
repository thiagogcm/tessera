package dev.nthings.tessera.wasm.example;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.h1;
import static dev.nthings.tessera.core.Elements.p;

import dev.nthings.tessera.compiler.CompiledSite;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.GraalWebImage;
import dev.nthings.tessera.compiler.OutputMode;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.compiler.SiteDefinition;
import dev.nthings.tessera.compiler.SiteRoute;
import dev.nthings.tessera.compiler.SiteWriter;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.WasmBundle;
import java.nio.file.Path;
import java.util.List;

/// Publishes the showcase with the public document API: the home and guide pages, plus one page per
/// feature that mounts only that feature's views from the two Web Image entries the build produces.
/// The browser suite serves exactly these pages.
public final class PublishedWeb {
  private PublishedWeb() {}

  public static void main(String[] args) {
    var build = Path.of(args[0]);
    SiteWriter.writeToDirectory(compile(build, "/assets/demo"), build.resolve("published-web"));
  }

  /// Compiles the site from the entries built in `build`, publishing bundle files under
  /// `assetBase`.
  public static CompiledSite compile(Path build, String assetBase) {
    var demo =
        GraalWebImage.read(build.resolve("tessera-demo.js"), build.resolve("tessera-demo.js.wasm"));
    var second =
        GraalWebImage.read(
            build.resolve("tessera-second.js"), build.resolve("tessera-second.js.wasm"));
    var counter = mount("counter", "counter", second, p().text("Counter is unavailable."));
    return Compiler.compile(
        SiteDefinition.builder()
            .outputMode(OutputMode.external(assetBase))
            .route(
                page(
                    "/",
                    "Published Web Images",
                    mount("app", "app", demo, a("/guide/").text("Open the static guide")),
                    mount("notes", "notes", demo, Field.of("Fallback notes", Forms.text("notes"))),
                    counter))
            .route(
                page(
                    "/guide",
                    "Static guide",
                    mount("broken", "broken", demo, a("/").text("Return to the static home")),
                    counter))
            .route(
                page(
                    "/forms",
                    "Forms",
                    mount("forms", "forms", demo, p().text("The profile form is loading.")),
                    mount("defaults", "defaults", demo, p().text("The defaults form is loading.")),
                    mount("shipping", "shipping", demo, p().text("Shipping is loading."))))
            .route(
                page(
                    "/forms/static", "Static forms", DocumentContent.of(DefaultsDemo.staticForm())))
            .route(page("/lists", "Lists", mount("lists", "lists", demo, p().text("Loading."))))
            .route(
                page(
                    "/lists/static",
                    "Static lists",
                    DocumentContent.of(KeyedListDemo.content(List.of("a", "b", "c"), true, false))))
            .route(page("/tabs", "Tabs", mount("tabs", "tabs", demo, p().text("Loading."))))
            .route(
                page(
                    "/dialogs", "Dialogs", mount("dialogs", "dialogs", demo, p().text("Loading."))))
            .route(page("/search", "Search", mount("search", "search", demo, p().text("Loading."))))
            .route(page("/events", "Events", mount("events", "events", demo, p().text("Loading."))))
            .route(
                page(
                    "/services",
                    "Services",
                    mount("services", "services", demo, p().text("Loading."))))
            .route(
                page(
                    "/lifecycle",
                    "Lifecycle",
                    mount("toolbar", "toolbar", demo, p().text("Loading.")),
                    mount("lifetime", "lifetime", demo, p().text("Loading.")),
                    mount("closure", "closure", demo, p().text("Loading.")),
                    mount("activity", "activity", demo, p().text("Loading."))))
            .route(
                page(
                    "/failures",
                    "Failures",
                    DocumentContent.of(
                        p().id(ElementRef.page("page-note")).text("A note owned by the page")),
                    mount("schedule", "schedule", demo, p().text("Loading.")),
                    mount("collisions", "collisions", demo, p().text("Loading.")),
                    counter))
            .build());
  }

  private static DocumentContent mount(
      String host, String view, WasmBundle bundle, IntoElement fallback) {
    return Documents.wasmMount(host, view, bundle, fallback);
  }

  private static SiteRoute page(String path, String title, DocumentContent... content) {
    var document = DocumentElement.of(Elements.main().child(h1().text(title))).children(content);
    return new SiteRoute(path, new PageDocument(title, document));
  }
}
