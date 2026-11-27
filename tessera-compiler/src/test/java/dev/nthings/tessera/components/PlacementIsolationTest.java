package dev.nthings.tessera.components;

import static dev.nthings.tessera.components.TableGrid.body;
import static dev.nthings.tessera.components.TableGrid.columnHeader;
import static dev.nthings.tessera.components.TableGrid.data;
import static dev.nthings.tessera.components.TableGrid.head;
import static dev.nthings.tessera.components.TableGrid.row;
import static dev.nthings.tessera.components.TableGrid.rowHeader;
import static dev.nthings.tessera.core.Elements.article;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("components placed in several scopes")
class PlacementIsolationTest {

  private static final List<String> REFERENCES =
      List.of(
          "for",
          "aria-controls",
          "aria-labelledby",
          "aria-describedby",
          "aria-errormessage",
          "commandfor",
          "interestfor",
          "headers");

  /// A reusable author scope that hosts one region of a page.
  private record Region(String scopeId, IntoElement content) implements Scope {
    @Override
    public String scopeName() {
      return "region";
    }

    @Override
    public TagElement expand(ViewContext context) {
      return article().child(content);
    }
  }

  private static IntoElement modalWithTrigger() {
    var modal = Modal.of("Confirm").scopeId("confirm").bodyText("Deploy?");
    return div().children(modal.triggerButton("Open"), modal);
  }

  private static Arguments placed(String name, Function<String, IntoElement> component) {
    return argumentSet(name, component);
  }

  static Stream<Arguments> components() {
    return Stream.of(
        placed(
            "content tabs",
            _ ->
                ContentTabs.of("Languages")
                    .scopeId("languages")
                    .tab("java", "Java", p().text("Builders"))
                    .tab("html", "HTML", p().text("Markup"))),
        placed(
            "dropdown",
            _ ->
                Dropdown.of("Actions")
                    .scopeId("actions")
                    .item(Dropdown.Item.link("/logs", "Logs"))),
        placed(
            "field",
            _ ->
                Field.of("Email", Forms.input("email", InputType.EMAIL))
                    .scopeId("email")
                    .hint("Work address")
                    .error("Required")),
        placed(
            "data table",
            placement ->
                DataTable.of(
                        "Sales in " + placement,
                        TableGrid.of(
                            head(row(columnHeader("Area"), columnHeader("Q1"))),
                            body(row(rowHeader("US"), data("12")))))
                    .scopeId("sales")),
        placed("modal and its trigger", _ -> modalWithTrigger()),
        placed(
            "button with a tooltip",
            _ ->
                Button.of("Save").interestTooltip(tip -> tip.child(span().text("Saves a draft")))));
  }

  @ParameterizedTest
  @MethodSource("components")
  @DisplayName("each placement gets its own ids and references only its own elements")
  void placementsStayIsolated(Function<String, IntoElement> component) {
    var page =
        renderMain(
            Card.of("Pricing").scopeId("pricing").body(component.apply("pricing")),
            Card.of("Support").scopeId("support").body(component.apply("support")),
            new Region("left", component.apply("left")),
            new Region("right", component.apply("right")));

    assertThat(page.elements("[id]")).extracting(Element::id).doesNotHaveDuplicates();
    var referrers =
        page.elements(String.join(", ", REFERENCES.stream().map(a -> "[" + a + "]").toList()));
    assertThat(referrers).isNotEmpty();
    for (var referrer : referrers) {
      for (var attribute : REFERENCES) {
        if (referrer.hasAttr(attribute)) {
          assertThat(page.referenced(referrer, attribute))
              .allSatisfy(target ->
                  assertThat(target.closest("article")).isSameAs(referrer.closest("article")));
        }
      }
    }
  }
}
