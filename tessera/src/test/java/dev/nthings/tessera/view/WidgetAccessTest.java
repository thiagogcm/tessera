package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.support.Snapshots.tags;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.ContentTabs.Activation;
import dev.nthings.tessera.components.Drawer;
import dev.nthings.tessera.components.Modal;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.view.WidgetAccess.SearchLink;
import dev.nthings.tessera.view.WidgetAccess.SearchResult;
import dev.nthings.tessera.view.WidgetAccess.TabPart;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Widget parts")
class WidgetAccessTest {

  /// Expands `body` in an explicit id scope, as a live adapter does.
  private record Probe(String scopeId, Function<ViewContext, TagElement> body) implements Scope {
    @Override
    public String scopeName() {
      return "probe";
    }

    @Override
    public TagElement expand(ViewContext context) {
      return body.apply(context);
    }
  }

  @Test
  @DisplayName("tabs bind each key to its tab and panel and render the plain host")
  void tabs() {
    var tabs =
        ContentTabs.of("Views")
            .initialSelected("removed")
            .activation(Activation.MANUAL)
            .tab("one", "One", p().text("1"))
            .disabledTab("two", "Two", p().text("2"))
            .tab("three", "Three", p().text("3"));
    var captured = new AtomicReference<WidgetAccess.TabsParts>();
    var snapshot =
        render(
            new Probe(
                "views",
                context -> {
                  var parts = WidgetAccess.tabs(tabs, context, false, null);
                  captured.set(parts);
                  return parts.host();
                }));
    var parts = captured.get();

    assertThat(parts.tabs())
        .extracting(TabPart::key, TabPart::enabled)
        .containsExactly(tuple("one", true), tuple("two", false), tuple("three", true));
    assertThat(parts.selected()).isEqualTo("one");
    assertThat(parts.manual()).isTrue();
    assertThat(tagWithAttribute(snapshot, "data-activation").tag().tagName()).isEqualTo("div");
    for (var tab : parts.tabs()) {
      assertThat(tagBoundTo(snapshot, tab.tab()).attributes()).containsEntry("role", "tab");
      assertThat(tagBoundTo(snapshot, tab.panel()).attributes()).containsEntry("role", "tabpanel");
    }
    assertThat(tagBoundTo(snapshot, parts.tabs().getFirst().tab()).attributes())
        .containsEntry("aria-selected", "true");
  }

  @Test
  @DisplayName("unadapted tabs keep their custom element host in a live render")
  void unadaptedTabs() {
    var snapshot = render(ContentTabs.of("Views").tab("one", "One", p().text("1")));

    assertThat(tags(snapshot.roots()).findFirst().orElseThrow().tag().tagName())
        .isEqualTo("tessera-tabs");
  }

  @Test
  @DisplayName("search binds each result link and the controls the palette drives")
  void search() {
    var palette =
        SearchPalette.of()
            .item("Guides", SearchPalette.Item.of("Install", "/install").description("Set up"))
            .item("Reference", SearchPalette.Item.of("API", "/api").badge("New"));
    var captured = new AtomicReference<WidgetAccess.SearchParts>();
    var snapshot =
        render(
            new Probe(
                "search",
                context -> {
                  var parts = WidgetAccess.search(palette, context);
                  captured.set(parts);
                  return parts.host();
                }));
    var parts = captured.get();

    assertThat(parts.results())
        .extracting(SearchResult::link)
        .containsExactly(
            new SearchLink("Guides", "Install", "/install", "Set up", null),
            new SearchLink("Reference", "API", "/api", null, "New"));
    for (var result : parts.results()) {
      assertThat(tagBoundTo(snapshot, result.ref()).attributes())
          .containsEntry("href", result.link().href());
    }
    assertThat(tagBoundTo(snapshot, parts.input()).attributes()).containsEntry("type", "search");
    assertThat(tagBoundTo(snapshot, parts.empty()).attributes()).containsEntry("role", "status");
    assertThat(tagWithAttribute(snapshot, "closedby").attributes()).doesNotContainKey("is");
  }

  @Test
  @DisplayName("dialogs report an authored open state on a closed host")
  void dialogs() {
    assertDialog(Modal.of("Preview").opened(), true);
    assertDialog(Drawer.of("Filters"), false);
  }

  private static void assertDialog(IntoElement dialog, boolean opened) {
    var captured = new AtomicReference<WidgetAccess.DialogParts>();
    var snapshot =
        render(
            new Probe(
                "dialog",
                context -> {
                  var parts = WidgetAccess.dialog(dialog, context);
                  captured.set(parts);
                  return parts.host();
                }));

    assertThat(captured.get().opened()).isEqualTo(opened);
    assertThat(tagWithAttribute(snapshot, "closedby").attributes()).doesNotContainKey("open");
  }

  private static ViewSnapshot.Tag<?> tagBoundTo(ViewSnapshot<?> snapshot, ElementRef ref) {
    var id = snapshot.references().get(ref);
    assertThat(id).isNotNull();
    return tags(snapshot.roots())
        .filter(tag -> id.equals(tag.attributes().get("id")))
        .findFirst()
        .orElseThrow();
  }

  private static ViewSnapshot.Tag<?> tagWithAttribute(ViewSnapshot<?> snapshot, String name) {
    return tags(snapshot.roots())
        .filter(tag -> tag.attributes().containsKey(name))
        .findFirst()
        .orElseThrow();
  }
}
