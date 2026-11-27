module dev.nthings.tessera.compiler {
  requires transitive dev.nthings.tessera;
  requires static transitive com.google.errorprone.annotations;
  requires static transitive org.jspecify;
  requires jdk.httpserver;
  requires owasp.encoder;

  exports dev.nthings.tessera.compiler;
  exports dev.nthings.tessera.document;
  exports dev.nthings.tessera.document.components;
}
