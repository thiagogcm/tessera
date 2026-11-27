package dev.nthings.tessera.markdown;

import static dev.nthings.tessera.core.Elements.a;
import static dev.nthings.tessera.core.Elements.blockquote;
import static dev.nthings.tessera.core.Elements.br;
import static dev.nthings.tessera.core.Elements.code;
import static dev.nthings.tessera.core.Elements.del;
import static dev.nthings.tessera.core.Elements.div;
import static dev.nthings.tessera.core.Elements.em;
import static dev.nthings.tessera.core.Elements.hr;
import static dev.nthings.tessera.core.Elements.input;
import static dev.nthings.tessera.core.Elements.li;
import static dev.nthings.tessera.core.Elements.ol;
import static dev.nthings.tessera.core.Elements.p;
import static dev.nthings.tessera.core.Elements.span;
import static dev.nthings.tessera.core.Elements.strong;
import static dev.nthings.tessera.core.Elements.tbody;
import static dev.nthings.tessera.core.Elements.td;
import static dev.nthings.tessera.core.Elements.text;
import static dev.nthings.tessera.core.Elements.th;
import static dev.nthings.tessera.core.Elements.thead;
import static dev.nthings.tessera.core.Elements.tr;
import static dev.nthings.tessera.core.Elements.ul;

import dev.nthings.tessera.components.Callout;
import dev.nthings.tessera.components.CodeBlock;
import dev.nthings.tessera.components.Heading;
import dev.nthings.tessera.components.Image;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.InputType;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.css.TextAlign;
import dev.nthings.tessera.document.DocumentComponent;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentElement;
import dev.nthings.tessera.document.Documents;
import dev.nthings.tessera.document.components.Footnote;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.commonmark.ext.footnotes.FootnoteDefinition;
import org.commonmark.ext.footnotes.FootnoteReference;
import org.commonmark.ext.footnotes.InlineFootnote;
import org.commonmark.ext.front.matter.YamlFrontMatterBlock;
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor;
import org.commonmark.ext.gfm.alerts.Alert;
import org.commonmark.ext.gfm.alerts.AlertTitle;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.heading.anchor.IdGenerator;
import org.commonmark.ext.image.attributes.ImageAttributes;
import org.commonmark.ext.task.list.items.TaskListItemMarker;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.LinkReferenceDefinition;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.jspecify.annotations.Nullable;

/// Converts one parsed Markdown document to Tessera content. Each node becomes [Out]: portable when
/// its subtree is plain elements, document content when it holds raw HTML, a footnote reference, or
/// a fence renderer's output. Containers stay portable while all their children are.
final class MarkdownConverter {

  /// The converted form of one node.
  private sealed interface Out {

    record Portable(Element element) implements Out {}

    record Document(DocumentContent content) implements Out {}

    default DocumentContent document() {
      return switch (this) {
        case Portable portable -> DocumentContent.of(portable.element());
        case Document document -> document.content();
      };
    }
  }

  private final String source;
  private final Map<String, Function<Fence, ? extends DocumentContent>> fences;
  private final UnaryOperator<String> urlResolver;
  private final boolean permalinks;
  private final boolean smartPunctuation;
  private final IdGenerator headingIds = IdGenerator.builder().build();
  private final Map<String, FootnoteDefinition> definitions = new HashMap<>();
  private final Map<String, Footnote> footnotes = new HashMap<>();
  private final List<DocumentContent> problems = new ArrayList<>();
  private int inlineFootnotes;

  MarkdownConverter(
      String source,
      Map<String, Function<Fence, ? extends DocumentContent>> fences,
      UnaryOperator<String> urlResolver,
      boolean permalinks,
      boolean smartPunctuation) {
    this.source = source;
    this.fences = fences;
    this.urlResolver = urlResolver;
    this.permalinks = permalinks;
    this.smartPunctuation = smartPunctuation;
  }

  FrontMatter frontMatter(Node document) {
    var block = document.getFirstChild();
    if (!(block instanceof YamlFrontMatterBlock)) {
      return FrontMatter.NONE;
    }
    var spans = block.getSourceSpans();
    var yaml = new StringBuilder();
    // The first and last spans are the `---` separators.
    for (int i = 1; i < spans.size() - 1; i++) {
      var span = spans.get(i);
      yaml.append(source, span.getInputIndex(), span.getInputIndex() + span.getLength())
          .append('\n');
    }
    return new FrontMatter(yaml.toString(), YamlFrontMatterVisitor.readData(document));
  }

