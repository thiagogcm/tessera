package dev.nthings.tessera.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Compiler-facing: marks a package that drives compilation. Source tracking stops at the first
/// frame in such a package, so compiler-built elements carry no source rather than the caller's
/// line.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface SourceBoundary {}
