(ns hub.server.core
  (:require [hub.server.midi]
            [hub.server.connectors]
            [hub.server.cv]
            [hub.server.http]
            [hub.server.patch :as patch]))

(defn start []
  (hub.server.http/server)
  (patch/init!))

;(defn ^:dev/before-load stop []
;  (ig/suspend! system)
;  (.close @!http-server))
