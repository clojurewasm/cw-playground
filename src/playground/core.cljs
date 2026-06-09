(ns playground.core
  (:require ["react-dom/client" :as rdc]
            ["@mantine/core" :refer [MantineProvider createTheme]]
            [io.factorhouse.hsx.core :as hsx]
            [io.factorhouse.rfx.core :as rfx]
            [playground.api :as api]
            [playground.events :as events]
            [playground.subs :as subs]
            [playground.state :as state]
            [playground.views :as views]))

(defonce ^:private ctx (rfx/init {:initial-value state/empty-db}))
(defonce ^:private root (atom nil))

(def ^:private theme
  (createTheme #js {:primaryColor "indigo" :defaultRadius "md"}))

(defn- register-effects! []
  (rfx/reg-fx :http/get
    (fn [{:keys [dispatch]} specs]
      (doseq [{:keys [path on-ok]} (if (map? specs) [specs] specs)]
        (api/get-json path
                      (fn [body] (dispatch (conj on-ok body)))
                      (fn [err] (js/console.error "GET" path err))))))
  (rfx/reg-fx :http/post
    (fn [{:keys [dispatch]} {:keys [path body on-ok on-err]}]
      (api/post-json path body
                     (fn [resp] (dispatch (conj on-ok resp)))
                     (fn [err]  (dispatch (conj on-err err)))))))

(defn- app []
  [:> rfx/RfxContextProvider #js {"value" ctx}
   [:> MantineProvider {:theme theme :defaultColorScheme "light"}
    [views/app-root]]])

(defn ^:dev/after-load mount []
  (when-not @root
    (reset! root (rdc/createRoot (.getElementById js/document "app"))))
  (.render ^js @root (hsx/create-element [app])))

(defn init []
  (events/register!)
  (subs/register!)
  (register-effects!)
  (mount)
  (rfx/dispatch ctx [:app/init]))
