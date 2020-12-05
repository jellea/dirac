(ns hub.server.patch
  (:require [integrant.core :as ig]))

(def default {:version 2,
              :entities
              {[:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75] {}
               [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac] {:x 22 :y 8 :channels [0]}
               [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd] {:x 36 :y 13}
               [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5] {:x 36 :y 21}
               [:type/wire :wire/one] {:from (ig/ref [:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75])
                                       :to (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])}
               [:type/wire :wire/two] {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
                                       :to (ig/ref [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd])}}})

(defonce !patch (atom default))
(defonce !system (atom nil))

(defn init! []
  (prn [:init-system @!patch])
  (try
    (-> @!patch :entities ig/prep ig/init ((partial reset! !system)))
    (catch :default e
      (js/console.error "Error initialising system: " e)))
  (prn [:initialised-system @!system]))

(defn stop! []
  (try
    (prn [:stopping-system @!system])
    (some-> @!system ig/halt!)
    (prn [:stopped-system])
    (reset! !system nil)
    (catch :default e
      (js/console.error "Error stopping system: " e))))

(defn restart! []
  (stop!)
  (init!))

(comment
  (cljs.pprint/pprint @!system)


  (stop!)
  (init!))

