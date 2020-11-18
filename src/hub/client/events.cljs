(ns hub.client.events
  (:require [integrant.core :as ig]
            [hub.client.specs]
            [medley.core :refer [map-kv]]
            [re-frame.core :as rf :refer [reg-event-db]]))

(def default-readers {'ig/ref ig/ref, 'ig/refset ig/refset})

(def patch
  {:version 2,
   :entities
   {[:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75] {:x 5 :y 5}
    [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac] {:x 22 :y 8 :channels [1]}
    [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd] {:x 36 :y 13}
    [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5] {:x 36 :y 21}
    [:type/wire :wire/u26b21cd2-5bd6-46e6-bdff-ec5497d307d2]
    {:from (ig/ref [:type/midi-in :node/ud90ba0ed-1d99-4a09-9755-8081e5160d75])
     :to (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])}
    [:type/wire :wire/uc4e89175-d512-46e1-8415-1e66a930e923]
    {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
     :to (ig/ref [:type/midi-out :node/u0f5343dd-edf8-4363-9c29-8b944c0788dd])}
    [:type/wire :wire/u23547fe9-f9c9-4b63-b148-18930c22685e]
    {:from (ig/ref [:type/filter :node/u5e7364fc-b8d1-4ed1-b3f5-e3f2803d74ac])
     :to (ig/ref [:type/cv-out :node/u5023d5a3-17b9-4524-b91a-9c32474a7ce5])}}})

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
          [wire-id wire-context] (some-> @(rf/subscribe [:app/modal]) :context :wire)
          norm (fn [i] (js/Math.floor (/ i 20)))
          new-node-id [(keyword "type" node-type) (keyword "node" (str "u" (random-uuid)))]
          new-node (cond-> {} last-click (assoc :x (norm x) :y (norm y)))]
      (cond->
        (assoc-in db [:patch :entities new-node-id] new-node)
        wire-context (->
                       ;; new in wire
                       (assoc-in [:patch :entities [:type/wire (keyword "wire" (str "u" (random-uuid)))]]
                                 {:from (:from wire-context) :to (ig/ref new-node-id)})
                       ;; new out wire
                       (assoc-in [:patch :entities [:type/wire (keyword "wire" (str "u" (random-uuid)))]]
                                 {:from (ig/ref new-node-id) :to (:to wire-context)})
                       ;; rm old wire
                       (update-in [:patch :entities] dissoc wire-id))))))

(reg-event-db :node/add-wire
  (fn [db [_ id]] 
    (let [{:keys [node-id]} @(rf/subscribe [:patcher/wiring])]
      ;; TODO make sure a b are wired right
      (assoc-in db [:patch :entities [(keyword "type" "wire") (keyword "wire" (str "u" (random-uuid)))]]
                {:from (ig/ref node-id) :to (ig/ref id)}))))

(reg-event-db :node/drag-move
  (fn [db [_ id {x :x y :y}]]
    (let [grid-size 20
          snap-to-grid #(- (js/Math.floor (/ % grid-size)) 2)] 
      (update-in db [:patch :entities id] assoc :x (snap-to-grid x) :y (snap-to-grid y)))))

(defn find-connected-in-wire [db selected-node-id]
  (let [entities (-> db :patch :entities)
        _ (prn [:s selected-node-id :e entities])]
    (some (fn [[id m]] (if (= selected-node-id (:to m))
                          [id m]))
          entities)))

(defn reroute-broken-wires [db selected-node-id]
  ;; 1. find the input wire (where to is selected-id)
  (let [selected-node-id* (ig/ref selected-node-id)
        [in-id {in-from :from}] (find-connected-in-wire db selected-node-id*)]
    (-> db
        ;; 2. remove input wire
        (update-in [:patch :entities] dissoc in-id)
        ;; 3. reroute output wire to input wires from
        (update-in [:patch :entities] (partial map-kv (fn [id {a :from b :to :as m}]
                                                        (if (= a selected-node-id*)
                                                          [id {:from in-from :to b}]
                                                          [id m])))))))

(defn delete-node [db]
  (let [selected-node-id @(rf/subscribe [:node/selected-id])]
    (->
      (reroute-broken-wires db selected-node-id)
      (update-in [:patch :entities] dissoc selected-node-id))))

(defmulti keyboard-action (fn [db event] [(.-key event) (.-metaKey event)]))

(defmethod keyboard-action ["Backspace" false]
  [db event]
  (.preventDefault event)
  (delete-node db))

(defmethod keyboard-action ["k" true]
  [db event] 
  (.preventDefault event)
  (rf/dispatch [:app/open-modal :command]))

(reg-event-db :keyup
  (fn [db [_ event]]             
    (keyboard-action db event)))
