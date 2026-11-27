package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.form;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.support.Diagnostics;
import dev.nthings.tessera.support.RenderedPage;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ContentTabs")
class ContentTabsTest {

  private static ContentTabs stages() {
    return ContentTabs.of("Build & test")
        .scopeId("stages")
        .tab("java", "Java", p().text("Builders"))
        .tab("html", "HTML", p().text("Markup"))
        .tab("css", "CSS", p().text("Styles"));
  }

  @Test
  @DisplayName("a named tab list controls one panel per tab, and only the first is shown")
  void linksTabsToPanels() {
    var page = renderMain(stages());

    assertThat(page.element("[role=tablist]")).hasAttribute("aria-label", "Build & test");
    var tabs = page.elements("[role=tab]");
    assertThat(tabs).extracting(Element::text).containsExactly("Java", "HTML", "CSS");
    for (var tab : tabs) {
      var panel = page.target(tab, "aria-controls");
      assertThat(panel).hasAttribute("role", "tabpanel");
      assertThat(page.target(panel, "aria-labelledby")).isSameAs(tab);
    }
    assertThat(tabs)
        .extracting(tab -> tab.attr("aria-selected") + " " + tab.attr("tabindex"))
        .containsExactly("true 0", "false -1", "false -1");
    assertThat(tabs)
        .extracting(tab -> page.target(tab, "aria-controls").hasAttr("hidden"))
        .containsExactly(false, true, true);
  }

  @Test
  @DisplayName("an initial selection wins over the first tab, and a disabled tab is skipped")
  void explicitSelection() {
    var page =
        renderMain(
            ContentTabs.of("Stages")
                .disabledTab("java", "Java", p().text("Builders"))
                .tab("html", "HTML", p().text("Markup"))
                .tab("css", "CSS", p().text("Styles"))
                .initialSelected("css"));

    var selected = page.element("[role=tab][aria-selected=true]");
    assertThat(selected).hasText("CSS").hasAttribute("tabindex", "0");
    assertThat(page.target(selected, "aria-controls")).hasText("Styles");
    assertThat(page.element("[role=tab]:contains(Java)"))
        .hasAttribute("disabled")
        .hasAttribute("aria-disabled", "true")
        .hasAttribute("tabindex", "-1");
  }

  @ParameterizedTest
  @ValueSource(strings = {"java", "rust"})
  @DisplayName("an initial selection must name an enabled tab")
  void reportsUnselectableInitialTab(String key) {
    var tabs =
        ContentTabs.of("Stages")
            .scopeId("stages")
            .disabledTab("java", "Java", p().text("Builders"))
            .tab("html", "HTML", p().text("Markup"))
            .initialSelected(key);

    assertThat(Diagnostics.of(() -> renderMain(tabs)))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo("tab-selection");
          assertThat(found.components()).containsExactly("content-tabs[stages]");
        });
  }

  @Test
  @DisplayName("tab buttons inside a form never submit it")
  void tabsInsideFormDoNotSubmit() {
    var page = renderMain(form().child(stages()));

    assertThat(page.elements("form [role=tab]"))
        .hasSize(3)
        .allSatisfy(tab -> assertThat(tab).hasAttribute("type", "button"));
  }

  @Test
  @DisplayName("nested tabs reusing keys keep their own panels and selection")
  void nestedTabsStayIndependent() {
    var inner =
        ContentTabs.of("Inner")
            .tab("java", "Inner Java", p().text("Inner builders"))
            .tab("html", "Inner HTML", p().text("Inner markup"))
            .initialSelected("html");
    var page =
        renderMain(
            ContentTabs.of("Outer")
                .tab("java", "Outer Java", inner)
                .tab("html", "Outer HTML", p().text("Outer markup")));

    assertThat(page.elements("[id]")).extracting(Element::id).doesNotHaveDuplicates();
    assertSelected(page, "Outer", "Outer Java");
    assertSelected(page, "Inner", "Inner HTML");
    for (var tab : page.elements("[role=tab]")) {
      assertThat(page.target(page.target(tab, "aria-controls"), "aria-labelledby")).isSameAs(tab);
    }
  }

  private static void assertSelected(RenderedPage page, String list, String tab) {
    assertThat(
            page.element("[role=tablist][aria-label=" + list + "]").select("[aria-selected=true]"))
        .singleElement()
        .extracting(Element::text)
        .isEqualTo(tab);
  }

  @ParameterizedTest
  @ValueSource(strings = {" ", ""})
  @DisplayName("a blank tab key is rejected")
  void blankTabKeyIsRejected(String key) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ContentTabs.of("Stages").tab(key, "Blank", p().text("x")));
  }

  static Stream<Arguments> collidingKeys() {
    return Stream.of(
        argumentSet("repeated key", "java", "java"),
        argumentSet("keys that give the same id", "c++", "c#"));
  }

  @ParameterizedTest
  @MethodSource("collidingKeys")
  @DisplayName("two tabs whose keys collide are reported as duplicate tabs")
  void reportsDuplicateTabKey(String first, String second) {
    var tabs =
        ContentTabs.of("Languages")
            .scopeId("languages")
            .tab(first, "First", p().text("One"))
            .tab(second, "Second", p().text("Two"));

    assertThat(Diagnostics.of(() -> renderMain(tabs)))
        .filteredOn(found -> found.rule().equals("duplicate-tab"))
        .singleElement()
        .satisfies(
            found -> assertThat(found.components()).containsExactly("content-tabs[languages]"));
  }

  @Test
  @DisplayName("tabs without any tab are reported")
  void reportsEmptyTabs() {
    assertThat(Diagnostics.rules(() -> renderMain(ContentTabs.of("Empty"))))
        .containsExactly("empty-content-tabs");
  }
}
