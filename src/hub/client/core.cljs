(ns hub.client.core
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [cljs.pprint]
            [hub.client.events]
            [hub.client.subs]))

(def node-ports {;; ins
                 :midi-in [:out]
                 :cv-in [:out]
                 :hid-in [:out]
                 ;; manip
                 :filter [:in :out]
                 :remap [:in :out]
                 :script [:in :out]
                 :switch [:in :out]
                 :spread [:in :out]
                 ;; outs
                 :cv-out [:in]
                 :dmx-out [:in]
                 :midi-out [:in]})

(def grid-size 20)

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

(defn node-ui [id {:keys [node-type x y config] :as n}]
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

(defn snap-to-grid [i]
  (- (* (js/Math.floor (/ i 20)) 20) 10))

(defn middle-pos [a b]
  {:x (snap-to-grid (/ (+ (:x a) (:x b)) 2))
   :y (/ (+ (:y a) (:y b)) 2)})

(defn make-path [{ax :x ay :y :as a} {bx :x by :y :as b}]
  (let [{mx :x my :y} (middle-pos a b)]
    (str "M" (+ ax 70) "," (+ ay 33)
         " L" (+ 32 mx) "," (+ ay 33)
         " L" (+ 32 mx) "," (+ by 33)
         " L" (- bx 7) "," (+ by 33))))

(defn wire-ui [[a b]]
  (let [patch @(rf/subscribe [:patch])
        {ax :x ay :y :as a*} (-> (get-in patch [:nodes a]) node-pos)
        {bx :x by :y :as b*} (-> (get-in patch [:nodes b]) node-pos)
        {mx :x my :y} (middle-pos a* b*)
        path (make-path a* b*)]
    [:<>
     [:path.wire-bg {:d path}]
     [:path.wire {:d path}]
     [:path.wire-hitbox {:d path
                         :on-click #(do
                                      (rf/dispatch [:patcher/set-coords mx my])
                                      (rf/dispatch [:app/open-modal :command {:wire [a b]}]))}]
     [:path.hidden {:d "M6.42857 8.57143V15L8.57143 15V8.57143L15 8.57143L15 6.42857L8.57143 6.42857V0H6.42857V6.42857L0 6.42857V8.57143L6.42857 8.57143Z"
                    :transform (str "translate(" (+ 24.5 mx) " " (+ 25 my) ")")}]]))

(defn mouse-move-handler [event]
  (when-let [{:keys [node-id port]} @(rf/subscribe [:patcher/wiring])]
    (rf/dispatch [:node/move-wiring {:x (.-clientX event) :y (- (.-clientY event) 40)}]))

  (when-let [id @(rf/subscribe [:node/dragging-id])]
    (rf/dispatch [:node/drag-move id {:x (.-clientX event)
                                      :y (.-clientY event)}])))

(defn patcher-ui []
  (let [patch @(rf/subscribe [:patch])
        {:keys [node-id coords port] :as wiring} @(rf/subscribe [:patcher/wiring])
        wires (:wires patch)]
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
         (fn [[id node]] [node-ui id node])
         (:nodes patch)))
     [:svg.wires
      (into
        [:<>]
        (map
          (fn [w] [wire-ui w])
          wires))
      (when coords
        [:path.wire.temp {:d (make-path (-> (get-in patch [:nodes node-id]) node-pos)
                                        coords)}])]]))

(defn debug-ui []
  (let [patch @(rf/subscribe [:patch])]
    [:pre.pprint
     (with-out-str (cljs.pprint/pprint patch))]))

(defn keyboard-shortcuts []
  (r/with-let [handler #(rf/dispatch-sync [:keyup %])
               _ (js/document.addEventListener "keydown" handler)]
              (finally (.removeEventListener js/document "keydown" handler))))

(defn command-item-ui [n]
  [:li {:on-click #(do (rf/dispatch [:node/add n])
                       (rf/dispatch [:app/open-modal nil]))}
   n])

(defn command-ui []
  (let [modal @(rf/subscribe [:app/modal])
        suggestions (cond
                      (some-> modal :context :wire) (keep (fn [[k v]] (when (= (count v) 2) k)) node-ports)
                      :else (keys node-ports))]
    [:div.command-menu
     (into
       [:div.command-menu-wrapper]
       (map (fn [n] [command-item-ui n]) suggestions))]))

(defn cmdk-button []
  [:img.cmdk-but {:src "cmdk.svg"
                  :on-click #(rf/dispatch [:app/open-modal :command])}])

(defn app []
  [:div.app
   [keyboard-shortcuts]
   [patcher-ui]
   [debug-ui]
   [cmdk-button]
   (when (= :command @(rf/subscribe [:app/modal-id])) [command-ui])])

(defn ^:dev/after-load start
  []
  (rdom/render [app]
               (.getElementById js/document "app")))

(defn ^:export init
  []
  (rf/dispatch [:init-db])
  (start))
