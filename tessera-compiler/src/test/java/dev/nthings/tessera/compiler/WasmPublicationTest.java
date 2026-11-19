package dev.nthings.tessera.compiler;

import static dev.nthings.tessera.core.Elements.img;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.template;
import static dev.nthings.tessera.core.Elements.textarea;
import static dev.nthings.tessera.document.Documents.wasmMount;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.WasmBundle;
import dev.nthings.tessera.document.WasmBundle.Kind;
import dev.nthings.tessera.document.WasmBundle.Resource;
import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.RenderedPage;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Wasm publication")
class WasmPublicationTest {
  private static final byte[] EMPTY_MODULE = {0, 'a', 's', 'm', 1, 0, 0, 0};

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

  static Stream<Arguments> invalidBundles() {
    var image = Resource.of("main.wasm", Kind.WASM, EMPTY_MODULE);
    return Stream.of(
        argumentSet(
            "a reference to a missing resource",
            (ThrowingCallable)
                () ->
                    new WasmBundle(
                        "run.js",
                        "main.wasm",
                        List.of(launcher("main.wasm", "missing.bin"), image))),
        argumentSet(
            "a resource the launcher never reaches",
            (ThrowingCallable)
                () -> new WasmBundle("run.js", "main.wasm", List.of(launcher(), image))),
        argumentSet(
            "bytes that no longer match their integrity",
            (ThrowingCallable)
                () ->
                    new Resource(
                        image.name(), image.kind(), new byte[] {1}, image.integrity(), List.of())),
        argumentSet(
            "a module without a WebAssembly header",
            (ThrowingCallable) () -> Resource.of("invalid.wasm", Kind.WASM, new byte[] {1})));
  }

  @ParameterizedTest
  @MethodSource("invalidBundles")
  @DisplayName("an incomplete or altered bundle is rejected")
  void rejectsIncompleteOrChangedBundles(ThrowingCallable creation) {
    assertThatIllegalArgumentException().isThrownBy(creation);
  }

  @Test
  @DisplayName("independently constructed bundles with identical resources are equal")
  void bundlesCompareByValue() {
    var first = bundle();
    var second = bundle();

    assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
  }

  private static String sha384(byte[] content) throws NoSuchAlgorithmException {
    return "sha384-"
        + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-384").digest(content));
  }

  @Test
  @DisplayName("every published resource verifies against the integrity the page declares for it")
  void publishesVerifiableResources() throws Exception {
    var view = "edit\"<&=view";
    var id = "editor\"<&";
    var compiled =
        Compiler.compile(
            new PageDocument("Mount", wasmMount(id, view, bundle(), p().text("Fallback"))),
            Theme.defaults(),
            OutputMode.external("/assets/./nested/"));
    var page = RenderedPage.of(compiled);
    var host = page.elementById(id);
    Map<String, Asset> assets =
        compiled.assets().stream().collect(Collectors.toMap(Asset::path, asset -> asset));

    assertThat(host.attr("data-tessera-view")).isEqualTo(view);
    assertThat(host.text()).isEqualTo("Fallback");
    assertThat(assets.keySet()).allMatch(path -> path.startsWith("/assets/nested/"));
    var declared = new LinkedHashMap<String, String>();
    declared.put(host.attr("data-tessera-entry"), host.attr("data-tessera-integrity"));
    var manifest = host.attr("data-tessera-resources").split(" ");
    for (var i = 0; i < manifest.length; i += 3) {
      declared.put(manifest[i + 1], manifest[i + 2]);
      if (manifest[i].endsWith(".wasm")) {
        assertThat(assets.get(manifest[i + 1]).contentType()).isEqualTo("application/wasm");
      }
    }
    var bootstrap = page.element("script[integrity]");
    assertThat(bootstrap).hasAttribute("crossorigin", "anonymous");
    declared.put(bootstrap.attr("src"), bootstrap.attr("integrity"));
    assertThat(declared).hasSize(4);
    for (var entry : declared.entrySet()) {
      assertThat(assets).containsKey(entry.getKey());
      assertThat(sha384(assets.get(entry.getKey()).content())).isEqualTo(entry.getValue());
    }
    assertThat(compiled.responseHeaders()).containsKey("Content-Security-Policy");
  }

  @Test
  @DisplayName("shared bundles publish once and the deployment policy applies only to mount pages")
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
    var rulesHeader = site.pageHeaders().get("/").get("Speculation-Rules");
    assertThat(rulesHeader).isNotNull();
    assertThat(site.files()).containsKey(rulesHeader.replace("\"", ""));
    var mounted = RenderedPage.of(site, "/");
    assertThat(mounted.elements("script[integrity]")).hasSize(1);
    assertThat(mounted.elements("script[type=speculationrules]")).isEmpty();
    assertThat(RenderedPage.of(site, "/plain").elements("script[type=speculationrules]"))
        .hasSize(1);
  }

  static Stream<Arguments> misplacedMounts() {
    var bundle = bundle();
    var mount = wasmMount("host", "view", bundle, p().text("Fallback"));
    return Stream.of(
        argumentSet(
            "a mount nested in another mount's fallback",
            wasmMount("outer", "view", bundle, wasmMount("inner", "view", bundle, p().text("x"))),
            "wasm-mount-placement"),
        argumentSet(
            "a mount inside a template",
            DocumentElement.of(template()).child(mount),
            "wasm-mount-placement"),
        argumentSet(
            "a mount inside a textarea",
            DocumentElement.of(textarea().ariaLabel("Text")).child(mount),
            "wasm-mount-placement"),
        argumentSet(
            "an unlabeled image in the fallback",
            wasmMount("view", "view", bundle, img("/cover.svg", "").removeAttr("alt")),
            "image-alt"));
  }

  @ParameterizedTest
  @MethodSource("misplacedMounts")
  @DisplayName("mount placement and fallback content are validated")
  void validatesMountPlacementAndFallback(DocumentContent content, String rule) {
    assertThat(Diagnostics.rules(() -> Compiler.compile(new PageDocument("Mount", content))))
        .contains(rule);
  }
}
