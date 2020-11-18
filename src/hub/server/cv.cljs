(ns hub.server.cv
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

;; CV OUT
(defmethod ig/prep-key :type/cv-out [_ config]
  (merge {:input (chan (sliding-buffer 16))} config))

(defmethod ig/init-key :type/cv-out [[_ id] {:keys [input] :as config}]
  (go-loop []
           (let [incoming (<! input)])
             ;(prn [:cv-out incoming]))
           (recur))
  config)
