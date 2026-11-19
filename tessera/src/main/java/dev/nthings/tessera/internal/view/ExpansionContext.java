package dev.nthings.tessera.internal.view;

import dev.nthings.tessera.core.Location;
import dev.nthings.tessera.core.OutlineHeading;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.ViewContext;
import dev.nthings.tessera.core.ViewKey;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/// The portable [ViewContext] of one expansion turn: its id scope and component path over the state
/// shared by the whole expansion. Expansion state and headings go to the expansion's
/// [ExpansionRegistry]. Document and live callbacks receive a context derived from it with their
/// family's operations. Derived contexts never change their parent.
public final class ExpansionContext implements ViewContext {

  private final ViewScope viewScope;
  private final ViewExpansion<?> expansion;
  private final List<String> componentPath;
  private final @Nullable SourceLocation origin;
  private final @Nullable String keyOwner;
  private final TagName.Namespace namespace;

  ExpansionContext(
      ViewScope viewScope,
      ViewExpansion<?> expansion,
      List<String> componentPath,
      @Nullable SourceLocation origin,
      @Nullable String keyOwner,
      TagName.Namespace namespace) {
    this.viewScope = viewScope;
    this.expansion = expansion;
    this.componentPath = componentPath;
    this.origin = origin;
    this.keyOwner = keyOwner;
    this.namespace = namespace;
  }

  TagName.Namespace namespace() {
    return namespace;
  }

  public ExpansionContext withNamespace(TagName.Namespace namespace) {
    return namespace == this.namespace
        ? this
        : new ExpansionContext(viewScope, expansion, componentPath, origin, keyOwner, namespace);
  }

  @Override
  public <T> T state(ViewKey<T> key) {
    return expansion.registry().value(key);
  }

  @Override
  public void report(String rule, String message, String fix) {
    expansion.reporter().report(location(), rule, message, fix);
  }

  void collectHeading(OutlineHeading heading, Location placedBy) {
    expansion.registry().registerHeading(heading, placedBy);
  }

  public ViewExpansion<?> expansion() {
    return expansion;
  }

  ViewScope idScope() {
    return viewScope;
  }

  List<String> components() {
    return componentPath;
  }

  @Nullable SourceLocation origin() {
    return origin;
  }

  public Location location() {
    return new Location(componentPath, "", null, origin);
  }

  @Nullable String keyOwner() {
    return keyOwner;
  }

  ExpansionContext withIdScope(String scopeId, boolean generated) {
    return new ExpansionContext(
        viewScope.extend(scopeId, generated), expansion, componentPath, origin, null, namespace);
  }

  ExpansionContext withKeyedScope(String slug) {
    return new ExpansionContext(
        viewScope.extendKeyed(slug), expansion, componentPath, origin, null, namespace);
  }

  ExpansionContext withKeyOwner(String owner) {
    return new ExpansionContext(viewScope, expansion, componentPath, origin, owner, namespace);
  }

  ExpansionContext withComponent(String component, @Nullable SourceLocation placedAt) {
    var path = new ArrayList<>(componentPath);
    path.add(component);
    return withPlacement(new Location(path, "", null, placedAt != null ? placedAt : origin));
  }

  public ExpansionContext withPlacement(Location placedBy) {
    return new ExpansionContext(
        viewScope, expansion, placedBy.components(), placedBy.source(), keyOwner, namespace);
  }
}
