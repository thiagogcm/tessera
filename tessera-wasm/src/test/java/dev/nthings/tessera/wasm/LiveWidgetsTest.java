package dev.nthings.tessera.wasm;

import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.view.ViewRenderer;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Live widgets")
class LiveWidgetsTest {

  private static List<ViewSnapshot.Tag<LiveBehavior>> render(LiveContent content) {
    var snapshot =
        ViewRenderer.render(
            _ -> LivePlacement.capture(content), "", Detached::new, LiveBehavior::validate);
    return tags(snapshot.roots()).toList();
  }

  /// The context of a render outside a browser, which has no root to update.
  private record Detached(ViewContext base) implements LiveContext {
    @Override
    public <T> T state(ViewKey<T> key) {
      return base.state(key);
    }

    @Override
    public void report(String rule, String message, String fix) {
      base.report(rule, message, fix);
    }

    @Override
    public BrowserRoot root() {
      throw new UnsupportedOperationException("No browser root outside a browser");
    }
  }

  private static Stream<ViewSnapshot.Tag<LiveBehavior>> tags(
      List<ViewSnapshot.Node<LiveBehavior>> nodes) {
    return nodes.stream()
        .flatMap(node ->
            switch (node) {
              case ViewSnapshot.Tag<LiveBehavior> tag ->
                  Stream.concat(Stream.of(tag), tags(tag.children()));
              case ViewSnapshot.Group<LiveBehavior> group -> tags(group.children());
              case ViewSnapshot.Text<LiveBehavior> _, ViewSnapshot.Special<LiveBehavior> _ ->
                  Stream.empty();
            });
  }

  private static String text(ViewSnapshot.Tag<LiveBehavior> tag) {
    return tag.children().stream()
        .map(child -> child instanceof ViewSnapshot.Text<LiveBehavior>(var text) ? text : "")
        .collect(Collectors.joining());
  }

  private static List<ViewSnapshot.Tag<LiveBehavior>> withRole(
      List<ViewSnapshot.Tag<LiveBehavior>> tags, String role) {
    return tags.stream().filter(tag -> role.equals(tag.attributes().get("role"))).toList();
  }

  @Test
  @DisplayName("tabs select the first enabled tab when the initial one is missing")
  void tabsFallBackToTheFirstEnabledTab() {
    var tags =
        render(
            LiveTabs.of(
                ContentTabs.of("Views")
                    .id("views")
                    .initialSelected("removed")
                    .disabledTab("one", "One", p().text("1"))
                    .tab("two", "Two", p().text("2"))
                    .tab("three", "Three", p().text("3"))));

    var tabs = withRole(tags, "tab");
    assertThat(tabs)
        .extracting(LiveWidgetsTest::text, tab -> tab.attributes().get("aria-selected"))
        .containsExactly(tuple("One", "false"), tuple("Two", "true"), tuple("Three", "false"));
    var panels =
        withRole(tags, "tabpanel").stream()
            .collect(Collectors.toMap(panel -> panel.attributes().get("id"), Function.identity()));
    for (var tab : tabs) {
      var panel = panels.get(tab.attributes().get("aria-controls"));
      assertThat(panel).as("panel of %s", text(tab)).isNotNull();
      assertThat(panel.attributes().get("aria-labelledby")).isEqualTo(tab.attributes().get("id"));
      assertThat(panel.attributes().containsKey("hidden"))
          .isEqualTo(!"true".equals(tab.attributes().get("aria-selected")));
    }
  }

  @Test
  @DisplayName("a search palette renders a search input and one link per result")
  void searchRendersResultLinks() {
    var tags =
        render(
            LiveSearchPalette.of(
                SearchPalette.of()
                    .id("search")
                    .item(
                        "Guides",
                        SearchPalette.Item.of("Install", "/install").description("Set up"))
                    .item("Reference", SearchPalette.Item.of("API", "/api").badge("New"))));

    assertThat(tags)
        .filteredOn(tag -> tag.tag().tagName().equals("input"))
        .singleElement()
        .satisfies(input -> assertThat(input.attributes()).containsEntry("type", "search"));
    assertThat(tags)
        .filteredOn(tag -> tag.tag().tagName().equals("a"))
        .extracting(tag -> tag.attributes().get("href"))
        .containsExactly("/install", "/api");
  }
}
