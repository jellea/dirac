(ns hub.server.core
  (:require [integrant.core :as ig] [cljs.tools.reader.edn :as edn]
            [cljs.core.async :refer [chan put! take! >! <! buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [macchiato.server :as http]
            [macchiato.middleware.resource :refer [wrap-resource]]))

(def perf_hooks (js/require "perf_hooks"))

(defn create-chan []
  (chan (sliding-buffer 16)))

;; WIRE
(defmethod ig/init-key :type/wire [[_ id] {:keys [from to] :as config}]
  (go-loop []
    (let [incoming-message (<! (:output from))]
      (>! (:input to) incoming-message))
    (recur)))


;; MIDI IN
(def midi (js/require "easymidi"))

(defmethod ig/prep-key :type/midi-in [_ config]
  (merge {:output (create-chan)} config))

(defmethod ig/init-key :type/midi-in [[_ id] {:keys [output] :as config}]
  (let [input  (-> (midi.getInputs) first (midi.Input.))]
    (.on ^js/midi.Input. input "noteon"
         (fn [msg]
           ;(js/console.log 1 (.now (.-performance perf_hooks)))
           (go (>! output msg)))))
  config)

(defmethod ig/halt-key! :type/midi-in [_ {:keys [output]}]
  (close! output))

;; FILTER
(defmethod ig/prep-key :type/filter [_ config]
  (merge {:input (create-chan) :output (create-chan)} config))

(defmethod ig/init-key :type/filter [[_ id] {:keys [input output channels] :as config}]
  (go-loop []
    (let [incoming (<! input)]
      (prn [:filter  incoming])
      (if (some #{(.-channel incoming)} channels)
        (>! output incoming)))
    (recur))
  config)

(defmethod ig/halt-key! :type/filter [_ {:keys [input output] :as config}]
  (close! input)
  (close! output))

;; MIDI OUT
(defmethod ig/prep-key :type/midi-out [_ config]
  (merge {:input (create-chan)} config))

(defmethod ig/init-key :type/midi-out [[_ id] {:keys [input] :as config}]
  (let [midi-output (midi.Output. "MIDI Monitor (Untitled)")]
    (go-loop []
      (let [incoming (<! input)]
        (try
          (.send ^js/midi.Input. midi-output "noteon" incoming)
          (catch :default e
            (js/console.error "Unable to send midi message"))))
        ;(js/console.log 2 (.now (.-performance perf_hooks))))
      (recur)))
  config)

(defmethod ig/halt-key! :type/midi-out [_ {:keys [input]}]
  (close! input))

;; CV OUT

(defmethod ig/prep-key :type/cv-out [_ config]
  (merge {:input (create-chan)} config))

(defmethod ig/init-key :type/cv-out [[_ id] {:keys [input] :as config}]
  (go-loop []
     (let [incoming (<! input)]
       (prn [:cv-out incoming]))
     (recur))
  config)

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

(defn handler
  [request callback]
  (callback {:status 200
             :body "Hello Macchiato"}))

(defn server []
  (let [host "127.0.0.1"
        port 3000]
    (http/start
      {:handler (wrap-resource handler "public")
       :host host
       :port port
       :on-success #(prn "hiii")})))

(defn start []
  (server)
  (-> config :entities ig/prep ig/init))


;(defn ^:dev/before-load stop []
;  (ig/suspend! system)
;  (.close @!http-server))
