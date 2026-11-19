package dev.nthings.tessera.components;

import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.support.AccessibleText;
import dev.nthings.tessera.support.Diagnostics;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("navigation components")
class NavigationComponentsTest {

  private static final Link HOME = new Link("/", "Home");
  private static final Link GUIDE = new Link("/guide", "Guide");

  @Test
  @DisplayName("breadcrumbs list every crumb as a link and mark only the last as current")
  void breadcrumbsMarkOnlyLastCrumbAsCurrent() {
    var nav = renderMain(Breadcrumbs.of(HOME, GUIDE)).element("nav");

    assertThat(nav).hasAttribute("aria-label", "Breadcrumb");
    assertThat(nav.select("a"))
        .extracting(a -> a.attr("href") + " " + a.text() + " " + a.attr("aria-current"))
        .containsExactly("/ Home ", "/guide Guide page");
  }

  @Test
  @DisplayName("breadcrumbs need at least one link")
  void breadcrumbsRejectEmptyTrail() {
    assertThatIllegalArgumentException().isThrownBy(Breadcrumbs::of);
  }

  @ParameterizedTest
  @ValueSource(strings = {"/guide", "/guide/", "/missing"})
  @DisplayName("nav tabs mark the link whose href matches exactly")
  void navTabsMarkExactRoute(String active) {
    var nav =
        renderMain(NavTabs.of("Sections", List.of(HOME, GUIDE)).active(active)).element("nav");

    assertThat(nav).hasAttribute("aria-label", "Sections");
    assertThat(nav.select("a")).extracting(Element::text).containsExactly("Home", "Guide");
    assertThat(nav.select("a[aria-current=page]"))
        .extracting(a -> a.attr("href"))
        .containsExactlyElementsOf(active.equals("/guide") ? List.of("/guide") : List.of());
  }

  static Stream<Arguments> pagers() {
    return Stream.of(
        argumentSet("previous only", DocPager.of().previous(HOME), List.of(HOME)),
        argumentSet("next only", DocPager.of().next(GUIDE), List.of(GUIDE)),
        argumentSet("both", DocPager.of().previous(HOME).next(GUIDE), List.of(HOME, GUIDE)));
  }

  @ParameterizedTest
  @MethodSource("pagers")
  @DisplayName("a doc pager links the sides it has, in reading order")
  void docPagerLinksItsSides(DocPager pager, List<Link> links) {
    var nav = renderMain(pager).element("nav");

    assertThat(nav).hasAttribute("aria-label", "Documentation navigation");
    var anchors = nav.select("a");
    assertThat(anchors)
        .extracting(a -> a.attr("href"))
        .containsExactlyElementsOf(links.stream().map(Link::href).toList());
    for (var index = 0; index < links.size(); index++) {
      assertThat(AccessibleText.of(anchors.get(index))).endsWith(links.get(index).label());
    }
  }

  static Stream<Arguments> repeatedLandmarks() {
    return Stream.of(
        argumentSet("breadcrumbs", Breadcrumbs.of(HOME), Breadcrumbs.of(HOME)),
        argumentSet(
            "nav tabs",
            NavTabs.of("Sections", List.of(HOME)),
            NavTabs.of("Sections", List.of(HOME))),
        argumentSet("doc pagers", DocPager.of().next(GUIDE), DocPager.of().next(GUIDE)));
  }

  @ParameterizedTest
  @MethodSource("repeatedLandmarks")
  @DisplayName("two navigation landmarks with one name are reported")
  void reportsRepeatedLandmarkName(IntoElement first, IntoElement second) {
    assertThat(Diagnostics.of(() -> renderMain(first, second)))
        .singleElement()
        .satisfies(found -> {
          assertThat(found.rule()).isEqualTo("landmark-name-unique");
          assertThat(found.tag()).isEqualTo("nav");
        });
  }

  @Test
  @DisplayName("distinct labels let two trails share a page")
  void distinctLabelsAreAccepted() {
    var page =
        renderMain(
            Breadcrumbs.of(HOME).label("Primary trail"),
            Breadcrumbs.of(HOME).label("Mobile trail"));

    assertThat(page.elements("nav"))
        .extracting(nav -> nav.attr("aria-label"))
        .containsExactly("Primary trail", "Mobile trail");
  }
}
