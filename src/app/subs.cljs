(ns app.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :patch
  (fn [db]
    (:patch db)))

(reg-sub :node/dragging?
  (fn [db [_ id]]
    (-> db :patcher :dragging-node (= id))))

(reg-sub :node/dragging-id
  (fn [db _]
    (-> db :patcher :dragging-node)))

(reg-sub :node/selected?
  (fn [db [_ id]]
    (-> db :patcher :selected-node (= id))))

(reg-sub :node/selected-id
  (fn [db _]
    (-> db :patcher :selected-node)))

(reg-sub :app/modal
  (fn [db _]
    (-> db :app :modal)))

(reg-sub :app/modal-id
  (fn [db _]
    (-> db :app :modal :id)))

(reg-sub :patcher/wiring
  (fn [db _]
    (some-> db :patcher :wiring)))
