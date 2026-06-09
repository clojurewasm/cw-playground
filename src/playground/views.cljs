(ns playground.views
  (:require [io.factorhouse.hsx.core :as hsx]
            [io.factorhouse.rfx.core :as rfx]
            [playground.editor :as editor]
            [playground.icons :as icons]
            [playground.state :as state]
            ["@mantine/core" :as mantine :refer [Box Group Stack Button ActionIcon
                                                 Text Title Badge Tooltip Menu
                                                 SegmentedControl ScrollArea Card
                                                 Loader Divider Anchor Code]]))

(def ^js MenuTarget (.-Target Menu))
(def ^js MenuDropdown (.-Dropdown Menu))
(def ^js MenuItem (.-Item Menu))
(def ^js MenuLabel (.-Label Menu))

(defn- icon [c props] (hsx/create-element [:> c props]))

;; ---------------------------------------------------------------- header ----

(defn- health-badge []
  (let [h (rfx/use-sub [:app/health])]
    (cond
      (nil? h) [:> Badge {:color "gray" :variant "light"} "connecting…"]
      (:cljw h) [:> Badge {:color "teal" :variant "light"
                           :leftSection (icon icons/lightning {:size 12 :weight "fill"})}
                 "cljw ready"]
      :else [:> Badge {:color "red" :variant "light"} "cljw not built"])))

(defn- header []
  [:> Group {:justify "space-between" :px "md" :py "xs"
             :style {:borderBottom "1px solid var(--mantine-color-gray-3)"}}
   [:> Group {:gap "xs" :align "center"}
    (icon icons/lightning {:size 24 :weight "fill" :color "var(--mantine-color-indigo-6)"})
    [:> Title {:order 4} "ClojureWasm Playground"]
    [:> Text {:size "xs" :c "dimmed"} "JVM-free Clojure, running in your terminal's runtime"]]
   [:> Group {:gap "xs"}
    [health-badge]
    [:> Tooltip {:label "Source on GitHub"}
     [:> ActionIcon {:variant "subtle" :color "gray" :size "lg"
                     :component "a" :href "https://github.com/clojurewasm/ClojureWasm" :target "_blank"}
      (icon icons/github {:size 20})]]]])

;; --------------------------------------------------------------- toolbar ----

