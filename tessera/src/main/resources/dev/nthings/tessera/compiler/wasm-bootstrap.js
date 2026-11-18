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

function requireIntegrity(integrity) {
  if (!/^sha384-[A-Za-z0-9+/]{64}$/.test(integrity ?? "")) {
    throw new Error("Missing or invalid SHA-384 integrity.");
  }
}

// One script execution per entry, with the VM captured before the next launcher runs.
async function loadLauncher(url, integrity) {
  requireIntegrity(integrity);
  const previous = globalThis.GraalVM;
  globalThis.GraalVM = undefined;
  try {
    await new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = url;
      script.integrity = integrity;
      script.crossOrigin = "anonymous";
      script.onload = resolve;
      script.onerror = () => reject(new Error(
        `Unable to load or verify the Java launcher at ${url}.`
      ));
      document.head.append(script);
    });
    const vm = globalThis.GraalVM;
    if (typeof vm?.run !== "function" || typeof vm.Config !== "function") {
      throw new Error(`Invalid Java launcher at ${url}.`);
    }
    return vm;
  } finally {
    globalThis.GraalVM = previous;
  }
}

async function loadResources(manifest, image) {
  const fields = manifest.split(" ");
  const resources = [];
  for (let index = 0; index < fields.length; index += 3) {
    const [name, url, integrity] = fields.slice(index, index + 3);
    resources.push((async () => {
      try {
        requireIntegrity(integrity);
        const response = await fetch(url, { integrity, mode: "same-origin" });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        if (name === image && response.headers.get("Content-Type")?.split(";")[0] !== "application/wasm") {
          throw new Error("Expected application/wasm.");
        }
        return [name, await response.arrayBuffer()];
      } catch (error) {
        throw new Error(`Unable to load or verify bundle resource ${url}. ${error.message}`, { cause: error });
      }
    })());
  }
  return new Map(await Promise.all(resources));
}

function report(hosts, error) {
  console.error(error);
  for (const host of hosts) {
    const status = document.createElement("p");
    status.setAttribute("role", "status");
    status.setAttribute("data-tessera-load-error", "");
    status.textContent = `Unable to start the Java browser application. ${error.message}`;
    host.append(status);
  }
}

async function runEntry(vm, group, manifest, image) {
  const resources = await loadResources(manifest, image);
  const config = new vm.Config();
  config.tesseraResources = resources;
  config.tesseraWasmBytes = resources.get(image);
  await vm.run(group.map(host => `${host.id}=${host.dataset.tesseraView}`), config);
}

async function start() {
  const hosts = [...document.querySelectorAll("[data-tessera-bundle]")];
  try {
    if (typeof WebAssembly === "undefined" || !WebAssembly.validate(capabilities)) {
      throw new Error("This application requires WebAssembly GC, exception handling and typed function references.");
    }
    const probe = await WebAssembly.instantiate(capabilities);
    probe.instance.exports.check();
  } catch (error) {
    report(hosts, error);
    return;
  }

  const entries = new Map();
  for (const host of hosts) {
    const data = host.dataset;
    const key = JSON.stringify([
      data.tesseraEntry, data.tesseraIntegrity, data.tesseraImage, data.tesseraResources
    ]);
    const group = entries.get(key) ?? [];
    group.push(host);
    entries.set(key, group);
  }
  const outcomes = [];
  for (const [key, group] of entries) {
    const [url, integrity, image, manifest] = JSON.parse(key);
    try {
      const vm = await loadLauncher(url, integrity);
      outcomes.push(runEntry(vm, group, manifest, image).catch(error =>
        report(group.filter(host => !host.hasAttribute("data-tessera-root")), error)
      ));
    } catch (error) {
      report(group, error);
    }
  }
  await Promise.all(outcomes);
}

start();
