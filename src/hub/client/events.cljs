(ns hub.client.events
  (:require [integrant.core :as ig]
            [hub.client.specs]
            [hub.client.db :as h.db]
            [medley.core :refer [map-kv]]
            [cognitect.transit :as t]
            [ajax.core :as ajax]
            [re-frame.core :as rf :refer [reg-event-db reg-event-fx]]))

(def default-readers {'ig/ref ig/ref, 'ig/refset ig/refset})

(def w (t/writer :json-verbose {:handlers {ig/Ref (t/write-handler (constantly "ig/ref") (fn [o] (:key o)))}}))
(def r (t/reader :json-verbose {:handlers {"ig/ref" (t/read-handler (fn [k] (ig/ref k)))}}))

(reg-event-db :init-db
  (fn [db] 
    (assoc db :patch h.db/default-patch)))

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


(reg-event-fx :send-patch-to-server
  (fn [{db :db}]
    {:http-xhrio {:method :post
                  :uri "/api/patch/current"
                  :content-type "application/transit+json"
                  :format (ajax/transit-request-format {:writer w
                                                        :type :json})
                  :response-format (ajax/transit-response-format {:read r
                                                                  :type :json})
                  :params (:patch db)}}))

;; Shortcuts

(defmulti keyboard-action (fn [db event] [(.-key event) (.-metaKey event)]))

(defmethod keyboard-action ["Backspace" false]
  [db event]
  (.preventDefault event)
  (delete-node db))

(defmethod keyboard-action ["Enter" true]
  [db event]
  (.preventDefault event)
  (rf/dispatch [:send-patch-to-server]))

(defmethod keyboard-action ["k" true]
  [db event] 
  (.preventDefault event)
  (rf/dispatch [:app/open-modal :command]))

(defmethod keyboard-action ["Escape" false]
  [db event]
  (.preventDefault event)
  (rf/dispatch [:node/select nil])
  (rf/dispatch [:app/open-modal nil]))

(defmethod keyboard-action ["ArrowDown" false]
  [db event]
  (.preventDefault event))

(defmethod keyboard-action ["ArrowUp" false]
  [db event]
  (.preventDefault event))

(defmethod keyboard-action :default
  [db event]
  db)

(reg-event-db :keyup
  (fn [db [_ event]]             
    (keyboard-action db event)))
