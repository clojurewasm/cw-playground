# ClojureWasm Playground

Run Clojure in your browser, evaluated by [ClojureWasm](https://github.com/clojurewasm/ClojureWasm)
(`cljw`) — a from-scratch Clojure runtime in Zig, no JVM. Submissions are
evaluated **in-process** on the server's `cljw` under a per-submission budget
(steps / deadline / heap), and can call sandboxed WebAssembly modules written in
Rust and Go via `cljw`'s Wasm FFI.

The Wasm FFI now runs **JIT-compiled by default** (cljw `v1.10.0`, embedding
zwasm `v2.5.0`): `(wasm/load …)` transparently rides zwasm's `:auto`
JIT-first engine, so a tight numeric loop inside a module executes as native machine
code — the `engine-select` and `jit-speed` examples run a ten-million-step loop in
**about 40 ms** (measured on an M4 Pro; the example prints its own `(time …)`, so
you see the real number rather than this one), byte-identical to the interpreter.
The engine is a runtime default; pass `{:engine :interp}` / `{:engine :jit}` to
pick one explicitly.

## What's inside

- **Frontend** (`src/`, ClojureScript + shadow-cljs) — a CodeMirror editor + a
  Wasm-module reference panel. The optimized release bundle is committed at
  `resources/public/js/main.js`.
- **Backend** (`server/`, ClojureWasm) — `playground.server` serves the SPA and a
  `/api/eval` endpoint; `playground.sandbox` bounds each submission with
  `cljw.eval/with-budget`. Runs on `cljw`, no JVM, no Babashka.
- **Wasm modules** (`modules/`, hand-written Rust + Go) — exposed to evaluated
  code via `(wasm/load …)` / `(wasm/run …)`. Origins in
  [`PROVENANCE.md`](./PROVENANCE.md).

## Run it

```sh
./run_local.sh        # builds cljw from source on first run, serves http://localhost:8080
```

Deploy to fly.io (or any container host) — see [`DEPLOY.md`](./DEPLOY.md). The repo
is self-contained: only `cljw` is built from source (Zig); the SPA and Wasm
modules are committed artifacts.

## License

See the ClojureWasm project for runtime licensing. Demo sources here are provided
as-is for illustration.
