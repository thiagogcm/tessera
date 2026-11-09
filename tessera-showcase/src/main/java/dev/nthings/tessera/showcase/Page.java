package dev.nthings.tessera.showcase;

import dev.nthings.tessera.components.Toc;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import java.util.List;
import java.util.function.Supplier;

/// Showcase route: nav metadata, search-indexed demo sections, and body (by default a
/// scroll-spy documentation layout).
record Page(
    String path,
    String navLabel,
    String title,
    String summary,
    List<Demo> demos,
    Supplier<? extends IntoElement> body) {

  static final String TOC_ID = "toc";

  static Page ofDemos(String path, String navLabel, String title, String summary, Demo... demos) {
    var sections = List.of(demos);
    return new Page(path, navLabel, title, summary, sections, () -> docLayout(sections));
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
    return body.get().intoElement();
  }

  static Element docLayout(List<? extends IntoElement> sections) {
    return Toc.docLayout(TOC_ID, "On this page", sections.toArray(IntoElement[]::new))
        .intoElement();
  }

  /// Fragment link to a demo section, by its page-level id.
  String sectionHref(Demo demo) {
    return path + "#" + demo.id();
  }
}
