(ns hub.server.core
  (:require [integrant.core :as ig]
            [cljs.tools.reader.edn :as edn]
            [macchiato.server :as http]
            [macchiato.middleware.resource :refer [wrap-resource]]))

(defmethod ig/init-key :type/wire [_ {:keys [from to]}])

(defmethod ig/init-key :type/midi-in [[_ id] {:keys []}])

(defmethod ig/init-key :type/filter [[_ id] {:keys []}]
  (prn [:testfilter id]))

(defmethod ig/init-key :type/midi-out [[_ id] {:keys []}])

(defmethod ig/init-key :type/cv-out [[_ id] {:keys []}])

(def config {:version 2,
             :entities
             {[:type/midi-in :node/midi-in-one] {}
              [:type/filter :node/filter-one] {:x 22 :y 8}
              [:type/midi-out :node/midi-out-one] {:x 36 :y 13}
              [:type/cv-out :node/cv-out-one] {:x 36 :y 21}
              [:type/wire :wire/one] {:from (ig/ref [:type/midi-in :node/midi-in-one])
                                      :to (ig/ref [:type/filter :node/filter-one])}
              [:type/wire :wire/two] {:from (ig/ref [:type/filter :node/filter-one])
                                      :to (ig/ref [:type/midi-out :node/midi-out-one])}
              [:type/wire :wire/three] {:from (ig/ref [:type/filter :node/filter-one])
                                        :to (ig/ref [:type/cv-out :node/cv-out-one])}}})

(-> config :entities ig/init)

(defn handler
  [request callback]
  (callback {:status 200
             :body "Hello Macchiato"}))

(defn server []
  (let [host "127.0.0.1"
        port 3000]
    (http/start
      {:handler    (wrap-resource handler "public")
       :host       host
       :port       port
       :on-success #(prn "hiii")})))

(defn start [] (server))