  DocumentContent body(Document document) {
    for (var node = document.getFirstChild(); node != null; node = node.getNext()) {
      if (node instanceof FootnoteDefinition definition) {
        definitions.putIfAbsent(definition.getLabel().toLowerCase(Locale.ROOT), definition);
      }
    }
    var blocks = new ArrayList<DocumentContent>();
    for (var node = document.getFirstChild(); node != null; node = node.getNext()) {
      var out = block(node);
      if (out != null) {
        blocks.add(out.document());
      }
    }
    blocks.addAll(problems);
    return Documents.fragment(blocks);
  }

  // --- Blocks -----------------------------------------------------------------------------

  private @Nullable Out block(Node node) {
    return switch (node) {
      case YamlFrontMatterBlock ignored -> null;
      case LinkReferenceDefinition ignored -> null;
      case FootnoteDefinition ignored -> null;
      case org.commonmark.node.Heading heading -> heading(heading);
      case Paragraph paragraph -> container(p(), inlines(paragraph));
      case BlockQuote quote -> container(blockquote(), blocks(quote));
      case BulletList list -> container(ul(), items(list));
      case OrderedList list -> container(orderedList(list), items(list));
      case FencedCodeBlock fenced -> fence(fenced);
      case IndentedCodeBlock indented -> portable(CodeBlock.of(trimEnd(indented.getLiteral())));
      case ThematicBreak ignored -> portable(hr());
      case HtmlBlock html -> new Out.Document(Documents.rawHtml(html.getLiteral()));
      case TableBlock tableBlock -> table(tableBlock);
      case Alert alert -> alert(alert);
      case TaskListItemMarker marker -> taskMarker(marker);
      default -> unsupported(node);
    };
  }

  private List<Out> blocks(Node parent) {
    var out = new ArrayList<Out>();
    for (var child = parent.getFirstChild(); child != null; child = child.getNext()) {
      var converted = block(child);
      if (converted != null) {
        out.add(converted);
      }
    }
    return out;
  }

  private Out heading(org.commonmark.node.Heading heading) {
    var plain = plainText(heading).strip();
    var ref = ElementRef.page(headingIds.generateId(plain.isEmpty() ? "section" : plain));
    var label = smart(plain);
    var content = portableOnly(inlines(heading), "a heading", heading);
    var element =
        Heading.of(heading.getLevel(), label, content.toArray(IntoElement[]::new))
            .id(ref)
            .outline(ref);
    return portable(permalinks && heading.getLevel() > 1 ? element.permalink() : element);
  }

  private TagElement orderedList(OrderedList list) {
    var start = list.getMarkerStartNumber();
    return start != null && start != 1 ? ol().attr("start", start.toString()) : ol();
  }

  private List<Out> items(ListBlock list) {
    var items = new ArrayList<Out>();
    for (var item = list.getFirstChild(); item != null; item = item.getNext()) {
      if (item instanceof ListItem listItem) {
        items.add(container(li(), itemContent(listItem, list.isTight())));
      }
    }
    return items;
  }

  /// A tight list item's paragraphs lose their `<p>`, as in GitHub's rendering.
  private List<Out> itemContent(ListItem item, boolean tight) {
    var content = new ArrayList<Out>();
    for (var child = item.getFirstChild(); child != null; child = child.getNext()) {
      if (tight && child instanceof Paragraph paragraph) {
        content.addAll(inlines(paragraph));
      } else {
        var converted = block(child);
        if (converted != null) {
          content.add(converted);
        }
      }
    }
    return content;
  }

  private Out fence(FencedCodeBlock fenced) {
    var info = fenced.getInfo() == null ? "" : fenced.getInfo().strip();
    var language = info.isEmpty() ? "" : info.split("\\s+", 2)[0];
    var code = trimEnd(fenced.getLiteral());
    var renderer = fences.get(language.toLowerCase(Locale.ROOT));
    if (renderer != null) {
      return new Out.Document(renderer.apply(new Fence(language, info, code)));
    }
    var block = CodeBlock.of(code);
    return portable(language.isEmpty() ? block : block.language(language));
  }

