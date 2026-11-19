package dev.nthings.tessera.core;

import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.components.ContentTabs;
import dev.nthings.tessera.components.SearchPalette;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import dev.nthings.tessera.internal.components.WidgetBridge;
import dev.nthings.tessera.internal.view.LinkedNode;
import dev.nthings.tessera.view.LiveNode;
import dev.nthings.tessera.view.ViewRenderer;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Core portable module contracts")
class PortableContractsTest {

  @Test
  @DisplayName("attribute enums do not expose wireValue publicly, while Role and Command do")
  void wireValueVisibilityIsRestrictedToRoleAndCommand() {
    for (var type :
        List.of(
            AriaChecked.class,
            AriaCurrent.class,
            AriaLive.class,
            AriaPopup.class,
            PopoverMode.class,
            ClosedBy.class)) {
      var publicWireValues =
          Arrays.stream(type.getMethods())
              .filter(m -> m.getName().equals("wireValue") && Modifier.isPublic(m.getModifiers()))
              .toList();
      assertThat(publicWireValues).as(type.getSimpleName()).isEmpty();
    }
    assertThat(Role.DOC_NOTEREF.wireValue()).isEqualTo("doc-noteref");
    assertThat(BuiltinCommand.SHOW_MODAL.wireValue()).isEqualTo("show-modal");
  }

  @Test
  @DisplayName("live widget bridges produce plain hosts without bundled custom-element behaviors")
  void liveWidgetBridgesOmitBundledBehavior() {
    var context =
        new ViewContext() {
          @Override
          public <T> T state(ViewKey<T> key) {
            throw new UnsupportedOperationException();
          }

          @Override
          public void report(String rule, String message, String fix) {}
        };
    var tabs = ContentTabs.of("Views").tab("one", "One", p().text("1"));
    var tabsParts = WidgetBridge.tabs(tabs, context, false, null);
    assertThat(AuthoringAccess.tag(tabsParts.host()).behavior).isNull();

    var search = SearchPalette.of().item("Docs", SearchPalette.Item.of("Home", "/"));
    var searchParts = WidgetBridge.search(search, context);
    assertThat(AuthoringAccess.tag(searchParts.host()).behavior).isNull();

    // Portable expansion still renders through ViewRenderer without compiler dependencies.
    var snapshot =
        ViewRenderer.<Void>render(
            _ -> new LiveNode.Portable<>(main().child(tabs)),
            "",
            base -> base,
            (_, _, _, _, _) -> {});
    var root = (LinkedNode.Tag<Void>) snapshot.roots().getFirst();
    var group = (LinkedNode.Group<Void>) root.children().getFirst();
    var tabHost = (LinkedNode.Tag<Void>) group.children().getFirst();
    assertThat(tabHost.tag().tagName()).isEqualTo("tessera-tabs");
  }
}
