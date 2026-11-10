package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.DocumentState.Contribution;
import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.DocumentKey;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.core.PageScript;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.css.StyleSheet;
import dev.nthings.tessera.view.Diagnostic;
import dev.nthings.tessera.view.Location;
import dev.nthings.tessera.view.ViewExpansion;
import dev.nthings.tessera.view.ViewExpansion.Reporter;
import dev.nthings.tessera.view.ViewNode;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/// The document backend of the shared [ViewExpansion]: orders one page's expansion (body, late
/// elements, end sections, head), links it, flattens its groups, and checks the document-only
/// element contracts.
final class DocumentPass implements ViewExpansion.Backend {
  private final String pageTitle;
  private final DocumentState contributions = new DocumentState();
  private final ViewExpansion expansion;
  private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
  private final PriorityQueue<Deferred> deferred = new PriorityQueue<>();
  private long sequence;

  private DocumentPass(String pageTitle, StyleSheet styles) {
    this.pageTitle = pageTitle;
    this.expansion = new ViewExpansion(this, styles);
  }

  private record Deferred(int priority, long sequence, ViewNode node)
      implements Comparable<Deferred> {
    @Override
    public int compareTo(Deferred other) {
      var order = Integer.compare(priority, other.priority);
      return order != 0 ? order : Long.compare(sequence, other.sequence);
    }
  }

  /// `headings` are the collected headings whose targets are bound, with their DOM ids.
  record Result(
      List<ViewSnapshot.Node> bodyNodes,
      List<ViewSnapshot.Node> headNodes,
      List<PageHeading> headings,
      DocumentState contributions,
      CompilationState compilation) {}

  static Result layout(
      PageDocument document, List<? extends IntoElement> siteHead, StyleSheet styles) {
    return new DocumentPass(document.title(), styles).document(document, siteHead);
  }

  private Result document(PageDocument document, List<? extends IntoElement> siteHead) {
    siteHead.forEach(head -> contributions.contributeHead(head.intoElement(), Location.ROOT));
    var body = expansion.expand(document.bodyRoot());
    resolveLate();
    // Re-read each turn: a section may register further sections.
    List<Contribution<Function<DocumentContext, ? extends IntoElement>>> sections;
    for (var i = 0; i < (sections = contributions.documentEndSections()).size(); i++) {
      var section = sections.get(i);
      defer(() -> expand(section, "Document end", section.content()), Integer.MAX_VALUE);
    }
    // Indexed: laying out a head element may contribute further head elements.
    var heads = new ArrayList<ViewNode>();
    for (var i = 0; i < contributions.headElements().size(); i++) {
      var head = contributions.headElements().get(i);
      heads.add(expand(head, "Head", _ -> head.content()));
    }

    var bodies = new ArrayList<ViewNode>();
    bodies.add(body);
    for (Deferred next; (next = deferred.poll()) != null; ) {
      bodies.add(next.node());
    }
    var bodyNodes = link(bodies);
    var headNodes = link(heads);
    var headings = resolveHeadings();
    var state = new CompilationState(pageTitle, expansion);
    return new Result(bodyNodes, headNodes, headings, contributions, state);
  }

  /// Links each document forest in one sibling namespace, including deferred body content and
  /// separately contributed head elements. Writers emit each group's children in its place.
  private List<ViewSnapshot.Node> link(List<ViewNode> nodes) {
    return expansion.link(new ViewNode.Fragment(null, Location.ROOT, nodes));
  }

  /// Resolves each collected heading's target; an unbound target is reported.
  private List<PageHeading> resolveHeadings() {
    var headings = new ArrayList<PageHeading>();
    for (var placed : expansion.document().placedHeadings()) {
      var heading = placed.heading();
      var domId =
          expansion.boundId(
              heading.target(), "Heading \"" + heading.title() + "\"", placed.placedBy());
      if (domId != null) {
        headings.add(new PageHeading(domId, heading.title(), heading.level()));
      }
    }
    return headings;
  }

  /// Expands a contribution in its registering component context, then its late elements.
  private ViewNode expand(
      Contribution<?> contribution,
      String phase,
      Function<DocumentContext, ? extends IntoElement> content) {
    var node = expansion.expand(contribution.placedBy(), phase, content);
    resolveLate();
    return node;
  }

  private void resolveLate() {
    for (Runnable next; (next = pending.poll()) != null; ) {
      next.run();
    }
  }

  @Override
  public RuntimeException failureContext(Diagnostic diagnostic) {
    return new CompilationException(List.of(diagnostic.atPage(pageTitle)));
  }

  @Override
  public ViewNode defer(
      Supplier<ViewNode> content, int priority, Location placedBy, Reporter reporter) {
    return defer(content, priority);
  }

  private ViewNode defer(Supplier<ViewNode> content, int priority) {
    // Expanded first, so deferred elements nested in it are queued before it.
    var node = content.get();
    deferred.add(new Deferred(priority, sequence++, node));
    return ViewNode.Empty.INSTANCE;
  }

  @Override
  public ViewNode late(Supplier<ViewNode> content, Location placedBy, Reporter reporter) {
    var slot = new ViewNode.Slot();
    pending.add(() -> slot.fill(content.get()));
    return slot;
  }

  @Override
  public ViewNode raw(String html, Location placedBy, Reporter reporter) {
    return new ViewSnapshot.Raw(html);
  }

  @Override
  public ViewNode effect(ViewSnapshot.Effect effect, Reporter reporter) {
    return reporter.reject(
        effect.location(),
        "native-effect",
        "An effect requires a browser mount, not static HTML output",
        "Start the work outside the page, or render the component in a browser mount.");
  }

  /// A document's values are fixed, so live styles compile as ordinary declarations.
  @Override
  public void liveDeclarations(
      Declarations live, Map<String, String> properties, Declarations into) {
    into.mergeFrom(live);
  }

  /// Checks the document contracts of the element.
  @Override
  public void checkElement(ViewNode.Tag tag, Declarations style, Reporter reporter) {
    if (tag.controlProperties().isControlled()) {
      reporter.report(
          tag.location(),
          "native-control-properties",
          "Controlled form properties require a browser mount, not static HTML output",
          "Use initial HTML attributes for static forms, or render the control in a browser mount.");
    }
    if (!tag.events().isEmpty()) {
      reporter.report(
          tag.location(),
          "native-event-handlers",
          "Java event handlers require a browser mount, not static HTML output",
          "Use a client behavior for static pages, or render the element in a browser mount.");
    }
  }

  @Override
  public void contributeHead(Element element, Location placedBy, Reporter reporter) {
    contributions.contributeHead(element, placedBy);
  }

  @Override
  public void atDocumentEnd(
      DocumentKey<?> owner,
      Function<DocumentContext, ? extends IntoElement> section,
      Location placedBy,
      Reporter reporter) {
    contributions.atDocumentEnd(owner, section, placedBy);
  }

  @Override
  public void contributeScript(PageScript behavior, Location placedBy, Reporter reporter) {
    contributions.contributeScript(behavior);
  }
}