  private Out table(TableBlock tableBlock) {
    var sections = new ArrayList<Out>();
    for (var section = tableBlock.getFirstChild(); section != null; section = section.getNext()) {
      var rows = new ArrayList<Out>();
      for (var row = section.getFirstChild(); row != null; row = row.getNext()) {
        if (row instanceof TableRow) {
          rows.add(container(tr(), cells(row)));
        }
      }
      if (section instanceof TableHead) {
        sections.add(container(thead(), rows));
      } else if (section instanceof TableBody) {
        sections.add(container(tbody(), rows));
      }
    }
    return container(Elements.table(), sections);
  }

  private List<Out> cells(Node row) {
    var cells = new ArrayList<Out>();
    for (var node = row.getFirstChild(); node != null; node = node.getNext()) {
      if (node instanceof TableCell cell) {
        var host = cell.isHeader() ? th().attr("scope", "col") : td();
        var alignment = cell.getAlignment();
        if (alignment != null) {
          host.textAlign(
              switch (alignment) {
                case LEFT -> TextAlign.START;
                case CENTER -> TextAlign.CENTER;
                case RIGHT -> TextAlign.END;
              });
        }
        cells.add(container(host, inlines(cell)));
      }
    }
    return cells;
  }

  private Out alert(Alert alert) {
    var kind = Callout.Kind.valueOf(alert.getType().toUpperCase(Locale.ROOT));
    var body = new ArrayList<Out>();
    String title = null;
    for (var child = alert.getFirstChild(); child != null; child = child.getNext()) {
      if (child instanceof AlertTitle alertTitle) {
        title = smart(plainText(alertTitle).strip());
      } else {
        var converted = block(child);
        if (converted != null) {
          body.add(converted);
        }
      }
    }
    var callout =
        Callout.of(kind, portableOnly(body, "an alert", alert).toArray(IntoElement[]::new));
    return portable(title == null || title.isEmpty() ? callout : callout.title(title));
  }

  // --- Inlines ----------------------------------------------------------------------------

  private List<Out> inlines(Node parent) {
    var out = new ArrayList<Out>();
    for (var child = parent.getFirstChild(); child != null; child = child.getNext()) {
      var converted = inline(child);
      if (converted != null) {
        out.add(converted);
      }
    }
    return out;
  }

  private @Nullable Out inline(Node node) {
    return switch (node) {
      case Text text -> portable(text(smartText(text)));
      case SoftLineBreak ignored -> portable(text("\n"));
      case HardLineBreak ignored -> portable(br());
      case Code code -> portable(code(code.getLiteral()));
      case Emphasis emphasis -> container(em(), inlines(emphasis));
      case StrongEmphasis strong -> container(strong(), inlines(strong));
      case Strikethrough strikethrough -> container(del(), inlines(strikethrough));
      case org.commonmark.node.Link link -> link(link);
      case org.commonmark.node.Image image -> image(image);
      case ImageAttributes ignored -> null;
      case HtmlInline html -> new Out.Document(Documents.rawHtml(html.getLiteral()));
      case FootnoteReference reference -> footnoteReference(reference);
      case InlineFootnote footnote -> inlineFootnote(footnote);
      default -> unsupported(node);
    };
  }

  private Out link(org.commonmark.node.Link link) {
    var anchor = a(urlResolver.apply(link.getDestination()));
    if (link.getTitle() != null) {
      anchor.title(link.getTitle());
    }
    return container(anchor, inlines(link));
  }

  private Out image(org.commonmark.node.Image node) {
    var image = Image.of(urlResolver.apply(node.getDestination()), smart(plainText(node)));
    var attributes = imageAttributes(node);
    var width = parsePositive(attributes.get("width"));
    var height = parsePositive(attributes.get("height"));
    if (width > 0 && height > 0) {
      image.dimensions(width, height);
    } else {
      image.unknownDimensions();
    }
    var title = node.getTitle();
    if (title != null) {
      image.element(img -> img.title(title));
    }
    return portable(image);
  }

  private static Map<String, String> imageAttributes(org.commonmark.node.Image image) {
    for (var child = image.getFirstChild(); child != null; child = child.getNext()) {
      if (child instanceof ImageAttributes attributes) {
        return attributes.getAttributes();
      }
    }
    return image.getNext() instanceof ImageAttributes attributes
        ? attributes.getAttributes()
        : Map.of();
  }

