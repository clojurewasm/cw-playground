(ns playground.icons
  "Phosphor icons, re-exported under cljs-friendly names. Render with hsx as
  (hsx/create-element [:> icons/play {:size 18 :weight \"bold\"}])."
  (:require ["@phosphor-icons/react" :refer [Play Lightning Cube Books Trash
                                             Copy GithubLogo Clock Warning
                                             CheckCircle Code ArrowClockwise
                                             CaretRight]]))

(def play Play)
(def lightning Lightning)
(def cube Cube)
(def books Books)
(def trash Trash)
(def copy Copy)
(def github GithubLogo)
(def clock Clock)
(def warning Warning)
(def check CheckCircle)
(def code Code)
(def refresh ArrowClockwise)
(def caret-right CaretRight)
