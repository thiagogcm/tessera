package dev.nthings.tessera.components;

import static dev.nthings.tessera.core.Elements.el;
import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.support.HtmlAssertions.assertThat;
import static dev.nthings.tessera.support.RenderedPage.renderMain;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Role;
import dev.nthings.tessera.core.SvgTag;
import dev.nthings.tessera.support.Diagnostics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Icon and SVG tags")
class IconTest {

  @Test
  @DisplayName("a stroked icon draws its shapes on a 24-unit grid in currentColor, hidden from AT")
  void drawsStrokedIcons() {
    var page =
        renderMain(
            Icon.stroked(
                20,
                Icon.path("M4 11a9 9 0 0 1 9 9"),
                Icon.circle(5, 19, 1),
                Icon.rect(3, 3, 18, 18, 2),
                Icon.ellipse(12, 12, 4, 2.5),
                Icon.line(1, 2, 3, 4),
                Icon.polyline("9 18 15 12 9 6"),
                Icon.polygon("1 1 2 2 1 2")));

    var icon = page.element("main > svg");
    assertThat(icon.attr("width")).isEqualTo("20");
    assertThat(icon.attr("viewBox")).isEqualTo("0 0 24 24");
    assertThat(icon.attr("stroke")).isEqualTo("currentColor");
    assertThat(icon.attr("stroke-width")).isEqualTo("2");
    assertThat(icon.attr("fill")).isEqualTo("none");
    assertThat(icon.attr("aria-hidden")).isEqualTo("true");
    assertThat(icon.children())
        .extracting(child -> child.tagName())
        .containsExactly("path", "circle", "rect", "ellipse", "line", "polyline", "polygon");
    assertThat(icon.selectFirst("rect").attr("rx")).isEqualTo("2");
    assertThat(icon.selectFirst("ellipse").attr("ry")).isEqualTo("2.5");
  }

  @Test
  @DisplayName("a meaningful icon can be named as an image")
  void namesMeaningfulIcons() {
    var page =
        renderMain(
            Icon.stroked(16, Icon.circle(12, 12, 9))
                .ariaHidden(false)
                .role(Role.IMG)
                .ariaLabel("Status"));

    var icon = page.element("main > svg");
    assertThat(icon.attr("role")).isEqualTo("img");
    assertThat(icon.attr("aria-label")).isEqualTo("Status");
    assertThat(icon.attr("aria-hidden")).isNotEqualTo("true");
  }

  @Test
  @DisplayName("gradients, symbols, text and titles keep their SVG names, case included")
  void writesDiagramTags() {
    var page =
        renderMain(
            Icon.svg(100, 40)
                .ariaHidden(false)
                .role(Role.IMG)
                .ariaLabel("Logo")
                .children(
                    el(SvgTag.TITLE).child(text("Logo")),
                    el(SvgTag.DESC).child(text("A mark and a word")),
                    el(SvgTag.DEFS)
                        .children(
                            el(SvgTag.LINEAR_GRADIENT)
                                .id(ElementRef.page("fade"))
                                .children(
                                    el(SvgTag.STOP).attr("offset", "0"),
                                    el(SvgTag.STOP).attr("offset", "1")),
                            el(SvgTag.RADIAL_GRADIENT).id(ElementRef.page("glow")),
                            el(SvgTag.CLIP_PATH)
                                .id(ElementRef.page("clip"))
                                .child(Icon.rect(0, 0, 40, 40, 4)),
                            el(SvgTag.MASK).id(ElementRef.page("mask")),
                            el(SvgTag.PATTERN).id(ElementRef.page("dots")),
                            el(SvgTag.MARKER).id(ElementRef.page("arrow")),
                            el(SvgTag.SYMBOL)
                                .id(ElementRef.page("mark"))
                                .attr("viewBox", "0 0 24 24")
                                .child(Icon.circle(12, 12, 10))),
                    el(SvgTag.USE).attr("href", "#mark").attr("width", "40"),
                    el(SvgTag.TEXT)
                        .attr("x", "48")
                        .attr("y", "26")
                        .children(text("tess"), el(SvgTag.TSPAN).child(text("era")))));

    var html = page.dom().selectFirst("main").html();
    assertThat(html)
        .contains("<linearGradient", "</linearGradient>", "<radialGradient", "<clipPath")
        .contains("<title>Logo</title>", "<tspan>era</tspan>", "<use href=\"#mark\"");
    assertThat(page.element("main svg text").text()).isEqualTo("tessera");
  }

  @Test
  @DisplayName("the new SVG tags stay inside svg")
  void reportsDiagramTagsOutsideSvg() {
    assertThat(Diagnostics.rules(() -> renderMain(el(SvgTag.LINEAR_GRADIENT))))
        .containsExactly("svg-namespace");
  }
}
