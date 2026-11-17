package dev.nthings.tessera.wasm;

/// Why a request did not complete: the HTTP `status` with the response body as `message`, or status
/// 0 with the browser's error message for a network failure. Cancelled and disposed requests report
/// nothing.
public record RequestFailure(int status, String message) {}
