(ns hub.server.midi
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(def midi (js/require "easymidi"))

(defn create-chan [] (chan (sliding-buffer 16)))

;; MIDI IN
(defmethod ig/prep-key :type/midi-in [_ config]
  (assoc config :output (create-chan)))


(defmethod ig/init-key :type/midi-in [[_ id] {:keys [output] :as config}]
  (let [input (midi.Input. "IAC Driver Bus 1") #_(-> (midi.getInputs) first (midi.Input.))]
    (.on ^js/midi.Input. input "noteon"
         (fn [msg]
           (prn [:inc msg])
           ;(js/console.log 1 (.now (.-performance perf_hooks)))
           (go (>! output msg)))))
  config)

(defmethod ig/halt-key! :type/midi-in [_ {:keys [output] :as config}]
  (close! output)
  (prn [:halt-midi-in]))

(defmethod ig/resume-key :type/midi-in [_ {:keys [output] :as config}]
  (prn [:resume-midi-in])
  (assoc config :output (create-chan)))

;; MIDI OUT
(defmethod ig/prep-key :type/midi-out [_ config]
  (assoc config :input (create-chan)))

(defmethod ig/init-key :type/midi-out [[_ id] {:keys [input] :as config}]
  ;; TODO don't crash when midi output not found
  (let [midi-output (midi.Output. "MIDI Monitor (Untitled)")]
    (go-loop []
             (when-let [incoming (<! input)]
               (try
                 (prn [:inc! incoming])
                 (.send ^js/midi.Input. midi-output "noteon" incoming)
                 (catch :default e
                   (js/console.error "Unable to send midi message"))))
             ;(js/console.log 2 (.now (.-performance perf_hooks))))
             (recur)))
  config)

(defmethod ig/halt-key! :type/midi-out [_ {:keys [input] :as config}]
  (close! input)
  (prn [:halt-midi-out]))

(defmethod ig/resume-key :type/midi-out [_ {:keys [input] :as config}]
  (prn [:resume-midi-out])
  config)
