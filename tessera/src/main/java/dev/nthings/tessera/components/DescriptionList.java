package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.GridTrack;
import dev.nthings.tessera.css.Tokens;
import dev.nthings.tessera.css.TrackList;
import java.util.ArrayList;
import java.util.List;

/// Description list: each term is a `<dt>` and each description a `<dd>`, laid out in two columns.
/// Create one with [#of()] and add terms.
public final class DescriptionList extends BuiltinComponent {

  private record Term(String term, Element description) {}

  private final List<Term> terms = new ArrayList<>();

  private DescriptionList() {}

  /// Creates an empty description list.
  public static DescriptionList of() {
    return new DescriptionList();
  }

  /// Adds `term` described by the plain text `description`.
  public DescriptionList term(String term, String description) {
    return term(term, Elements.text(description));
  }

  /// Adds `term` described by `description`, which may contain markup.
  public DescriptionList term(String term, IntoElement description) {
    terms.add(new Term(term, description.intoElement()));
    return this;
  }

  @Override
  IntoElement expand(ViewContext context) {
    var dl =
        Elements.dl()
            .grid()
            .gridColumns(
                TrackList.of(
                    GridTrack.minmax(GridTrack.length(CssLength.rem(8)), GridTrack.maxContent()),
                    GridTrack.fr(1)))
            .gap(Tokens.SPACE_3);
    for (var entry : terms) {
      dl.children(
          Elements.dt().fontWeight(600).textColor(Tokens.TEXT_PRIMARY).text(entry.term()),
          Elements.dd().textColor(Tokens.TEXT_MUTED).child(entry.description()));
    }
    return dl;
  }
}
