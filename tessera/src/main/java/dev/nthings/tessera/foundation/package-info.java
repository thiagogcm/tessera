/// Helpers with no dependencies on the rest of Tessera. [FluentBuilder] adds `onlyIf` and
/// `ifPresent` to builder chains; [KotlinReceiver] marks the callback interfaces that Kotlin's
/// `sam-with-receiver` compiler plugin turns into receiver lambdas; [Fnv1a64] is the
/// compiler-facing hashing and identifier sanitization behind ids, transition names and group
/// names; [Decimals] formats numbers for CSS and ARIA values.
@NullMarked
package dev.nthings.tessera.foundation;

import org.jspecify.annotations.NullMarked;
