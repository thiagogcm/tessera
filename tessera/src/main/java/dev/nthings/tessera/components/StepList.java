package dev.nthings.tessera.components;

import dev.nthings.tessera.core.DocumentContext;
import dev.nthings.tessera.core.Element;
import dev.nthings.tessera.core.Elements;
import dev.nthings.tessera.core.IntoElement;
import dev.nthings.tessera.css.Counter;
import dev.nthings.tessera.css.CssColor;
import dev.nthings.tessera.css.CssContent;
import dev.nthings.tessera.css.CssLength;
import dev.nthings.tessera.css.CssNumber;
import dev.nthings.tessera.css.CustomProperty;
import dev.nthings.tessera.css.ScopeStyle;
import dev.nthings.tessera.css.Tokens;
import java.util.List;

/// Ordered list drawn as numbered steps. The step badges shade toward the accent color as the
/// steps progress. Create one with [#of(String...)] for plain text steps or
/// [#of(IntoElement...)] for steps with markup.
///
/// Without an [#id(String)], the compiler numbers instances `step-list-<n>`.
public final class StepList extends BuiltinScope<StepList> {

  private static final Counter STEP = Counter.named("step");
  private static final CustomProperty STEP_INDEX = CustomProperty.named("step-index");
  private static final CustomProperty STEP_TOTAL = CustomProperty.named("step-total");

  private final List<Element> steps;

  private StepList(List<Element> steps) {
    this.steps = steps;
  }

  /// Creates a step list of plain text `steps`.
  public static StepList of(String... steps) {
    return new StepList(ListItems.texts(steps));
  }

  /// Creates a step list of `steps`, each of which may contain markup.
  public static StepList of(IntoElement... steps) {
    return new StepList(ListItems.elements(steps));
  }

  @Override
  public StepList self() {
    return this;
  }

  @Override
  String scopeName() {
    return "step-list";
  }

  @Override
  void configureScope(ScopeStyle scope) {
    scope
        .root(s -> s.listStyleNone().flex().flexCol().gap(Tokens.SPACE_3).counterReset(STEP))
        .select(
            ":scope > li",
            s ->
                s.customProperty(STEP_INDEX, CssNumber.siblingIndex())
                    .customProperty(STEP_TOTAL, CssNumber.siblingCount())
                    .counterIncrement(STEP)
                    .flex()
                    .itemsBaseline()
                    .gap(Tokens.SPACE_3))
        .select(
            ":scope > li::before",
            s ->
                s.content(CssContent.counter(STEP))
                    .inlineFlex()
                    .itemsCenter()
                    .justifyCenter()
                    .minInlineSize(CssLength.rem(1.75))
                    .blockSize(CssLength.rem(1.75))
                    .rounded(Tokens.RADIUS_FULL)
                    .fontWeight(600)
                    .tabularNums()
                    .bg(
                        Tokens.ACCENT_PRIMARY.mix(
                            CssColor.Space.OKLAB,
                            CssLength.pct(20)
                                .plus(
                                    CssLength.pct(60)
                                        .times(CssNumber.property(STEP_INDEX))
                                        .dividedBy(CssNumber.property(STEP_TOTAL))),
                            Tokens.SURFACE_SUBTLE))
                    .textColor(Tokens.TEXT_PRIMARY));
  }

  @Override
  IntoElement expand(DocumentContext context) {
    return Elements.ol().children(ListItems.wrap(steps));
  }
}
