package dev.nthings.tessera.showcase;

import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/// Showcase route: navigation metadata plus either demo sections (laid out with a scroll-spy
/// table of contents) or custom content.
record Page(
    String path,
    String navLabel,
    String title,
    String summary,
    List<Demo> demos,
    @Nullable Supplier<? extends IntoElement> customContent) {

  static final String TOC_ID = "toc";

  static Page ofDemos(String path, String navLabel, String title, String summary, Demo... demos) {
    return new Page(path, navLabel, title, summary, List.of(demos), null);
  }

  static Page custom(
      String path,
      String navLabel,
      String title,
      String summary,
      Supplier<? extends IntoElement> content) {
    return new Page(path, navLabel, title, summary, List.of(), content);
  }

  Element content() {
    var custom = customContent;
    if (custom != null) {
      return custom.get().intoElement();
    }
    return Toc.docLayout(TOC_ID, "On this page", demos.toArray(IntoElement[]::new)).intoElement();
  }

  /// Fragment link to a demo section (sections are scoped inside the doc layout view).
  String sectionHref(Demo demo) {
    return path + "#" + TOC_ID + "-layout--" + demo.id();
  }
}
