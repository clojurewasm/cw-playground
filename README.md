# ClojureWasm Playground

Run Clojure in your browser, evaluated by [ClojureWasm](https://github.com/clojurewasm/ClojureWasm)
(`cljw`) — a from-scratch Clojure runtime in Zig, no JVM. Submissions are
evaluated **in-process** on the server's `cljw` under a per-submission budget
(steps / deadline / heap), and can call sandboxed WebAssembly modules written in
Rust and Go via `cljw`'s Wasm FFI.

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
