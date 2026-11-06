package dev.nthings.tessera.components;

import dev.nthings.tessera.core.Action;
import dev.nthings.tessera.core.BuiltinCommand;
import dev.nthings.tessera.core.ElementRef;
import org.jspecify.annotations.Nullable;

/// Tone, surface, size, and behavior shared by [Button] and [IconButton].
final class ButtonModel {

  Button.Tone tone;
  Button.Variant variant;
  Button.Size size = Button.Size.MD;
  boolean submit;
  boolean disabled;
  boolean busy;
  @Nullable ElementRef domRef;
  @Nullable ElementRef commandTarget;
  @Nullable BuiltinCommand command;
  @Nullable Action action;

  ButtonModel(Button.Tone tone, Button.Variant variant) {
    this.tone = tone;
    this.variant = variant;
  }
}
