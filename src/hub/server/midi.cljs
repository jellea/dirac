(ns hub.server.midi
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [async-error.core :refer-macros [go-try <?]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(def midi (js/require "easymidi"))

(defn create-chan [] (chan (sliding-buffer 16)))

;; MIDI IN
(defmethod ig/prep-key :type/midi-in [_ config]
  (assoc config :output (create-chan)
                :midi-input (midi.Input. "IAC Driver Bus 1"))) #_(-> (midi.getInputs) first (midi.Input.))

(defmethod ig/init-key :type/midi-in [[_ id] {:keys [midi-input output] :as config}]
  (.on ^js/midi.Input. midi-input "noteon"
       (fn [msg]
         (prn [:inc msg])
         ;(js/console.log 1 (.now (.-performance perf_hooks)))
         (go-try (>! output msg))))
  config)

(defmethod ig/halt-key! :type/midi-in [id {:keys [midi-input output] :as config}]
  (some-> midi-input .removeAllListeners)
  (some-> midi-input .close)
  (some-> output close!)
  (prn [:halt-midi-in]))

(defmethod ig/resume-key :type/midi-in [_ {:keys [output] :as config}]
  (prn [:resume-midi-in])
  (assoc config :output (create-chan)))

;; MIDI OUT
(defmethod ig/prep-key :type/midi-out [_ config]
  (assoc config :input (create-chan)
                :midi-output (midi.Output. "MIDI Monitor (Untitled)")))

(defmethod ig/init-key :type/midi-out [[_ id] {:keys [input midi-output] :as config}]
  (go-try
    (loop []
      (when-let [incoming (<? input)]
        (prn [:inc! incoming])
        (.send ^js/midi.Input. midi-output "noteon" incoming)
        ;(js/console.log 2 (.now (.-performance perf_hooks)))
        (recur))))
  config)

(defmethod ig/halt-key! :type/midi-out [_ {:keys [input midi-output node-loop] :as config}]
  (prn [:midi-out config])
  (close! input)
  ;(close! node-loop)
  (.close midi-output)
  ;(some-> input close!)
  ;(some-> midi-output .close)
  (prn [:halt-midi-out]))

(defmethod ig/resume-key :type/midi-out [_ {:keys [input] :as config}]
  (prn [:resume-midi-out])
  config)
