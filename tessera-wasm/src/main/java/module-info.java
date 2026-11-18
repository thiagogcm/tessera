module dev.nthings.tessera.wasm {
  requires transitive dev.nthings.tessera;
  requires static transitive org.jspecify;
  requires org.graalvm.webimage.api;

  exports dev.nthings.tessera.wasm;
}
