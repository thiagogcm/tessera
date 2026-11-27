package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.HtmlAssertions.assertThatIllegalArgumentException;
import static dev.nthings.tessera.support.RenderedPage.renderMain;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import dev.nthings.tessera.core.IntoElement;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Carousel")
class CarouselTest {

  private static final Pattern ANCHOR_NAME = Pattern.compile("anchor-name:(--[\\w-]+)");
  private static final Pattern POSITION_ANCHOR = Pattern.compile("position-anchor:(--[\\w-]+)");

  @Test
  @DisplayName("is a focusable region named by its label that holds each slide in order")
  void trackIsFocusableRegion() {
    var page = renderMain(Carousel.of("Demo").slide(p().text("1")).slide(p().text("2")));

    var track = page.element("[role=region]");
    assertThat(track).hasAttribute("aria-label", "Demo").hasAttribute("tabindex", "0");
    assertThat(track.select("p")).extracting(Element::text).containsExactly("1", "2");
  }

  static Stream<Arguments> separateCarousels() {
    return Stream.of(
        argumentSet(
            "one id in two cards",
            Card.of("Pricing")
                .scopeId("pricing")
                .body(Carousel.of("Pricing shots").scopeId("gallery").slide(p().text("1"))),
            Card.of("Support")
                .scopeId("support")
                .body(Carousel.of("Support shots").scopeId("gallery").slide(p().text("2")))),
        argumentSet(
            "non-ASCII labels",
            Carousel.of("写真").slide(p().text("1")),
            Carousel.of("動画").slide(p().text("2"))));
  }

  @ParameterizedTest
  @MethodSource("separateCarousels")
  @DisplayName("each carousel anchors its scroll buttons to its own track")
  void scrollButtonsAnchorToTheirOwnTrack(IntoElement first, IntoElement second) {
    var page = renderMain(first, second);

    var anchors =
        page.elements("[role=region]").stream()
            .map(page::scopeBody)
            .map(css -> {
              var named = ANCHOR_NAME.matcher(css).results().map(m -> m.group(1)).toList();
              assertThat(named).hasSize(1);
              assertThat(POSITION_ANCHOR.matcher(css).results().map(m -> m.group(1)))
                  .containsOnly(named.getFirst());
              return named.getFirst();
            })
            .toList();
    assertThat(anchors).hasSize(2).doesNotHaveDuplicates();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " "})
  @DisplayName("rejects a blank label, which would leave the region unnamed")
  void rejectsBlankLabel(String label) {
    assertThatIllegalArgumentException().isThrownBy(() -> Carousel.of(label));
  }
}
