(ns hub.server.http
  (:require [cognitect.transit :as t]
            [bidi.bidi :as bidi]
            [hub.server.patch :as h.patch :refer [!patch]]
            [integrant.core :as ig]
            [macchiato.util.response :as r]
            [macchiato.server :as http]
            [macchiato.middleware.defaults :as defaults]
            [macchiato.middleware.restful-format :as rf]
            [macchiato.middleware.resource :refer [wrap-resource]]))

(def w (t/writer :json {:handlers {ig/Ref (t/write-handler (constantly "ig/ref") (fn [o] (:key o)))}}))
(def r (t/reader :json {:handlers {"ig/ref" (t/read-handler (fn [[a b]] (ig/ref [a b])))}}))

(defn log [x]
  (prn [:logxxx x])
  x)

(defmethod rf/deserialize-request "application/transit+json"
  [{:keys [body]}]
  (log (t/read r body)))

(defn api-test [req res raise]
  (-> "Ack"
      (r/ok)
      (r/content-type "plain-text")
      (res)))

(defn not-found [req res raise]
  (-> "<h1>404</h1>"
      (r/not-found)
      (r/content-type "text/html")
      (res)))

(defn index-page [req res raise]
  (-> (r/file "public/index.html")
      (r/content-type "text/html")
      (res)))

(defn current-patch [req res raise]
  (->
    (t/write w @!patch)
    (r/ok)
    (r/content-type "application/transit+json")
    (res)))

(defn override-current-patch [req res raise]
  ;; Do something
  (prn [:override-cure! req])
  (when-let [new-patch (->> req :body)]
    (reset! !patch new-patch)
    (h.patch/restart!)
    (prn [:done!])
    (-> (t/write w @!patch)
        (r/ok)
        (r/content-type "application/transit+json")
        (res))))

(def routes
  ["/" {"api/test" {:get api-test}
        "api/patch/current" {:get current-patch
                             :post (rf/wrap-restful-format override-current-patch)}
        "" {:get index-page}}])

(defn router [req res raise]
  ((:handler (bidi/match-route* routes (:uri req) req) not-found)
   req res raise))

(defn server []
  (let [host "127.0.0.1"
        port 4000]
    (http/start
      ;; TODO enable anti-forgery token
      {:handler (-> router
                    (defaults/wrap-defaults (assoc-in defaults/site-defaults [:security :anti-forgery] false)))
       :host host
       :port port
       :on-success #(js/console.log "HTTP server started on port " port)})))
