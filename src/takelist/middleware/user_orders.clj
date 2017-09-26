(ns takelist.middleware.user-orders
  (:require [takelist.db :as db]))

(defn wrap-user-orders [handler db]
  (fn [{:keys [user] :as req}]
    (let [req (cond-> req
                user (assoc :user-orders (db/list-user-orders db user)))]
      (handler req))))
