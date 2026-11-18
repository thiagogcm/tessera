package dev.nthings.tessera.internal.authoring;

import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.ElementRef;
import dev.nthings.tessera.core.SourceLocation;
import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.core.TagName;
import dev.nthings.tessera.core.TransitionKey;
import dev.nthings.tessera.css.Declarations;
import dev.nthings.tessera.document.DocumentHeading;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/// Mutable data owned by one portable tag builder. Expansion copies the collections it needs.
public final class TagData {
  /// Attributes whose URL a browser navigates to or loads: a script URL there runs, and an id
  /// written there would become a URL that skips the URL checks.
  public static final Set<String> URL_ATTRIBUTES =
      Set.of("href", "xlink:href", "src", "action", "formaction", "poster", "data");

  public final TagName tag;
  public final @Nullable SourceLocation source;
  public final Declarations style = new Declarations();
  public final Map<String, String> attributes = new LinkedHashMap<>();
  public final Map<String, IdReference> idReferences = new LinkedHashMap<>();
  public final List<Element> children = new ArrayList<>();
  public final List<ElementRef> refs = new ArrayList<>(1);

  public InitialControl initial = InitialControl.NONE;
  public @Nullable Declarations live;
  public @Nullable BundledBehavior behavior;
  public @Nullable TagElement interestTooltip;
  public boolean anchorSelf;
  public @Nullable ElementRef anchorTarget;
  public @Nullable TransitionKey transitionKey;
  public @Nullable DocumentHeading navigationHeading;

  public TagData(TagName tag, @Nullable SourceLocation source) {
    this.tag = tag;
    this.source = source;
  }

  public void requireChildren() {
    if (tag.isVoid()) {
      throw new IllegalStateException(
          "HTML5 void element <" + tag.tagName() + "> cannot have children");
    }
  }
}
