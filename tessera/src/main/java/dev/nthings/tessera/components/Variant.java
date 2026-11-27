package dev.nthings.tessera.components;

/// Surface style shared by [Button], [IconButton] and [Badge], painted in the component's tone:
///
/// - `SOLID`: filled with the tone.
/// - `TINTED`: a light tint of the tone with tone text.
/// - `OUTLINE`: transparent with a tone border.
/// - `GHOST`: transparent with tone text and no border.
///
/// [Button] defaults to `SOLID`, [IconButton] to `GHOST` and [Badge] to `TINTED`.
public enum Variant {
  SOLID,
  TINTED,
  OUTLINE,
  GHOST
}
