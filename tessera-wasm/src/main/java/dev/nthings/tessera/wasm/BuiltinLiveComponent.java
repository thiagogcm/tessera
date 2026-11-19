package dev.nthings.tessera.wasm;

/// Package-private base for built-in adaptive widgets so `expand` stays off their public API.
abstract class BuiltinLiveComponent implements LiveContent {

  abstract LiveContent expand(LiveContext context);

  @Override
  public final LiveTree intoLiveTree() {
    return LivePlacement.component(getClass(), this::expand);
  }
}
