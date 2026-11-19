package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.support.Snapshots.styleRules;
import static dev.nthings.tessera.support.Snapshots.tags;
import static dev.nthings.tessera.view.LiveDom.dom;
import static dev.nthings.tessera.view.LiveDom.page;
import static dev.nthings.tessera.view.LiveDom.semantics;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.compiler.Compiler;
import dev.nthings.tessera.compiler.PageDocument;
import dev.nthings.tessera.components.Button;
import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.HtmlTag;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Theme;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.internal.view.LinkedNode;
import dev.nthings.tessera.support.Diagnostics;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Static and live backend parity")
class BackendParityTest {

  @Test
  @DisplayName("both backends render the same semantics and the same component and utility rules")
  void expansionPreservesSemanticsAndStyles() {
    var content =
        main()
            .children(
                Button.of("Save"),
                Field.of("Name", Forms.text("name")).hint("Full name"),
                fragment().children(p().text("Visible"), p().text("Details")),
                ul().role(Role.LIST)
                    .child(fragment().child(li().role(Role.LISTITEM).text("Entry"))));
    var live = render(content);
    var page = Compiler.compile(new PageDocument("Editor", content));

    assertThat(semantics(dom(live))).isEqualTo(semantics(Jsoup.parse(page.html())));
    assertThat(styleRules(live))
        .filteredOn(
            rule -> rule.startsWith("@layer utilities{") || rule.startsWith("@layer components{"))
        .isNotEmpty()
        .allSatisfy(rule ->
            assertThat(page.css())
                .contains(rule.substring(rule.indexOf('{') + 1, rule.length() - 1)));
  }

  static Stream<Arguments> invalidContent() {
    var command = ElementRef.scoped("target");
    var label = ElementRef.scoped("label-target");
    return Stream.of(
        Arguments.of(
            main().child(Elements.el(HtmlTag.IMG).attr("src", "/image.png")),
            List.of("image-alt", "image-dimensions")),
        Arguments.of(
            main()
                .children(
                    Elements.button().text("Open").command(command, BuiltinCommand.SHOW_MODAL),
                    fragment().child(div().id(command))),
            List.of("incompatible-command-target")),
        Arguments.of(
            main()
                .children(
                    Elements.label().labelFor(label).text("Name"),
                    fragment().child(div().id(label))),
            List.of("label-target")));
  }

  @ParameterizedTest
  @MethodSource("invalidContent")
  @DisplayName("both backends reject the same element and reference errors")
  void bothBackendsRejectTheSameErrors(IntoElement content, List<String> rules) {
    assertThat(Diagnostics.rules(() -> render(content))).containsExactlyInAnyOrderElementsOf(rules);
    assertThat(Diagnostics.rules(() -> page(content))).containsExactlyInAnyOrderElementsOf(rules);
  }

  @Test
  @DisplayName("mounted rules apply beneath the mount host and never restyle the host page")
  void embeddedRulesStayUnderTheMountHost() {
    var theme = Theme.defaults();
    var rules = styleRules(render(main().child(Button.of("Save"))));
    var base = StyleSheet.embeddedBaseRules(theme);

    assertThat(rules)
        .isNotEmpty()
        .noneMatch(rule -> rule.startsWith("@layer reset{") || rule.startsWith("@layer theme{"));
    var hosts =
        List.of(
            "[" + StyleSheet.ROOT_ATTRIBUTE + "]",
            "[" + StyleSheet.THEME_ATTRIBUTE + "=\"" + StyleSheet.themeKey(theme) + "\"]");
    assertThat(base.stream().flatMap(rule -> selectors(rule).stream()))
        .isNotEmpty()
        .allSatisfy(selector -> assertThat(hosts).anyMatch(selector::startsWith));
    assertThat(String.join("", base)).doesNotContain(":root", "@view-transition");
    var custom = theme.with(Tokens.ACCENT_PRIMARY, CssColor.oklch(55, 0.18, 150));
    assertThat(StyleSheet.themeKey(theme))
        .isEqualTo(StyleSheet.themeKey(Theme.defaults()))
        .isNotEqualTo(StyleSheet.themeKey(custom));
  }

  @Test
  @DisplayName("an outline heading renders alike in both backends and feeds the document outline")
  void outlineHeadingFeedsTheDocumentOutline() {
    var target = ElementRef.page("intro");
    var heading = Heading.of(2, "Intro").id(target).outline(target);
    var live = dom(render(main().child(heading)));
    var page = page(DocumentElement.of(main()).child(heading).child(new OutlineEcho()));

    for (var document : List.of(live, page)) {
      assertThat(document.select("h2").text()).isEqualTo("Intro");
      assertThat(document.select("h2").attr("id")).isEqualTo("intro");
    }
    assertThat(page.select("p").text()).isEqualTo("Intro");
  }

