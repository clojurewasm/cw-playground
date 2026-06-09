# Wasm module provenance

Every `.wasm` in [`modules/`](./modules/) is **first-party, hand-written source**
that lives in this repo under [`wasm-src/`](./wasm-src/) — there are no vendored
third-party library binaries. The committed `.wasm` files are build artifacts of
that source; rebuild them with [`scripts/build_wasm.sh`](./scripts/build_wasm.sh).
They are committed (not gitignored) so the repo deploys self-contained.

| `modules/*.wasm`                          | Source (`wasm-src/`)        | Language | Target                    | How it runs            |
|-------------------------------------------|-----------------------------|----------|---------------------------|------------------------|
| `numtheory.wasm`                          | `rust/numtheory/src/lib.rs` | Rust     | `wasm32-unknown-unknown`  | `(wasm/call m "gcd" …)` |
| `mandelbrot.wasm`                         | `rust/mandelbrot/src/lib.rs`| Rust     | `wasm32-unknown-unknown`  | `(wasm/call m "escape" …)` |
| `chaos.wasm`                              | `rust/chaos/src/lib.rs`     | Rust     | `wasm32-unknown-unknown`  | `(wasm/call m "logistic" …)` |
| `go_json.wasm`                            | `go/json/main.go`           | Go       | `wasip1` (`GOOS=wasip1`)  | `(wasm/run … {:stdin …})` |
| `go_sha256.wasm`                          | `go/sha256/main.go`         | Go       | `wasip1`                  | `(wasm/run … {:stdin …})` |
| `go_sort.wasm`                            | `go/sort/main.go`           | Go       | `wasip1`                  | `(wasm/run … {:args …})` |

- **Rust modules** are pure-compute, no imports (`wasm32-unknown-unknown`); each
  crate's exact dependency set is pinned by its `Cargo.lock`.
- **Go modules** are whole WASI command programs (Go stdlib only:
  `encoding/json`, `crypto/sha256`, `sort`); the Go toolchain version is pinned by
  each `go.mod` (`go 1.26.2`).
- The catalogue the frontend renders (function names, signatures, examples) is
  [`modules/manifest.edn`](./modules/manifest.edn).

## Rebuilding

```sh
bash scripts/build_wasm.sh   # needs: cargo + the wasm32-unknown-unknown target, and go
```

This rebuilds every module from `wasm-src/` into `modules/`. The Rust target is
installed with `rustup target add wasm32-unknown-unknown`; Go cross-compiles with
`GOOS=wasip1 GOARCH=wasm`.
