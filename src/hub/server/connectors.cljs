(ns hub.server.connectors
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(defn create-chan [] (chan (sliding-buffer 16)))

;; WIRE
(defmethod ig/init-key :type/wire [[_ id] {:keys [from to] :as config}]
  (go-loop []
           (let [incoming-message (<! (:output from))]
             (>! (:input to) incoming-message))
           (recur)))

;; FILTER
(defmethod ig/prep-key :type/filter [_ config]
  (merge {:input (create-chan) :output (create-chan)} config))

(defmethod ig/init-key :type/filter [[_ id] {:keys [input output channels] :as config}]
  (go-loop []
           (let [incoming (<! input)]
             ;(prn [:filter  incoming])
             (if (some #{(.-channel incoming)} channels)
               (>! output incoming)))
           (recur))
  config)

(defmethod ig/halt-key! :type/filter [_ {:keys [input output] :as config}]
  (close! input)
  (close! output))

