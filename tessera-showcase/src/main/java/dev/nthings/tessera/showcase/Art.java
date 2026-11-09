package dev.nthings.tessera.showcase;

import dev.nthings.tessera.compiler.Asset;
import java.util.List;
import java.util.Locale;

/// Generated SVG artwork published as static site assets.
final class Art {

  static final String DOCS_BANNER = "/assets/docs-banner.svg";
  static final String MEDIA_BANNER = "/assets/media-card.svg";

  private Art() {}

  static List<Asset> assets() {
    return List.of(
        Asset.text(DOCS_BANNER, banner("Tessera docs")),
        Asset.text(MEDIA_BANNER, banner("Media card")));
  }

  /// Wide gradient banner with a tessellated tile motif.
  static String banner(String label) {
    var svg = new StringBuilder();
    svg.append("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1200 480'>")
        .append("<defs><linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>")
        .append("<stop offset='0' stop-color='oklch(55% 0.2 265)'/>")
        .append("<stop offset='1' stop-color='oklch(70% 0.15 200)'/>")
        .append("</linearGradient></defs><rect width='1200' height='480' fill='url(#g)'/>");
    for (var row = 0; row < 6; row++) {
      for (var col = 0; col < 15; col++) {
        var opacity = 0.04 + ((row * 7 + col * 3) % 9) * 0.02;
        svg.append("<rect x='")
            .append(col * 80 + 8)
            .append("' y='")
            .append(row * 80 + 8)
            .append("' width='64' height='64' rx='12' fill='white' fill-opacity='")
            .append(String.format(Locale.ROOT, "%.2f", opacity))
            .append("'/>");
      }
    }
    svg.append("<text x='64' y='400' font-family='system-ui' font-size='72' font-weight='700' ")
        .append("fill='white'>")
        .append(label)
        .append("</text></svg>");
    return svg.toString();
  }
}
