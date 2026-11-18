package dev.nthings.tessera.css;

import dev.nthings.tessera.core.TagElement;
import dev.nthings.tessera.internal.authoring.AuthoringAccess;

/// The receivers of the CSS traits. Writes go to declarations kept by each builder.
interface StyleTarget<SELF extends StyleTarget<SELF>> {

  SELF self();

  static Declarations declarations(StyleTarget<?> target) {
    return switch ((Styled<?>) target) {
      case Declarations declarations -> declarations;
      case TagElement element -> AuthoringAccess.tag(element).style;
    };
  }
}
