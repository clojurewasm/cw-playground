# Running & deploying the ClojureWasm playground

The app is **self-contained**: `cljw` is built from source (the pinned ClojureWasm
ref `v1.10.0`, `-Dwasm` ReleaseSafe; zwasm `v2.5.0` resolves via
ClojureWasm's `build.zig.zon` tag pin), and the SPA + Wasm modules are committed.
Local and fly obtain `cljw` the same way — local via `run_local.sh`, fly via the
root `Dockerfile`. The Wasm FFI runs **JIT-compiled by default** — a runtime engine
choice, not a build flag, so the build options above are unchanged.

## Local

```sh
./run_local.sh                 # builds cljw on first run (~1 min), serves :8080
PG_PORT=9000 ./run_local.sh    # override any knob
```

Needs `git` + Zig 0.16 for the one-time `cljw` build (cached in `.cache/`). The
cljw build is the only from-source step; the frontend (`resources/public/js`) and
Wasm modules (`modules/`) are committed artifacts (see
[`PROVENANCE.md`](./PROVENANCE.md)).

### Rebuilding the artifacts (only when you change them)

```sh
npm ci && npm run release      # rebuild the SPA → resources/public/js/main.js
bash scripts/build_wasm.sh     # rebuild the Wasm modules → modules/*.wasm
```

## Deploy to fly.io

Because the `Dockerfile` + `fly.toml` are at the repo root and the build context
is the repo itself, you can deploy by **selecting this repo** in fly.io
("Deploy from GitHub"), or from a checkout:

```sh
fly launch        # first time: creates the app from fly.toml
fly deploy        # subsequent deploys
```

The image builds `cljw` from source in stage 1 (Zig only — no Node/Java/Rust/Go
toolchains needed, since the SPA + Wasm are committed) and runs it in a slim
stage 2. Scale-to-zero is on (`auto_stop_machines`), so an idle demo costs
nothing. Tune the per-eval budgets via `fly.toml [env]` (`PG_EVAL_*`).

### Auto-deploy on push — already wired

`.github/workflows/fly-deploy.yml` runs `flyctl deploy --remote-only` on every
push to `main`, and two deploys of this app never overlap (the workflow takes a
`concurrency` group; a newer push cancels the one in flight).

It needs a `FLY_API_TOKEN` repo secret (`fly tokens create deploy -a cw-playground`;
[fly docs](https://fly.io/docs/launch/continuous-deployment-with-github-actions/)).
Without it the workflow FAILS rather than skipping — a deploy you believed
happened and did not is worse than a red check.

**So: pushing to `main` deploys.** Land a change on a branch if you do not want
that.
