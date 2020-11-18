(ns hub.server.http
  (:require
    [bidi.bidi :as bidi]
    [macchiato.util.response :as r]
    [macchiato.server :as http]
    [macchiato.middleware.defaults :as defaults]
    [macchiato.middleware.resource :refer [wrap-resource]]))

(defn api-test [req res raise]
  (-> "ok"
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

(def routes
  ["/" {"" {:get index-page}
        "api/test" {:get api-test}}])

(defn router [req res raise]
  ((:handler (bidi/match-route* routes (:uri req) req) not-found)
   req res raise))

(defn server []
  (let [host "127.0.0.1"
        port 4000]
    (http/start
      {:handler (defaults/wrap-defaults router defaults/site-defaults)
       :host host
       :port port
       :on-success #(js/console.log "HTTP server started on port " port)})))
