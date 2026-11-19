package dev.nthings.tessera.document;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.internal.document.DocumentNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.owasp.encoder.Encode;

/// Static factories for content that only a static document places. Build markup with `Elements`
/// and wrap it in [DocumentElement] when it holds document children.
public final class Documents {

  private static final Pattern SCRIPT_CLOSE = Pattern.compile("</script", Pattern.CASE_INSENSITIVE);

  private Documents() {}

  /// Creates a node for pre-sanitized HTML or inline SVG, emitted verbatim without escaping,
  /// parsing or validation. Ids, links and ARIA inside it are not checked by `duplicate-id`,
  /// `missing-fragment`, site link validation or the accessibility rules, so never pass
  /// user-controlled text. Browser roots do not accept raw HTML.
  public static DocumentContent rawHtml(String trustedHtml) {
    return new DocumentPlacement(new DocumentNode.Raw(trustedHtml));
  }

  /// Creates a `<script>` element of `type`, such as `module` or `speculationrules`, whose body is
  /// the trusted `source`. Scripts belong to document assembly: portable elements cannot be script
  /// elements. A literal `</script` in `source` is escaped so it cannot close the element; nothing
  /// else is validated, so never pass user-controlled text. Throws IllegalArgumentException if
  /// `type` is blank.
  public static DocumentContent script(String type, String source) {
    if (type.isBlank()) {
      throw new IllegalArgumentException("A script type must not be blank");
    }
    return rawHtml(
        "<script type=\""
            + Encode.forHtmlAttribute(type)
            + "\">"
            + SCRIPT_CLOSE.matcher(source).replaceAll("<\\\\/script")
            + "</script>");
  }

  /// Moves `child` to the end of `<body>` at priority 0, for content that cannot sit in its
  /// parent's content model, such as a dialog or popover inside a parent that does not allow it.
  /// The child keeps its id scope and DOM ids.
  public static DocumentContent deferred(IntoElement child) {
    return deferred(DocumentContent.of(child), 0);
  }

  /// Moves `child` to the end of `<body>` with `priority`. Lower priorities are placed first, and
  /// equal priorities keep placement order. The child keeps its id scope and DOM ids.
  public static DocumentContent deferred(IntoElement child, int priority) {
    return deferred(DocumentContent.of(child), priority);
  }

  /// Moves document `child` to the end of `<body>` at priority 0.
  public static DocumentContent deferred(DocumentContent child) {
    return deferred(child, 0);
  }

  /// Moves document `child` to the end of `<body>` with `priority`.
  public static DocumentContent deferred(DocumentContent child, int priority) {
    return new DocumentPlacement(new DocumentNode.Deferred(DocumentNode.capture(child), priority));
  }

  /// Creates content resolved after the rest of the page body is expanded, in the context where it
  /// appears. Use it to read what other elements register, such as [DocumentContext#headings()].
  /// Late content resolves first in, first out, before document-end sections and head
  /// contributions.
  public static DocumentContent late(Function<DocumentContext, ? extends DocumentContent> content) {
    return new DocumentPlacement(new DocumentNode.Late(content));
  }

  /// Declares a host for `view` from an already built `bundle`. The compiler validates and
  /// publishes the fallback as document content. The host id is a page-wide DOM id and cannot
  /// contain `=` (the host/view separator) or a null character. A view must be nonblank and contain
  /// no null character.
  public static DocumentContent wasmMount(
      String id, String view, WasmBundle bundle, DocumentContent fallback) {
    if (id.indexOf('=') >= 0 || id.indexOf(0) >= 0) {
      throw new IllegalArgumentException("Invalid Wasm mount host id: " + id);
    }
    if (view.isBlank() || view.indexOf(0) >= 0) {
      throw new IllegalArgumentException("A Wasm mount needs a nonblank view name");
    }
    return new DocumentPlacement(
        new DocumentNode.Mount(ElementRef.page(id), view, bundle, DocumentNode.capture(fallback)));
  }

  /// Declares a Wasm host with portable fallback content.
  public static DocumentContent wasmMount(
      String id, String view, WasmBundle bundle, IntoElement fallback) {
    return wasmMount(id, view, bundle, DocumentContent.of(fallback));
  }

  /// Groups document `children` as siblings without wrapper markup.
  public static DocumentContent fragment(DocumentContent... children) {
    return fragment(List.of(children));
  }

  /// Groups document `children` as siblings without wrapper markup, in iteration order.
  public static DocumentContent fragment(Collection<? extends DocumentContent> children) {
    var nodes = new ArrayList<DocumentNode>(children.size());
    for (var child : children) {
      nodes.add(DocumentNode.capture(child));
    }
    return new DocumentPlacement(new DocumentNode.Fragment(nodes));
  }
}
