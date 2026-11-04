package dev.nthings.tessera.support;

import static dev.nthings.tessera.core.Elements.main;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.render.PageCompiler;
import dev.nthings.tessera.render.PageCompiler.CompiledPage;
import dev.nthings.tessera.render.PageDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/// A compiled page paired with its parsed DOM. A test states what to render once, then asserts on
/// markup ([#dom()], [#element(String)]) and styles ([#css()], [#ruleOf(Element, String)]) without
/// re-compiling or re-parsing.
public record RenderedPage(CompiledPage compiled, Document dom) {

  /// Compiles `body` as the whole `<body>` of a page.
  public static RenderedPage render(IntoElement body) {
    return of(new PageDocument("Test", body));
  }

  /// Compiles `content` wrapped in a `<main>`.
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

  /// The first element matching `selector`, failing the test with the selector if there is none.
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

  /// The element with DOM id `id`, failing the test if there is none.
  public Element elementById(String id) {
    var found = dom.getElementById(id);
    if (found == null) {
      throw new AssertionError("No element has id: " + id);
    }
    return found;
  }

  /// The rule body of `element`'s generated class with the given `prefix` (`u-`, `i-`, `cq-`).
  public String ruleOf(Element element, String prefix) {
    return CssBlocks.classRule(css(), CssBlocks.generatedClass(element.className(), prefix));
  }
}
