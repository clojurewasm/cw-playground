(ns playground.subs
  (:require [io.factorhouse.rfx.core :as rfx]))

(defn register! []
  (rfx/reg-sub :app/running?   (fn [db _] (:running? db)))
  (rfx/reg-sub :app/result     (fn [db _] (:result db)))
  (rfx/reg-sub :app/modules    (fn [db _] (:modules db)))
  (rfx/reg-sub :app/commands   (fn [db _] (:commands db)))
  (rfx/reg-sub :app/examples   (fn [db _] (:examples db)))
  (rfx/reg-sub :app/health     (fn [db _] (:health db)))
  (rfx/reg-sub :app/active-tab (fn [db _] (:active-tab db))))
