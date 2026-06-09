(ns playground.editor
  "CodeMirror 6 editor with Clojure syntax + paren matching. The editor owns the
  text (uncontrolled); read it with `get-code`, replace it with `set-code!`."
  (:require ["react" :as react]
            ["@codemirror/state" :refer [EditorState]]
            ["@codemirror/view" :refer [EditorView keymap lineNumbers
                                        drawSelection highlightActiveLine]]
            ["@codemirror/commands" :refer [defaultKeymap history historyKeymap
                                            indentWithTab]]
            ["@codemirror/language" :refer [syntaxHighlighting defaultHighlightStyle
                                            bracketMatching indentOnInput]]
            ["@nextjournal/lang-clojure" :refer [clojure]]))

(defonce ^:private view-atom (atom nil))

(defn get-code []
  (if-let [v @view-atom] (.toString (.. v -state -doc)) ""))

(defn set-code! [code]
  (when-let [v @view-atom]
    (.dispatch v #js {:changes #js {:from 0
                                    :to (.. v -state -doc -length)
                                    :insert code}})
    (.focus v)))

(defn append-code! [code]
  (when-let [v @view-atom]
    (let [len (.. v -state -doc -length)
          ins (str (when (pos? len) "\n\n") code)]
      (.dispatch v #js {:changes #js {:from len :to len :insert ins}
                        :selection #js {:anchor (+ len (count ins))}
                        :scrollIntoView true}))
    (.focus v)))

(defn- make-view [parent initial on-run]
  (let [run-binding #js {:key "Mod-Enter" :preventDefault true
                         :run (fn [_] (on-run) true)}
        exts #js [(lineNumbers)
                  (history)
                  (drawSelection)
                  (bracketMatching)
                  (indentOnInput)
                  (highlightActiveLine)
                  (syntaxHighlighting defaultHighlightStyle)
                  (clojure)
                  EditorView.lineWrapping
                  (.of keymap
                       (.concat #js [run-binding indentWithTab]
                                defaultKeymap historyKeymap))]
        state (.create EditorState #js {:doc initial :extensions exts})]
    (EditorView. #js {:state state :parent parent})))

(defn editor
  "Props: {:initial string :on-run (fn [])}. on-run fires on Cmd/Ctrl+Enter."
  [{:keys [initial on-run]}]
  (let [ref (react/useRef nil)]
    (react/useEffect
     (fn []
       (when (and (.-current ref) (nil? @view-atom))
         (reset! view-atom (make-view (.-current ref) (or initial "") on-run)))
       (fn []
         (when @view-atom (.destroy @view-atom) (reset! view-atom nil))))
     #js [])
    [:div {:ref ref :style {:height "100%" :overflow "auto"
                            :fontSize "14px" :background "#fff"}}]))
