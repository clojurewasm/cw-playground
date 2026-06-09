#!/usr/bin/env bash
# Build every Rust wasm sample to wasm32-unknown-unknown (pure compute, no imports)
# and copy the .wasm artifacts into modules/ (the playground's wasm-module dir;
# its name matches the eval-side `(wasm/load "modules/…")` convention).
set -euo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
OUT="$ROOT/modules"
mkdir -p "$OUT"

build_rust() {
  local crate="$1"
  echo "==> rust: $crate"
  ( cd "wasm-src/rust/$crate" && cargo build --release --target wasm32-unknown-unknown )
  cp "wasm-src/rust/$crate/target/wasm32-unknown-unknown/release/$crate.wasm" "$OUT/$crate.wasm"
  local sz
  sz=$(wc -c < "$OUT/$crate.wasm")
  echo "    -> modules/$crate.wasm (${sz} bytes)"
}

build_go() {
  local name="$1"; shift
  echo "==> go: $name"
  ( cd "wasm-src/go/$name" && GOOS=wasip1 GOARCH=wasm go build -o "$OUT/go_$name.wasm" main.go )
  echo "    -> modules/go_$name.wasm ($(wc -c < "$OUT/go_$name.wasm") bytes)"
}

for c in numtheory mandelbrot chaos; do
  build_rust "$c"
done

# Go programs (GOOS=wasip1) — run via (wasm/run …) with a WASI host. Larger
# (~3 MB each, Go runtime); gitignored, rebuilt from source here.
if command -v go >/dev/null 2>&1; then
  for g in json sha256 sort; do build_go "$g"; done
else
  echo "(go not found — skipping Go wasip1 modules)"
fi

echo
echo "All wasm modules built into $OUT:"
ls -la "$OUT"/*.wasm
