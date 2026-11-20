package dev.nthings.tessera.wasm;

/// The outcome of [Host#fetchText(String, java.util.function.Consumer)]: either [Ok] with the
/// response body, [HttpError] for a non-2xx HTTP response, or [NetworkError] when no HTTP response
/// was received.
public sealed interface FetchResult
    permits FetchResult.Ok, FetchResult.HttpError, FetchResult.NetworkError {

  /// The request succeeded with a 2xx status and `body`.
  record Ok(String body) implements FetchResult {}

  /// The server returned a non-2xx HTTP `status` with `body`.
  record HttpError(int status, String body) implements FetchResult {}

  /// The request failed before receiving an HTTP response (such as a network or CORS error).
  record NetworkError(String message) implements FetchResult {}
}
