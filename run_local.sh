#!/usr/bin/env bash
# Run the ClojureWasm playground locally — machine-independent.
#
# cljw is built FROM SOURCE (no assumption about your machine, no sibling repo):
# the pinned ClojureWasm ref is cloned and built ReleaseSafe with -Dwasm (the
# WebAssembly FFI the playground's eval needs). zwasm resolves automatically via
# ClojureWasm's build.zig.zon tag pin. The build is cached under .cache/ (first
# run only). This is the SAME way the Dockerfile obtains cljw — local and fly are
# symmetric.
#
#   ./run_local.sh                       # build cljw if needed, then serve :8080
#   PG_PORT=9000 ./run_local.sh          # override any env knob
#   CLJW=/path/to/cljw ./run_local.sh    # use an existing cljw, skip the build
#   CLJW_REF=<sha|branch> ./run_local.sh # pin a different ClojureWasm ref
#
# Needs (one-time cljw build only): git + Zig 0.16 on PATH.
set -euo pipefail
cd "$(dirname "$0")"

CLJW_REF="${CLJW_REF:-cw-from-scratch}"
CACHE_DIR=".cache/cljw"
CLJW="${CLJW:-$CACHE_DIR/zig-out/bin/cljw}"

if [ ! -x "$CLJW" ]; then
  command -v git >/dev/null || { echo "git not found (needed to fetch cljw source)" >&2; exit 1; }
  command -v zig >/dev/null || { echo "zig 0.16 not found (needed to build cljw)" >&2; exit 1; }
  echo "Building cljw from source ($CLJW_REF) — first run only, ~1 min…"
  rm -rf "$CACHE_DIR"
  git clone --branch "$CLJW_REF" --depth 1 \
    https://github.com/clojurewasm/ClojureWasm.git "$CACHE_DIR"
  ( cd "$CACHE_DIR" && zig build -Dwasm -Doptimize=ReleaseSafe )
fi

export PG_PORT="${PG_PORT:-8080}"
export PG_PUBLIC_DIR="${PG_PUBLIC_DIR:-resources/public}"
export PG_WASM_DIR="${PG_WASM_DIR:-modules}"
export PG_EXAMPLES="${PG_EXAMPLES:-resources/examples.edn}"
export CLJW_FS_ROOT="${CLJW_FS_ROOT:-$PWD}"   # FS-jail: confine slurp/File to the repo.
# PG_EVAL_* bound each /api/eval submission via with-budget (sandbox.clj) — NOT the
# CLJW_EVAL_* process-wide budget, which would meter (and kill) the server itself.
export PG_EVAL_MAX_STEPS="${PG_EVAL_MAX_STEPS:-50000000}"
export PG_EVAL_DEADLINE_MS="${PG_EVAL_DEADLINE_MS:-5000}"
export PG_EVAL_MAX_HEAP_MB="${PG_EVAL_MAX_HEAP_MB:-128}"

echo "ClojureWasm playground → http://localhost:$PG_PORT   (cljw: $CLJW)"
exec "$CLJW" -M:cljw -m playground.server
