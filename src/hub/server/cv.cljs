(ns hub.server.cv
  (:require [integrant.core :as ig]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

;; CV OUT
(defmethod ig/prep-key :type/cv-out [_ config]
  (assoc config :input (chan (sliding-buffer 16))))

(defmethod ig/init-key :type/cv-out [[_ id] {:keys [input] :as config}]
  (go-loop []
           (when-let [incoming (<! input)]
             (prn [:cv-out incoming]))
           (recur))
  config)

(defmethod ig/resume-key :type/cv-out [_ config]
  (prn [:resume-cv-out config])
  config)
