(ns playground.server
  "HTTP backend for the ClojureWasm playground, running ON cljw (no babashka,
  D-355). Serves the compiled SPA + a tiny JSON API; /api/eval runs untrusted
  code IN-PROCESS through cljw.eval/with-budget (playground.sandbox). Path
  confinement is the deploy FS-jail (CLJW_FS_ROOT, ADR-0123): every slurp /
  File op is jail-resolved, so a traversal raises and is served as 404."
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [cljw.json :as json]
            [cljw.fs :as fs]
            [cljw.http.server :as http]
            [playground.sandbox :as sandbox]))

(def ^:private public-dir (or (System/getenv "PG_PUBLIC_DIR") "resources/public"))
;; The wasm-module dir; its name matches the eval-side load convention
;; (`(wasm/load "modules/…")` resolves cwd-relative under the FS-jail).
(def ^:private wasm-dir   (or (System/getenv "PG_WASM_DIR") "modules"))
(def ^:private examples-edn (or (System/getenv "PG_EXAMPLES") "resources/examples.edn"))
(def ^:private port (Integer/parseInt (or (System/getenv "PG_PORT") "8080")))

(def ^:private cors
  {"access-control-allow-origin"  "*"
   "access-control-allow-methods" "GET, POST, OPTIONS"
   "access-control-allow-headers" "content-type"})

(def ^:private content-types
  {"html" "text/html; charset=utf-8" "js" "text/javascript; charset=utf-8"
   "css" "text/css; charset=utf-8"   "json" "application/json; charset=utf-8"
   "map" "application/json" "svg" "image/svg+xml" "ico" "image/x-icon"
   "wasm" "application/wasm" "png" "image/png" "woff2" "font/woff2"})

(defn- ext [path]
  (let [i (str/last-index-of path ".")] (when i (subs path (inc i)))))

(defn- json-resp [status body]
  {:status status
   :headers (merge {"content-type" "application/json; charset=utf-8"
                    "cache-control" "no-store"} cors)
   :body (json/encode body)})

(defn- read-edn-file [path]
  ;; slurp is jail-confined; a missing / out-of-jail file just yields nil.
  (try (edn/read-string (slurp path)) (catch Throwable _ nil)))

(def ^:private manifest
  (delay (or (read-edn-file (fs/path (fs/file wasm-dir "manifest.edn"))) {:modules []})))

(def ^:private examples
  (delay (or (read-edn-file examples-edn) [])))

(defn- slurp-file
  "Slurp `path` if it is a regular file inside the jail; nil otherwise (a
  traversal / missing file raises in the jail and is swallowed to nil)."
  [path]
  (try (when (fs/regular-file? path) (slurp path)) (catch Throwable _ nil)))

(defn- serve-static [uri]
  (let [rel  (if (= uri "/") "index.html" (subs uri 1))
        path (fs/path (fs/file public-dir rel))
        body (slurp-file path)]
    (cond
      body
      {:status 200
       :headers {"content-type" (get content-types (ext rel) "application/octet-stream")
                 "cache-control" "no-store"}
       :body body}

      ;; SPA fallback: an unknown non-API path serves index.html (client routing).
      (not (str/starts-with? uri "/api"))
      (if-let [idx (slurp-file (fs/path (fs/file public-dir "index.html")))]
        {:status 200 :headers {"content-type" "text/html; charset=utf-8"} :body idx}
        {:status 404 :headers {"content-type" "text/plain"} :body "not found"})

      :else {:status 404 :headers {"content-type" "text/plain"} :body "not found"})))

(defn- handle-eval [req]
  (let [body (some-> (:body req) json/decode)
        code (:code body)
        res  (sandbox/run code)]
    (json-resp (if (:error res) 400 200) res)))

(defn handler [req]
  (let [uri (:uri req) m (:request-method req)]
    (cond
      (= m :options) {:status 204 :headers cors}
      (and (= uri "/api/eval") (= m :post)) (handle-eval req)
      (= uri "/api/modules")  (json-resp 200 @manifest)
      (= uri "/api/examples") (json-resp 200 @examples)
      (= uri "/api/health")   (json-resp 200 {:ok true :cljw (sandbox/binary-ready?)})
      :else (update (serve-static uri) :headers merge (when (str/starts-with? uri "/api") cors)))))

(defn -main [& _]
  (println (str "ClojureWasm playground backend (in-process eval) on http://localhost:" port))
  (println (str "  serving:  " public-dir))
  (println (str "  wasm dir: " wasm-dir))
  (http/run-server handler {:port port}))