(defn- examples-menu []
  (let [examples (rfx/use-sub [:app/examples])]
    [:> Menu {:shadow "md" :width 280 :position "bottom-start"}
     [:> MenuTarget
      [:> Button {:variant "default" :size "sm"
                  :leftSection (icon icons/books {:size 16})} "Examples"]]
     [:> MenuDropdown
      [:> MenuLabel "Load an example"]
      (for [{:keys [id title code]} examples]
        ^{:key id}
        [:> MenuItem {:onClick #(editor/set-code! code)} title])]]))

(defn- toolbar []
  (let [dispatch (rfx/use-dispatch)
        running? (rfx/use-sub [:app/running?])
        result   (rfx/use-sub [:app/result])
        run!     #(dispatch [:app/run (editor/get-code)])]
    [:> Group {:justify "space-between" :px "md" :py "xs"
               :style {:borderBottom "1px solid var(--mantine-color-gray-2)"}}
     [:> Group {:gap "xs"}
      [:> Button {:color "indigo" :size "sm" :onClick run! :loading running?
                  :leftSection (icon icons/play {:size 16 :weight "fill"})}
       "Run"]
      [:> Tooltip {:label "Cmd/Ctrl + Enter"}
       [:> Text {:size "xs" :c "dimmed"} "⌘⏎"]]
      [examples-menu]
      [:> Button {:variant "subtle" :color "gray" :size "sm"
                  :onClick #(dispatch [:app/clear])
                  :leftSection (icon icons/trash {:size 16})} "Clear"]]
     (when result
       [:> Group {:gap "xs"}
        (when (:timed-out result)
          [:> Badge {:color "orange" :variant "light"
                     :leftSection (icon icons/clock {:size 12})} "timed out"])
        [:> Badge {:color (if (zero? (:exit result)) "teal" "red") :variant "light"}
         (str "exit " (:exit result))]
        [:> Badge {:variant "light" :color "gray"
                   :leftSection (icon icons/clock {:size 12})}
         (str (:ms result) " ms")]])]))

;; -------------------------------------------------------------- output ------

(defn- output-view []
  (let [running? (rfx/use-sub [:app/running?])
        result   (rfx/use-sub [:app/result])]
    (cond
      running?
      [:> Group {:justify "center" :py "xl"} [:> Loader {:size "sm"}]
       [:> Text {:c "dimmed" :size "sm"} "Evaluating on ClojureWasm…"]]

      (nil? result)
      [:> Text {:c "dimmed" :size "sm" :p "md"}
       "Press Run to evaluate. Each top-level form's value is printed REPL-style."]

      :else
      [:> Box {:p "md"}
       (when (:error result)
         [:> Text {:c "red" :size "sm"} (:error result)])
       (when (seq (:out result))
         [:> Box {:component "pre"
                  :style {:whiteSpace "pre-wrap" :margin 0 :fontSize "13px"
                          :fontFamily "ui-monospace, SFMono-Regular, Menlo, monospace"}}
          (:out result)])
       (when (seq (:err result))
         [:> Box {:component "pre" :mt (if (seq (:out result)) "sm" 0)
                  :style {:whiteSpace "pre-wrap" :margin 0 :fontSize "13px"
                          :color "var(--mantine-color-red-7)"
                          :fontFamily "ui-monospace, SFMono-Regular, Menlo, monospace"}}
          (:err result)])
       (when (and (zero? (or (:exit result) 0))
                  (empty? (:out result)) (empty? (:err result)))
         [:> Text {:c "dimmed" :size "sm"} "(no output)"])])))

(defn- module-card [{:keys [file lang title blurb fns]}]
  [:> Card {:withBorder true :radius "md" :padding "sm" :mb "sm"}
   [:> Group {:justify "space-between" :mb 4}
    [:> Group {:gap 6 :align "center"}
     (icon icons/cube {:size 16 :weight "duotone" :color "var(--mantine-color-indigo-6)"})
     [:> Text {:fw 600 :size "sm"} title]]
    [:> Badge {:size "xs" :variant "light" :color "orange"} lang]]
   [:> Text {:size "xs" :c "dimmed" :mb "xs"} blurb]
   [:> Text {:size "xs" :c "dimmed" :mb 6}
    [:> Code (str "(wasm/load \"modules/" file "\")")]]
   [:> Stack {:gap 4}
    (for [{:keys [name sig doc example]} fns]
      ^{:key name}
      [:> Tooltip {:label doc :position "left" :withArrow true}
       [:> Group {:gap 6 :wrap "nowrap"
                  :style {:cursor "pointer"}
                  :onClick #(editor/append-code!
                             (str "(def m (wasm/load \"modules/" file "\"))\n" example))}
        (icon icons/caret-right {:size 12 :color "var(--mantine-color-indigo-5)"})
        [:> Code {:style {:fontSize "12px"}} name]
        [:> Text {:size "xs" :c "dimmed"} sig]]])]])

(defn- command-card [{:keys [file lang title blurb example]}]
  [:> Card {:withBorder true :radius "md" :padding "sm" :mb "sm"}
   [:> Group {:justify "space-between" :mb 4}
    [:> Group {:gap 6 :align "center"}
     (icon icons/cube {:size 16 :weight "duotone" :color "var(--mantine-color-teal-6)"})
     [:> Text {:fw 600 :size "sm"} title]]
    [:> Badge {:size "xs" :variant "light" :color "cyan"} lang]]
   [:> Text {:size "xs" :c "dimmed" :mb "xs"} blurb]
   [:> Tooltip {:label "Insert this call" :position "left" :withArrow true}
    [:> Group {:gap 6 :wrap "nowrap" :style {:cursor "pointer"}
               :onClick #(editor/append-code! example)}
     (icon icons/caret-right {:size 12 :color "var(--mantine-color-teal-5)"})
     [:> Code {:style {:fontSize "11px"}} (str "(wasm/run \"modules/" file "\" …)")]]]])

(defn- modules-view []
  (let [modules (rfx/use-sub [:app/modules])
        commands (rfx/use-sub [:app/commands])]
    [:> ScrollArea {:style {:height "100%"}}
     [:> Box {:p "md"}
      [:> Text {:size "xs" :c "dimmed" :mb "sm"}
       "Pre-built WebAssembly on the load path. Click to insert a call."]
      [:> Text {:size "xs" :fw 700 :tt "uppercase" :c "dimmed" :mb 6} "Rust — wasm/call (pure compute)"]
      (for [m modules] ^{:key (:file m)} [module-card m])
      (when (seq commands)
        [:<>
         [:> Text {:size "xs" :fw 700 :tt "uppercase" :c "dimmed" :mt "md" :mb 6} "Go — wasm/run (WASI programs)"]
         (for [c commands] ^{:key (:file c)} [command-card c])])]]))

(defn- result-pane []
  (let [dispatch (rfx/use-dispatch)
        tab      (rfx/use-sub [:app/active-tab])]
    [:> Stack {:gap 0 :style {:height "100%"}}
     [:> Box {:px "md" :py "xs"
              :style {:borderBottom "1px solid var(--mantine-color-gray-2)"}}
      [:> SegmentedControl
       {:size "xs"
        :value (clojure.core/name tab)
        :onChange #(dispatch [:app/set-tab (keyword %)])
        :data [#js {:label "Output" :value "output"}
               #js {:label "Wasm modules" :value "modules"}]}]]
     [:> ScrollArea {:style {:flex 1}}
      (case tab
        :modules [modules-view]
        [output-view])]]))

;; ---------------------------------------------------------------- root ------

(defn app-root []
  (let [dispatch (rfx/use-dispatch)
        run!     #(dispatch [:app/run (editor/get-code)])]
    [:> Stack {:gap 0 :style {:height "100vh"}}
     [header]
     [toolbar]
     [:> Box {:style {:display "flex" :flex 1 :minHeight 0}}
      [:> Box {:style {:flex 1 :minWidth 0 :borderRight "1px solid var(--mantine-color-gray-3)"}}
       [editor/editor {:initial state/default-code :on-run run!}]]
      [:> Box {:style {:flex 1 :minWidth 0}}
       [result-pane]]]]))
