package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.custom;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.table;
import static dev.nthings.tessera.core.Elements.tbody;
import static dev.nthings.tessera.core.Elements.td;
import static dev.nthings.tessera.core.Elements.tr;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.catchThrowableOfType;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.compiler.CompilationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Fragment and custom element authoring")
class ElementSurfaceTest {

  @Test
  @DisplayName("a fragment keeps direct child content-model positions")
  void fragmentKeepsDirectChildren() {
    var page = renderMain(ul().child(fragment().children(li().text("One"), li().text("Two"))));

    assertThat(page.elements("ul > li"))
        .extracting(element -> element.text())
        .containsExactly("One", "Two");
    assertThat(page.elements("ul > div")).isEmpty();
  }

  @Test
  @DisplayName("an unlabelled button inside a fragment is reported")
  void reportsFragmentDiagnostics() {
    var failure =
        catchThrowableOfType(
            CompilationException.class, () -> renderMain(fragment().child(button())));

    assertThat(failure.diagnostics())
        .singleElement()
        .satisfies(
            d -> {
              assertThat(d.rule()).isEqualTo("interactive-label");
              assertThat(d.message())
                  .isEqualTo(
                      "Interactive <button> element must have text content or an aria-label / aria-labelledby / title attribute");
              assertThat(d.fix())
                  .isEqualTo(
                      "Add visible label text, .ariaLabel(...), or .ariaLabelledBy(...) referencing a label.");
              assertThat(d.location().page()).isEqualTo("Test");
              assertThat(d.location().components()).isEmpty();
              assertThat(d.location().tag()).isEqualTo("button");
              assertThat(d.location().domId()).isNull();
            });
  }

  record Items() implements Scope {
    @Override
    public String scopeId() {
      return "items";
    }

    @Override
    public Element expand(DocumentContext context) {
      return ul().children(li().text("One"), li().text("Two"));
    }
  }

  record Row() implements Scope {
    @Override
    public String scopeId() {
      return "row";
    }

    @Override
    public Element expand(DocumentContext context) {
      return tr().child(td().text("Cell"));
    }
  }

  @Test
  @DisplayName("a list scope uses the ul as its host")
  void listScopeUsesTheList() {
    var page = renderMain(new Items());

    assertThat(page.element("main > ul"))
        .hasAttribute("data-scope", "items")
        .hasAttribute("data-scope-id", "items");
    assertThat(page.elements("main > ul > li"))
        .extracting(element -> element.text())
        .containsExactly("One", "Two");
    assertThat(page.elements("main > div")).isEmpty();
  }

  @Test
  @DisplayName("a row scope uses the tr as its host")
  void rowScopeUsesTheRow() {
    var page = renderMain(table().child(tbody().child(new Row())));

    assertThat(page.element("table > tbody > tr"))
        .hasAttribute("data-scope", "row")
        .hasAttribute("data-scope-id", "row");
    assertThat(page.element("table > tbody > tr > td")).hasText("Cell");
    assertThat(page.elements("tr > div")).isEmpty();
  }

  @Test
  @DisplayName("a custom element renders its content")
  void customElementRendersContent() {
    var page = renderMain(custom("my-widget").text("Hello"));

    assertThat(page.elements("main > *")).hasSize(1);
    assertThat(page.element("main > my-widget")).hasTagName("my-widget").hasText("Hello");
  }
}
