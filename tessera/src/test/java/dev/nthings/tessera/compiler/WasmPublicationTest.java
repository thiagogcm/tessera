package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.template;
import static dev.nthings.tessera.core.Elements.textarea;
import static dev.nthings.tessera.document.Documents.wasmMount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.WasmBundle;
import dev.nthings.tessera.document.WasmBundle.Kind;
import dev.nthings.tessera.document.WasmBundle.Resource;
import dev.nthings.tessera.support.RenderedPage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Wasm publication")
class WasmPublicationTest {
  private static final byte[] EMPTY_MODULE = {0, 'a', 's', 'm', 1, 0, 0, 0};

  @Test
  @DisplayName("validates the complete bundle graph and supplied digests")
  void rejectsIncompleteOrChangedBundles() {
    var image = Resource.of("main.wasm", Kind.WASM, EMPTY_MODULE);
    var launcher = launcher("main.wasm", "missing.bin");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new WasmBundle("run.js", "main.wasm", List.of(launcher, image)))
        .withMessageContaining("Missing bundle resource: missing.bin");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new WasmBundle("run.js", "main.wasm", List.of(launcher(), image)))
        .withMessageContaining("Unreachable bundle resource: main.wasm");
    assertThatIllegalArgumentException()
        .isThrownBy(() ->
            new Resource(image.name(), image.kind(), new byte[] {1}, image.integrity(), List.of()))
        .withMessageContaining("SHA-384 integrity mismatch");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> Resource.of("invalid.wasm", Kind.WASM, new byte[] {1}))
        .withMessageContaining("Invalid WebAssembly header");
  }

  @Test
  @DisplayName("publishes all reachable resources and escapes the mount name")
  void publishesGraphAndEscapesMountAttributes() {
    var bundle = bundle();
    var view = "edit\"<&=view";
    var id = "editor\"<&";
    var compiled =
        Compiler.compile(
            new PageDocument("Mount", wasmMount(id, view, bundle, p().text("Fallback"))),
            Theme.defaults(),
            OutputMode.external("/assets/./nested/"));
    var page = RenderedPage.of(compiled);
    var host = page.elementById(id);
    assertThat(host.attr("data-tessera-view")).isEqualTo(view);
    assertThat(host.text()).isEqualTo("Fallback");
    assertThat(page.elements("script[integrity][crossorigin=anonymous]")).hasSize(1);
    assertThat(compiled.assets()).hasSize(5);
    assertThat(compiled.assets()).allMatch(asset -> asset.path().startsWith("/assets/nested/"));
    assertThat(compiled.assets())
        .filteredOn(asset -> asset.path().endsWith(".wasm"))
        .singleElement()
        .satisfies(asset -> {
          assertThat(asset.contentType()).isEqualTo("application/wasm");
          assertThat(asset.content()).containsExactly(EMPTY_MODULE);
          assertThat(host.attr("data-tessera-resources")).contains(asset.path());
        });
    assertThat(compiled.assets())
        .filteredOn(asset -> asset.path().endsWith(".bin"))
        .singleElement()
        .satisfies(asset -> assertThat(asset.textContent()).isEqualTo("data"));
    assertThat(compiled.responseHeaders())
        .containsEntry("Content-Security-Policy", BundlePublication.CSP);
  }

  @Test
  @DisplayName("shared bundles publish once and the deployment policy applies to mount pages")
  void deduplicatesAcrossPagesAndKeepsPolicyLocal() {
    var home =
        DocumentElement.of(main())
            .child(wasmMount("one", "a", bundle(), p().text("Home")))
            .child(wasmMount("two", "b", bundle(), p().text("More")));
    var site =
        Compiler.compile(
            SiteDefinition.builder()
                .outputMode(OutputMode.external("/"))
                .route(new SiteRoute("/", new PageDocument("Mount", home)))
                .route(
                    new SiteRoute(
                        "/share",
                        new PageDocument(
                            "Share", wasmMount("one", "a", bundle(), p().text("Share")))))
                .route(new SiteRoute("/plain", new PageDocument("Plain", main().text("Plain"))))
                .build());
    assertThat(site.files().keySet()).filteredOn(path -> path.startsWith("/wasm/")).hasSize(4);
    assertThat(site.pageHeaders().keySet()).containsExactly("/", "/share");
    var homeHeaders = site.pageHeaders().get("/");
    assertThat(site.files().values())
        .filteredOn(asset -> asset.contentType().equals("application/speculationrules+json"))
        .singleElement()
        .satisfies(asset ->
            assertThat(homeHeaders).containsEntry("Speculation-Rules", '"' + asset.path() + '"'));
    var mounted = RenderedPage.of(site, "/");
    assertThat(mounted.elements("script[integrity]")).hasSize(1);
    assertThat(mounted.elements("script[type=speculationrules]")).isEmpty();
    assertThat(RenderedPage.of(site, "/plain").elements("script[type=speculationrules]"))
        .hasSize(1);
  }

  @Test
  @DisplayName("mount fallback remains validated document content and cannot hold a nested mount")
  void validatesFallbackAndMountPlacement() {
    var bundle = bundle();
    var nested =
        wasmMount(
            "outer", "view", bundle, wasmMount("inner", "view", bundle, p().text("Fallback")));
    assertThatThrownBy(() -> Compiler.compile(new PageDocument("Nested", nested)))
        .isInstanceOf(CompilationException.class)
        .hasMessageContaining("wasm-mount-placement");
    var mount = wasmMount("host", "view", bundle, p().text("Fallback"));
    for (var container : List.of(template(), textarea().ariaLabel("Text"))) {
      var inaccessible =
          new PageDocument("Unavailable mount", DocumentElement.of(container).child(mount));
      assertThatThrownBy(() -> Compiler.compile(inaccessible))
          .isInstanceOf(CompilationException.class)
          .hasMessageContaining("wasm-mount-placement");
    }
    var unlabeled = wasmMount("view", "view", bundle, img("/cover.svg", "").removeAttr("alt"));
    assertThatThrownBy(() -> Compiler.compile(new PageDocument("Image", unlabeled)))
        .isInstanceOf(CompilationException.class)
        .hasMessageContaining("image-alt");
  }

  private static WasmBundle bundle() {
    return new WasmBundle(
        "run.js",
        "main.wasm",
        List.of(
            launcher("main.wasm"),
            Resource.of("main.wasm", Kind.WASM, EMPTY_MODULE, "extra/data.bin"),
            Resource.of("extra/data.bin", Kind.DATA, "data".getBytes(StandardCharsets.UTF_8))));
  }

  private static Resource launcher(String... references) {
    return Resource.of("run.js", Kind.LAUNCHER, new byte[0], references);
  }
}
