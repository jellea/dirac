(ns hub.client.specs
  (:require [clojure.spec.alpha :as s]))

;; How to test the integretity of the wires, so they make sense?
;; 1) Lookup node-type in nodes 
;; 2) Check if node-type has in or out
;; 3) Check position

(defn lookup-node-type [id nodes])

(s/def ::wire (s/coll-of uuid? :kind vector? :count 2 :distinct true))
(s/def ::wires (s/coll-of ::wire :kind vector? :distinct true))

(def node->ports {:midi-in [:out]
                  :cv-in [:out]
                  :hid-in [:out]
                  :filter [:in :out]
                  :remap [:in :out]
                  :script [:in :out]
                  :switch [:in :out]
                  :spread [:in :out]
                  :cv-out [:in]
                  :dmx-out [:in]
                  :midi-out [:in]})

;;(s/def ::node-type (-> node-ports keys set))
;;(s/def ::node (s/keys :req-un [::node-type]))
;;(s/def ::nodes (s/coll-of ::node))
