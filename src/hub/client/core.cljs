(ns hub.client.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [cljs.pprint]
            [hub.client.events]
            [hub.client.subs]
            [hub.client.components.node.views :as h.node]
            [hub.client.components.wire.views :as h.wire]
            [hub.client.components.command-menu.views :as h.command-menu]))

(def grid-size 20)

(defn mouse-move-handler [event]
  (when-let [{:keys [node-id port]} @(rf/subscribe [:patcher/wiring])]
    (rf/dispatch [:node/move-wiring {:x (.-clientX event) :y (- (.-clientY event) 40)}]))

  (when-let [id @(rf/subscribe [:node/dragging-id])]
    (rf/dispatch [:node/drag-move id {:x (.-clientX event)
                                      :y (.-clientY event)}])))

(defn patcher-ui []
  (let [patch @(rf/subscribe [:patch])
        {:keys [node-id coords port] :as wiring} @(rf/subscribe [:patcher/wiring])
        wires (filter (fn [[[t]]] (= t :type/wire)) (:entities patch))
        nodes (filter (fn [[[t]]] (not= t :type/wire)) (:entities patch))]
    [:div.patcher {:class (cond-> []
                                  node-id (conj "wiring" port))
                   :on-mouse-up #(do
                                   (rf/dispatch [:node/start-wiring nil])
                                   (rf/dispatch [:app/open-modal nil])
                                   (rf/dispatch [:node/start-drag nil])
                                   (rf/dispatch [:node/select nil]))
                   :on-double-click #(do
                                       (.preventDefault %)
                                       (rf/dispatch [:patcher/set-coords (.-clientX %) (.-clientY %)])
                                       (rf/dispatch [:app/open-modal :command]))
                   :on-mouse-move mouse-move-handler}
     (into
       [:div.nodes]
       (map
         (fn [[id n]] [h.node/node-ui id n])
         nodes))
     [:svg.wires
      (into
        [:<>]
        (map
          (fn [w] [h.wire/wire-ui w])
          wires))
      (when coords
        [:path.wire.temp {:d (h.wire/make-path (-> (get-in patch [:entities node-id]) h.node/node-pos)
                                        coords)}])]]))

(defn debug-ui []
  (let [patch @(rf/subscribe [:patch])]
    [:pre.pprint
     (with-out-str (cljs.pprint/pprint patch))]))

(defn keyboard-shortcuts []
  (r/with-let [handler #(rf/dispatch-sync [:keyup %])
               _ (js/document.addEventListener "keydown" handler)]
    (finally (.removeEventListener js/document "keydown" handler))))

(defn app []
  [:div.app
   [keyboard-shortcuts]
   [patcher-ui]
   [debug-ui]
   [h.command-menu/cmdk-button]
   (when (= :command @(rf/subscribe [:app/modal-id])) [h.command-menu/command-ui])])

(defn ^:dev/after-load start []
  (rdom/render [app] (.getElementById js/document "app")))

(defn ^:export init []
  (rf/dispatch [:init-db])
  (start))
