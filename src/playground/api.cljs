(ns playground.api
  "Thin fetch wrapper. In shadow dev (served on :3000) the backend is a separate
  origin (:8080, CORS-enabled); in the packaged build it is same-origin.")

(def base
  (if (= js/window.location.port "3000") "http://localhost:8080" ""))

(defn- ->clj [js-obj] (js->clj js-obj :keywordize-keys true))

(defn get-json
  "GET path -> promise of parsed body (clj) ; calls (on-ok body) / (on-err msg)."
  [path on-ok on-err]
  (-> (js/fetch (str base path) #js {:cache "no-store"})
      (.then (fn [r] (.json r)))
      (.then (fn [j] (on-ok (->clj j))))
      (.catch (fn [e] (on-err (str e))))))

(defn post-json
  ;; (POST is never cached, so no :cache override needed.)
  "POST path with JSON `body` -> (on-ok parsed) / (on-err msg)."
  [path body on-ok on-err]
  (-> (js/fetch (str base path)
                #js {:method "POST"
                     :headers #js {"content-type" "application/json"}
                     :body (js/JSON.stringify (clj->js body))})
      (.then (fn [r] (.json r)))
      (.then (fn [j] (on-ok (->clj j))))
      (.catch (fn [e] (on-err (str e))))))
