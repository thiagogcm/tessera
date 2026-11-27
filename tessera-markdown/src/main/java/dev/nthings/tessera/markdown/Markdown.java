package dev.nthings.tessera.markdown;

import com.google.errorprone.annotations.CheckReturnValue;
import dev.nthings.tessera.core.DocumentContent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.footnotes.FootnotesExtension;
import org.commonmark.ext.front.matter.YamlFrontMatterExtension;
import org.commonmark.ext.gfm.alerts.AlertsExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.image.attributes.ImageAttributesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Document;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

/// Parses Markdown into a [MarkdownPage]. The syntax is CommonMark with the GitHub Flavored
/// Markdown extensions: tables, strikethrough, task lists, autolinks, footnotes (including inline
/// `^[notes]`), and alerts such as `> [!NOTE]` (with custom titles and nesting). Two more
/// extensions read YAML front matter and image sizes written as `![alt](src){width=640
/// height=480}`.
///
/// The body is built from Tessera elements rather than an HTML string:
///
/// | Markdown | Tessera |
/// | --- | --- |
/// | Heading | [dev.nthings.tessera.components.Heading] with a GitHub-style id, in the page outline |
/// | Fenced or indented code | [dev.nthings.tessera.components.CodeBlock], or a [Fence] renderer |
/// | Alert | [dev.nthings.tessera.components.Callout] |
/// | Footnote | [dev.nthings.tessera.document.components.Footnote] |
/// | Image | [dev.nthings.tessera.components.Image] |
/// | Table | Native `<table>` markup with column alignment |
/// | Raw HTML | [dev.nthings.tessera.document.Documents#rawHtml(String)], emitted as written |
///
/// Links keep their `href`, so site link validation checks them; [#withUrlResolver(UnaryOperator)]
/// can rewrite them first, for example from `guide.md` to `/guide/`. Raw HTML is trusted: parse
/// only Markdown you control. Headings, alerts, and footnotes hold portable content, so raw HTML or
/// a footnote reference inside one reports `markdown-unplaceable-content` when the page compiles.
///
/// A `Markdown` value is immutable and safe to share; each `with` method returns a new one.
public final class Markdown {

  private static final Parser PARSER =
      Parser.builder()
          .extensions(extensions())
          .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
          .build();

  private final Map<String, Function<Fence, ? extends DocumentContent>> fences;
  private final UnaryOperator<String> urlResolver;
  private final boolean permalinks;
  private final boolean smartPunctuation;

  private Markdown(
      Map<String, Function<Fence, ? extends DocumentContent>> fences,
      UnaryOperator<String> urlResolver,
      boolean permalinks,
      boolean smartPunctuation) {
    this.fences = Map.copyOf(fences);
    this.urlResolver = urlResolver;
    this.permalinks = permalinks;
    this.smartPunctuation = smartPunctuation;
  }

  /// Markdown with no fence renderers and URLs kept as written.
  public static Markdown of() {
    return new Markdown(Map.of(), UnaryOperator.identity(), false, false);
  }

  /// Returns a copy that renders fenced code blocks of `language`, such as `mermaid`, with
  /// `renderer` instead of a [dev.nthings.tessera.components.CodeBlock]. The language is matched
  /// against the first word of the info string, ignoring case. A later renderer for the same
  /// language replaces the earlier one. Throws IllegalArgumentException if `language` is blank.
  @CheckReturnValue
  public Markdown withFence(String language, Function<Fence, ? extends DocumentContent> renderer) {
    if (language.isBlank()) {
      throw new IllegalArgumentException("Fence language cannot be blank");
    }
    var copy = new HashMap<>(fences);
    copy.put(language.strip().toLowerCase(java.util.Locale.ROOT), renderer);
    return new Markdown(copy, urlResolver, permalinks, smartPunctuation);
  }

  /// Returns a copy that passes every link and image URL through `resolver` before it is placed,
  /// such as to turn `../guide.md#install` into `/guide/#install`.
  @CheckReturnValue
  public Markdown withUrlResolver(UnaryOperator<String> resolver) {
    return new Markdown(fences, resolver, permalinks, smartPunctuation);
  }

  /// Returns a copy that ends each heading below the first level with a link to itself; see
  /// [dev.nthings.tessera.components.Heading#permalink()].
  @CheckReturnValue
  public Markdown withHeadingPermalinks() {
    return new Markdown(fences, urlResolver, true, smartPunctuation);
  }

  /// Returns a copy that turns straight quotes into curly ones, `--` into an en dash, `---` into an
  /// em dash, and `...` into an ellipsis, in text outside code. Heading ids keep the plain
  /// punctuation, so links to them stay the same.
  @CheckReturnValue
  public Markdown withSmartPunctuation() {
    return new Markdown(fences, urlResolver, permalinks, true);
  }

  /// Parses `source` into its front matter and document body. Parsing never fails: text that is not
  /// valid Markdown syntax stays text.
  public MarkdownPage parse(String source) {
    var document = (Document) PARSER.parse(source);
    var converter =
        new MarkdownConverter(source, fences, urlResolver, permalinks, smartPunctuation);
    return new MarkdownPage(converter.frontMatter(document), converter.body(document));
  }

  private static List<Extension> extensions() {
    return List.of(
        YamlFrontMatterExtension.create(),
        TablesExtension.create(),
        StrikethroughExtension.create(),
        TaskListItemsExtension.create(),
        AutolinkExtension.create(),
        FootnotesExtension.builder().inlineFootnotes(true).build(),
        AlertsExtension.builder().allowCustomTitles(true).allowNestedAlerts(true).build(),
        ImageAttributesExtension.create());
  }
}
