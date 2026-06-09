(ns playground.sandbox
  "Evaluate untrusted Clojure code IN-PROCESS on cljw via cljw.eval/with-budget
  (ADR-0125 / D-355). No babashka, no subprocess: the budget breach
  (steps / deadline / heap) is RECOVERED as a value, so a runaway eval cannot
  hang or OOM the server — it returns an :error and the process survives. cljw's
  read-string is eval-free (ADR-0122) so reading the user's form is itself safe;
  evaluation is what the budget bounds."
  (:require [clojure.string :as str]))

(defn- env-int [k default]
  (Integer/parseInt (or (System/getenv k) default)))

;; Per-eval budget knobs use PG_EVAL_* (NOT CLJW_EVAL_*): the CLJW_EVAL_* env vars
;; arm cljw's PROCESS-WIDE budget at startup (installFromEnv), which would kill
;; the long-running server itself (e.g. the deadline trips a few seconds in).
;; The budget must apply ONLY to the eval — via with-budget below — so the server
;; stays unmetered while each submission is bounded.
(def config
  {:max-steps   (env-int "PG_EVAL_MAX_STEPS" "50000000")
   :deadline-ms (env-int "PG_EVAL_DEADLINE_MS" "5000")
   :max-heap-mb (env-int "PG_EVAL_MAX_HEAP_MB" "128")
   :max-code    200000})

(defn binary-ready?
  "Always true: evaluation is in-process (no external cljw binary to locate)."
  []
  true)

(defn run
  "Evaluate `code` under the in-process execution budget and return
  {:out <stdout string> :value <pr-str of the result> :ms <int>} on success,
  or {:error <message> :timed-out <bool>} on a user error or a budget breach.
  A budget breach is uncatchable from the evaluated code (it cannot swallow its
  own timeout) but is recovered HERE as the :cljw.eval/exhausted marker."
  [code]
  (cond
    (not (string? code))                {:error "code must be a string"}
    (str/blank? code)                   {:out "" :value "nil" :ms 0}
    (> (count code) (:max-code config)) {:error "code too large"}
    :else
    (let [t0  (System/nanoTime)
          res (cljw.eval/with-budget
                {:max-steps   (:max-steps config)
                 :deadline-ms (:deadline-ms config)
                 :max-heap-mb (:max-heap-mb config)}
                (fn []
                  ;; A normal exception (user code throws) is catchable here and
                  ;; becomes {:error …}; a BUDGET breach is uncatchable, escapes
                  ;; this try, and surfaces as the :cljw.eval/exhausted marker.
                  (try
                    (let [result (volatile! nil)
                          out    (with-out-str
                                   (vreset! result (eval (read-string (str "(do " code "\n)")))))]
                      {:out out :value (pr-str @result)})
                    (catch Throwable e
                      {:error (or (ex-message e) (str e)) :out ""}))))
          ms  (quot (- (System/nanoTime) t0) 1000000)]
      (if (and (map? res) (contains? res :cljw.eval/exhausted))
        (let [axis (name (:cljw.eval/exhausted res))]
          {:error (str "execution exceeded its " axis " budget and was stopped")
           :timed-out true
           :ms ms})
        (assoc res :ms ms)))))
