(ns hub.client.db
  (:require [integrant.core :as ig]))


(def default-patch
  {:version 2,
   :entities
   {[:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75] {:x 5 :y 5}
    [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac] {:x 22 :y 8 :channels [1]}
    [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd] {:x 36 :y 13}
    [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5] {:x 36 :y 21}
    [:type/wire :wire/u26b21cd2-5bd6-46e6-bdff-ec5497d307d2]
    {:from (ig/ref [:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75])
     :to (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])}
    [:type/wire :wire/uc4e89175-d512-46e1-8415-1e66a930e923]
    {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
     :to (ig/ref [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd])}
    [:type/wire :wire/u23547fe9-f9c9-4b63-b148-18930c22685e]
    {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
     :to (ig/ref [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5])}}})

(def default-props
  {:type/filter {:channels [1]}
   :type/comment {:text "This is a comment"}})

(def props
  {:type/filter {:channels []
                 :notes []}
   :type/midi-out {:device "bla"}
   :type/midi-in {:device "bla"}})
