# ClojureWasm playground: container image (self-contained, root-context).
#
# cljw comes from the pinned GitHub release tarball of BuddhiLW/ClojureWasm
# (ReleaseSafe, -Dwasm, -Dcpu=baseline), verified against CLJW_SHA256. No Zig,
# no from-source build. The SPA bundle and Wasm modules are committed artifacts
# (see DEPLOY.md, PROVENANCE.md), so no Node/JDK/Rust/Go stage either.
#
# .github/workflows/cljw-bump.yml rewrites the two ARG defaults below when a new
# cljw release appears; keep each on its own `ARG NAME=value` line.

# --- Stage 1: fetch + verify the cljw release binary ---
FROM debian:bookworm-slim AS cljw
ARG CLJW_VERSION=v1.15.1
ARG CLJW_SHA256=cb7ad4a5a137a70fb3ef061c782ef4ced5b9a5a9c3df5a7dcc6a444ac5d13a0c
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates curl && rm -rf /var/lib/apt/lists/*
WORKDIR /dl
RUN curl -fsSLo cljw.tar.gz \
      "https://github.com/BuddhiLW/ClojureWasm/releases/download/${CLJW_VERSION}/cljw-linux-x86_64.tar.gz" && \
    echo "${CLJW_SHA256}  cljw.tar.gz" | sha256sum -c - && \
    tar -xzf cljw.tar.gz cljw && \
    install -m 0755 cljw /usr/local/bin/cljw && \
    /usr/local/bin/cljw --version

# --- Stage 2: runtime (cljw + the committed app: server + static SPA + wasm) ---
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates && rm -rf /var/lib/apt/lists/* && \
    groupadd --gid 10001 app && \
    useradd --uid 10001 --gid 10001 --no-create-home --home-dir /tmp \
        --shell /usr/sbin/nologin app
WORKDIR /app
COPY --from=cljw /usr/local/bin/cljw /usr/local/bin/cljw
COPY deps.edn ./deps.edn
COPY server/ ./server/
COPY resources/ ./resources/
COPY modules/ ./modules/

# Env-driven config (override with `docker run -e` or the k8s Deployment env).
# The image runs with a read-only root filesystem: anything written goes to
# /tmp (HOME points there), which the runtime must mount writable.
ENV PG_PORT=8080 \
    PG_PUBLIC_DIR=/app/resources/public \
    PG_WASM_DIR=/app/modules \
    PG_EXAMPLES=/app/resources/examples.edn \
    CLJW_FS_ROOT=/app \
    PG_EVAL_MAX_STEPS=50000000 \
    PG_EVAL_DEADLINE_MS=5000 \
    PG_EVAL_MAX_HEAP_MB=128 \
    HOME=/tmp \
    TMPDIR=/tmp

USER 10001:10001
EXPOSE 8080
# run-server binds 0.0.0.0:PG_PORT.
CMD ["cljw", "-M:cljw", "-m", "playground.server"]
