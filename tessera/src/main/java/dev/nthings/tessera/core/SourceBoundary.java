package dev.nthings.tessera.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a library package whose code drives compilation, such as the page and site compilers.
///
/// JVM source tracking stops walking the stack at the first frame in a marked package: elements
/// built there on the compiler's own behalf belong to no line of authoring code, so they carry no
/// source rather than the line that happened to call the compiler.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface SourceBoundary {}
