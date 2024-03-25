(ns converter
  (:require [clojure.string :as str]
            [re-frame.core :as re-frame]
            ["@js-joda/core" :as joda]
            ["@js-joda/timezone" :as joda-tz]))

;; Functions to perform the timzone conversion

(def log (.-log js/console))

(defn- pad-zero [n]
  (let [s (str n)]
    (if (= 2 (count s)) s (str "0" s))))

(defn convert-timezone 
  """ This function handles the logic of converting between time zones
    the input string is in the format of hh:mm:ss
    returns a string in the same format
  """
  [time-str from-tz to-tz]
  (let [[hour minute second] (map int (str/split time-str #":"))
        time-now (joda/ZonedDateTime.now)
        from-year (.year time-now)
        from-month (.monthValue time-now)
        from-day (.dayOfMonth time-now)
        from-datetime (joda/ZonedDateTime.of from-year from-month from-day hour minute second 0 (joda/ZoneId.of from-tz))        
        to-datetime (.withZoneSameInstant from-datetime (joda/ZoneId.of to-tz))
        to-hour (.hour to-datetime)
        to-minute (.minute to-datetime)
        to-second (.second to-datetime)]
    (str (pad-zero to-hour) ":" (pad-zero to-minute) ":" (pad-zero to-second))))

;; Define the initial application state
(re-frame/reg-event-db       
 :initialize                 
 (fn [_ _]                   
   {:input-time 0
    :input-time-str "00:00:00"
    :input-timezone "Africa/Abidjan"
    :output-timezone "Africa/Abidjan"}))

(re-frame/reg-event-db
  :update-input-time
  (fn [db [_ time-int time-str]]
    (assoc db :input-time time-int
              :input-time-str time-str)))

(re-frame/reg-event-db
  :update-output-time
  (fn [db [_ time]]
    (assoc db :output-time time)))

(re-frame/reg-event-db
  :edit-timezone
  (fn [db [_ field timezone]]
    (assoc db field timezone)))

;; Query
(re-frame/reg-sub
 :input-time
 (fn [db _]     
   (:input-time db)))

(re-frame/reg-sub
 :input-time-str
 (fn [db _]     
   (:input-time-str db)))

(re-frame/reg-sub
 :input-timezone
 (fn [db _]     
   (:input-timezone db)))

(re-frame/reg-sub
 :output-timezone
 (fn [db _]     
   (:output-timezone db)))

;; Utility function
(defn- get-number-value [e]
  (-> e .-target .-valueAsNumber))

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
  (let [input-time @(re-frame/subscribe [:input-time])
        input-time-sanitized (if (js/isNaN input-time) 0 input-time)]
    (log "input-time number: " input-time)
    [:div
      [:label "Enter Time"]
      [:input {:type "time"
               :step "1"
               :style input-style
               :data-value-as-number input-time-sanitized 
               :on-change (fn [e] (re-frame/dispatch [:update-input-time (get-number-value e) (get-value e)]))}]]))

(defn output-time-view []
  (let [input-time-str @(re-frame/subscribe [:input-time-str])
        input-timezone @(re-frame/subscribe [:input-timezone])
        output-timezone @(re-frame/subscribe [:output-timezone])
        output-time (convert-timezone input-time-str input-timezone output-timezone)]
    [:div
      [:label "Converted Time:"]
      [:div {:style {:width "256px" }} output-time]]))

(defn component []
  [:div
    [:h1 "Timezone Converter"]
    [input-time-view]
    [timezone-dropdown "input"]
    [timezone-dropdown "output"]
    [output-time-view]])
