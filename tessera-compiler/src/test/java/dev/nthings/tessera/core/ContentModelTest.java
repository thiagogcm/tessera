package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.table;
import static dev.nthings.tessera.core.Elements.tbody;
import static dev.nthings.tessera.core.Elements.td;
import static dev.nthings.tessera.core.Elements.tr;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.tuple;
import static dev.nthings.tessera.support.RenderedPage.render;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.Diagnostics.Found;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Content model")
class ContentModelTest {

  /// A scope whose host is the element `host` builds.
  record Hosted(String scopeId, TagElement host) implements Scope {
    @Override
    public TagElement expand(ViewContext context) {
      return host;
    }
  }

  static Stream<Arguments> scopeHosts() {
    return Stream.of(
        argumentSet(
            "list",
            (Function<Scope, IntoElement>) scope -> main().child(scope),
            new Hosted("items", ul().children(li().text("One"), li().text("Two"))),
            "main > ul > li"),
        argumentSet(
            "table row",
            (Function<Scope, IntoElement>)
                scope -> main().child(table().child(tbody().child(scope))),
            new Hosted("row", tr().children(td().text("One"), td().text("Two"))),
            "table > tbody > tr > td"));
  }

  @ParameterizedTest
  @MethodSource("scopeHosts")
  @DisplayName("a scope's own element is its host, so no wrapper breaks the parent's content model")
  void scopeHostsKeepTheContentModel(Function<Scope, IntoElement> place, Scope scope, String path) {
    var page = render(place.apply(scope));

    assertThat(page.elements(path).eachText()).containsExactly("One", "Two");
    assertThat(page.elements("[data-scope-id]"))
        .singleElement()
        .satisfies(host -> assertThat(host.attr("data-scope-id")).isEqualTo(scope.scopeId()));
  }

  @Test
  @DisplayName("content placed through a fragment is validated like direct children")
  void fragmentsAreValidated() {
    assertThat(Diagnostics.of(() -> render(main().child(fragment().child(button())))))
        .extracting(Found::rule, Found::tag)
        .containsExactly(tuple("interactive-label", "button"));
  }
}
