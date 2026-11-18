package dev.nthings.tessera.compiler;

import dev.nthings.tessera.compiler.DocumentState.Contribution;
import dev.nthings.tessera.core.Diagnostic;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.document.DocumentContent;
import dev.nthings.tessera.document.DocumentContext;
import dev.nthings.tessera.document.PageScript;
import dev.nthings.tessera.internal.authoring.DocumentNode;
import dev.nthings.tessera.internal.css.StyleSheet;
import dev.nthings.tessera.view.DocumentExpansion;
import dev.nthings.tessera.view.ViewNode;
import dev.nthings.tessera.view.ViewSnapshot;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Function;
import java.util.function.Supplier;

/// The document backend of the shared [ViewExpansion]: orders one page's expansion (body, late
/// elements, end sections, head), links it, flattens its groups, and checks the document-only
/// element contracts.
final class DocumentPass implements DocumentExpansion.Assembly {
  private final String pageTitle;
  private final DocumentState contributions = new DocumentState();
  private final DocumentExpansion expansion;
  private final BundlePublication bundles;
  private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
  private final PriorityQueue<Deferred> deferred = new PriorityQueue<>();
  private long sequence;

  private DocumentPass(String pageTitle, StyleSheet styles, BundlePublication bundles) {
    this.pageTitle = pageTitle;
    this.bundles = bundles;
    this.expansion = new DocumentExpansion(this, styles);
  }

  private record Deferred(int priority, long sequence, ViewNode<String> node)
      implements Comparable<Deferred> {
    @Override
    public int compareTo(Deferred other) {
      var order = Integer.compare(priority, other.priority);
      return order != 0 ? order : Long.compare(sequence, other.sequence);
    }
  }

  /// `headings` are the collected headings whose targets are bound, with their DOM ids.
  record Result(
      List<ViewSnapshot.Node<String>> bodyNodes,
      List<ViewSnapshot.Node<String>> headNodes,
      List<PageHeading> headings,
      DocumentState contributions,
      CompilationState compilation) {}

  static Result layout(PageDocument document, StyleSheet styles, BundlePublication bundles) {
    return new DocumentPass(document.title(), styles, bundles).document(document);
  }

  private Result document(PageDocument document) {
    var body = expansion.expandDocument(Location.ROOT, DocumentNode.capture(document.bodyRoot()));
    resolveLate();
    // Sections and head content may register further sections and head content in either
    // direction, so both lists are read by index until neither grows.
    var heads = new ArrayList<ViewNode<String>>();
    var sectionCount = 0;
    var headCount = 0;
    List<Contribution<Function<DocumentContext, ? extends DocumentContent>>> sections;
    while (sectionCount < (sections = contributions.documentEndSections()).size()
        || headCount < contributions.headElements().size()) {
      for (; sectionCount < sections.size(); sectionCount++) {
        var section = sections.get(sectionCount);
        defer(() -> expand(section, "Document end", section.content()), Integer.MAX_VALUE);
        sections = contributions.documentEndSections();
      }
      for (; headCount < contributions.headElements().size(); headCount++) {
        var head = contributions.headElements().get(headCount);
        heads.add(expansion.expandDocument(head.placedBy(), head.content()));
        resolveLate();
      }
    }

    var bodies = new ArrayList<ViewNode<String>>();
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
  private List<ViewSnapshot.Node<String>> link(List<ViewNode<String>> nodes) {
    return expansion.link(new ViewNode.Fragment<>(null, Location.ROOT, nodes));
  }

  /// Resolves each collected heading's target; an unbound target is reported.
  private List<PageHeading> resolveHeadings() {
    var headings = new ArrayList<PageHeading>();
    for (var placed : expansion.registry().placedHeadings()) {
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
  private ViewNode<String> expand(
      Contribution<?> contribution,
      String phase,
      Function<DocumentContext, ? extends DocumentContent> content) {
    var node = expansion.expandDocument(contribution.placedBy(), phase, content);
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
  public ViewNode<String> defer(Supplier<ViewNode<String>> content, int priority) {
    // Expanded first, so deferred elements nested in it are queued before it.
    var node = content.get();
    deferred.add(new Deferred(priority, sequence++, node));
    return new ViewNode.Empty<>();
  }

  @Override
  public ViewNode<String> late(Supplier<ViewNode<String>> content) {
    var slot = new ViewNode.Slot<String>();
    pending.add(() -> slot.fill(content.get()));
    return slot;
  }

  @Override
  public void contributeHead(DocumentNode content, Location placedBy) {
    contributions.contributeHead(content, placedBy);
  }

  @Override
  public void atDocumentEnd(
      ViewKey<?> owner,
      Function<DocumentContext, ? extends DocumentContent> section,
      Location placedBy) {
    contributions.atDocumentEnd(owner, section, placedBy);
  }

  @Override
  public Map<String, String> mountAttributes(DocumentNode.Mount mount) {
    var entry = bundles.entry(mount.bundle());
    contributions.contributeBundle(bundles.bootstrap(), entry);
    var attributes = new LinkedHashMap<String, String>();
    attributes.put(BundlePublication.MOUNT_ATTRIBUTE, "");
    attributes.put("data-tessera-entry", entry.launcher().asset().path());
    attributes.put("data-tessera-integrity", entry.launcher().integrity());
    attributes.put("data-tessera-image", entry.image());
    attributes.put("data-tessera-resources", entry.resources());
    attributes.put("data-tessera-view", mount.view());
    return attributes;
  }

  @Override
  public void contributeScript(PageScript script) {
    contributions.contributeScript(script);
  }
}
