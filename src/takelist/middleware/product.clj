(ns takelist.middleware.product
  (:require [clojure.java.jdbc :as j]
            [takelist.db :as db]))

(defn wrap-product [handler db]
  (fn [request]
    (let [params (:params request)
          id (:product-id params)
          product (db/find-product db id)]
      (handler (assoc request :product product)))))
