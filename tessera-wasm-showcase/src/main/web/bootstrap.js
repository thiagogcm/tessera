// Executes struct allocation, try_table/throw and call_ref in a small capability probe.
// The bytes encode this module:
//
// (module
//   (type $record (struct (field i32)))
//   (type $callback (func (result i32)))
//   (tag $failure)
//   (func $callback (type $callback) (i32.const 1))
//   (elem declare func $callback)
//   (func (export "check") (result i32)
//     (drop (struct.new $record (i32.const 0)))
//     (block $caught
//       (try_table (catch $failure $caught)
//         (throw $failure)))
//     (call_ref $callback (ref.func $callback))))
const capabilities = new Uint8Array([
  0, 97, 115, 109, 1, 0, 0, 0, 1, 12, 3, 96, 0, 1, 127, 96, 0, 0, 95, 1, 127, 0,
  3, 3, 2, 0, 0, 13, 3, 1, 0, 1, 7, 9, 1, 5, 99, 104, 101, 99, 107, 0, 1, 9, 5,
  1, 3, 0, 1, 0, 10, 32, 2, 4, 0, 65, 1, 11, 25, 0, 65, 0, 251, 0, 2, 26, 2, 64,
  31, 64, 1, 0, 0, 0, 8, 0, 11, 0, 11, 210, 0, 20, 0, 11
]);

// Hosts declare their entry's launcher and the view to mount (default: the host id).
function declaredEntries() {
  const entries = new Map();
  for (const host of document.querySelectorAll("[data-tessera-entry]")) {
    if (!host.id) throw new Error("A Tessera mount host needs an id.");
    const launcher = host.dataset.tesseraEntry;
    const mounts = entries.get(launcher) ?? [];
    mounts.push(`${host.id}=${host.dataset.tesseraView || host.id}`);
    entries.set(launcher, mounts);
  }
  return entries;
}

// Loads one launcher and returns its GraalVM object. Every launcher assigns the same global, so
// the object is captured as soon as the script runs and the previous value is put back.
async function loadLauncher(launcher) {
  const previous = globalThis.GraalVM;
  await new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.src = launcher;
    script.onload = resolve;
    script.onerror = () => reject(new Error(
      `Unable to load ${launcher}. Check that the Java launcher is deployed and reachable.`
    ));
    document.head.append(script);
  });
  const vm = globalThis.GraalVM;
  globalThis.GraalVM = previous;
  if (!vm || typeof vm.run !== "function") {
    throw new Error(`${launcher} did not define a GraalVM launcher.`);
  }
  return vm;
}

// Runs one loaded entry with its declared hosts. An explicit Config keeps the run independent of
// the global the launcher would otherwise read.
async function runEntry(vm, launcher, mounts) {
  try {
    await vm.run(mounts, new vm.Config());
  } catch (error) {
    throw new Error(
      `Java runtime/image initialization failed for ${launcher}. Check ${launcher}.wasm and its ` +
      `application/wasm content type. ${error.message}`,
      { cause: error }
    );
  }
}

async function start() {
  if (typeof WebAssembly === "undefined" || !WebAssembly.validate(capabilities)) {
    throw new Error(
      "This application requires WebAssembly GC, exception handling and typed function references. " +
      "Update to a current Chromium or Firefox browser."
    );
  }

  // Launchers execute one at a time, since each assigns the same global, but download in
  // parallel, starting while the capability probe instantiates. Each loads once however many
  // hosts it serves, and their entries run concurrently. A failing entry leaves the others
  // mounted. Outcomes resolve to the error or null, so no rejection is left unhandled while later
  // launchers load.
  const entries = declaredEntries();
  for (const launcher of entries.keys()) {
    const preload = document.createElement("link");
    preload.rel = "preload";
    preload.as = "script";
    preload.href = launcher;
    document.head.append(preload);
  }
  const probe = await WebAssembly.instantiate(capabilities);
  probe.instance.exports.check();

  const outcomes = [];
  for (const [launcher, mounts] of entries) {
    try {
      const vm = await loadLauncher(launcher);
      outcomes.push(runEntry(vm, launcher, mounts).then(() => null, error => error));
    } catch (error) {
      outcomes.push(Promise.resolve(error));
    }
  }
  const failures = (await Promise.all(outcomes)).filter(Boolean);
  if (failures.length > 0) {
    throw new AggregateError(failures, failures.map(failure => failure.message).join(" "));
  }
}

start().catch(error => {
  console.error(error);
  const message = document.getElementById("error");
  message.textContent = `Unable to start the Java browser application: ${error.message}`;
  message.hidden = false;
});
