package dev.nthings.tessera.showcase;

import dev.nthings.tessera.core.DocumentContent;
import dev.nthings.tessera.document.components.DocLayout;
import java.util.List;
import java.util.function.Supplier;

/// Showcase route: nav metadata, search-indexed demo sections, and body (by default a scroll-spy
/// documentation layout).
record Page(
    String path,
    String navLabel,
    String title,
    String summary,
    List<Demo> demos,
    Supplier<? extends DocumentContent> body) {

  static Page ofDemos(String path, String navLabel, String title, String summary, Demo... demos) {
    var sections = List.of(demos);
    return new Page(path, navLabel, title, summary, sections, () -> docLayout(sections));
  }

  static Page custom(
      String path,
      String navLabel,
      String title,
      String summary,
      Supplier<? extends DocumentContent> content) {
    return new Page(path, navLabel, title, summary, List.of(), content);
  }

  DocumentContent content() {
    return body.get();
  }

  static DocumentContent docLayout(List<? extends DocumentContent> sections) {
    return DocLayout.of().children(sections);
  }

  /// Fragment link to a demo section, by its page-level id.
  String sectionHref(Demo demo) {
    return path + "#" + demo.id();
  }
}
