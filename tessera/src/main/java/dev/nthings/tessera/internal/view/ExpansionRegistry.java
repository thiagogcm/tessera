package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.core.ViewKey;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The state of one expansion that every backend keeps the same way, for one page or one attempted
/// live render: [dev.nthings.tessera.core.ViewContext#state(ViewKey)] values and outline headings,
/// at most one per target. A rejected render discards it with its expansion.
public final class ExpansionRegistry {

  /// A registered heading with the component context that registered it.
  public record Heading(OutlineHeading heading, Location placedBy) {}

  private final Map<ViewKey<?>, Object> values = new HashMap<>();
  private final List<Heading> headings = new ArrayList<>();
  // ElementRef keeps identity equality, so a plain set tells targets apart by handle.
  private final Set<ElementRef> headingTargets = new HashSet<>();

  public ExpansionRegistry() {}

  public <T> T value(ViewKey<T> key) {
    return AuthoringAccess.state(key, values);
  }

  public void registerHeading(OutlineHeading heading, Location placedBy) {
    if (headingTargets.add(heading.target())) {
      headings.add(new Heading(heading, placedBy));
    }
  }

  public List<Heading> placedHeadings() {
    return List.copyOf(headings);
  }

  public List<OutlineHeading> headings() {
    return headings.stream().map(Heading::heading).toList();
  }
}
