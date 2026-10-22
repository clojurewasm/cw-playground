# Running and deploying the ClojureWasm playground

The app is self-contained. `cljw` is the pinned release binary of
[BuddhiLW/ClojureWasm](https://github.com/BuddhiLW/ClojureWasm/releases)
(`v1.15.3`, ReleaseSafe with the Wasm FFI), checked against a pinned sha256.
The SPA bundle (`resources/public/js`) and the Wasm modules (`modules/`) are
committed artifacts (see [`PROVENANCE.md`](./PROVENANCE.md)).

## Local

```sh
./run_local.sh                 # fetches cljw on first run (cached in .cache/), serves :8080
PG_PORT=9000 ./run_local.sh    # override any knob
```

### Rebuilding the artifacts (only when you change them)

```sh
npm ci && npm run release      # rebuild the SPA into resources/public/js/main.js, then commit it
bash scripts/build_wasm.sh     # rebuild the Wasm modules into modules/*.wasm
```

## Container image

`ghcr.io/clojurewasm/cw-playground`, tags `main-<7-char sha>` and `latest`.
`.github/workflows/image.yml` builds and pushes it on every push to `main`.
`.github/workflows/cljw-bump.yml` checks daily for a newer cljw release, verifies
it, commits the new pin to `main` and publishes a fresh image.

```sh
docker build -t cw-playground .
docker run --rm -p 8080:8080 --read-only --tmpfs /tmp cw-playground
```

The image runs as UID 10001 and works with a read-only root filesystem: it only
writes to `/tmp`, which must be writable (an `emptyDir` in Kubernetes).

| env | default | meaning |
|---|---|---|
| `PG_PORT` | `8080` | listen port (binds 0.0.0.0) |
| `PG_PUBLIC_DIR` | `/app/resources/public` | static SPA root |
| `PG_WASM_DIR` | `/app/modules` | Wasm modules + `manifest.edn` |
| `PG_EXAMPLES` | `/app/resources/examples.edn` | example snippets |
| `CLJW_FS_ROOT` | `/app` | FS jail for every slurp / File op |
| `PG_EVAL_MAX_STEPS` | `50000000` | per-eval step budget |
| `PG_EVAL_DEADLINE_MS` | `5000` | per-eval wall-clock budget |
| `PG_EVAL_MAX_HEAP_MB` | `128` | per-eval heap budget |

Use `PG_EVAL_*`, never `CLJW_EVAL_*`: the latter arms a process-wide budget that
would stop the server itself.

## Kubernetes

Served at <https://cljw.buddhilw.com/playground/>. The gateway strips the
`/playground` prefix, so the server sees `/`, `/api/eval`, `/js/main.js`. Every
URL the page uses is relative, so the app works under any prefix; the gateway
should redirect `/playground` to `/playground/`.

- Probes: `GET /healthz` returns `200 ok` (no eval, no file read).
- Container port `8080`; `securityContext`: `runAsNonRoot: true`,
  `runAsUser: 10001`, `readOnlyRootFilesystem: true`; mount an `emptyDir` at
  `/tmp`.
