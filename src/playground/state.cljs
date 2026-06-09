(ns playground.state)

(def default-code
  ";; Welcome to the ClojureWasm Playground.
;; This runs real ClojureWasm (cljw) — a JVM-free Clojure runtime in Zig —
;; not Clojure on the JVM. Press Run (or Cmd/Ctrl+Enter).

(println \"Hello from ClojureWasm!\")

;; Clojure's numeric tower is intact:
(/ 1 3)
(reduce * (range 1 21)) ; 20!

;; Call a Rust module compiled to WebAssembly, over the FFI:
(def m (wasm/load \"modules/numtheory.wasm\"))
(println \"100th prime =\" (wasm/call m \"nth_prime\" 100))
")

(def empty-db
  {:running?    false
   :result      nil        ; {:out :err :exit :ms :timed-out}
   :modules     []         ; wasm/call modules (from GET /api/modules :modules)
   :commands    []         ; wasm/run command modules (… :commands)
   :examples    []         ; from GET /api/examples
   :health      nil        ; {:ok bool :cljw bool}
   :active-tab  :output})  ; :output | :modules
