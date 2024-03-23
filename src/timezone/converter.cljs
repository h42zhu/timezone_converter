(ns converter
  (:require [clojure.string :as str]
            [re-frame.core :as re-frame]
            ["@js-joda/core" :as joda]
            ["@js-joda/timezone" :as joda-tz]))

;; Functions to perform the timzone conversion

(def log (.-log js/console))

(defn convert-timezone [time-str from-tz to-tz]
  (let [time-parts (str/split time-str #":")
        hour (int (first time-parts))
        minute (int (second time-parts))
        second (int (nth time-parts 2))
        
        from-datetime (joda/ZonedDateTime.of 2000 1 1 hour minute second 0 (joda/ZoneId.of from-tz))
        ;; converted (formatInTimeZone date "America/New_York" "yyyy-MM-dd HH:mm:ss zzz")
        to-datetime (.withZoneSameInstant from-datetime (joda/ZoneId.of to-tz))
        ]
    (log hour minute second from-datetime to-datetime from-tz to-tz)
    (str to-datetime)))

;; Define the initial application state
(re-frame/reg-event-db       
 :initialize                 
 (fn [_ _]                   
   {:input-time "00:00:00"   
    :input-timezone "Africa/Abidjan"
    :output-timezone "Africa/Abidjan"}))

(re-frame/reg-event-db
  :update-input-time
  (fn [db [_ time]]
    (assoc db :input-time time)))

(re-frame/reg-event-db
  :update-output-time
  (fn [db [_ time]]
    (assoc db :output-time time)))

(re-frame/reg-event-db
  :edit-timezone
  (fn [db [_ field timezone]]
    (assoc db field timezone)))

;; Event dispatch
(defn dispatch-input-time-change
  [val]
  (re-frame/dispatch [:update-input-time val]))

;; Query
(re-frame/reg-sub
 :input-time
 (fn [db _]     
   (:input-time db)))

(re-frame/reg-sub
 :input-timezone
 (fn [db _]     
   (:input-timezone db)))

(re-frame/reg-sub
 :output-timezone
 (fn [db _]     
   (:output-timezone db)))

;; Utility function
(defn- get-value [e]
  (-> e .-target .-value))

(defn get-all-timezones []
  (let [timezones (js/Intl.supportedValuesOf "timeZone")]
    (mapv #(str %) timezones)))

;; Define components
(def input-style {:border "1px solid #CCC"
                  :width "256px" 
                  :margin-left "8px"})

(defn timezone-dropdown [label]
  (let [label-keyword (if (= "input" label) :input-timezone :output-timezone)
        label-text (if (= "input" label) "From Time Zone" "To Time Zone")
        timezone @(re-frame/subscribe [label-keyword])]
    [:div
      [:label label-text]
      [:select {:value timezone
                :style input-style
                :on-change (fn [e] (re-frame/dispatch [:edit-timezone label-keyword (get-value e)]))}
      (map-indexed (fn [idx elem] [:option {:key idx} elem]) (get-all-timezones))]]))

(defn input-time-view []
  (let [input-time @(re-frame/subscribe [:input-time])]
    [:div
      [:label "Enter Time"]
      [:input {:type "time"
               :step "1"
               :style input-style
               :value input-time
               :on-change (fn [e] (dispatch-input-time-change (get-value e)))}]]))

(defn output-time-view []
  (let [input-time @(re-frame/subscribe [:input-time])
        input-timezone @(re-frame/subscribe [:input-timezone])
        output-timezone @(re-frame/subscribe [:output-timezone])
        output-time (convert-timezone input-time input-timezone output-timezone)]
    [:div
      [:label "Converted Time:"]
      [:div {:style {:width "256px" }} output-time]]))

(defn component []
  [:div
    [:h1 "Timezone Converter"]
    [input-time-view]
    [timezone-dropdown "input"]
    [timezone-dropdown "output"]
    [output-time-view]
    [:button "Convert"]])
