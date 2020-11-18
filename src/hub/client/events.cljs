(ns hub.client.events
  (:require [hub.client.specs]
            [re-frame.core :as rf :refer [reg-event-db]]))
              

(def patch 
  {:version 1
   :wires #{[#uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf6" #uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf7"]
            [#uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf7" #uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf8"]}
   :nodes {#uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf6" {:node-type :midi-in :x 7 :y 5}
           #uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf7" {:node-type :filter :x 22 :y 8}
           #uuid "f81d4fae-7dec-11d0-a765-00a0c91e6bf8" {:node-type :midi-out :x 36 :y 13}}})

(reg-event-db :init-db
  (fn [db] 
    (assoc db :patch patch)))

(reg-event-db :app/open-modal
  (fn [db [_ id context]]
    (assoc-in db [:app :modal] {:id id :context context})))

(reg-event-db :patcher/set-coords
  (fn [db [_ x y]]
    (assoc-in db [:patcher :last-click-coords] {:x x :y y})))

(reg-event-db :node/select
  (fn [db [_ id]]
    (assoc-in db [:patcher :selected-node] id)))

(reg-event-db :node/start-drag 
  (fn [db [_ id]]
    (assoc-in db [:patcher :dragging-node] id)))

(reg-event-db :node/start-wiring
  (fn [db [_ w]]
    (assoc-in db [:patcher :wiring] w)))

(reg-event-db :node/move-wiring
  (fn [db [_ coords]]
    (assoc-in db [:patcher :wiring :coords] coords)))

(reg-event-db :node/add
  (fn [db [_ node-type]]
    (let [{:keys [x y] :as last-click} (-> db :patcher :last-click-coords)
          wire-context (some-> @(rf/subscribe [:app/modal]) :context :wire)
          norm (fn [i] (js/Math.floor (/ i 20)))
          new-node-id (random-uuid)
          new-node (cond-> {:node-type node-type} last-click (assoc :x (norm x) :y (norm y)))]
      (cond->
        (assoc-in db [:patch :nodes new-node-id] new-node)
        wire-context (-> 
                       ;; in wire
                       (update-in [:patch :wires] conj [(first wire-context) new-node-id] [new-node-id (second wire-context)])
                       ;; rm old wire
                       (update-in [:patch :wires] (partial remove #(= wire-context %))))))))

(reg-event-db :node/add-wire
  (fn [db [_ id]] 
    (let [{:keys [node-id]} @(rf/subscribe [:patcher/wiring])]
      ;; TODO make sure a b are wired right
      (update-in db [:patch :wires] conj [node-id id]))))
                
(reg-event-db :node/drag-move
  (fn [db [_ id {x :x y :y}]]
    (let [grid-size 20
          snap-to-grid #(- (js/Math.floor (/ % grid-size)) 2)] 
      (update-in db [:patch :nodes id] assoc :x (snap-to-grid x) :y (snap-to-grid y)))))

(defn reroute-broken-wires [db selected-node-id]
  (let [wires (-> db :patch :wires)
        [in-a in-b] (some (fn [[a b]] (if (= b selected-node-id) [a b])) wires)
        new-wires (->> wires 
                       (map (fn [[a b]] (if (= a selected-node-id) [in-a b] [a b])))
                       (remove (fn [[a b]] (or (nil? a) (nil? b))))  
                       (remove #(= [in-a in-b] %)))]
    (assoc-in db [:patch :wires] new-wires))) 

(defn delete-node [db]
  (let [selected-node-id @(rf/subscribe [:node/selected-id])]
    (-> 
      (reroute-broken-wires db selected-node-id)
      (update-in [:patch :nodes] dissoc selected-node-id))))

(defmulti keyboard-action (fn [db event] [(.-key event) (.-metaKey event)]))

(defmethod keyboard-action ["Backspace" false]
  [db event] 
  (delete-node db))

(defmethod keyboard-action ["k" true]
  [db event] 
  (.preventDefault event)
  (rf/dispatch [:app/open-modal :command]))

(reg-event-db :keyup
  (fn [db [_ event]]             
    (keyboard-action db event)))
