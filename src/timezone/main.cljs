(ns main
  (:require [reagent.dom  :as dom]
            [re-frame.core :as re-frame]
            [converter :as c]))

(defn init []
  (re-frame/dispatch-sync [:initialize])
  (dom/render [c/component] 
    (js/document.getElementById "app")))