package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.button;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.support.Snapshots.styleRules;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;

import dev.nthings.tessera.compiler.CompilationException;
import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.core.Effect;
import dev.nthings.tessera.core.EffectElement;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.State;
import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Live tree contracts")
class LiveContractTest {

  private static final Effect NOTHING = _ -> {};

  @Test
  @DisplayName("effects take their position in a keyed group of the linked tree without starting")
  void effectsTakeTheirPositionInAKeyedGroupOfTheLinkedTreeWithoutStarting() {
    var started = new int[1];
    var snapshot =
        render(div().children(Elements.effect("ticker", _ -> started[0]++), button().text("Go")));

    var root = (ViewSnapshot.Tag) snapshot.roots().getFirst();
    assertThat(root.children().getFirst())
        .isInstanceOfSatisfying(
            ViewSnapshot.Group.class,
            group -> {
              assertThat(group.key()).isEqualTo("ticker");
              assertThat(group.definition()).isEqualTo(EffectElement.class);
              assertThat(group.children()).singleElement().isInstanceOf(ViewSnapshot.Effect.class);
            });
    assertThat(started[0]).isZero();
  }

  @Test
  @DisplayName("effect keys share the sibling namespace with keyed children")
  void effectKeysShareTheSiblingNamespaceWithKeyedChildren() {
    assertThatThrownBy(() ->
            render(div().children(Elements.effect("a", NOTHING), Elements.effect("a", NOTHING))))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("duplicate-key")
        .hasMessageContaining("Duplicate sibling key 'a'");
    assertThatThrownBy(() ->
            render(div().children(Elements.effect("a", NOTHING), button().text("A").key("a"))))
        .isInstanceOf(ViewException.class)
        .hasMessageContaining("duplicate-key")
        .hasMessageContaining("Duplicate sibling key 'a'");

    var separate =
        render(
            div()
                .children(
                    div().child(Elements.effect("a", NOTHING)),
                    div().child(button().text("A").key("a"))));
    assertThat(separate.roots()).hasSize(1);
  }

  @Test
  @DisplayName("static output rejects effects and event handlers")
  void staticOutputRejectsEffectsAndEventHandlers() {
    var failure =
        catchThrowableOfType(
            CompilationException.class,
            () ->
                Compiler.compile(
                    new PageDocument(
                        "Live",
                        main()
                            .children(
                                Elements.effect("ticker", NOTHING),
                                Button.of("Save").onClick(() -> {})))));

    assertThat(failure.diagnostics())
        .extracting(Diagnostic::rule)
        .containsExactly("native-effect", "native-event-handlers");
  }

  @Test
  @DisplayName("blank event types and sibling keys are rejected")
  void blankEventTypesAndSiblingKeysAreRejected() {
    assertThatThrownBy(() -> div().onEvent(" ", _ -> {}))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Elements.effect("", NOTHING))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> div().key(" ")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("live declarations keep one rule in browser mounts and compile as values statically")
  void liveDeclarationsKeepOneRuleInBrowserMountsAndCompileAsValuesStatically() {
    var classes = new HashSet<String>();
    for (var width = 1; width <= 3; width++) {
      var size = CssLength.px(width);
      var snapshot = render(div().live(style -> style.inlineSize(size)));
      var tag = (ViewSnapshot.Tag) snapshot.roots().getFirst();

      assertThat(tag.liveProperties()).containsExactly(entry("--tl-inline-size", width + "px"));
      classes.add(tag.attributes().get("class"));
      assertThat(styleRules(snapshot))
          .anySatisfy(rule -> assertThat(rule).contains("inline-size:var(--tl-inline-size)"))
          .contains("@property --tl-inline-size{syntax:\"*\";inherits:false}")
          .noneSatisfy(rule -> assertThat(rule).contains("inline-size:" + size.toCss()));
    }
    assertThat(classes).hasSize(1);

    var css =
        Compiler.compile(
                new PageDocument(
                    "Live", main().child(div().live(style -> style.inlineSize(CssLength.px(7))))))
            .css();
    assertThat(css).contains("inline-size:7px").doesNotContain("--tl-");
  }

  @Test
  @DisplayName("conditional styles written as live styles stay ordinary rules")
  void conditionalStylesWrittenAsLiveStylesStayOrdinaryRules() {
    var snapshot =
        render(
            div()
                .live(style ->
                    style
                        .inlineSize(CssLength.px(3))
                        .on(State.HOVER, hover -> hover.opacity(0.5))));
    var tag = (ViewSnapshot.Tag) snapshot.roots().getFirst();

    assertThat(tag.liveProperties()).containsOnlyKeys("--tl-inline-size");
    assertThat(styleRules(snapshot))
        .anySatisfy(rule -> assertThat(rule).startsWith("@layer interactive{").contains("opacity"));
  }
}
