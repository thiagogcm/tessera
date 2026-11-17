package dev.nthings.tessera.foundation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Marks a single-method callback interface whose parameter is the object being configured, such as
/// `StyleBlock` and `ScopeBlock` in `dev.nthings.tessera.css`. Java ignores it. Kotlin code that
/// registers this annotation with the `sam-with-receiver` compiler plugin writes those callbacks as
/// receiver lambdas: `div().on(State.HOVER) { bg(Tokens.SURFACE_SUBTLE) }` instead of
/// `div().on(State.HOVER) { it.bg(Tokens.SURFACE_SUBTLE) }`.
@Documented
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface KotlinReceiver {}