  private static int parsePositive(@Nullable String value) {
    if (value == null) {
      return 0;
    }
    try {
      return Math.max(0, Integer.parseInt(value.strip()));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private Out footnoteReference(FootnoteReference reference) {
    var label = reference.getLabel().toLowerCase(Locale.ROOT);
    var footnote = footnotes.get(label);
    if (footnote == null) {
      var definition = definitions.get(label);
      if (definition == null) {
        return portable(text("[^" + reference.getLabel() + "]"));
      }
      footnote = Footnote.of(footnoteBody(blocks(definition), definition)).id(label);
      footnotes.put(label, footnote);
    }
    return new Out.Document(footnote.reference());
  }

  private Out inlineFootnote(InlineFootnote node) {
    inlineFootnotes++;
    var body = span().children(portableOnly(inlines(node), "a footnote", node));
    return new Out.Document(Footnote.of(body).id("inline-" + inlineFootnotes).reference());
  }

  /// A one-paragraph note keeps its text inline; longer notes keep their blocks.
  private IntoElement footnoteBody(List<Out> blocks, Node definition) {
    var content = portableOnly(blocks, "a footnote", definition);
    var first = definition.getFirstChild();
    if (content.size() == 1 && first instanceof Paragraph paragraph && first.getNext() == null) {
      return span().children(portableOnly(inlines(paragraph), "a footnote", definition));
    }
    return div().children(content);
  }

  private Out taskMarker(TaskListItemMarker marker) {
    var checkbox =
        input(InputType.CHECKBOX)
            .boolAttr("disabled")
            .initialChecked(marker.isChecked())
            .ariaLabel(marker.isChecked() ? "Done" : "Not done");
    return portable(checkbox);
  }

  // --- Helpers ----------------------------------------------------------------------------

  private static Out portable(IntoElement element) {
    return new Out.Portable(element.intoElement());
  }

  /// `host` holding `children`: portable while every child is, else a document element.
  private static Out container(TagElement host, List<Out> children) {
    if (children.stream().allMatch(Out.Portable.class::isInstance)) {
      for (var child : children) {
        host.child(((Out.Portable) child).element());
      }
      return new Out.Portable(host);
    }
    var element = DocumentElement.of(host);
    for (var child : children) {
      element.child(child.document());
    }
    return new Out.Document(element);
  }

  /// The portable children of a container that only takes portable content. Document children are
  /// dropped and reported when the page compiles.
  private List<IntoElement> portableOnly(List<Out> children, String container, Node node) {
    var portable = new ArrayList<IntoElement>();
    for (var child : children) {
      if (child instanceof Out.Portable(var element)) {
        portable.add(element);
      } else {
        report(
            "Raw HTML, a footnote reference, or a fence renderer's output inside "
                + container
                + " cannot be placed"
                + at(node)
                + ".",
            "Move it out of " + container + ", or write it as Markdown.");
      }
    }
    return portable;
  }

  private Out unsupported(Node node) {
    report(
        "Markdown node "
            + node.getClass().getSimpleName()
            + " has no Tessera form"
            + at(node)
            + ".",
        "Remove the construct or write it as raw HTML.");
    return new Out.Portable(Elements.empty());
  }

  private void report(String message, String fix) {
    problems.add(
        (DocumentComponent)
            context -> {
              context.report("markdown-unplaceable-content", message, fix);
              return Documents.fragment();
            });
  }

  private static String at(Node node) {
    var spans = node.getSourceSpans();
    return spans.isEmpty() ? "" : " on line " + (spans.getFirst().getLineIndex() + 1);
  }

  private String smart(String text) {
    return smartPunctuation ? SmartPunctuation.apply(text, false) : text;
  }

  /// A text run that follows inline content, such as the `'s` after `*word*`, starts mid-word.
  private String smartText(Text text) {
    if (!smartPunctuation) {
      return text.getLiteral();
    }
    var previous = text.getPrevious();
    boolean midWord =
        previous != null
            && !(previous instanceof SoftLineBreak || previous instanceof HardLineBreak);
    return SmartPunctuation.apply(text.getLiteral(), midWord);
  }

  private static String plainText(Node node) {
    var text = new StringBuilder();
    appendText(node, text);
    return text.toString();
  }

  private static void appendText(Node node, StringBuilder text) {
    for (var child = node.getFirstChild(); child != null; child = child.getNext()) {
      switch (child) {
        case Text literal -> text.append(literal.getLiteral());
        case Code code -> text.append(code.getLiteral());
        case SoftLineBreak ignored -> text.append(' ');
        case HardLineBreak ignored -> text.append(' ');
        default -> appendText(child, text);
      }
    }
  }

  private static String trimEnd(String code) {
    return code.endsWith("\n") ? code.substring(0, code.length() - 1) : code;
  }
}
