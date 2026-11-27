package dev.nthings.tessera.support;

import static dev.nthings.tessera.core.Elements.main;

import dev.nthings.tessera.compiler.CompiledPage;
import dev.nthings.tessera.compiler.CompiledSite;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.document.DocumentElement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/// A compiled page with its parsed DOM, so assertions on markup and styles never re-compile or
/// re-parse.
public record RenderedPage(CompiledPage compiled, Document dom) {

  public static RenderedPage render(IntoElement body) {
    return of(PageDocument.of("Test", body));
  }

  /// Compiles a page whose body is document `body`.
  public static RenderedPage render(DocumentContent body) {
    return of(PageDocument.of("Test", body));
  }

  public static RenderedPage renderMain(IntoElement... content) {
    return render(main().children(content));
  }

  /// Compiles a page whose `<main>` holds document `content`.
  public static RenderedPage renderDocumentMain(DocumentContent... content) {
    return render(DocumentElement.of(main()).children(content));
  }

  public static RenderedPage of(PageDocument document) {
    return of(Compiler.forPage().compile(document));
  }

  /// The page at `route` of `site`, with the site's shared stylesheet and the route's response
  /// headers. Its assets are empty: the site publishes them in [CompiledSite#files()].
  public static RenderedPage of(CompiledSite site, String route) {
    var html = site.pages().get(route);
    if (html == null) {
      throw new AssertionError("No page at route: " + route);
    }
    return of(
        new CompiledPage(
            html, site.css(), List.of(), site.headers().getOrDefault(route, Map.of())));
  }

  public static RenderedPage of(CompiledPage compiled) {
    return new RenderedPage(compiled, Jsoup.parse(compiled.html()));
  }

  public String html() {
    return compiled.html();
  }

  public String css() {
    return compiled.css();
  }

  /// First element matching `selector`; fails the test if there is none.
  public Element element(String selector) {
    var found = dom.selectFirst(selector);
    if (found == null) {
      throw new AssertionError("No element matches: " + selector);
    }
    return found;
  }

  public Elements elements(String selector) {
    return dom.select(selector);
  }

  /// Element with DOM id `id`; fails the test if there is none.
  public Element elementById(String id) {
    var found = dom.getElementById(id);
    if (found == null) {
      throw new AssertionError("No element has id: " + id);
    }
    return found;
  }

  /// The elements named by the space-separated id references in `element`'s `attribute`; fails the
  /// test if the attribute is missing or any reference does not resolve.
  public List<Element> referenced(Element element, String attribute) {
    if (!element.hasAttr(attribute) || element.attr(attribute).isBlank()) {
      throw new AssertionError(
          "Expected " + element.shallowClone().outerHtml() + " to have " + attribute);
    }
    return Arrays.stream(element.attr(attribute).trim().split("\\s+"))
        .map(this::elementById)
        .toList();
  }

  /// The single element `element`'s `attribute` references.
  public Element target(Element element, String attribute) {
    var targets = referenced(element, attribute);
    if (targets.size() != 1) {
      throw new AssertionError("Expected one " + attribute + " target but got " + targets.size());
    }
    return targets.getFirst();
  }

  /// The `@scope` body of the scope that contains `element`.
  public String scopeBody(Element element) {
    var current = element;
    while (current != null && !current.hasAttr("data-scope")) {
      current = current.parent();
    }
    if (current == null) {
      throw new AssertionError("No data-scope ancestor");
    }
    return CssBlocks.block(
        css(),
        "@scope ([data-scope=\""
            + current.attr("data-scope")
            + "\"]) to (:scope [data-scope] > *){");
  }

  /// The rule body of `element`'s generated class with the given `prefix` (`u-`, `i-`, `cq-`).
  public String ruleOf(Element element, String prefix) {
    return CssBlocks.classRule(css(), CssBlocks.generatedClass(element.className(), prefix));
  }
}
