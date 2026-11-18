module dev.nthings.tessera {
  requires static transitive com.google.errorprone.annotations;
  requires static transitive org.jspecify;
  requires jdk.httpserver;
  requires owasp.encoder;

  exports dev.nthings.tessera.compiler;
  exports dev.nthings.tessera.components;
  exports dev.nthings.tessera.core;
  exports dev.nthings.tessera.css;
  exports dev.nthings.tessera.document;
  exports dev.nthings.tessera.document.components;
  exports dev.nthings.tessera.foundation;
  exports dev.nthings.tessera.navigation;
  exports dev.nthings.tessera.view to
      dev.nthings.tessera.wasm;
}
