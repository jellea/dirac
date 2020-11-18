(ns hub.server.core
  (:require [hub.server.midi]
            [hub.server.connectors]
            [hub.server.cv]
            [hub.server.http]
            [integrant.core :as ig]))

(def config {:version 2,
             :entities
             {[:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75] {}
              [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac] {:x 22 :y 8 :channels [1]}
              [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd] {:x 36 :y 13}
              [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5] {:x 36 :y 21}
              [:type/wire :wire/one] {:from (ig/ref [:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75])
                                      :to (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])}
              [:type/wire :wire/two] {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
                                      :to (ig/ref [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd])}
              [:type/wire :wire/three] {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
                                        :to (ig/ref [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5])}}})

(defn start []
  (hub.server.http/server)
  (-> config :entities ig/prep ig/init))


;(defn ^:dev/before-load stop []
;  (ig/suspend! system)
;  (.close @!http-server))
