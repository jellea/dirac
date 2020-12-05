(ns hub.client.components.command-menu.views
  (:require [re-frame.core :as rf]
            [hub.client.components.node.views :as h.node]))

(defn command-item-ui [i n]
  [:li {:on-click #(do (rf/dispatch [:node/add n])
                       (rf/dispatch [:app/open-modal nil]))}
   n])

(defn command-ui []
  (let [modal @(rf/subscribe [:app/modal])
        suggestions (cond
                      (some-> modal :context :wire) (keep (fn [[k v]] (when (= (count v) 2) k)) h.node/node-ports)
                      :else (keys h.node/node-ports))]
    [:div.command-menu
     (into
       [:div.command-menu-wrapper]
       (map-indexed (fn [i n] [command-item-ui i n]) suggestions))]))

(defn cmdk-button []
  [:img.cmdk-but {:src "cmdk.svg"
                  :on-click #(rf/dispatch [:app/open-modal :command])}])