  @Test
  @DisplayName("an unbound outline fails static linking and still renders live")
  void unboundOutlineFailsStaticLinking() {
    var content = main().child(Heading.of(2, "Missing").outline(ElementRef.page("missing")));

    assertThat(Diagnostics.rules(() -> page(content))).containsExactly("unbound-reference");
    var live = dom(render(content));
    assertThat(live.select("h2").text()).isEqualTo("Missing");
    assertThat(live.select("h2").hasAttr("id")).isFalse();
  }

  @Test
  @DisplayName("bundled icons render as SVG elements in both backends")
  void bundledIconsRenderAsSvg() {
    var content = main().child(Callout.of(Callout.Kind.NOTE, "Read this"));
    var page = page(content);
    var snapshot = render(content);

    assertThat(page.select("aside svg > *")).isNotEmpty();
    var svg =
        tags(snapshot.roots())
            .filter(tag -> tag.tag().tagName().equals("svg"))
            .findFirst()
            .orElseThrow();
    assertThat(tags(List.of(svg)))
        .allSatisfy(tag -> assertThat(tag.tag().namespace()).isEqualTo(TagName.Namespace.SVG));
    assertThat(semantics(dom(snapshot))).isEqualTo(semantics(page));
  }

  @Test
  @DisplayName("a scope host and a nested scope keep their own markers, with no wrapper added")
  void scopesKeepTheirOwnMarkers() {
    var content =
        main().children(new ItemList(), new Weighted("outer", 600, div().child(new Inner())));

    for (var document : List.of(dom(render(content)), page(content))) {
      var list = document.selectFirst("main > ul[data-scope-id=items]");
      var outer = document.selectFirst("main > div[data-scope-id=outer]");
      var inner = document.selectFirst("main > div > p[data-scope-id=outer--inner]");
      assertThat(list).isNotNull();
      assertThat(list.children().eachText()).containsExactly("One", "Two");
      assertThat(outer).isNotNull();
      assertThat(inner).isNotNull();
      assertThat(outer.attr("data-scope")).isNotEqualTo(inner.attr("data-scope"));
    }
  }

  @Test
  @DisplayName(
      "live declarations compile to plain values in static output and custom properties live")
  void liveDeclarationsCompileDifferentlyPerBackend() {
    var box = div().live(style -> style.inlineSize(CssLength.px(7)));
    var staticCss = Compiler.compile(new PageDocument("Live", main().child(box))).css();
    var liveTag = (LinkedNode.Tag<?>) render(box).roots().getFirst();

    assertThat(staticCss).contains("inline-size:7px").doesNotContain("--tl-");
    assertThat(liveTag.liveProperties()).containsEntry("--tl-inline-size", "7px");
  }

  /// The selectors of the rules in `css`, looking through at-rules.
  private static List<String> selectors(String css) {
    var found = new ArrayList<String>();
    var depth = 0;
    var prelude = new StringBuilder();
    var openAtRules = new ArrayList<Integer>();
    for (var c : css.toCharArray()) {
      if (c == '{') {
        var text = prelude.toString().strip();
        if (text.startsWith("@")) {
          openAtRules.add(depth);
        } else if (openAtRules.contains(depth - 1) || depth == 0) {
          for (var selector : text.split(",(?![^()]*\\))")) {
            found.add(selector.strip());
          }
        }
        depth++;
        prelude.setLength(0);
      } else if (c == '}') {
        depth--;
        openAtRules.remove(Integer.valueOf(depth));
        prelude.setLength(0);
      } else if (c == ';') {
        prelude.setLength(0);
      } else {
        prelude.append(c);
      }
    }
    return found;
  }

  /// Renders the titles collected before this component expands.
  private static final class OutlineEcho implements DocumentComponent {
    @Override
    public DocumentContent expand(DocumentContext context) {
      var titles = context.headings().stream().map(OutlineHeading::title).toList();
      return DocumentContent.of(p().text(String.join(",", titles)));
    }
  }

  private record ItemList() implements Scope {
    @Override
    public String scopeId() {
      return "items";
    }

    @Override
    public TagElement expand(ViewContext context) {
      return ul().children(li().text("One"), li().text("Two"));
    }
  }

  private record Weighted(String scopeId, int weight, TagElement content) implements Scope {
    @Override
    public String scopeName() {
      return "weighted-" + scopeId;
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(style -> style.fontWeight(weight));
    }

    @Override
    public TagElement expand(ViewContext context) {
      return content;
    }
  }

  private record Inner() implements Scope {
    @Override
    public String scopeId() {
      return "inner";
    }

    @Override
    public String scopeName() {
      return "inner";
    }

    @Override
    public void configureScope(ScopeStyle scope) {
      scope.root(style -> style.fontWeight(700));
    }

    @Override
    public TagElement expand(ViewContext context) {
      return p().text("Inside");
    }
  }
}
