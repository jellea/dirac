(ns hub.server.connectors
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(defn create-chan [] (chan (sliding-buffer 16)))

;; WIRE
(defmethod ig/init-key :type/wire [[_ id] {:keys [from to] :as config}]
  (go-loop []
           (when-let [incoming-message (<! (:output from))]
             (prn [:wire incoming-message])
             (>! (:input to) incoming-message))
           (recur))
  config)

(defmethod ig/halt-key! :type/wire [_ {}]
  (prn [:killed-wire]))

(defmethod ig/resume-key :type/wire [_ config]
  (prn [:resume-wire])
  config)

;; FILTER
(defmethod ig/prep-key :type/filter [_ config]
  (assoc config :input (create-chan) :output (create-chan)))

(defmethod ig/init-key :type/filter [[_ id] {:keys [input output channels] :as config}]
  (go-loop []
           (when-let [incoming (<! input)]
             (prn [:filter! incoming])
             (when (some #{(.-channel incoming)} channels)
               (prn [:filter! channels])
               (>! output incoming)))
           (recur))
  config)

(defmethod ig/halt-key! :type/filter [_ {:keys [input output] :as config}]
  (close! input)
  (close! output)
  (prn [:killed-filter]))

(defmethod ig/resume-key :type/filter [key config old-config old-impl]
  config)
  ;(prn [:resume-filter config old-config old-impl]))
  ;(reset! (:channels old-impl) (:channels config)))
