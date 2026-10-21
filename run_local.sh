#!/usr/bin/env bash
# Run the ClojureWasm playground locally.
#
# cljw comes from the pinned GitHub release of BuddhiLW/ClojureWasm, the same
# release the Dockerfile installs (linux-x86_64 or macos-aarch64), verified
# against the release's .sha256 sidecar and cached under .cache/ (first run
# only).
#
#   ./run_local.sh                       # fetch cljw if needed, then serve :8080
#   PG_PORT=9000 ./run_local.sh          # override any env knob
#   CLJW=/path/to/cljw ./run_local.sh    # use an existing cljw, skip the fetch
#   CLJW_VERSION=vX.Y.Z ./run_local.sh   # try a different release
#
# Needs (one-time fetch only): curl, tar, sha256sum or shasum.
set -euo pipefail
cd "$(dirname "$0")"

CLJW_VERSION="${CLJW_VERSION:-v1.15.1}"
CACHE_DIR=".cache/cljw-$CLJW_VERSION"
CLJW="${CLJW:-$CACHE_DIR/cljw}"

if [ ! -x "$CLJW" ]; then
  case "$(uname -s)-$(uname -m)" in
    Linux-x86_64)  asset=cljw-linux-x86_64.tar.gz ;;
    Darwin-arm64)  asset=cljw-macos-aarch64.tar.gz ;;
    *) echo "no prebuilt cljw for $(uname -s)-$(uname -m); set CLJW=/path/to/cljw" >&2; exit 1 ;;
  esac
  url="https://github.com/BuddhiLW/ClojureWasm/releases/download/$CLJW_VERSION/$asset"
  echo "Fetching cljw $CLJW_VERSION ($asset), first run only"
  rm -rf "$CACHE_DIR" && mkdir -p "$CACHE_DIR"
  curl -fsSLo "$CACHE_DIR/$asset" "$url"
  curl -fsSLo "$CACHE_DIR/$asset.sha256" "$url.sha256"
  if command -v sha256sum >/dev/null; then
    ( cd "$CACHE_DIR" && sha256sum -c "$asset.sha256" )
  else
    ( cd "$CACHE_DIR" && shasum -a 256 -c "$asset.sha256" )
  fi
  tar -xzf "$CACHE_DIR/$asset" -C "$CACHE_DIR" cljw
fi

export PG_PORT="${PG_PORT:-8080}"
export PG_PUBLIC_DIR="${PG_PUBLIC_DIR:-resources/public}"
export PG_WASM_DIR="${PG_WASM_DIR:-modules}"
export PG_EXAMPLES="${PG_EXAMPLES:-resources/examples.edn}"
export CLJW_FS_ROOT="${CLJW_FS_ROOT:-$PWD}"   # FS-jail: confine slurp/File to the repo.
# PG_EVAL_* bound each /api/eval submission via with-budget (sandbox.clj), NOT the
# CLJW_EVAL_* process-wide budget, which would meter (and kill) the server itself.
export PG_EVAL_MAX_STEPS="${PG_EVAL_MAX_STEPS:-50000000}"
export PG_EVAL_DEADLINE_MS="${PG_EVAL_DEADLINE_MS:-5000}"
export PG_EVAL_MAX_HEAP_MB="${PG_EVAL_MAX_HEAP_MB:-128}"

echo "ClojureWasm playground: http://localhost:$PG_PORT   (cljw: $CLJW)"
exec "$CLJW" -M:cljw -m playground.server
