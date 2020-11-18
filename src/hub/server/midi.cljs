(ns hub.server.midi
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(def midi (js/require "easymidi"))

(defn create-chan [] (chan (sliding-buffer 16)))

;; MIDI IN
(defmethod ig/prep-key :type/midi-in [_ config]
  (merge {:output (create-chan)} config))

(defmethod ig/init-key :type/midi-in [[_ id] {:keys [output] :as config}]
  (let [input  (-> (midi.getInputs) first (midi.Input.))]
    (.on ^js/midi.Input. input "noteon"
         (fn [msg]
           ;(js/console.log 1 (.now (.-performance perf_hooks)))
           (go (>! output msg)))))
  config)

(defmethod ig/halt-key! :type/midi-in [_ {:keys [output]}]
  (close! output))

;; MIDI OUT
(defmethod ig/prep-key :type/midi-out [_ config]
  (merge {:input (create-chan)} config))

(defmethod ig/init-key :type/midi-out [[_ id] {:keys [input] :as config}]
  (let [midi-output (midi.Output. "MIDI Monitor (Untitled)")]
    (go-loop []
             (let [incoming (<! input)]
               (try
                 (.send ^js/midi.Input. midi-output "noteon" incoming)
                 (catch :default e
                   (js/console.error "Unable to send midi message"))))
             ;(js/console.log 2 (.now (.-performance perf_hooks))))
             (recur)))
  config)

(defmethod ig/halt-key! :type/midi-out [_ {:keys [input]}]
  (close! input))
