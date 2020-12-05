(ns hub.server.connectors
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [async-error.core :refer-macros [go-try <?]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(defn create-chan [] (chan (sliding-buffer 16)))

(defn log [x]
  (prn [:logxxx x])
  x)

;; WIRE
(defmethod ig/init-key :type/wire [[_ id] {:keys [from to] :as config}]
  (go-try
    (loop []
      ;(prn [:wire-conf config])
      (when-let [incoming-message (some-> from :output <?)]
        ;(prn [:wire incoming-message])
        (some-> to :input (>! incoming-message))
        (recur)))))

(defmethod ig/resume-key :type/wire [_ config]
  (prn [:resume-wire])
  config)

;; FILTER
(defmethod ig/prep-key :type/filter [_ config]
  (assoc config :input (create-chan) :output (create-chan)))

(defmethod ig/init-key :type/filter [[_ id] {:keys [input output channels] :as config}]
  (go-try
    (loop []
      (when-let [incoming (<? input)]
        ;(prn [:filter! incoming])
        (if (some #{(.-channel incoming)} channels)
          (>! output incoming)
          (prn "midi channel filter!"))
        (recur))))
  config)

(defmethod ig/halt-key! :type/filter [_ {:keys [input output] :as config}]
  (prn [:killing-filter])
  ;(some-> input close!)
  ;(some-> output close!)
  (prn [:killed-filter]))

;(defmethod ig/resume-key :type/filter [key config old-config old-impl]
;  config)
  ;(prn [:resume-filter config old-config old-impl]))
  ;(reset! (:channels old-impl) (:channels config)))
