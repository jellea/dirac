(ns hub.client.components.node.views
  (:require [re-frame.core :as rf]))

(def node-ports {;; ins
                 :type/midi-in [:out]
                 :type/cv-in [:out]
                 :type/hid-in [:out]
                 ;; manip
                 :type/filter [:in :out]
                 :type/remap [:in :out]
                 :type/script [:in :out]
                 :type/switch [:in :out]
                 :type/spread [:in :out]
                 ;; outs
                 :type/cv-out [:in]
                 :type/dmx-out [:in]
                 :type/midi-out [:in]})

(defn port-ui [id p]
  [:div {:class ["port" (name p)]
         :on-mouse-up #(do
                         (.stopPropagation %)
                         (when (some-> @(rf/subscribe [:patcher/wiring]) :node-id (not= id))
                           (rf/dispatch [:node/add-wire id])
                           (rf/dispatch [:node/start-wiring nil])))
         :on-mouse-down #(do (.stopPropagation %)
                             (rf/dispatch [:node/start-wiring {:node-id id :port p}]))}])

(defn node-pos [{:keys [x y] :as n}]
  {:x (+ 10 (* x 20))
   :y (+ 10 (* y 20))})

(defn node-ui [[node-type nid :as id] {:keys [x y config] :as n}]
  [:div.node {:style {:top (-> (node-pos n) :y)
                      :left (-> (node-pos n) :x)}
              :class [(when @(rf/subscribe [:node/dragging? id]) "dragging")
                      (when @(rf/subscribe [:node/selected? id]) "selected")]
              :on-double-click #(do (.stopPropagation %)
                                    (rf/dispatch [:app/open-modal :node-config]))
              :on-mouse-down #(do
                                (rf/dispatch [:node/select id])
                                (rf/dispatch [:node/start-drag id]))
              :on-mouse-up #(do
                              (.stopPropagation %)
                              (rf/dispatch [:node/start-drag nil]))}
   [:span node-type]
   (into
     [:div.ports]
     (map (fn [p] [port-ui id p]) (node-ports node-type)))])