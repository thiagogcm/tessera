package dev.nthings.tessera.view;

import static dev.nthings.tessera.core.Elements.article;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.fragment;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.main;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.ul;
import static dev.nthings.tessera.support.Snapshots.render;
import static dev.nthings.tessera.view.LiveDom.dom;
import static dev.nthings.tessera.view.LiveDom.page;
import static dev.nthings.tessera.view.LiveDom.semantics;
import static org.assertj.core.api.Assertions.assertThat;

import dev.nthings.tessera.components.Field;
import dev.nthings.tessera.components.Forms;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.Scope;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.support.Diagnostics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Keyed identity")
class KeyedIdentityTest {

  private static TagElement keyedFields(List<String> names) {
    var container = main();
    names.forEach(name ->
        container.child(
            Field.of("Field " + name, Forms.text(name)).hint("About " + name).key(name)));
    return container;
  }

  /// Each input's id by its name, after checking its label and description resolve to it.
  private static Map<String, String> fieldIds(Document document) {
    var ids = new HashMap<String, String>();
    for (var input : document.select("input")) {
      assertThat(document.getElementById(input.attr("aria-describedby")))
          .as("description of %s", input.attr("name"))
          .isNotNull();
      assertThat(document.select("label[for=" + input.id() + "]")).hasSize(1);
      ids.put(input.attr("name"), input.id());
    }
    return ids;
  }

  @Test
  @DisplayName("keyed fields keep their ids when siblings are inserted, removed and reordered")
  void keyedFieldsKeepTheirIds() {
    var first = fieldIds(dom(render(keyedFields(List.of("a", "b")))));
    var next = fieldIds(dom(render(keyedFields(List.of("c", "b", "a")))));
    var removed = fieldIds(page(keyedFields(List.of("b"))));

    assertThat(next).containsAllEntriesOf(first);
    assertThat(removed).containsEntry("b", first.get("b"));
    assertThat(fieldIds(page(keyedFields(List.of("a", "b"))))).isEqualTo(first);
  }

  @Test
  @DisplayName("keys are exact sibling identities, not normalized DOM identifiers")
  void keysAreExactSiblingIdentities() {
    var keys = List.of("row-a", "row_a", "Row-a", "row a");

    for (var document : List.of(dom(render(keyedFields(keys))), page(keyedFields(keys)))) {
      assertThat(fieldIds(document).values()).hasSize(4).doesNotHaveDuplicates();
    }
  }

  @Test
  @DisplayName("a key repeated among siblings is reported in both backends")
  void duplicateSiblingKeysAreReported() {
    var repeated = keyedFields(List.of("a", "a"));

    assertThat(Diagnostics.rules(() -> render(repeated))).contains("duplicate-key");
    assertThat(Diagnostics.rules(() -> page(repeated))).contains("duplicate-key");
  }

  @Test
  @DisplayName("the same key under separately named parents gives distinct, linked ids")
  void sameKeyUnderNamedParents() {
    var content =
        main()
            .children(
                div().id(ElementRef.scoped("left")).child(keyed("Left")),
                div().id(ElementRef.scoped("right")).child(keyed("Right")));
    var late =
        DocumentElement.of(main())
            .children(
                DocumentElement.of(div().id(ElementRef.scoped("left")))
                    .child(Documents.late(_ -> DocumentContent.of(keyed("Left")))),
                DocumentElement.of(div().id(ElementRef.scoped("right")))
                    .child(Documents.late(_ -> DocumentContent.of(keyed("Right")))));

    for (var document : List.of(dom(render(content)), page(content), page(late))) {
      assertThat(fieldIds(document).values()).hasSize(2).doesNotHaveDuplicates();
    }
  }

  private static IntoElement keyed(String label) {
    return Field.of(label, Forms.text(label.toLowerCase())).hint(label + " hint").key("same");
  }

  @Test
  @DisplayName("keyed and unkeyed fragments place their children directly in the parent")
  void fragmentsPreserveDirectChildren() {
    var content =
        main()
            .child(
                ul().children(
                        fragment().children(li().text("First"), li().text("Second")).key("pair"),
                        fragment().child(li().text("Third")),
                        li().text("Fourth").key("fourth")));
    var page = page(content);

    assertThat(semantics(dom(render(content)))).isEqualTo(semantics(page));
    assertThat(page.select("main > ul > *").eachText())
        .containsExactly("First", "Second", "Third", "Fourth");
  }

  @Test
  @DisplayName("a keyed scope keeps its host markers and scope id when siblings change")
  void keyedScopeKeepsItsHost() {
    var alone = main().child(new Card().key("a"));
    var crowded = main().children(new Card().key("z"), new Card().key("a"));

    var hosts = new ArrayList<Element>();
    for (var document :
        List.of(dom(render(alone)), page(alone), dom(render(crowded)), page(crowded))) {
      var host = document.select("main > article").last();
      assertThat(host).isNotNull();
      assertThat(host.attr("data-scope")).isNotBlank();
      assertThat(host.select("p").text()).isEqualTo("Body");
      assertThat(document.select("main > :not(article)")).isEmpty();
      hosts.add(host);
    }
    assertThat(hosts)
        .extracting(host -> host.attr("data-scope-id"))
        .containsOnly(hosts.getFirst().attr("data-scope-id"));
  }

  /// A scope with a generated id and an article host.
  private record Card() implements Scope {
    @Override
    public String scopeName() {
      return "card";
    }

    @Override
    public TagElement expand(ViewContext context) {
      return article().child(p().text("Body"));
    }
  }
}
