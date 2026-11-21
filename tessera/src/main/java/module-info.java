module dev.nthings.tessera {
  requires static transitive com.google.errorprone.annotations;
  requires static transitive org.jspecify;
  requires owasp.encoder;

  exports dev.nthings.tessera.components;
  exports dev.nthings.tessera.core;
  exports dev.nthings.tessera.css;
  exports dev.nthings.tessera.foundation;
  exports dev.nthings.tessera.internal.authoring to
      dev.nthings.tessera.compiler,
      dev.nthings.tessera.wasm;
  exports dev.nthings.tessera.internal.components to
      dev.nthings.tessera.compiler,
      dev.nthings.tessera.wasm;
  exports dev.nthings.tessera.internal.css to
      dev.nthings.tessera.compiler,
      dev.nthings.tessera.wasm;
  exports dev.nthings.tessera.internal.foundation to
      dev.nthings.tessera.compiler;
  exports dev.nthings.tessera.internal.view to
      dev.nthings.tessera.compiler,
      dev.nthings.tessera.wasm;
  exports dev.nthings.tessera.view to
      dev.nthings.tessera.wasm;
}
