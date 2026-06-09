(ns playground.events
  (:require [io.factorhouse.rfx.core :as rfx]))

(defn register! []
  (rfx/reg-event-fx :app/init
    (fn [{:keys [db]} _]
      {:db db
       :http/get [{:path "/api/modules"  :on-ok [:modules/loaded]}
                  {:path "/api/examples" :on-ok [:examples/loaded]}
                  {:path "/api/health"   :on-ok [:health/loaded]}]}))

  (rfx/reg-event-db :modules/loaded  (fn [db [_ body]] (assoc db :modules (:modules body) :commands (:commands body))))
  (rfx/reg-event-db :examples/loaded (fn [db [_ body]] (assoc db :examples body)))
  (rfx/reg-event-db :health/loaded   (fn [db [_ body]] (assoc db :health body)))

  ;; `code` is read from the editor in the view and passed in, keeping this pure.
  (rfx/reg-event-fx :app/run
    (fn [{:keys [db]} [_ code]]
      {:db (assoc db :running? true :active-tab :output)
       :http/post {:path "/api/eval" :body {:code code}
                   :on-ok [:app/run-result] :on-err [:app/run-error]}}))

  (rfx/reg-event-db :app/run-result
    (fn [db [_ body]] (assoc db :running? false :result body)))

  (rfx/reg-event-db :app/run-error
    (fn [db [_ msg]]
      (assoc db :running? false
             :result {:out "" :err (str "Request failed: " msg) :exit -1})))

  (rfx/reg-event-db :app/set-tab (fn [db [_ t]] (assoc db :active-tab t)))
  (rfx/reg-event-db :app/clear   (fn [db _] (assoc db :result nil))))
