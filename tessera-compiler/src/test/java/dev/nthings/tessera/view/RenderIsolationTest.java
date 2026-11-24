package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.support.Snapshots.styleRules;
import static dev.nthings.tessera.view.LiveDom.dom;
import static dev.nthings.tessera.view.LiveDom.page;
import static dev.nthings.tessera.view.LiveDom.semantics;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.Component;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.State;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.components.Footnote;
import dev.nthings.tessera.support.Diagnostics;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Render isolation")
class RenderIsolationTest {
  private static final ViewKey<AtomicInteger> COUNTER =
      ViewKey.of("counter", AtomicInteger.class, AtomicInteger::new);

  /// A portable component that numbers itself through expansion state.
  private record Numbered() implements Component {
    @Override
    public IntoElement expand(ViewContext context) {
      return p().text(Integer.toString(context.state(COUNTER).incrementAndGet()));
    }
  }

  @Test
  @DisplayName("a turn reusing the previous turn's stylesheet emits exactly the rules it registers")
  void reusedStylesheetsEmitOnlyTheirOwnRules() {
    var shared = div().p(Tokens.SPACE_2).on(State.HOVER, s -> s.bg(Tokens.SURFACE_SUBTLE));
    var first = main().children(shared, p().text("Gone").m(Tokens.SPACE_4));
    var second = main().children(shared, p().text("New").gap(Tokens.SPACE_3));

    var reusing = render(second, "root", render(first, "root").styles());

    assertThat(styleRules(reusing))
        .containsExactlyElementsOf(styleRules(render(second, "root")))
        .noneMatch(rule -> rule.contains("margin"));
  }

  @Test
  @DisplayName("expansion state starts fresh for every render, including after a rejected one")
  void expansionStateIsPerRender() {
    var content = main().children(new Numbered(), new Numbered());
    var page = page(content);

    for (var turn = 0; turn < 2; turn++) {
      var live = dom(render(content));
      assertThat(live.select("p").eachText()).containsExactly("1", "2");
      assertThat(semantics(live)).isEqualTo(semantics(page));
    }
    Diagnostics.of(() -> render(main().children(new Numbered().key("a"), new Numbered().key("a"))));
    assertThat(dom(render(main().child(new Numbered()))).select("p").text()).isEqualTo("1");
  }

  @Test
  @DisplayName("independent renders of one component keep their reference scopes apart")
  void independentRendersKeepReferenceScopesSeparate() {
    var field = Field.of("Name", Forms.text("name")).hint("Full name");
    var left = dom(render(field, "left"));
    var right = dom(render(field, "right"));

    assertThat(left.select("input").attr("id")).isNotEqualTo(right.select("input").attr("id"));
    for (var document : List.of(left, right)) {
      var input = document.selectFirst("input");
      assertThat(document.select("label").attr("for")).isEqualTo(input.id());
      assertThat(document.getElementById(input.attr("aria-describedby"))).isNotNull();
    }
  }

  @Test
  @DisplayName("footnote numbering does not leak between compilations")
  void footnotesDoNotLeakBetweenCompilations() {
    var content =
        DocumentElement.of(main())
            .child(DocumentElement.of(p()).child(Footnote.of("Reference text").reference()));

    for (var turn = 0; turn < 2; turn++) {
      var page = page(content);
      assertThat(page.select("[role=doc-noteref]").text()).isEqualTo("[1]");
      assertThat(page.select("[role=doc-footnote]")).hasSize(1);
    }
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(strings = {"style", "render"})
  @DisplayName("a failing nested callback names its scopes and leaves the next render unaffected")
  void failedCallbacksNameTheirScopes(String phase) {
    var broken =
        new Panel(
            "outer",
            new Panel("inner", p(), phase.equals("style"), phase.equals("render")),
            false,
            false);

    var failure = catchThrowable(() -> render(broken));

    assertThat(failure).isInstanceOf(IllegalStateException.class);
    assertThat(failure.getSuppressed())
        .singleElement()
        .isInstanceOfSatisfying(
            ViewException.class,
            context ->
                assertThat(context.diagnostics())
                    .singleElement()
                    .satisfies(diagnostic ->
                        assertThat(String.join(" ", diagnostic.location().components()))
                            .contains("outer", "outer--inner")));
    var next = dom(render(new Panel("fresh", p().text("Ready"), false, false)));
    assertThat(next.select("[data-scope-id]").attr("data-scope-id")).isEqualTo("fresh");
    assertThat(next.text()).isEqualTo("Ready");
  }

  private record Panel(String id, IntoElement child, boolean failStyle, boolean failRender)
      implements Scope {
    @Override
    public String scopeId() {
      return id;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      if (failStyle) {
        throw new IllegalStateException("Callback failed");
      }
      scope.root(style -> style.fontWeight(600));
    }

    @Override
    public TagElement expand(ViewContext context) {
      if (failRender) {
        throw new IllegalStateException("Callback failed");
      }
      return div().child(child);
    }
  }
}
