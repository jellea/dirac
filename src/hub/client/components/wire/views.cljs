(ns hub.client.components.wire.views
  (:require [re-frame.core :as rf]
            [hub.client.components.node.views :as h.node]))

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

(defn wire-ui [[id {:keys [from to]} :as w]]
  (let [patch @(rf/subscribe [:patch])
        {ax :x ay :y :as a*} (-> (get-in patch [:entities (:key from)]) h.node/node-pos)
        {bx :x by :y :as b*} (-> (get-in patch [:entities (:key to)]) h.node/node-pos)
        {mx :x my :y} (middle-pos a* b*)
        path (make-path a* b*)]
    [:<>
     [:path.wire-bg {:d path}]
     [:path.wire {:d path}]
     [:path.wire-hitbox {:d path
                         :on-click #(do
                                      (rf/dispatch [:patcher/set-coords mx my])
                                      (rf/dispatch [:app/open-modal :command {:wire w}]))}]
     [:path.hidden {:d "M6.42857 8.57143V15L8.57143 15V8.57143L15 8.57143L15 6.42857L8.57143 6.42857V0H6.42857V6.42857L0 6.42857V8.57143L6.42857 8.57143Z"
                    :transform (str "translate(" (+ 24.5 mx) " " (+ 25 my) ")")}]]))
