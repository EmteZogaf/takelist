(ns takelist.db
  (:require [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [clojure.java.jdbc :as j]
            [clojure.spec :as s]
            [clojure.spec.gen :as g]
            [clojure.string :as str]
            [takelist.pull :as pull]
            [takelist.spec :as spec]
            [takelist.util :as u])
  (:import [java.util UUID]))

(s/fdef find-user-query
  :args (s/cat :props (s/and (s/coll-of keyword?) #(pos? (count %)))
               :constraints (s/map-of keyword? any?))
  :ret string?
  :fn #(and (= (count (filter #{\=} (:ret %)))
               (count (-> % :args :constraints)))))

(defn find-user-query [props constraints]
  (format
    "select %s from tkl_user %s %s"
    (str/join ", " (map name props))
    (if (seq constraints) "where" "")
    (str/join " and " (for [[key] constraints] (str (name key) " = ?")))))

(s/fdef find-user
  :args (s/cat :db ::spec/db
               :props (s/coll-of keyword?)
               :constraints (s/keys :opt-un [:user/name :user/issuer :user/subject]))
  :ret map?)

(defn find-user
  "Searches for a unique user using the specified constraints and returns its
  properties as requested.

  Throws an exception if more than one user was found."
  [db props constraints]
  (let [sql-params (cons (find-user-query props constraints) (map second constraints))]
    (u/only (j/query db sql-params))))

(s/fdef create-user!
  :args (s/cat :db ::spec/db :user (s/keys :req-un [:user/name :user/issuer :user/subject]))
  :ret uuid?)

(defn create-user! [db {:keys [name issuer subject]}]
  (assert name)
  (assert issuer)
  (assert subject)
  (let [id (UUID/randomUUID)]
    (j/insert! db "tkl_user" [:id :name :issuer :subject] [id name issuer subject])
    id))

(defn update-user! [db id props]
  (j/update! db "tkl_user" props ["id = ?" id])
  nil)

(s/fdef list-products
  :args (s/cat :db ::spec/db)
  :ret (s/coll-of (s/keys :req-un [:product/id :product/name])))

(defn list-products [db]
  (j/query db ["SELECT id, name FROM tkl_product ORDER BY name"]))

(s/fdef find-order
  :args (s/cat :db ::spec/db :query ::pull/query :id uuid?)
  :ret (s/nilable :takelist/order))

(defn find-order [db query id]
  (when-let [order (first (pull/pull db query [:order/id id]))]
    (if (some #{:order/order-date} query)
      (update order :order/order-date time-coerce/from-date)
      order)))

(s/fdef create-order!
  :args (s/cat :db ::spec/db
               :user (s/keys :req-un [:user/id])
               :product (s/keys :req-un [:product/id])
               :amount pos-int?)
  :ret :takelist/order)

(defn create-order! [db {user-id :id} {product-id :id} amount]
  (let [order-id (UUID/randomUUID)
        order-date (time/now)]
    (j/insert! db "tkl_order"
               [:id :product_id :user_id :order_date :amount]
               [order-id product-id user-id (time-coerce/to-date order-date) amount])
    #:order{:id order-id
            :product-id product-id
            :user-id user-id
            :order-date order-date
            :amount amount}))
