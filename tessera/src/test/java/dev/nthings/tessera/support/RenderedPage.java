package dev.nthings.tessera.support;

import static dev.nthings.tessera.core.Elements.main;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.render.CompiledPage;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/// A compiled page with its parsed DOM, so assertions on markup and styles never re-compile
/// or re-parse.
public record RenderedPage(CompiledPage compiled, Document dom) {

  public static RenderedPage render(IntoElement body) {
    return of(new PageDocument("Test", body));
  }

  public static RenderedPage renderMain(IntoElement... content) {
    return render(main().children(content));
  }

  public static RenderedPage of(PageDocument document) {
    return of(PageCompiler.compile(document));
  }

  public static RenderedPage of(CompiledPage compiled) {
    return new RenderedPage(compiled, Jsoup.parse(compiled.html()));
  }

  public String html() {
    return compiled.html();
  }

  public String css() {
    return compiled.cssContent();
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

  /// The `@scope` body of the view that contains `element`.
  public String scopeBody(Element element) {
    var current = element;
    while (current != null && !current.hasAttr("data-view")) {
      current = current.parent();
    }
    if (current == null) {
      throw new AssertionError("No data-view ancestor");
    }
    return CssBlocks.block(
        css(),
        "@scope ([data-view=\"" + current.attr("data-view") + "\"]) to (:scope [data-view] > *){");
  }

  /// The rule body of `element`'s generated class with the given `prefix` (`u-`, `i-`, `cq-`).
  public String ruleOf(Element element, String prefix) {
    return CssBlocks.classRule(css(), CssBlocks.generatedClass(element.className(), prefix));
  }
}
